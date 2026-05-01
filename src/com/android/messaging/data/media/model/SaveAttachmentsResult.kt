package com.android.messaging.data.media.model

internal data class SaveAttachmentsResult(
    val imageCount: Int,
    val videoCount: Int,
    val otherCount: Int,
    val failCount: Int,
)
