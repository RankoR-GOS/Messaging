package com.android.messaging.ui.conversation.v2.messages.ui.preview

import android.net.Uri
import androidx.core.net.toUri
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageUiModel
import com.android.messaging.util.ContentType
import java.time.LocalDateTime
import java.time.ZoneId

private const val PREVIEW_CONVERSATION_ID = "preview-conversation"
private const val PREVIEW_MEDIA_URI_PREFIX = "content://com.android.messaging.preview"
private const val PREVIEW_TEXT_CONTENT_TYPE = "text/plain"

internal fun previewConversationMessage(
    messageId: String,
    text: String?,
    isIncoming: Boolean,
    senderDisplayName: String?,
    timestamp: Long,
    canClusterWithPrevious: Boolean,
    canClusterWithNext: Boolean,
    parts: List<ConversationMessagePartUiModel> = previewConversationParts(text = text),
    mmsSubject: String? = null,
    protocol: ConversationMessageUiModel.Protocol = previewConversationProtocol(
        parts = parts,
        mmsSubject = mmsSubject,
    ),
): ConversationMessageUiModel {
    val status = defaultPreviewConversationMessageStatus(isIncoming = isIncoming)

    return previewConversationMessage(
        messageId = messageId,
        text = text,
        isIncoming = isIncoming,
        senderDisplayName = senderDisplayName,
        timestamp = timestamp,
        canClusterWithPrevious = canClusterWithPrevious,
        canClusterWithNext = canClusterWithNext,
        status = status,
        parts = parts,
        mmsSubject = mmsSubject,
        protocol = protocol,
    )
}

internal fun previewConversationMessage(
    messageId: String,
    text: String?,
    isIncoming: Boolean,
    senderDisplayName: String?,
    timestamp: Long,
    canClusterWithPrevious: Boolean,
    canClusterWithNext: Boolean,
    status: ConversationMessageUiModel.Status,
    parts: List<ConversationMessagePartUiModel> = previewConversationParts(text = text),
    mmsSubject: String? = null,
    protocol: ConversationMessageUiModel.Protocol = previewConversationProtocol(
        parts = parts,
        mmsSubject = mmsSubject,
    ),
): ConversationMessageUiModel {
    return ConversationMessageUiModel(
        messageId = messageId,
        conversationId = PREVIEW_CONVERSATION_ID,
        text = text,
        parts = parts,
        sentTimestamp = timestamp,
        receivedTimestamp = timestamp,
        displayTimestamp = timestamp,
        status = status,
        isIncoming = isIncoming,
        senderDisplayName = senderDisplayName,
        senderAvatarUri = null,
        senderContactLookupKey = null,
        canClusterWithPrevious = canClusterWithPrevious,
        canClusterWithNext = canClusterWithNext,
        mmsSubject = mmsSubject,
        protocol = protocol,
    )
}

private fun defaultPreviewConversationMessageStatus(
    isIncoming: Boolean,
): ConversationMessageUiModel.Status {
    return when {
        isIncoming -> ConversationMessageUiModel.Status.Incoming.Complete
        else -> ConversationMessageUiModel.Status.Outgoing.Complete
    }
}

internal fun previewConversationAudioPart(
    uniqueId: String,
    caption: String? = null,
): ConversationMessagePartUiModel {
    return previewConversationMediaPart(
        uniqueId = uniqueId,
        contentType = ContentType.AUDIO_MP3,
        caption = caption,
        width = 0,
        height = 0,
    )
}

internal fun previewConversationImagePart(
    uniqueId: String,
    caption: String? = null,
    width: Int = 1600,
    height: Int = 1200,
): ConversationMessagePartUiModel {
    return previewConversationMediaPart(
        uniqueId = uniqueId,
        contentType = ContentType.IMAGE_JPEG,
        caption = caption,
        width = width,
        height = height,
    )
}

internal fun previewConversationMediaPart(
    uniqueId: String,
    contentType: String,
    caption: String? = null,
    width: Int = 0,
    height: Int = 0,
): ConversationMessagePartUiModel {
    return ConversationMessagePartUiModel(
        contentType = contentType,
        text = caption,
        contentUri = previewConversationMediaUri(uniqueId = uniqueId),
        width = width,
        height = height,
    )
}

internal fun previewConversationTextPart(
    text: String,
): ConversationMessagePartUiModel {
    return ConversationMessagePartUiModel(
        contentType = PREVIEW_TEXT_CONTENT_TYPE,
        text = text,
        contentUri = null,
        width = 0,
        height = 0,
    )
}

internal fun previewConversationUnsupportedPart(
    uniqueId: String,
    contentType: String = "application/octet-stream",
): ConversationMessagePartUiModel {
    return previewConversationMediaPart(
        uniqueId = uniqueId,
        contentType = contentType,
    )
}

internal fun previewConversationVCardPart(
    uniqueId: String,
    caption: String? = null,
): ConversationMessagePartUiModel {
    return previewConversationMediaPart(
        uniqueId = uniqueId,
        contentType = ContentType.TEXT_VCARD,
        caption = caption,
        width = 0,
        height = 0,
    )
}

internal fun previewConversationVideoPart(
    uniqueId: String,
    caption: String? = null,
    width: Int = 1280,
    height: Int = 720,
): ConversationMessagePartUiModel {
    return previewConversationMediaPart(
        uniqueId = uniqueId,
        contentType = ContentType.VIDEO_MP4,
        caption = caption,
        width = width,
        height = height,
    )
}

private fun previewConversationMediaUri(uniqueId: String): Uri {
    return "$PREVIEW_MEDIA_URI_PREFIX/$uniqueId".toUri()
}

private fun previewConversationParts(
    text: String?,
): List<ConversationMessagePartUiModel> {
    return when {
        text.isNullOrBlank() -> emptyList()
        else -> listOf(previewConversationTextPart(text = text))
    }
}

private fun previewConversationProtocol(
    parts: List<ConversationMessagePartUiModel>,
    mmsSubject: String?,
): ConversationMessageUiModel.Protocol {
    return when {
        !mmsSubject.isNullOrBlank() -> ConversationMessageUiModel.Protocol.MMS
        parts.any { part -> part.contentUri != null } -> ConversationMessageUiModel.Protocol.MMS
        else -> ConversationMessageUiModel.Protocol.SMS
    }
}

internal fun previewConversationTimestamp(
    dayOffset: Long,
    hour: Int,
    minute: Int,
): Long {
    return LocalDateTime.now()
        .minusDays(dayOffset)
        .withHour(hour)
        .withMinute(minute)
        .withSecond(0)
        .withNano(0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}
