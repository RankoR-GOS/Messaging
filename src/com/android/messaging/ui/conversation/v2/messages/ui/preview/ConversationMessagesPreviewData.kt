package com.android.messaging.ui.conversation.v2.messages.ui.preview

import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

internal fun previewConversationMessages(): ImmutableList<ConversationMessageUiModel> {
    return (
        previewConversationStandaloneAndPairMessages() +
            previewConversationTripletMessages() +
            previewConversationOutgoingMessages()
        ).toPersistentList()
}

private fun previewConversationStandaloneAndPairMessages(): List<ConversationMessageUiModel> {
    return listOf(
        previewConversationMessage(
            messageId = "standalone-incoming",
            text = "Standalone incoming",
            isIncoming = true,
            senderDisplayName = "+15550001234",
            timestamp = previewConversationTimestamp(dayOffset = 2, hour = 9, minute = 12),
            canClusterWithPrevious = false,
            canClusterWithNext = false,
        ),
        previewConversationMessage(
            messageId = "pair-top",
            text = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            isIncoming = true,
            senderDisplayName = "+15550001234",
            timestamp = previewConversationTimestamp(dayOffset = 2, hour = 9, minute = 15),
            canClusterWithPrevious = false,
            canClusterWithNext = true,
        ),
        previewConversationMessage(
            messageId = "pair-bottom",
            text = null,
            isIncoming = true,
            senderDisplayName = "+15550001234",
            timestamp = previewConversationTimestamp(dayOffset = 2, hour = 9, minute = 16),
            canClusterWithPrevious = true,
            canClusterWithNext = false,
            parts = listOf(
                previewConversationImagePart(
                    uniqueId = "pair-bottom-image",
                    width = 1600,
                    height = 1200,
                ),
            ),
            protocol = ConversationMessageUiModel.Protocol.MMS,
        ),
    )
}

private fun previewConversationTripletMessages(): List<ConversationMessageUiModel> {
    return listOf(
        previewConversationMessage(
            messageId = "triplet-top",
            text = "Triplet top",
            isIncoming = true,
            senderDisplayName = "+15550004567",
            timestamp = previewConversationTimestamp(dayOffset = 1, hour = 18, minute = 10),
            canClusterWithPrevious = false,
            canClusterWithNext = true,
        ),
        previewConversationMessage(
            messageId = "triplet-middle",
            text = null,
            isIncoming = true,
            senderDisplayName = "+15550004567",
            timestamp = previewConversationTimestamp(dayOffset = 1, hour = 18, minute = 11),
            canClusterWithPrevious = true,
            canClusterWithNext = true,
            parts = previewConversationTripletMiddleParts(),
            protocol = ConversationMessageUiModel.Protocol.MMS,
        ),
        previewConversationMessage(
            messageId = "triplet-bottom",
            text = null,
            isIncoming = true,
            senderDisplayName = "+15550004567",
            timestamp = previewConversationTimestamp(dayOffset = 1, hour = 18, minute = 12),
            canClusterWithPrevious = true,
            canClusterWithNext = false,
            parts = listOf(
                previewConversationAudioPart(
                    uniqueId = "triplet-bottom-audio",
                ),
                previewConversationVCardPart(
                    uniqueId = "triplet-bottom-vcard",
                ),
                previewConversationLocationVCardPart(
                    uniqueId = "triplet-bottom-location-vcard",
                ),
            ),
            protocol = ConversationMessageUiModel.Protocol.MMS,
        ),
    )
}

private fun previewConversationTripletMiddleParts(): List<ConversationMessagePartUiModel> {
    return listOf(
        previewConversationImagePart(
            uniqueId = "triplet-middle-image-1",
            width = 1400,
            height = 1400,
        ),
        previewConversationImagePart(
            uniqueId = "triplet-middle-image-2",
            width = 1400,
            height = 1400,
        ),
        previewConversationImagePart(
            uniqueId = "triplet-middle-image-3",
            width = 1400,
            height = 1400,
        ),
    )
}

private fun previewConversationOutgoingMessages(): List<ConversationMessageUiModel> {
    return listOf(
        previewConversationMessage(
            messageId = "outgoing-standalone",
            text = "Outgoing standalone",
            isIncoming = false,
            senderDisplayName = null,
            timestamp = previewConversationTimestamp(dayOffset = 0, hour = 13, minute = 4),
            canClusterWithPrevious = false,
            canClusterWithNext = false,
        ),
        previewConversationMessage(
            messageId = "outgoing-top",
            text = null,
            isIncoming = false,
            senderDisplayName = null,
            timestamp = previewConversationTimestamp(dayOffset = 0, hour = 13, minute = 5),
            canClusterWithPrevious = false,
            canClusterWithNext = true,
            parts = listOf(
                previewConversationVideoPart(
                    uniqueId = "outgoing-top-video",
                    width = 1280,
                    height = 720,
                ),
            ),
            protocol = ConversationMessageUiModel.Protocol.MMS,
        ),
        previewConversationMessage(
            messageId = "outgoing-bottom",
            text = "Outgoing pair bottom",
            isIncoming = false,
            senderDisplayName = null,
            timestamp = previewConversationTimestamp(dayOffset = 0, hour = 13, minute = 6),
            canClusterWithPrevious = true,
            canClusterWithNext = false,
            status = ConversationMessageUiModel.Status.Outgoing.Delivered,
        ),
    )
}
