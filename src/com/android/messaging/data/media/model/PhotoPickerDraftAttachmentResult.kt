package com.android.messaging.data.media.model

import com.android.messaging.data.conversation.model.draft.PhotoPickerDraftAttachment

internal sealed interface PhotoPickerDraftAttachmentResult {
    data class Resolved(
        val photoPickerDraftAttachment: PhotoPickerDraftAttachment,
    ) : PhotoPickerDraftAttachmentResult

    data class Failed(
        val sourceContentUri: String,
    ) : PhotoPickerDraftAttachmentResult
}
