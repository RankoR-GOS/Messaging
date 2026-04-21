package com.android.messaging.ui.conversation.v2.messages.ui.attachment

import android.net.Uri
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationAttachmentItem
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationInlineAttachmentKind
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationMessageAttachment
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
                    part = ConversationMessagePartUiModel(
                        contentType = "audio/x-wav",
                        text = null,
                        contentUri = Uri.parse("content://mms/part/audio-1"),
                        width = 0,
                        height = 0,
                    ),
                ),
            ).toImmutableList(),
        )

        val inlineAttachment =
            (sections.trailingItems.single() as ConversationAttachmentItem.Inline)
                .attachment

        assertEquals(ConversationInlineAttachmentKind.AUDIO, inlineAttachment.kind)
        assertEquals("content://mms/part/audio-1", inlineAttachment.contentUri)
        assertEquals(R.string.audio_attachment_content_description, inlineAttachment.titleTextResId)
        assertNull(inlineAttachment.titleText)
        assertNull(inlineAttachment.subtitleTextResId)
    }

    @Test
    fun vcardAttachment_mapsToInlineVCardAttachment() {
        val sections = buildConversationAttachmentSections(
            attachments = listOf(
                ConversationMessageAttachment.Media(
                    key = "attachment-1",
                    part = ConversationMessagePartUiModel(
                        contentType = "text/x-vCard",
                        text = null,
                        contentUri = Uri.parse("content://mms/part/vcard-1"),
                        width = 0,
                        height = 0,
                    ),
                ),
            ).toImmutableList(),
        )

        val inlineAttachment =
            (sections.trailingItems.single() as ConversationAttachmentItem.Inline)
                .attachment

        assertEquals(ConversationInlineAttachmentKind.VCARD, inlineAttachment.kind)
        assertNull(inlineAttachment.contentUri)
        assertEquals(R.string.notification_vcard, inlineAttachment.titleTextResId)
        assertEquals(R.string.vcard_tap_hint, inlineAttachment.subtitleTextResId)
    }
}
