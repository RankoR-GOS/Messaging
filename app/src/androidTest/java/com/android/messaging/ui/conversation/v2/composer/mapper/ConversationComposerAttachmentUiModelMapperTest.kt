package com.android.messaging.ui.conversation.v2.composer.mapper

import com.android.messaging.data.conversation.model.draft.ConversationDraftPendingAttachment
import com.android.messaging.data.conversation.model.draft.ConversationDraftPendingAttachmentKind
import com.android.messaging.ui.conversation.v2.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.v2.messages.mapper.ConversationVCardAttachmentUiModelMapper
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationComposerAttachmentUiModelMapperTest {

    private val conversationVCardAttachmentUiModelMapper =
        mockk<ConversationVCardAttachmentUiModelMapper>()

    private val mapper = ConversationComposerAttachmentUiModelMapperImpl(
        conversationVCardAttachmentUiModelMapper = conversationVCardAttachmentUiModelMapper,
    )

    @Test
    fun genericPendingAttachment_mapsToGenericPendingUiModel() {
        val attachmentUiModels = mapper.map(
            attachments = emptyList(),
            pendingAttachments = listOf(
                ConversationDraftPendingAttachment(
                    pendingAttachmentId = "pending-1",
                    contentUri = "content://media/pending/1",
                    contentType = "image/jpeg",
                    displayName = "pending.jpg",
                ),
            ),
        )

        assertEquals(
            ComposerAttachmentUiModel.Pending.Generic(
                key = "pending-1",
                contentUri = "content://media/pending/1",
                contentType = "image/jpeg",
                displayName = "pending.jpg",
            ),
            attachmentUiModels.single(),
        )
    }

    @Test
    fun genericAudioPendingAttachment_mapsToGenericPendingUiModel() {
        val attachmentUiModels = mapper.map(
            attachments = emptyList(),
            pendingAttachments = listOf(
                ConversationDraftPendingAttachment(
                    pendingAttachmentId = "pending-audio-1",
                    contentUri = "content://media/pending/audio/1",
                    contentType = "audio/3gpp",
                    displayName = "audio.3gp",
                ),
            ),
        )

        assertEquals(
            ComposerAttachmentUiModel.Pending.Generic(
                key = "pending-audio-1",
                contentUri = "content://media/pending/audio/1",
                contentType = "audio/3gpp",
                displayName = "audio.3gp",
            ),
            attachmentUiModels.single(),
        )
    }

    @Test
    fun audioFinalizingPendingAttachment_mapsToAudioFinalizingPendingUiModel() {
        val attachmentUiModels = mapper.map(
            attachments = emptyList(),
            pendingAttachments = listOf(
                ConversationDraftPendingAttachment(
                    pendingAttachmentId = "pending-audio-finalizing-1",
                    contentUri = "pending://audio/pending-audio-finalizing-1",
                    contentType = "audio/3gpp",
                    kind = ConversationDraftPendingAttachmentKind.AudioFinalizing,
                ),
            ),
        )

        assertEquals(
            ComposerAttachmentUiModel.Pending.AudioFinalizing(
                key = "pending-audio-finalizing-1",
                contentUri = "pending://audio/pending-audio-finalizing-1",
                contentType = "audio/3gpp",
                displayName = "",
            ),
            attachmentUiModels.single(),
        )
    }
}
