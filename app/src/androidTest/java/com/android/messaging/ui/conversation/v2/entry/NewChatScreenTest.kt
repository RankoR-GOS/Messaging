package com.android.messaging.ui.conversation.v2.entry

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import com.android.messaging.R
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import com.android.messaging.ui.conversation.v2.NEW_CHAT_CONTACT_RESOLVING_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.v2.recipientpicker.RecipientPickerModel
import com.android.messaging.ui.conversation.v2.recipientpicker.model.RecipientPickerUiState
import com.android.messaging.ui.core.AppTheme
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NewChatScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        unmockkAll()
        clearAllMocks()
    }

    @Test
    fun interactions_forwardQueryContactAndCreateGroupCallbacks() {
        val pickerModel = mockk<RecipientPickerModel>()
        val onContactClick = mockk<(String) -> Unit>(relaxed = true)
        val onCreateGroupClick = mockk<() -> Unit>(relaxed = true)
        val uiStateFlow = MutableStateFlow(
            RecipientPickerUiState(
                contacts = listOf(
                    recipient(
                        id = "1",
                        displayName = "Ada Lovelace",
                        destination = "+1 555 0100",
                    ),
                ).toImmutableList(),
            ),
        )
        every { pickerModel.uiState } returns uiStateFlow
        every { pickerModel.onLoadMore() } just runs
        every { pickerModel.onQueryChanged(query = any()) } just runs

        setScreenContent(
            pickerModel = pickerModel,
            onContactClick = onContactClick,
            onCreateGroupClick = onCreateGroupClick,
        )

        composeTestRule
            .onNode(matcher = hasSetTextAction())
            .performTextInput("Ada")
        composeTestRule
            .onNodeWithText("Ada Lovelace")
            .performClick()
        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(R.string.conversation_new_group),
            )
            .performClick()

        verify(exactly = 1) {
            pickerModel.onQueryChanged(query = "Ada")
        }
        verify(exactly = 1) {
            onContactClick.invoke("+1 555 0100")
        }
        verify(exactly = 1) {
            onCreateGroupClick.invoke()
        }
    }

    @Test
    fun scrollingNearTheEnd_requestsLoadMoreWhenAllowed() {
        val pickerModel = mockk<RecipientPickerModel>()
        val uiStateFlow = MutableStateFlow(
            RecipientPickerUiState(
                contacts = List(size = 30) { index ->
                    recipient(
                        id = "$index",
                        displayName = "Contact $index",
                        destination = "+1 555 ${
                            index.toString().padStart(length = 4, padChar = '0')
                        }",
                    )
                }.toImmutableList(),
                canLoadMore = true,
            ),
        )
        every { pickerModel.uiState } returns uiStateFlow
        every { pickerModel.onLoadMore() } just runs
        every { pickerModel.onQueryChanged(query = any()) } just runs

        setScreenContent(pickerModel = pickerModel)

        composeTestRule
            .onNode(matcher = hasScrollToIndexAction())
            .performScrollToIndex(index = 31)
        composeTestRule.waitForIdle()

        verify(exactly = 1) {
            pickerModel.onLoadMore()
        }
    }

    @Test
    fun resolvingState_keepsCreateGroupButtonEnabledAndShowsRowProgressIndicatorAfterDelay() {
        val pickerModel = mockk<RecipientPickerModel>()
        val uiStateFlow = MutableStateFlow(
            RecipientPickerUiState(
                contacts = listOf(
                    recipient(
                        id = "1",
                        displayName = "Ada Lovelace",
                        destination = "+1 555 0100",
                    ),
                ).toImmutableList(),
            ),
        )
        every { pickerModel.uiState } returns uiStateFlow
        every { pickerModel.onLoadMore() } just runs
        every { pickerModel.onQueryChanged(query = any()) } just runs

        setScreenContent(
            pickerModel = pickerModel,
            isResolvingConversation = true,
            isResolvingIndicatorVisible = true,
            resolvingRecipientDestination = "+1 555 0100",
        )

        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(R.string.conversation_new_group),
            )
            .assertIsEnabled()
        composeTestRule
            .onNodeWithText("Ada Lovelace")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(R.string.start_new_conversation),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(NEW_CHAT_CONTACT_RESOLVING_INDICATOR_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun resolvingState_hidesRowProgressIndicatorBeforeDelay() {
        val pickerModel = mockk<RecipientPickerModel>()
        val uiStateFlow = MutableStateFlow(
            RecipientPickerUiState(
                contacts = listOf(
                    recipient(
                        id = "1",
                        displayName = "Ada Lovelace",
                        destination = "+1 555 0100",
                    ),
                ).toImmutableList(),
            ),
        )
        every { pickerModel.uiState } returns uiStateFlow
        every { pickerModel.onLoadMore() } just runs
        every { pickerModel.onQueryChanged(query = any()) } just runs

        setScreenContent(
            pickerModel = pickerModel,
            isResolvingConversation = true,
            isResolvingIndicatorVisible = false,
            resolvingRecipientDestination = "+1 555 0100",
        )

        composeTestRule
            .onAllNodesWithTag(NEW_CHAT_CONTACT_RESOLVING_INDICATOR_TEST_TAG)
            .assertCountEquals(expectedSize = 0)
        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(R.string.conversation_new_group),
            )
            .assertIsEnabled()
    }

    private fun setScreenContent(
        pickerModel: RecipientPickerModel,
        isResolvingConversation: Boolean = false,
        isResolvingIndicatorVisible: Boolean = false,
        onContactClick: (String) -> Unit = {},
        onCreateGroupClick: () -> Unit = {},
        resolvingRecipientDestination: String? = null,
    ) {
        composeTestRule.setContent {
            AppTheme {
                NewChatScreen(
                    isResolvingConversation = isResolvingConversation,
                    isResolvingConversationIndicatorVisible = isResolvingIndicatorVisible,
                    onContactClick = onContactClick,
                    onCreateGroupClick = onCreateGroupClick,
                    pickerModel = pickerModel,
                    resolvingRecipientDestination = resolvingRecipientDestination,
                )
            }
        }
    }

    private fun recipient(
        id: String,
        displayName: String,
        destination: String,
    ): ConversationRecipient {
        return ConversationRecipient(
            id = id,
            displayName = displayName,
            destination = destination,
            secondaryText = destination,
        )
    }
}
