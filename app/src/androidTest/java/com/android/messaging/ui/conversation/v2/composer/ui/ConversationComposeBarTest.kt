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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.height
import com.android.messaging.ui.conversation.v2.CONVERSATION_SEND_BUTTON_SHAPE_CIRCLE
import com.android.messaging.ui.conversation.v2.CONVERSATION_SEND_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_TEXT_FIELD_TEST_TAG
import com.android.messaging.ui.conversation.v2.conversationShapeSemanticsKey
import com.android.messaging.ui.core.AppTheme
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
                    messageText = currentMessageText,
                    isMessageFieldEnabled = true,
                    isAttachmentActionEnabled = false,
                    isSendActionEnabled = true,
                    onAttachmentClick = {},
                    onMessageTextChange = { updatedText ->
                        currentMessageText = updatedText
                        messageText = updatedText
                    },
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
        setComposeBarContent(
            messageText = "Hello",
            isSendActionEnabled = false,
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .assertIsNotEnabled()
    }

    @Test
    fun textField_canBeDisabled() {
        composeTestRule.setContent {
            AppTheme {
                ConversationComposeBar(
                    messageText = "",
                    isMessageFieldEnabled = false,
                    isAttachmentActionEnabled = false,
                    isSendActionEnabled = false,
                    onAttachmentClick = {},
                    onMessageTextChange = {},
                    onSendClick = {},
                )
            }
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_TEXT_FIELD_TEST_TAG)
            .assertIsNotEnabled()
    }

    @Test
    fun sendButton_performsHapticFeedbackOnClick() {
        val hapticFeedback = RecordingHapticFeedback()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides hapticFeedback) {
                AppTheme {
                    ConversationComposeBar(
                        messageText = "Hello",
                        isMessageFieldEnabled = true,
                        isAttachmentActionEnabled = false,
                        isSendActionEnabled = true,
                        onAttachmentClick = {},
                        onMessageTextChange = {},
                        onSendClick = {},
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(HapticFeedbackType.ContextClick),
                hapticFeedback.performedFeedbackTypes,
            )
        }
    }

    private fun setComposeBarContent(
        messageText: String,
        isSendActionEnabled: Boolean = true,
    ) {
        composeTestRule.setContent {
            AppTheme {
                ConversationComposeBar(
                    messageText = messageText,
                    isMessageFieldEnabled = true,
                    isAttachmentActionEnabled = false,
                    isSendActionEnabled = isSendActionEnabled,
                    onAttachmentClick = {},
                    onMessageTextChange = {},
                    onSendClick = {},
                )
            }
        }
    }

    private class RecordingHapticFeedback : HapticFeedback {
        val performedFeedbackTypes = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            performedFeedbackTypes += hapticFeedbackType
        }
    }
}
