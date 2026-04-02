package com.android.messaging.ui.conversation.v2.mediapicker

import android.content.ContentResolver
import androidx.core.net.toUri
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.media.model.ConversationMediaItem
import com.android.messaging.datamodel.MediaScratchFileProvider
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationCapturedMedia
import com.android.messaging.util.LogUtil
import com.android.messaging.util.core.extension.unitFlow
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn

internal interface ConversationAttachmentBridge {
    fun createDraftAttachments(
        mediaItems: Collection<ConversationMediaItem>,
    ): List<ConversationDraftAttachment>

    fun createDraftAttachment(
        capturedMedia: ConversationCapturedMedia,
    ): ConversationDraftAttachment

    fun deleteTemporaryAttachment(
        contentUri: String,
    ): Flow<Unit>
}

internal class ConversationAttachmentBridgeImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : ConversationAttachmentBridge {

    override fun createDraftAttachments(
        mediaItems: Collection<ConversationMediaItem>,
    ): List<ConversationDraftAttachment> {
        return mediaItems.map { mediaItem ->
            ConversationDraftAttachment(
                contentType = mediaItem.contentType,
                contentUri = mediaItem.contentUri,
                width = mediaItem.width,
                height = mediaItem.height,
            )
        }
    }

    override fun createDraftAttachment(
        capturedMedia: ConversationCapturedMedia,
    ): ConversationDraftAttachment {
        return ConversationDraftAttachment(
            contentType = capturedMedia.contentType,
            contentUri = capturedMedia.contentUri,
            width = capturedMedia.width,
            height = capturedMedia.height,
        )
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

    private companion object {
        private const val TAG = "ConversationAttachmentBridge"
    }
}
