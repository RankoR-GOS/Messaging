package com.android.messaging.data.message.mapper

import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.domain.message.model.MessageProtocol
import com.android.messaging.domain.message.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MessageDataMapperImplTest {

    private val mapper = MessageDataMapperImpl()

    @Test
    fun protocolOf_mapsEveryStoredProtocol() {
        val protocols = mapOf(
            MessageData.PROTOCOL_UNKNOWN to MessageProtocol.UNKNOWN,
            MessageData.PROTOCOL_SMS to MessageProtocol.SMS,
            MessageData.PROTOCOL_MMS to MessageProtocol.MMS,
            MessageData.PROTOCOL_MMS_PUSH_NOTIFICATION to MessageProtocol.MMS_PUSH_NOTIFICATION,
        )

        protocols.forEach { (value, protocol) ->
            assertEquals(protocol, mapper.protocolOf(value))
        }
    }

    @Test
    fun protocolOf_mapsUnexpectedValueToUnknown() {
        assertEquals(MessageProtocol.UNKNOWN, mapper.protocolOf(UNEXPECTED_VALUE))
    }

    @Test
    fun statusOf_mapsEveryStoredStatusToItsOwnStatus() {
        val statuses = listOf(
            MessageData.BUGLE_STATUS_UNKNOWN,
            MessageData.BUGLE_STATUS_OUTGOING_COMPLETE,
            MessageData.BUGLE_STATUS_OUTGOING_DELIVERED,
            MessageData.BUGLE_STATUS_OUTGOING_DRAFT,
            MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND,
            MessageData.BUGLE_STATUS_OUTGOING_SENDING,
            MessageData.BUGLE_STATUS_OUTGOING_RESENDING,
            MessageData.BUGLE_STATUS_OUTGOING_AWAITING_RETRY,
            MessageData.BUGLE_STATUS_OUTGOING_FAILED,
            MessageData.BUGLE_STATUS_OUTGOING_FAILED_EMERGENCY_NUMBER,
            MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
            MessageData.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD,
            MessageData.BUGLE_STATUS_INCOMING_RETRYING_MANUAL_DOWNLOAD,
            MessageData.BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING,
            MessageData.BUGLE_STATUS_INCOMING_RETRYING_AUTO_DOWNLOAD,
            MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED,
            MessageData.BUGLE_STATUS_INCOMING_EXPIRED_OR_NOT_AVAILABLE,
        )

        assertEquals(MessageStatus.entries, statuses.map(mapper::statusOf))
    }

    @Test
    fun statusOf_returnsNullForUnexpectedValue() {
        assertNull(mapper.statusOf(UNEXPECTED_VALUE))
    }

    private companion object {
        private const val UNEXPECTED_VALUE = 42
    }
}
