package com.android.messaging.ui.conversation.v2.mediapicker.repository

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract.Contacts
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.datamodel.MediaScratchFileProvider
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.ContentType
import com.android.messaging.util.LogUtil
import com.android.messaging.util.core.extension.typedFlow
import com.android.messaging.util.core.extension.unitFlow
import com.android.messaging.util.db.ext.getStringOrNull
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn

internal interface ConversationAttachmentRepository {
    fun createDraftAttachmentFromContact(
        contactUri: String,
    ): Flow<ConversationDraftAttachment?>

    fun deleteTemporaryAttachment(
        contentUri: String,
    ): Flow<Unit>
}

internal class ConversationAttachmentRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : ConversationAttachmentRepository {

    override fun createDraftAttachmentFromContact(
        contactUri: String,
    ): Flow<ConversationDraftAttachment?> {
        return typedFlow {
            queryDraftAttachmentFromContact(contactUri = contactUri)
        }.catch { throwable ->
            if (throwable is CancellationException) {
                throw throwable
            }

            LogUtil.w(
                TAG,
                "Failed to resolve contact draft attachment for $contactUri",
                throwable,
            )
            emit(null)
        }.flowOn(ioDispatcher)
    }

    override fun deleteTemporaryAttachment(contentUri: String): Flow<Unit> {
        return unitFlow {
            val attachmentUri = contentUri.toUri()
            if (MediaScratchFileProvider.isMediaScratchSpaceUri(attachmentUri)) {
                contentResolver.delete(attachmentUri, null, null)
            }
        }.catch { throwable ->
            if (throwable is CancellationException) {
                throw throwable
            }

            LogUtil.w(TAG, "Failed to delete temporary attachment $contentUri", throwable)
            emit(Unit)
        }.flowOn(ioDispatcher)
    }

    private fun queryDraftAttachmentFromContact(
        contactUri: String,
    ): ConversationDraftAttachment? {
        val lookupKey = contentResolver.query(
            contactUri.toUri(),
            arrayOf(Contacts.LOOKUP_KEY),
            null,
            null,
            null,
        )?.use { cursor ->
            val lookupKeyColumnIndex = cursor.getColumnIndexOrThrow(Contacts.LOOKUP_KEY)

            when {
                cursor.moveToFirst() -> cursor.getStringOrNull(lookupKeyColumnIndex)
                else -> null
            }
        }

        if (lookupKey.isNullOrBlank()) {
            LogUtil.w(TAG, "Unable to resolve contact lookup key for $contactUri")
            return null
        }

        val vCardUri = Uri.withAppendedPath(
            Contacts.CONTENT_VCARD_URI,
            lookupKey,
        )

        return ConversationDraftAttachment(
            contentType = ContentType.TEXT_VCARD,
            contentUri = vCardUri.toString(),
        )
    }

    private companion object {
        private const val TAG = "ConversationAttachmentRepository"
    }
}
