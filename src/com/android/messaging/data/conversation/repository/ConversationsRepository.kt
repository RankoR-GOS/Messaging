package com.android.messaging.data.conversation.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.conversation.model.metadata.ConversationMetadata
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ConversationListItemData
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.db.ReversedCursor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal interface ConversationsRepository {
    fun getConversationMetadata(conversationId: String): Flow<ConversationMetadata?>
    fun getConversationMessages(conversationId: String): Flow<List<ConversationMessageData>>
}

internal class ConversationsRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : ConversationsRepository {

    override fun getConversationMetadata(conversationId: String): Flow<ConversationMetadata?> {
        val uri = MessagingContentProvider.buildConversationMetadataUri(conversationId)

        return observeUri(uri = uri)
            .flowOn(defaultDispatcher)
            .map {
                queryConversationMetadata(uri = uri)
            }
            .flowOn(ioDispatcher)
    }

    override fun getConversationMessages(conversationId: String): Flow<List<ConversationMessageData>> {
        val uri = MessagingContentProvider.buildConversationMessagesUri(conversationId)

        return observeUri(uri = uri)
            .conflate()
            .flowOn(defaultDispatcher)
            .map {
                queryConversationMessages(uri = uri)
            }
            .flowOn(ioDispatcher)
    }

    private fun observeUri(uri: Uri): Flow<Unit> {
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

    private fun queryConversationMetadata(uri: Uri): ConversationMetadata? {
        return contentResolver
            .query(
                uri,
                ConversationListItemData.PROJECTION,
                null,
                null,
                null,
            )
            ?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use null
                }

                ConversationMetadata(
                    conversationName = cursor.getString(
                        cursor.getColumnIndexOrThrow(ConversationColumns.NAME),
                    ).orEmpty(),
                    selfParticipantId = cursor.getString(
                        cursor.getColumnIndexOrThrow(ConversationColumns.CURRENT_SELF_ID),
                    ).orEmpty(),
                    isGroupConversation = cursor.getInt(
                        cursor.getColumnIndexOrThrow(ConversationColumns.PARTICIPANT_COUNT),
                    ) > 1,
                    participantCount = cursor.getInt(
                        cursor.getColumnIndexOrThrow(ConversationColumns.PARTICIPANT_COUNT),
                    ),
                    composerAvailability = ConversationComposerAvailability.editable(),
                )
            }
    }

    private fun queryConversationMessages(uri: Uri): List<ConversationMessageData> {
        return contentResolver
            .query(
                uri,
                ConversationMessageData.getProjection(),
                null, null, null,
            )
            ?.use { rawCursor ->
                val reversedCursor = ReversedCursor(cursor = rawCursor)

                buildList {
                    while (reversedCursor.moveToNext()) {
                        add(ConversationMessageData().apply { bind(reversedCursor) })
                    }
                }
            }
            ?: emptyList()
    }
}
