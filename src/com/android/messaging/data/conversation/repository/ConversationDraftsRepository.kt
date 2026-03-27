package com.android.messaging.data.conversation.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import androidx.core.net.toUri
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ConversationListItemData
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.MessagePartData
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.LogUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal interface ConversationDraftsRepository {
    fun observeConversationDraft(conversationId: String): Flow<ConversationDraft>

    suspend fun saveDraft(
        conversationId: String,
        draft: ConversationDraft,
    )
}

internal class ConversationDraftsRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : ConversationDraftsRepository {

    override fun observeConversationDraft(conversationId: String): Flow<ConversationDraft> {
        val draftChangeUri = MessagingContentProvider.buildConversationMetadataUri(conversationId)

        return observeDraftChanges(uri = draftChangeUri)
            .conflate()
            .map { loadConversationDraft(conversationId = conversationId) }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveDraft(
        conversationId: String,
        draft: ConversationDraft,
    ) {
        withContext(context = ioDispatcher) {
            val message = createDraftMessage(
                conversationId = conversationId,
                draft = draft,
            )
            val boundMessage = bindDraftParticipantsIfNeeded(
                conversationId = conversationId,
                message = message,
            ) ?: return@withContext

            BugleDatabaseOperations.updateDraftMessageData(
                DataModel.get().database,
                conversationId,
                boundMessage,
                BugleDatabaseOperations.UPDATE_MODE_ADD_DRAFT,
            )

            MessagingContentProvider.notifyConversationMetadataChanged(conversationId)
        }
    }

    private fun observeDraftChanges(uri: Uri): Flow<Unit> {
        return callbackFlow {
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }

            contentResolver.registerContentObserver(uri, true, observer)
            trySend(Unit)

            awaitClose {
                contentResolver.unregisterContentObserver(observer)
            }
        }
    }

    private fun loadConversationDraft(conversationId: String): ConversationDraft {
        val database = DataModel.get().database
        val conversation = ConversationListItemData
            .getExistingConversation(database, conversationId)
            ?: return ConversationDraft()

        val draftMessage = BugleDatabaseOperations.readDraftMessageData(
            database,
            conversationId,
            conversation.selfId,
        )

        return createConversationDraft(
            conversation = conversation,
            draftMessage = draftMessage,
        )
    }

    private fun createConversationDraft(
        conversation: ConversationListItemData,
        draftMessage: MessageData?,
    ): ConversationDraft {
        val attachments = draftMessage
            ?.parts
            ?.asSequence()
            ?.filter { part -> part.isAttachment }
            ?.mapNotNull(::createDraftAttachmentOrNull)
            ?.toList()
            ?: emptyList()

        val selfParticipantId = draftMessage
            ?.selfId
            ?.takeIf { selfParticipantId -> selfParticipantId.isNotBlank() }
            ?: conversation.selfId.orEmpty()

        return ConversationDraft(
            messageText = draftMessage?.messageText.orEmpty(),
            subjectText = draftMessage?.mmsSubject.orEmpty(),
            selfParticipantId = selfParticipantId,
            attachments = attachments,
        )
    }

    private fun createDraftMessage(
        conversationId: String,
        draft: ConversationDraft,
    ): MessageData {
        val selfParticipantId = draft.selfParticipantId.takeIf { selfParticipantId ->
            selfParticipantId.isNotBlank()
        }
        val messageParts = draft.attachments.mapNotNull(::createMessagePartDataOrNull)

        val isMms = draft.subjectText.isNotBlank() || messageParts.isNotEmpty()

        val message = when {
            isMms -> MessageData.createDraftMmsMessage(
                conversationId,
                selfParticipantId,
                draft.messageText,
                draft.subjectText,
            )

            else -> MessageData.createDraftSmsMessage(
                conversationId,
                selfParticipantId,
                draft.messageText,
            )
        }

        messageParts.forEach(message::addPart)

        return message
    }

    private fun bindDraftParticipantsIfNeeded(
        conversationId: String,
        message: MessageData,
    ): MessageData? {
        if (message.selfId != null && message.participantId != null) {
            return message
        }

        val conversation = ConversationListItemData.getExistingConversation(
            DataModel.get().database,
            conversationId,
        ) ?: run {
            LogUtil.w(
                TAG,
                "Conversation $conversationId was deleted before saving draft ${message.messageId}",
            )
            return null
        }

        val selfParticipantId = conversation.selfId
        if (message.selfId == null) {
            message.bindSelfId(selfParticipantId)
        }
        if (message.participantId == null) {
            message.bindParticipantId(selfParticipantId)
        }

        return message
    }

    private fun createDraftAttachmentOrNull(part: MessagePartData): ConversationDraftAttachment? {
        val contentType = part
            .contentType
            ?.takeIf { value -> value.isNotBlank() }
            ?: run {
                LogUtil.w(TAG, "Dropping draft attachment with blank contentType")
                return null
            }

        val contentUri = part
            .contentUri
            ?.toString()
            ?.takeIf { value -> value.isNotBlank() }
            ?: run {
                LogUtil.w(TAG, "Dropping draft attachment with blank contentUri")
                return null
            }

        return ConversationDraftAttachment(
            contentType = contentType,
            contentUri = contentUri,
            captionText = part.text.orEmpty(),
            width = normalizePartDimension(size = part.width),
            height = normalizePartDimension(size = part.height),
        )
    }

    private fun createMessagePartDataOrNull(
        attachment: ConversationDraftAttachment,
    ): MessagePartData? {
        if (attachment.contentType.isBlank()) {
            LogUtil.w(TAG, "Dropping draft attachment with blank contentType during save")
            return null
        }

        if (attachment.contentUri.isBlank()) {
            LogUtil.w(TAG, "Dropping draft attachment with blank contentUri during save")
            return null
        }

        val captionText = attachment.captionText.takeIf { value -> value.isNotBlank() }
        val contentUri = attachment.contentUri.toUri()
        val width = toLegacyPartDimension(size = attachment.width)
        val height = toLegacyPartDimension(size = attachment.height)

        captionText?.let { nonBlankCaptionText ->
            return MessagePartData.createMediaMessagePart(
                nonBlankCaptionText,
                attachment.contentType,
                contentUri,
                width,
                height,
            )
        }

        return MessagePartData.createMediaMessagePart(
            attachment.contentType,
            contentUri,
            width,
            height,
        )
    }

    private fun normalizePartDimension(size: Int): Int? {
        return size.takeIf { it != MessagePartData.UNSPECIFIED_SIZE }
    }

    private fun toLegacyPartDimension(size: Int?): Int {
        return size ?: MessagePartData.UNSPECIFIED_SIZE
    }

    private companion object {
        private const val TAG = "ConversationDraftsRepository"
    }
}
