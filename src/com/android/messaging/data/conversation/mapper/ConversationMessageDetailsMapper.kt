package com.android.messaging.data.conversation.mapper

import com.android.messaging.data.conversation.model.message.ConversationMessageDetails
import com.android.messaging.data.conversation.model.message.ConversationMessageDetailsData
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.data.message.mapper.MessageDataMapper
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.domain.message.model.MessageProtocol
import com.android.messaging.mmslib.pdu.PduHeaders
import com.android.messaging.sms.MmsUtils
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal interface ConversationMessageDetailsMapper {
    fun map(
        data: ConversationMessageDetailsData,
        activeSubscriptionCount: Int,
        debug: ConversationMessageDetails.Debug?,
    ): ConversationMessageDetails
}

internal class ConversationMessageDetailsMapperImpl @Inject constructor(
    private val messageDataMapper: MessageDataMapper,
) : ConversationMessageDetailsMapper {

    override fun map(
        data: ConversationMessageDetailsData,
        activeSubscriptionCount: Int,
        debug: ConversationMessageDetails.Debug?,
    ): ConversationMessageDetails {
        val message = data.message
        val type = when (messageDataMapper.protocolOf(message.protocol)) {
            MessageProtocol.SMS -> ConversationMessageDetails.Type.SMS

            MessageProtocol.MMS,
            MessageProtocol.MMS_PUSH_NOTIFICATION,
            MessageProtocol.UNKNOWN,
            -> ConversationMessageDetails.Type.MMS
        }

        return ConversationMessageDetails(
            type = type,
            sender = message.senderDisplayDestination?.takeIf(String::isNotBlank),
            recipients = recipients(data),
            sentTimestamp = sentTimestamp(
                message = message,
                type = type,
            ),
            receivedTimestamp = receivedTimestamp(message),
            priority = when (type) {
                ConversationMessageDetails.Type.MMS -> priority(value = message.smsPriority)
                ConversationMessageDetails.Type.SMS -> null
            },
            sizeBytes = when (type) {
                ConversationMessageDetails.Type.MMS -> {
                    message.smsMessageSize.takeIf { it > 0 }?.toLong()
                }

                ConversationMessageDetails.Type.SMS -> null
            },
            subscriptionLabel = subscriptionLabel(
                self = data.selfParticipant,
                activeSubscriptionCount = activeSubscriptionCount,
            ),
            debug = debug,
        )
    }

    private fun recipients(data: ConversationMessageDetailsData): ImmutableList<String> {
        val message = data.message
        val senderId = message.participantId
        val selfId = message.selfParticipantId
        val includeSelf = message.isIncoming

        return data.participants
            .asSequence()
            .filterNot { participant ->
                participant.id == senderId
            }
            .filterNot { participant ->
                participant.isSelf && (participant.id != selfId || !includeSelf)
            }
            .mapNotNull { participant ->
                participant.displayDestination?.takeIf(String::isNotBlank)
            }
            .toImmutableList()
    }

    private fun sentTimestamp(
        message: ConversationMessageData,
        type: ConversationMessageDetails.Type,
    ): Long? {
        return when {
            message.isIncoming && type == ConversationMessageDetails.Type.SMS -> {
                message.sentTimeStamp.takeIf { it != MmsUtils.INVALID_TIMESTAMP }
            }

            !message.isIncoming && message.isSendComplete -> message.receivedTimeStamp

            else -> null
        }
    }

    private fun receivedTimestamp(message: ConversationMessageData): Long? {
        return message.receivedTimeStamp.takeIf { message.isIncoming }
    }

    private fun priority(value: Int): ConversationMessageDetails.Priority {
        return when (value) {
            PduHeaders.PRIORITY_HIGH -> ConversationMessageDetails.Priority.HIGH
            PduHeaders.PRIORITY_LOW -> ConversationMessageDetails.Priority.LOW
            else -> ConversationMessageDetails.Priority.NORMAL
        }
    }

    private fun subscriptionLabel(
        self: ParticipantData?,
        activeSubscriptionCount: Int,
    ): ConversationSubscriptionLabel? {
        return when {
            self == null || activeSubscriptionCount < 2 -> null
            !self.isActiveSubscription || self.isDefaultSelf -> null

            else -> {
                self.subscriptionName
                    ?.takeIf(String::isNotBlank)
                    ?.let(ConversationSubscriptionLabel::Named)
                    ?: ConversationSubscriptionLabel.Slot(self.displaySlotId)
            }
        }
    }
}
