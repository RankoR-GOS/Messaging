package com.android.messaging.ui.conversation.v2.messages.ui.attachment

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.CONVERSATION_INLINE_AUDIO_ATTACHMENT_PLAY_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_INLINE_AUDIO_ATTACHMENT_PROGRESS_TEST_TAG
import com.android.messaging.ui.core.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConversationInlineAudioAttachmentRowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun idleState_hidesProgressIndicator() {
        setAudioAttachmentContent(
            isPlaying = false,
            progress = 0f,
        )

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_INLINE_AUDIO_ATTACHMENT_PROGRESS_TEST_TAG)
            .assertCountEquals(0)
    }

    @Test
    fun progressIndicator_animatesInAndOutAsPlaybackStateChanges() {
        composeTestRule.mainClock.autoAdvance = false

        var isPlaying by mutableStateOf(value = false)
        var progress by mutableFloatStateOf(value = 0f)

        composeTestRule.setContent {
            AppTheme {
                ConversationInlineAudioAttachmentRowContent(
                    colors = rememberConversationInlineAudioAttachmentColors(
                        isIncoming = true,
                        isSelectionMode = false,
                        useStandaloneAudioAttachmentBackground = false,
                    ),
                    isSelectionMode = false,
                    isPlaying = isPlaying,
                    title = "Audio attachment",
                    durationLabel = "00:18",
                    progress = progress,
                    onClick = {},
                    onLongClick = {},
                )
            }
        }

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_INLINE_AUDIO_ATTACHMENT_PROGRESS_TEST_TAG)
            .assertCountEquals(0)

        composeTestRule.runOnIdle {
            progress = 0.45f
        }
        composeTestRule.mainClock.advanceTimeBy(milliseconds = 500L)

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_INLINE_AUDIO_ATTACHMENT_PROGRESS_TEST_TAG)
            .assertCountEquals(1)

        composeTestRule.runOnIdle {
            progress = 0f
            isPlaying = false
        }
        composeTestRule.mainClock.advanceTimeBy(milliseconds = 500L)

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_INLINE_AUDIO_ATTACHMENT_PROGRESS_TEST_TAG)
            .assertCountEquals(0)
    }

    @Test
    fun playButton_clickTogglesContentDescription() {
        var isPlaying by mutableStateOf(value = false)
        var clicks = 0
        val activity = composeTestRule.activity
        val playLabel = activity.getString(R.string.audio_play_content_description)
        val pauseLabel = activity.getString(R.string.audio_pause_content_description)

        composeTestRule.setContent {
            AppTheme {
                ConversationInlineAudioAttachmentRowContent(
                    colors = rememberConversationInlineAudioAttachmentColors(
                        isIncoming = true,
                        isSelectionMode = false,
                        useStandaloneAudioAttachmentBackground = false,
                    ),
                    isSelectionMode = false,
                    isPlaying = isPlaying,
                    title = "Audio attachment",
                    durationLabel = "00:18",
                    progress = 0f,
                    onClick = {
                        clicks += 1
                        isPlaying = !isPlaying
                    },
                    onLongClick = {},
                )
            }
        }

        composeTestRule
            .onAllNodesWithContentDescription(playLabel)
            .assertCountEquals(1)
        composeTestRule
            .onNodeWithTag(
                testTag = CONVERSATION_INLINE_AUDIO_ATTACHMENT_PLAY_BUTTON_TEST_TAG,
                useUnmergedTree = true,
            )
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, clicks)
        }

        composeTestRule
            .onAllNodesWithContentDescription(pauseLabel)
            .assertCountEquals(1)
    }

    private fun setAudioAttachmentContent(
        isPlaying: Boolean,
        progress: Float,
    ) {
        composeTestRule.setContent {
            AppTheme {
                ConversationInlineAudioAttachmentRowContent(
                    colors = rememberConversationInlineAudioAttachmentColors(
                        isIncoming = true,
                        isSelectionMode = false,
                        useStandaloneAudioAttachmentBackground = false,
                    ),
                    isSelectionMode = false,
                    isPlaying = isPlaying,
                    title = "Audio attachment",
                    durationLabel = "00:18",
                    progress = progress,
                    onClick = {},
                    onLongClick = {},
                )
            }
        }
    }
}
