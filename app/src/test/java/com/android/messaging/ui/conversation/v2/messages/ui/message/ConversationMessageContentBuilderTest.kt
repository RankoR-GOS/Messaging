package com.android.messaging.ui.conversation.v2.messages.ui.message

import android.net.Uri
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationAttachmentItem
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationInlineAttachmentKind
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationMessageContentBuilderTest {

    @Test
    fun attachmentOnlyAudioMessage_doesNotUseMimeTypeAsBodyText() {
        val message = createMessage(
            text = null,
            parts = listOf(
                createMediaPart(
                    contentType = "audio/x-wav",
                    contentUri = "content://mms/part/audio-1",
                ),
            ),
        )

        val content = buildConversationMessageContent(
            message = message,
            subjectText = null,
        )

        assertNull(content.bodyText)
        assertTrue(content.isAttachmentOnly)
        assertEquals(1, content.attachmentSections.trailingItems.size)

        val inlineAttachment =
            (content.attachmentSections.trailingItems.single() as ConversationAttachmentItem.Inline)
                .attachment
        assertEquals(ConversationInlineAttachmentKind.AUDIO, inlineAttachment.kind)
        assertEquals("content://mms/part/audio-1", inlineAttachment.contentUri)
    }

    @Test
    fun attachmentCaption_isUsedAsBodyText() {
        val message = createMessage(
            text = null,
            parts = listOf(
                createMediaPart(
                    contentType = "audio/x-wav",
                    contentUri = "content://mms/part/audio-1",
                    text = "Ambient room tone",
                ),
            ),
        )

        val content = buildConversationMessageContent(
            message = message,
            subjectText = null,
        )

        assertEquals("Ambient room tone", content.bodyText)
        assertFalse(content.isAttachmentOnly)
    }

    private fun createMessage(
        text: String?,
        parts: List<ConversationMessagePartUiModel>,
    ): ConversationMessageUiModel {
        return ConversationMessageUiModel(
            messageId = "message-1",
            conversationId = "conversation-1",
            text = text,
            parts = parts,
            sentTimestamp = 1L,
            receivedTimestamp = 1L,
            displayTimestamp = 1L,
            status = ConversationMessageUiModel.Status.Outgoing.Complete,
            isIncoming = false,
            senderDisplayName = null,
            senderAvatarUri = null,
            senderContactLookupKey = null,
            canClusterWithPrevious = false,
            canClusterWithNext = false,
            canCopyMessageToClipboard = true,
            canDownloadMessage = false,
            canForwardMessage = true,
            canResendMessage = false,
            mmsSubject = null,
            protocol = ConversationMessageUiModel.Protocol.MMS,
        )
    }

    private fun createMediaPart(
        contentType: String,
        contentUri: String,
        text: String? = null,
    ): ConversationMessagePartUiModel {
        return ConversationMessagePartUiModel(
            contentType = contentType,
            text = text,
            contentUri = Uri.parse(contentUri),
            width = 0,
            height = 0,
        )
    }
}
