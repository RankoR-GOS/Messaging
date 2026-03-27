package com.android.messaging.data.conversation.model.draft

internal data class ConversationDraftAttachment(
    val contentType: String,
    val contentUri: String,
    val captionText: String = "",
    val width: Int? = null,
    val height: Int? = null,
)
