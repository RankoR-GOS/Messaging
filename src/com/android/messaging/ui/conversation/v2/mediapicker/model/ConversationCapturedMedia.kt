package com.android.messaging.ui.conversation.v2.mediapicker.model

internal data class ConversationCapturedMedia(
    val contentUri: String,
    val contentType: String,
    val width: Int? = null,
    val height: Int? = null,
)
