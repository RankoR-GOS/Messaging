package com.android.messaging.ui.conversation.v2.composer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.platform.app.InstrumentationRegistry
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_AUDIO_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_CONTACT_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_AUDIO_RECORDING_BAR_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_AUDIO_RECORDING_LOCK_AFFORDANCE_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_SEND_BUTTON_SHAPE_CIRCLE
import com.android.messaging.ui.conversation.v2.CONVERSATION_SEND_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_TEXT_FIELD_TEST_TAG
import com.android.messaging.ui.conversation.v2.audio.model.ConversationAudioRecordingPhase
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
                    onLockedAudioRecordingStartRequest = {},
                    onMessageTextChange = { updatedText ->
                        currentMessageText = updatedText
                        messageText = updatedText
                    },
                    onAudioRecordingStartRequest = {},
                    onAudioRecordingFinish = {},
                    onAudioRecordingLock = { false },
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
                    onLockedAudioRecordingStartRequest = {},
                    onMessageTextChange = {},
                    onAudioRecordingStartRequest = {},
                    onAudioRecordingFinish = {},
                    onAudioRecordingLock = { false },
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
                    onLockedAudioRecordingStartRequest = {},
                    onMessageTextChange = {},
                    onAudioRecordingStartRequest = {},
                    onAudioRecordingFinish = {},
                    onAudioRecordingLock = { false },
                    onAudioRecordingCancel = {},
                    onSendClick = {},
                )
            }
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_TEXT_FIELD_TEST_TAG)
            .assertIsNotEnabled()
    }

    @Test
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
                        onLockedAudioRecordingStartRequest = {},
                        onMessageTextChange = {},
                        onAudioRecordingStartRequest = {},
                        onAudioRecordingFinish = {},
                        onAudioRecordingLock = { false },
                        onAudioRecordingCancel = {},
                        onSendClick = {},
                    )
                }
            }
        }

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG)
            .assertCountEquals(expectedSize = 0)

        composeTestRule
            .onNodeWithTag(
                testTag = CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG,
                useUnmergedTree = true,
            )
            .performClick()

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG)
            .assertCountEquals(expectedSize = 1)
        composeTestRule
            .onAllNodesWithTag(CONVERSATION_ATTACHMENT_AUDIO_MENU_ITEM_TEST_TAG)
            .assertCountEquals(expectedSize = 1)
        composeTestRule
            .onAllNodesWithTag(CONVERSATION_ATTACHMENT_CONTACT_MENU_ITEM_TEST_TAG)
            .assertCountEquals(expectedSize = 1)

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
                    onLockedAudioRecordingStartRequest = {},
                    onMessageTextChange = {},
                    onAudioRecordingStartRequest = {},
                    onAudioRecordingFinish = {},
                    onAudioRecordingLock = { false },
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
    fun attachmentMenuAudioItem_forwardsLockedRecordingStartRequest() {
        var audioClicks = 0

        composeTestRule.setContent {
            AppTheme {
                ConversationComposeBar(
                    audioRecording = ConversationAudioRecordingUiState(),
                    messageText = "Message with text",
                    isMessageFieldEnabled = true,
                    isAttachmentActionEnabled = true,
                    isRecordActionEnabled = true,
                    isSendActionEnabled = true,
                    shouldShowRecordAction = false,
                    onContactAttachClick = {},
                    onMediaPickerClick = {},
                    onLockedAudioRecordingStartRequest = {
                        audioClicks += 1
                    },
                    onMessageTextChange = {},
                    onAudioRecordingStartRequest = {},
                    onAudioRecordingFinish = {},
                    onAudioRecordingLock = { false },
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
            .onNodeWithTag(CONVERSATION_ATTACHMENT_AUDIO_MENU_ITEM_TEST_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, audioClicks)
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
                    onLockedAudioRecordingStartRequest = {},
                    onMessageTextChange = {},
                    onAudioRecordingStartRequest = {},
                    onAudioRecordingFinish = {},
                    onAudioRecordingLock = { false },
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
            .assertCountEquals(expectedSize = 0)
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
    fun recordingState_showsRecordingBarAndLockAffordanceWithoutChangingActionButtonHeight() {
        setComposeBarContent(
            audioRecording = ConversationAudioRecordingUiState(
                phase = ConversationAudioRecordingPhase.Recording,
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
        val lockAffordanceBounds = composeTestRule
            .onNodeWithTag(CONVERSATION_AUDIO_RECORDING_LOCK_AFFORDANCE_TEST_TAG)
            .getUnclippedBoundsInRoot()

        composeTestRule
            .onNodeWithTag(CONVERSATION_AUDIO_RECORDING_LOCK_AFFORDANCE_TEST_TAG)
            .assertIsDisplayed()

        assertEquals(
            recordingBarBounds.height.value,
            sendButtonBounds.height.value,
            0.5f,
        )
        assertTrue(lockAffordanceBounds.bottom.value <= sendButtonBounds.top.value + 8f)
        assertTrue(
            kotlin.math.abs(
                (
                    (lockAffordanceBounds.left.value + lockAffordanceBounds.right.value) / 2f
                    ) - (
                    (sendButtonBounds.left.value + sendButtonBounds.right.value) / 2f
                    ),
            ) <= 8f,
        )
    }

    @Test
    fun lockedRecordingState_showsStopButtonFromUiState() {
        setComposeBarContent(
            audioRecording = recordingAudioState(isLocked = true),
            messageText = "",
            shouldShowRecordAction = false,
        )

        composeTestRule
            .onNodeWithContentDescription(
                getString(id = R.string.audio_record_stop_content_description),
            )
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithTag(CONVERSATION_AUDIO_RECORDING_LOCK_AFFORDANCE_TEST_TAG)
            .assertCountEquals(expectedSize = 0)
    }

    @Test
    fun activeRecording_controlsStayEnabledWhenRecordStartActionIsDisabled() {
        var finishRequests = 0
        setComposeBarContent(
            audioRecording = recordingAudioState(isLocked = true),
            messageText = "",
            isRecordActionEnabled = false,
            shouldShowRecordAction = false,
            onAudioRecordingFinish = {
                finishRequests += 1
            },
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .assertIsEnabled()
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, finishRequests)
        }
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
                    onLockedAudioRecordingStartRequest = {},
                    onMessageTextChange = {},
                    onAudioRecordingStartRequest = {
                        startRequests += 1
                        audioRecording = recordingAudioState()
                    },
                    onAudioRecordingFinish = {
                        finishRequests += 1
                        audioRecording = ConversationAudioRecordingUiState()
                    },
                    onAudioRecordingLock = { false },
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

    @Test
    fun longPressAndDragLeft_cancelsRecording() {
        var audioRecording by mutableStateOf(ConversationAudioRecordingUiState())
        var startRequests = 0
        var finishRequests = 0
        var cancelRequests = 0
        val cancelDragDistancePx = with(composeTestRule.density) {
            (AUDIO_RECORD_CANCEL_THRESHOLD + 24.dp).toPx()
        }

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
                    onLockedAudioRecordingStartRequest = {},
                    onMessageTextChange = {},
                    onAudioRecordingStartRequest = {
                        startRequests += 1
                        audioRecording = recordingAudioState()
                    },
                    onAudioRecordingFinish = {
                        finishRequests += 1
                        audioRecording = ConversationAudioRecordingUiState()
                    },
                    onAudioRecordingLock = { false },
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
                moveBy(
                    Offset(
                        x = -cancelDragDistancePx,
                        y = 0f,
                    ),
                )
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(1, startRequests)
            assertEquals(0, finishRequests)
            assertEquals(1, cancelRequests)
        }
    }

    @Test
    fun lockGesture_emitsConfirmHapticAndKeepsRecordingActiveUntilStopTap() {
        var audioRecording by mutableStateOf(ConversationAudioRecordingUiState())
        var startRequests = 0
        var lockRequests = 0
        var finishRequests = 0
        var cancelRequests = 0
        var shouldShowRecordAction by mutableStateOf(true)
        val hapticFeedback = createHapticFeedbackMock()
        val lockDragDistancePx = with(composeTestRule.density) {
            (AUDIO_RECORD_LOCK_THRESHOLD + 24.dp).toPx()
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides hapticFeedback) {
                AppTheme {
                    ConversationComposeBar(
                        audioRecording = audioRecording,
                        messageText = "",
                        isMessageFieldEnabled = true,
                        isAttachmentActionEnabled = false,
                        isRecordActionEnabled = true,
                        isSendActionEnabled = false,
                        shouldShowRecordAction = shouldShowRecordAction,
                        onContactAttachClick = {},
                        onMediaPickerClick = {},
                        onLockedAudioRecordingStartRequest = {},
                        onMessageTextChange = {},
                        onAudioRecordingStartRequest = {
                            startRequests += 1
                            shouldShowRecordAction = false
                            audioRecording = recordingAudioState()
                        },
                        onAudioRecordingFinish = {
                            finishRequests += 1
                            audioRecording = finalizingAudioState()
                        },
                        onAudioRecordingLock = {
                            lockRequests += 1
                            audioRecording = recordingAudioState(isLocked = true)
                            true
                        },
                        onAudioRecordingCancel = {
                            cancelRequests += 1
                            audioRecording = ConversationAudioRecordingUiState()
                            shouldShowRecordAction = true
                        },
                        onSendClick = {},
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .performTouchInput {
                down(center)
                advanceEventTime(durationMillis = 700L)
                moveBy(
                    Offset(
                        x = 0f,
                        y = -lockDragDistancePx,
                    ),
                )
                up()
            }

        composeTestRule
            .onNodeWithContentDescription(
                getString(id = R.string.audio_record_stop_content_description),
            )
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            assertEquals(1, startRequests)
            assertEquals(1, lockRequests)
            assertEquals(0, finishRequests)
            assertEquals(0, cancelRequests)
            assertEquals(ConversationAudioRecordingPhase.Recording, audioRecording.phase)
            assertTrue(audioRecording.isLocked)
            verify(exactly = 1) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
            }
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .performClick()

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_AUDIO_RECORDING_BAR_TEST_TAG)
            .assertCountEquals(expectedSize = 0)
        composeTestRule
            .onAllNodesWithTag(CONVERSATION_AUDIO_RECORDING_LOCK_AFFORDANCE_TEST_TAG)
            .assertCountEquals(expectedSize = 0)

        composeTestRule.runOnIdle {
            assertEquals(1, finishRequests)
            assertEquals(0, cancelRequests)
            assertEquals(ConversationAudioRecordingPhase.Finalizing, audioRecording.phase)
        }
    }

    @Test
    fun lockedRecording_canStillSlideLeftToCancel() {
        var audioRecording by mutableStateOf(ConversationAudioRecordingUiState())
        var finishRequests = 0
        var cancelRequests = 0
        val lockDragDistancePx = with(composeTestRule.density) {
            (AUDIO_RECORD_LOCK_THRESHOLD + 24.dp).toPx()
        }
        val cancelDragDistancePx = with(composeTestRule.density) {
            (AUDIO_RECORD_CANCEL_THRESHOLD + 24.dp).toPx()
        }

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
                    onLockedAudioRecordingStartRequest = {},
                    onMessageTextChange = {},
                    onAudioRecordingStartRequest = {
                        audioRecording = recordingAudioState()
                    },
                    onAudioRecordingFinish = {
                        finishRequests += 1
                        audioRecording = ConversationAudioRecordingUiState()
                    },
                    onAudioRecordingLock = {
                        audioRecording = recordingAudioState(isLocked = true)
                        true
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
                moveBy(
                    Offset(
                        x = 0f,
                        y = -lockDragDistancePx,
                    ),
                )
                up()
            }

        composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .performTouchInput {
                down(center)
                moveBy(
                    Offset(
                        x = -cancelDragDistancePx,
                        y = 0f,
                    ),
                )
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(0, finishRequests)
            assertEquals(1, cancelRequests)
            assertEquals(ConversationAudioRecordingPhase.Idle, audioRecording.phase)
        }
    }

    private fun setComposeBarContent(
        audioRecording: ConversationAudioRecordingUiState = ConversationAudioRecordingUiState(),
        messageText: String,
        isSendActionEnabled: Boolean = true,
        isAttachmentActionEnabled: Boolean = false,
        isRecordActionEnabled: Boolean = true,
        shouldShowRecordAction: Boolean = false,
        onAudioRecordingFinish: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AppTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    ConversationComposeBar(
                        audioRecording = audioRecording,
                        messageText = messageText,
                        isMessageFieldEnabled = true,
                        isAttachmentActionEnabled = isAttachmentActionEnabled,
                        isRecordActionEnabled = isRecordActionEnabled,
                        isSendActionEnabled = isSendActionEnabled,
                        shouldShowRecordAction = shouldShowRecordAction,
                        onContactAttachClick = {},
                        onMediaPickerClick = {},
                        onLockedAudioRecordingStartRequest = {},
                        onMessageTextChange = {},
                        onAudioRecordingStartRequest = {},
                        onAudioRecordingFinish = onAudioRecordingFinish,
                        onAudioRecordingLock = { false },
                        onAudioRecordingCancel = {},
                        onSendClick = {},
                    )
                }
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

    private fun recordingAudioState(
        durationMillis: Long = 0L,
        isLocked: Boolean = false,
    ): ConversationAudioRecordingUiState {
        return ConversationAudioRecordingUiState(
            phase = ConversationAudioRecordingPhase.Recording,
            durationMillis = durationMillis,
            isLocked = isLocked,
        )
    }

    private fun finalizingAudioState(): ConversationAudioRecordingUiState {
        return ConversationAudioRecordingUiState(
            phase = ConversationAudioRecordingPhase.Finalizing,
        )
    }
}
