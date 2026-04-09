package com.android.messaging.ui.conversation.v2.composer.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_PREVIEW_LIST_TEST_TAG
import com.android.messaging.ui.conversation.v2.composer.model.ConversationComposerAttachmentUiState
import com.android.messaging.ui.conversation.v2.conversationAttachmentPreviewItemTestTag
import com.android.messaging.ui.conversation.v2.conversationAttachmentPreviewRemoveButtonTestTag
import com.android.messaging.ui.core.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConversationAttachmentPreviewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyAttachments_doNotRenderList() {
        setAttachmentPreviewContent(attachments = emptyList())

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_ATTACHMENT_PREVIEW_LIST_TEST_TAG)
            .assertCountEquals(0)
    }

    @Test
    fun mixedAttachments_rendersAllItems() {
        setAttachmentPreviewContent()

        composeTestRule
            .onNodeWithTag(conversationAttachmentPreviewItemTestTag(PENDING_KEY))
            .assertExists()
        composeTestRule
            .onNodeWithTag(conversationAttachmentPreviewItemTestTag(RESOLVED_KEY))
            .assertExists()
    }

    @Test
    fun resolvedAttachment_clickForwardsCallback() {
        val clicks = mutableListOf<ConversationComposerAttachmentUiState.Resolved>()
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
        attachments: List<ConversationComposerAttachmentUiState> =
            listOf(PENDING_ATTACHMENT, RESOLVED_ATTACHMENT),
        onPendingAttachmentRemove: (String) -> Unit = {},
        onResolvedAttachmentClick: (ConversationComposerAttachmentUiState.Resolved) -> Unit = {},
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

        private val PENDING_ATTACHMENT = ConversationComposerAttachmentUiState.Pending(
            key = PENDING_KEY,
            contentType = "image/jpeg",
            contentUri = "content://media/pending/1",
            displayName = "pending.jpg",
        )
        private val RESOLVED_ATTACHMENT = ConversationComposerAttachmentUiState.Resolved(
            key = RESOLVED_KEY,
            contentType = "video/mp4",
            contentUri = RESOLVED_CONTENT_URI,
            captionText = "Caption",
            width = 640,
            height = 480,
        )
    }
}
