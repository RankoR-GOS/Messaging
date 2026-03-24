package com.android.messaging.ui.conversation.v2.component.util

import com.android.messaging.ui.conversation.v2.model.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.v2.model.ConversationMessageUiModel
import java.time.LocalDateTime
import java.time.ZoneId

private const val PREVIEW_CONVERSATION_ID = "preview-conversation"
private const val PREVIEW_TEXT_CONTENT_TYPE = "text/plain"

internal fun previewConversationMessage(
    messageId: String,
    text: String,
    isIncoming: Boolean,
    senderDisplayName: String?,
    timestamp: Long,
    canClusterWithPrevious: Boolean,
    canClusterWithNext: Boolean,
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
    )
}

internal fun previewConversationMessage(
    messageId: String,
    text: String,
    isIncoming: Boolean,
    senderDisplayName: String?,
    timestamp: Long,
    canClusterWithPrevious: Boolean,
    canClusterWithNext: Boolean,
    status: ConversationMessageUiModel.Status,
): ConversationMessageUiModel {
    return ConversationMessageUiModel(
        messageId = messageId,
        conversationId = PREVIEW_CONVERSATION_ID,
        text = text,
        parts = listOf(
            ConversationMessagePartUiModel(
                contentType = PREVIEW_TEXT_CONTENT_TYPE,
                text = text,
                contentUri = null,
                width = 0,
                height = 0,
            ),
        ),
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
        mmsSubject = null,
        protocol = ConversationMessageUiModel.Protocol.SMS,
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
