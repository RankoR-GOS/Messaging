package com.android.messaging.data.message.mapper

import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.domain.message.model.MessageProtocol
import com.android.messaging.domain.message.model.MessageStatus
import javax.inject.Inject

internal interface MessageDataMapper {
    fun protocolOf(protocol: Int): MessageProtocol

    fun statusOf(status: Int): MessageStatus?
}

internal class MessageDataMapperImpl @Inject constructor() : MessageDataMapper {

    override fun protocolOf(protocol: Int): MessageProtocol {
        return when (protocol) {
            MessageData.PROTOCOL_SMS -> MessageProtocol.SMS
            MessageData.PROTOCOL_MMS -> MessageProtocol.MMS
            MessageData.PROTOCOL_MMS_PUSH_NOTIFICATION -> MessageProtocol.MMS_PUSH_NOTIFICATION
            else -> MessageProtocol.UNKNOWN
        }
    }

    @Suppress("CyclomaticComplexMethod")
    override fun statusOf(status: Int): MessageStatus? {
        return when (status) {
            MessageData.BUGLE_STATUS_UNKNOWN -> MessageStatus.UNKNOWN
            MessageData.BUGLE_STATUS_OUTGOING_COMPLETE -> MessageStatus.OUTGOING_COMPLETE
            MessageData.BUGLE_STATUS_OUTGOING_DELIVERED -> MessageStatus.OUTGOING_DELIVERED
            MessageData.BUGLE_STATUS_OUTGOING_DRAFT -> MessageStatus.OUTGOING_DRAFT
            MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND -> MessageStatus.OUTGOING_YET_TO_SEND
            MessageData.BUGLE_STATUS_OUTGOING_SENDING -> MessageStatus.OUTGOING_SENDING
            MessageData.BUGLE_STATUS_OUTGOING_RESENDING -> MessageStatus.OUTGOING_RESENDING
            MessageData.BUGLE_STATUS_OUTGOING_AWAITING_RETRY -> {
                MessageStatus.OUTGOING_AWAITING_RETRY
            }

            MessageData.BUGLE_STATUS_OUTGOING_FAILED -> MessageStatus.OUTGOING_FAILED
            MessageData.BUGLE_STATUS_OUTGOING_FAILED_EMERGENCY_NUMBER -> {
                MessageStatus.OUTGOING_FAILED_EMERGENCY_NUMBER
            }

            MessageData.BUGLE_STATUS_INCOMING_COMPLETE -> MessageStatus.INCOMING_COMPLETE
            MessageData.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD -> {
                MessageStatus.INCOMING_YET_TO_MANUAL_DOWNLOAD
            }

            MessageData.BUGLE_STATUS_INCOMING_RETRYING_MANUAL_DOWNLOAD -> {
                MessageStatus.INCOMING_RETRYING_MANUAL_DOWNLOAD
            }

            MessageData.BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING -> {
                MessageStatus.INCOMING_MANUAL_DOWNLOADING
            }

            MessageData.BUGLE_STATUS_INCOMING_RETRYING_AUTO_DOWNLOAD -> {
                MessageStatus.INCOMING_RETRYING_AUTO_DOWNLOAD
            }

            MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING -> {
                MessageStatus.INCOMING_AUTO_DOWNLOADING
            }

            MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED -> {
                MessageStatus.INCOMING_DOWNLOAD_FAILED
            }

            MessageData.BUGLE_STATUS_INCOMING_EXPIRED_OR_NOT_AVAILABLE -> {
                MessageStatus.INCOMING_EXPIRED_OR_NOT_AVAILABLE
            }

            else -> null
        }
    }
}
