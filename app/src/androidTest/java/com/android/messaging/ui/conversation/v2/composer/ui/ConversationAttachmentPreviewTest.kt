package com.android.messaging.ui.conversation.v2.composer.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_PREVIEW_LIST_TEST_TAG
import com.android.messaging.ui.conversation.v2.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.v2.conversationAttachmentPreviewItemTestTag
import com.android.messaging.ui.conversation.v2.conversationAttachmentPreviewRemoveButtonTestTag
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentUiModel
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConversationAttachmentPreviewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyAttachments_doNotRenderList() {
        setAttachmentPreviewContent(attachments = persistentListOf())

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_ATTACHMENT_PREVIEW_LIST_TEST_TAG)
            .assertCountEquals(0)
    }

    @Test
    fun mixedAttachments_rendersAllItems() {
        setAttachmentPreviewContent()

        composeTestRule
            .onNodeWithTag(conversationAttachmentPreviewItemTestTag(PENDING_KEY))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(conversationAttachmentPreviewItemTestTag(RESOLVED_KEY))
            .assertIsDisplayed()
    }

    @Test
    fun resolvedVisualAttachment_clickForwardsCallback() {
        val clicks = mutableListOf<ComposerAttachmentUiModel.Resolved>()
        setAttachmentPreviewContent(
            onResolvedAttachmentClick = { clicks += it },
        )

        composeTestRule
            .onNodeWithTag(conversationAttachmentPreviewItemTestTag(RESOLVED_KEY))
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(RESOLVED_ATTACHMENT), clicks)
        }
    }

    @Test
    fun vCardAttachment_rendersPreparedUiModelText() {
        setAttachmentPreviewContent(
            attachments = persistentListOf(VCARD_ATTACHMENT),
        )

        composeTestRule
            .onNodeWithText("Sam Rivera")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("555-000-8901")
            .assertIsDisplayed()
    }

    @Test
    fun pendingAttachment_removeButtonForwardsCallback() {
        val removals = mutableListOf<String>()
        setAttachmentPreviewContent(
            onPendingAttachmentRemove = { removals += it },
        )

        composeTestRule
            .onNodeWithTag(conversationAttachmentPreviewRemoveButtonTestTag(PENDING_KEY))
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(PENDING_KEY), removals)
        }
    }

    @Test
    fun resolvedAttachment_removeButtonForwardsCallback() {
        val removals = mutableListOf<String>()
        setAttachmentPreviewContent(
            onResolvedAttachmentRemove = { removals += it },
        )

        composeTestRule
            .onNodeWithTag(conversationAttachmentPreviewRemoveButtonTestTag(RESOLVED_KEY))
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(RESOLVED_CONTENT_URI), removals)
        }
    }

    private fun setAttachmentPreviewContent(
        attachments: ImmutableList<ComposerAttachmentUiModel> =
            persistentListOf(PENDING_ATTACHMENT, RESOLVED_ATTACHMENT),
        onPendingAttachmentRemove: (String) -> Unit = {},
        onResolvedAttachmentClick: (ComposerAttachmentUiModel.Resolved) -> Unit = {},
        onResolvedAttachmentRemove: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            AppTheme {
                ConversationAttachmentPreview(
                    attachments = attachments,
                    onPendingAttachmentRemove = onPendingAttachmentRemove,
                    onResolvedAttachmentClick = onResolvedAttachmentClick,
                    onResolvedAttachmentRemove = onResolvedAttachmentRemove,
                )
            }
        }
    }

    private companion object {
        private const val PENDING_KEY = "pending-1"
        private const val RESOLVED_KEY = "resolved-1"
        private const val RESOLVED_CONTENT_URI = "content://media/resolved/1"

        private val PENDING_ATTACHMENT = ComposerAttachmentUiModel.Pending(
            key = PENDING_KEY,
            contentType = "image/jpeg",
            contentUri = "content://media/pending/1",
            displayName = "pending.jpg",
        )
        private val RESOLVED_ATTACHMENT = ComposerAttachmentUiModel.Resolved.VisualMedia.Video(
            key = RESOLVED_KEY,
            contentType = "video/mp4",
            contentUri = RESOLVED_CONTENT_URI,
            captionText = "Caption",
            width = 640,
            height = 480,
        )
        private val VCARD_ATTACHMENT = ComposerAttachmentUiModel.Resolved.VCard(
            key = "resolved-vcard-1",
            contentType = "text/x-vCard",
            contentUri = "content://contacts/as_vcard/1",
            vCardUiModel = ConversationVCardAttachmentUiModel(
                type = ConversationVCardAttachmentType.CONTACT,
                titleText = "Sam Rivera",
                subtitleText = "555-000-8901",
            ),
        )
    }
}
