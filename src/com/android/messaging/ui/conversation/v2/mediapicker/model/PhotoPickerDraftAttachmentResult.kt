package com.android.messaging.ui.conversation.v2.mediapicker.model

internal sealed interface PhotoPickerDraftAttachmentResult {
    data class Resolved(
        val photoPickerDraftAttachment: PhotoPickerDraftAttachment,
    ) : PhotoPickerDraftAttachmentResult

    data class Failed(
        val sourceContentUri: String,
    ) : PhotoPickerDraftAttachmentResult
}
