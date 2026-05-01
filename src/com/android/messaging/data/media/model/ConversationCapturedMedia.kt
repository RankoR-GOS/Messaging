package com.android.messaging.data.media.model

internal data class ConversationCapturedMedia(
    val contentUri: String,
    val contentType: String,
    val width: Int? = null,
    val height: Int? = null,
)
