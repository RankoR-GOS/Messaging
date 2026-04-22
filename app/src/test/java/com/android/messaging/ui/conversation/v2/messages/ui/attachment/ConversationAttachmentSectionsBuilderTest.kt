package com.android.messaging.ui.conversation.v2.messages.ui.attachment

import android.net.Uri
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationAttachmentItem
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationInlineAttachment
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationMessageAttachment
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagePartUiModel
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationAttachmentSectionsBuilderTest {

    @Test
    fun audioAttachment_mapsToInlineAudioAttachment() {
        val sections = buildConversationAttachmentSections(
            attachments = listOf(
                ConversationMessageAttachment.Media(
                    key = "attachment-1",
                    part = ConversationMessagePartUiModel.Attachment.Audio(
                        text = null,
                        contentType = "audio/x-wav",
                        contentUri = Uri.parse("content://mms/part/audio-1"),
                        width = 0,
                        height = 0,
                    ),
                ),
            ).toImmutableList(),
        )

        val inlineAttachment =
            (sections.trailingItems.single() as ConversationAttachmentItem.Inline)
                .attachment as ConversationInlineAttachment.Audio

        assertEquals("content://mms/part/audio-1", inlineAttachment.contentUri)
        assertEquals(R.string.audio_attachment_content_description, inlineAttachment.titleTextResId)
        assertNull(inlineAttachment.titleText)
    }

    @Test
    fun vcardAttachment_mapsToInlineVCardAttachment_andPreservesUiModel() {
        val vCardUiModel = ConversationVCardAttachmentUiModel(
            type = ConversationVCardAttachmentType.LOCATION,
            titleText = "Pier 57",
            subtitleText = "25 11th Ave New York NY 10011 United States",
        )

        val sections = buildConversationAttachmentSections(
            attachments = listOf(
                ConversationMessageAttachment.Media(
                    key = "attachment-1",
                    part = ConversationMessagePartUiModel.Attachment.VCard(
                        text = null,
                        contentType = "text/x-vCard",
                        contentUri = Uri.parse("content://mms/part/vcard-1"),
                        width = 0,
                        height = 0,
                        vCardUiModel = vCardUiModel,
                    ),
                ),
            ).toImmutableList(),
        )

        val inlineAttachment =
            (sections.trailingItems.single() as ConversationAttachmentItem.Inline)
                .attachment as ConversationInlineAttachment.VCard

        assertEquals("content://mms/part/vcard-1", inlineAttachment.contentUri)
        assertEquals(ConversationVCardAttachmentType.LOCATION, inlineAttachment.type)
        assertEquals("Pier 57", inlineAttachment.titleText)
        assertEquals("25 11th Ave New York NY 10011 United States", inlineAttachment.subtitleText)
    }
}
