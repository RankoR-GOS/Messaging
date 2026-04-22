package com.android.messaging.ui.conversation.v2.composer.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.height
import androidx.test.platform.app.InstrumentationRegistry
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_CONTACT_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_AUDIO_RECORDING_BAR_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_SEND_BUTTON_SHAPE_CIRCLE
import com.android.messaging.ui.conversation.v2.CONVERSATION_SEND_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_TEXT_FIELD_TEST_TAG
import com.android.messaging.ui.conversation.v2.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.v2.conversationShapeSemanticsKey
import com.android.messaging.ui.core.AppTheme
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConversationComposeBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun singleLineInput_keepsTextFieldAndSendButtonHeightsEqual() {
        setComposeBarContent(
            messageText = "Hello",
        )

        val textFieldBounds = composeTestRule
            .onNodeWithTag(CONVERSATION_TEXT_FIELD_TEST_TAG)
            .getUnclippedBoundsInRoot()

        val sendButtonBounds = composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .getUnclippedBoundsInRoot()

        assertEquals(
            textFieldBounds.height.value,
            sendButtonBounds.height.value,
            0.5f,
        )
    }

    @Test
    fun multiLineInput_growsTextFieldWithoutGrowingSendButton() {
        setComposeBarContent(
            messageText = "Line 1\nLine 2\nLine 3\nLine 4",
        )

        val textFieldBounds = composeTestRule
            .onNodeWithTag(CONVERSATION_TEXT_FIELD_TEST_TAG)
            .getUnclippedBoundsInRoot()
        val sendButtonBounds = composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .getUnclippedBoundsInRoot()

        assertTrue(textFieldBounds.height > sendButtonBounds.height)
    }

    @Test
    fun sendButton_exposesCircleShapeSemantics() {
        setComposeBarContent(
            messageText = "Hello",
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    conversationShapeSemanticsKey,
                    CONVERSATION_SEND_BUTTON_SHAPE_CIRCLE,
                ),
            )
    }

    @Test
    fun enabledState_andCallbacks_areWiredCorrectly() {
        var messageText = ""
        var sendClicks = 0

        composeTestRule.setContent {
            var currentMessageText by remember {
                mutableStateOf(value = "")
            }

            AppTheme {
                ConversationComposeBar(
                    audioRecording = ConversationAudioRecordingUiState(),
                    messageText = currentMessageText,
                    isMessageFieldEnabled = true,
                    isAttachmentActionEnabled = false,
                    isRecordActionEnabled = true,
                    isSendActionEnabled = true,
                    shouldShowRecordAction = false,
                    onContactAttachClick = {},
                    onMediaPickerClick = {},
                    onMessageTextChange = { updatedText ->
                        currentMessageText = updatedText
                        messageText = updatedText
                    },
                    onAudioRecordingStartRequest = {},
                    onAudioRecordingFinish = {},
                    onAudioRecordingCancel = {},
                    onSendClick = {
                        sendClicks += 1
                    },
                )
            }
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_TEXT_FIELD_TEST_TAG)
            .performTextInput("Hello")

        composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .assertIsEnabled()
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals("Hello", messageText)
            assertEquals(1, sendClicks)
        }
    }

    @Test
    fun sendButton_canBeDisabled() {
        var sendClicks = 0

        composeTestRule.setContent {
            AppTheme {
                ConversationComposeBar(
                    audioRecording = ConversationAudioRecordingUiState(),
                    messageText = "Hello",
                    isMessageFieldEnabled = true,
                    isAttachmentActionEnabled = false,
                    isRecordActionEnabled = true,
                    isSendActionEnabled = false,
                    shouldShowRecordAction = false,
                    onContactAttachClick = {},
                    onMediaPickerClick = {},
                    onMessageTextChange = {},
                    onAudioRecordingStartRequest = {},
                    onAudioRecordingFinish = {},
                    onAudioRecordingCancel = {},
                    onSendClick = {
                        sendClicks += 1
                    },
                )
            }
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(0, sendClicks)
        }
    }

    @Test
    fun textField_canBeDisabled() {
        composeTestRule.setContent {
            AppTheme {
                ConversationComposeBar(
                    audioRecording = ConversationAudioRecordingUiState(),
                    messageText = "",
                    isMessageFieldEnabled = false,
                    isAttachmentActionEnabled = false,
                    isRecordActionEnabled = true,
                    isSendActionEnabled = false,
                    shouldShowRecordAction = false,
                    onContactAttachClick = {},
                    onMediaPickerClick = {},
                    onMessageTextChange = {},
                    onAudioRecordingStartRequest = {},
                    onAudioRecordingFinish = {},
                    onAudioRecordingCancel = {},
                    onSendClick = {},
                )
            }
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_TEXT_FIELD_TEST_TAG)
            .assertIsNotEnabled()
    }

    fun attachmentButton_performsHapticFeedbackAndOpensMenu() {
        val hapticFeedback = createHapticFeedbackMock()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides hapticFeedback) {
                AppTheme {
                    ConversationComposeBar(
                        audioRecording = ConversationAudioRecordingUiState(),
                        messageText = "",
                        isMessageFieldEnabled = true,
                        isAttachmentActionEnabled = true,
                        isRecordActionEnabled = true,
                        isSendActionEnabled = false,
                        shouldShowRecordAction = false,
                        onContactAttachClick = {},
                        onMediaPickerClick = {},
                        onMessageTextChange = {},
                        onAudioRecordingStartRequest = {},
                        onAudioRecordingFinish = {},
                        onAudioRecordingCancel = {},
                        onSendClick = {},
                    )
                }
            }
        }

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG)
            .assertCountEquals(0)

        composeTestRule
            .onNodeWithTag(
                testTag = CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG,
                useUnmergedTree = true,
            )
            .performClick()

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG)
            .assertCountEquals(1)
        composeTestRule
            .onAllNodesWithTag(CONVERSATION_ATTACHMENT_CONTACT_MENU_ITEM_TEST_TAG)
            .assertCountEquals(1)

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
            }
        }
    }

    @Test
    fun attachmentMenuMediaItem_forwardsCallback() {
        var mediaClicks = 0

        composeTestRule.setContent {
            AppTheme {
                ConversationComposeBar(
                    audioRecording = ConversationAudioRecordingUiState(),
                    messageText = "",
                    isMessageFieldEnabled = true,
                    isAttachmentActionEnabled = true,
                    isRecordActionEnabled = true,
                    isSendActionEnabled = false,
                    shouldShowRecordAction = false,
                    onContactAttachClick = {},
                    onMediaPickerClick = {
                        mediaClicks += 1
                    },
                    onMessageTextChange = {},
                    onAudioRecordingStartRequest = {},
                    onAudioRecordingFinish = {},
                    onAudioRecordingCancel = {},
                    onSendClick = {},
                )
            }
        }

        composeTestRule
            .onNodeWithTag(
                testTag = CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG,
                useUnmergedTree = true,
            )
            .performClick()
        composeTestRule
            .onNodeWithTag(CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, mediaClicks)
        }
    }

    @Test
    fun attachmentMenuContactItem_forwardsCallback() {
        var contactClicks = 0

        composeTestRule.setContent {
            AppTheme {
                ConversationComposeBar(
                    audioRecording = ConversationAudioRecordingUiState(),
                    messageText = "",
                    isMessageFieldEnabled = true,
                    isAttachmentActionEnabled = true,
                    isRecordActionEnabled = true,
                    isSendActionEnabled = false,
                    shouldShowRecordAction = false,
                    onContactAttachClick = {
                        contactClicks += 1
                    },
                    onMediaPickerClick = {},
                    onMessageTextChange = {},
                    onAudioRecordingStartRequest = {},
                    onAudioRecordingFinish = {},
                    onAudioRecordingCancel = {},
                    onSendClick = {},
                )
            }
        }

        composeTestRule
            .onNodeWithTag(
                testTag = CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG,
                useUnmergedTree = true,
            )
            .performClick()
        composeTestRule
            .onNodeWithTag(CONVERSATION_ATTACHMENT_CONTACT_MENU_ITEM_TEST_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, contactClicks)
        }
    }

    @Test
    fun emptyMessage_withRecordActionVisible_showsRecordButton() {
        setComposeBarContent(
            messageText = "",
            shouldShowRecordAction = true,
        )

        composeTestRule
            .onNodeWithContentDescription(
                getString(id = R.string.audio_record_view_content_description),
            )
            .assertIsDisplayed()
    }

    @Test
    fun recordingState_showsRecordingBarWithoutChangingActionButtonHeight() {
        setComposeBarContent(
            audioRecording = ConversationAudioRecordingUiState(
                isRecording = true,
                durationMillis = 1_000L,
            ),
            messageText = "",
            shouldShowRecordAction = true,
        )

        val recordingBarBounds = composeTestRule
            .onNodeWithTag(CONVERSATION_AUDIO_RECORDING_BAR_TEST_TAG)
            .getUnclippedBoundsInRoot()
        val sendButtonBounds = composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .getUnclippedBoundsInRoot()

        assertEquals(
            recordingBarBounds.height.value,
            sendButtonBounds.height.value,
            0.5f,
        )
    }

    @Test
    fun longPressRecordButton_startsAndFinishesRecording() {
        var audioRecording by mutableStateOf(ConversationAudioRecordingUiState())
        var startRequests = 0
        var finishRequests = 0
        var cancelRequests = 0

        composeTestRule.setContent {
            AppTheme {
                ConversationComposeBar(
                    audioRecording = audioRecording,
                    messageText = "",
                    isMessageFieldEnabled = true,
                    isAttachmentActionEnabled = false,
                    isRecordActionEnabled = true,
                    isSendActionEnabled = false,
                    shouldShowRecordAction = true,
                    onContactAttachClick = {},
                    onMediaPickerClick = {},
                    onMessageTextChange = {},
                    onAudioRecordingStartRequest = {
                        startRequests += 1
                        audioRecording = ConversationAudioRecordingUiState(
                            isRecording = true,
                        )
                    },
                    onAudioRecordingFinish = {
                        finishRequests += 1
                        audioRecording = ConversationAudioRecordingUiState()
                    },
                    onAudioRecordingCancel = {
                        cancelRequests += 1
                        audioRecording = ConversationAudioRecordingUiState()
                    },
                    onSendClick = {},
                )
            }
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .performTouchInput {
                down(center)
                advanceEventTime(durationMillis = 700L)
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(1, startRequests)
            assertEquals(1, finishRequests)
            assertEquals(0, cancelRequests)
        }
    }

    fun attachmentButton_canBeDisabled() {
        setComposeBarContent(
            messageText = "",
            isSendActionEnabled = false,
            isAttachmentActionEnabled = false,
        )

        composeTestRule
            .onNodeWithTag(
                testTag = CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG,
                useUnmergedTree = true,
            )
            .performClick()
        composeTestRule
            .onAllNodesWithTag(CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG)
            .assertCountEquals(0)
    }

    private fun setComposeBarContent(
        audioRecording: ConversationAudioRecordingUiState = ConversationAudioRecordingUiState(),
        messageText: String,
        isSendActionEnabled: Boolean = true,
        isAttachmentActionEnabled: Boolean = false,
        shouldShowRecordAction: Boolean = false,
    ) {
        composeTestRule.setContent {
            AppTheme {
                ConversationComposeBar(
                    audioRecording = audioRecording,
                    messageText = messageText,
                    isMessageFieldEnabled = true,
                    isAttachmentActionEnabled = isAttachmentActionEnabled,
                    isRecordActionEnabled = true,
                    isSendActionEnabled = isSendActionEnabled,
                    shouldShowRecordAction = shouldShowRecordAction,
                    onContactAttachClick = {},
                    onMediaPickerClick = {},
                    onMessageTextChange = {},
                    onAudioRecordingStartRequest = {},
                    onAudioRecordingFinish = {},
                    onAudioRecordingCancel = {},
                    onSendClick = {},
                )
            }
        }
    }

    private fun getString(id: Int): String {
        return InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .getString(id)
    }

    private fun createHapticFeedbackMock(): HapticFeedback {
        val hapticFeedback = mockk<HapticFeedback>()
        every {
            hapticFeedback.performHapticFeedback(any())
        } just runs
        return hapticFeedback
    }
}
