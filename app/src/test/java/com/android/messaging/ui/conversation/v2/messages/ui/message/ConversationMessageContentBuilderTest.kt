package com.android.messaging.ui.conversation.v2.messages.ui.message

import android.net.Uri
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationAttachmentItem
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationInlineAttachment
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentUiModel
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
                createAudioPart(
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

        val attachment = content.attachmentSections.trailingItems.single()
            as ConversationAttachmentItem.Inline

        val inlineAttachment = attachment.attachment as ConversationInlineAttachment.Audio
        assertEquals("content://mms/part/audio-1", inlineAttachment.contentUri)
    }

    @Test
    fun attachmentOnlyVCardMessage_buildsInlineVCardAttachment_withoutBodyText() {
        val vCardUiModel = ConversationVCardAttachmentUiModel(
            type = ConversationVCardAttachmentType.CONTACT,
            titleText = "Sam Rivera",
            subtitleText = "sam@example.com",
        )
        val message = createMessage(
            text = null,
            parts = listOf(
                createVCardPart(
                    contentUri = "content://mms/part/vcard-1",
                    vCardUiModel = vCardUiModel,
                ),
            ),
        )

        val content = buildConversationMessageContent(
            message = message,
            subjectText = null,
        )

        assertNull(content.bodyText)
        assertTrue(content.isAttachmentOnly)

        val trailingAttachment = content.attachmentSections.trailingItems.single()

        val inlineAttachment = (trailingAttachment as ConversationAttachmentItem.Inline)
            .attachment as ConversationInlineAttachment.VCard
        assertEquals("content://mms/part/vcard-1", inlineAttachment.contentUri)
        assertEquals(ConversationVCardAttachmentType.CONTACT, inlineAttachment.type)
        assertEquals("Sam Rivera", inlineAttachment.titleText)
        assertEquals("sam@example.com", inlineAttachment.subtitleText)
    }

    @Test
    fun attachmentCaption_isUsedAsBodyText() {
        val message = createMessage(
            text = null,
            parts = listOf(
                createAudioPart(
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
            canSaveAttachments = false,
            mmsSubject = null,
            protocol = ConversationMessageUiModel.Protocol.MMS,
        )
    }

    private fun createAudioPart(
        contentUri: String,
        text: String? = null,
    ): ConversationMessagePartUiModel.Attachment.Audio {
        return ConversationMessagePartUiModel.Attachment.Audio(
            text = text,
            contentType = "audio/x-wav",
            contentUri = Uri.parse(contentUri),
            width = 0,
            height = 0,
        )
    }

    private fun createVCardPart(
        contentUri: String,
        vCardUiModel: ConversationVCardAttachmentUiModel,
    ): ConversationMessagePartUiModel.Attachment.VCard {
        return ConversationMessagePartUiModel.Attachment.VCard(
            text = null,
            contentType = "text/x-vCard",
            contentUri = Uri.parse(contentUri),
            width = 0,
            height = 0,
            vCardUiModel = vCardUiModel,
        )
    }
}
