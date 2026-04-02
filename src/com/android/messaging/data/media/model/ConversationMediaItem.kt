package com.android.messaging.data.media.model

internal data class ConversationMediaItem(
    val mediaId: String,
    val contentUri: String,
    val contentType: String,
    val mediaType: ConversationMediaType,
    val width: Int?,
    val height: Int?,
    val durationMillis: Long?,
)

internal enum class ConversationMediaType {
    Image,
    Video,
}
