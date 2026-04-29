package com.android.messaging.ui.conversation.v2.mediapicker.model

import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment

internal data class PhotoPickerDraftAttachment(
    val sourceContentUri: String,
    val draftAttachment: ConversationDraftAttachment,
)
