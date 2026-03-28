package com.android.messaging.data.conversation.mapper

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.MessagePartData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationDraftMessageDataMapperImplTest {

    private val mapper = ConversationDraftMessageDataMapperImpl()

    @Test
    fun map_createsDraftSmsMessageForPlainTextDraft() {
        val message = mapper.map(
            conversationId = "conversation-1",
            draft = ConversationDraft(
                messageText = "Hello",
                selfParticipantId = "self-1",
            ),
        )

        assertEquals("conversation-1", message.conversationId)
        assertEquals("self-1", message.selfId)
        assertEquals("self-1", message.participantId)
        assertEquals(MessageData.PROTOCOL_SMS, message.protocol)
        assertEquals("Hello", message.messageText)
        assertEquals(1, messageParts(message = message).size)
        assertTrue(messageParts(message = message).single().isText)
    }

    @Test
    fun map_createsDraftMmsMessageForSubjectOnlyDraft() {
        val message = mapper.map(
            conversationId = "conversation-1",
            draft = ConversationDraft(
                subjectText = "Subject",
                selfParticipantId = "self-1",
            ),
        )

        assertEquals(MessageData.PROTOCOL_MMS, message.protocol)
        assertEquals("Subject", message.mmsSubject)
        assertEquals("", message.messageText)
        assertTrue(messageParts(message = message).isEmpty())
    }

    @Test
    fun map_keepsSelfAndParticipantUnsetWhenSelfParticipantIdIsBlank() {
        val message = mapper.map(
            conversationId = "conversation-1",
            draft = ConversationDraft(
                messageText = "Hello",
                selfParticipantId = "",
            ),
        )

        assertNull(message.selfId)
        assertNull(message.participantId)
    }

    private fun messageParts(message: MessageData): List<MessagePartData> {
        val parts = mutableListOf<MessagePartData>()
        for (part in message.parts) {
            parts += part
        }
        return parts
    }
}
