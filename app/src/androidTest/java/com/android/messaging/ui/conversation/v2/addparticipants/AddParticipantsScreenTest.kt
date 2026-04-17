package com.android.messaging.ui.conversation.v2.addparticipants

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import com.android.messaging.ui.conversation.v2.ADD_PARTICIPANTS_CONFIRM_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.addParticipantsContactRowTestTag
import com.android.messaging.ui.conversation.v2.addparticipants.model.AddParticipantsEffect
import com.android.messaging.ui.conversation.v2.addparticipants.model.AddParticipantsUiState
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AddParticipantsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        unmockkAll()
        clearAllMocks()
    }

    @Test
    fun queryContactClicksAndConfirmButton_areForwardedToScreenModel() {
        val screenModel = mockk<AddParticipantsModel>()
        val screenUiStateFlow = MutableStateFlow(
            AddParticipantsUiState(
                recipientPickerUiState = RecipientPickerUiState(
                    contacts = listOf(
                        recipient(
                            id = "2",
                            displayName = "Bob",
                            destination = "+1 555 0101",
                        ),
                    ).toImmutableList(),
                ),
                isLoadingConversationParticipants = false,
                selectedRecipientDestinations = listOf("+1 555 0101").toImmutableList(),
            ),
        )
        val effectsFlow = MutableSharedFlow<AddParticipantsEffect>()

        every { screenModel.effects } returns effectsFlow
        every { screenModel.uiState } returns screenUiStateFlow
        every { screenModel.onConfirmClick() } just runs
        every { screenModel.onConversationIdChanged(conversationId = any()) } just runs
        every { screenModel.onLoadMore() } just runs
        every { screenModel.onQueryChanged(query = any()) } just runs
        every { screenModel.onRecipientClicked(destination = any()) } just runs

        setScreenContent(
            screenModel = screenModel,
        )

        composeTestRule
            .onNode(matcher = hasSetTextAction())
            .performTextInput("Bob")
        composeTestRule
            .onNodeWithTag(addParticipantsContactRowTestTag(contactId = "2"))
            .performClick()
        composeTestRule
            .onNodeWithTag(ADD_PARTICIPANTS_CONFIRM_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .performClick()

        verify(exactly = 1) {
            screenModel.onConversationIdChanged(conversationId = "conversation-1")
        }
        verify(exactly = 1) {
            screenModel.onQueryChanged(query = "Bob")
        }
        verify(exactly = 1) {
            screenModel.onRecipientClicked(destination = "+1 555 0101")
        }
        verify(exactly = 1) {
            screenModel.onConfirmClick()
        }
    }

    @Test
    fun navigateEffect_forwardsResolvedConversationId() {
        val screenModel = mockk<AddParticipantsModel>()
        val screenUiStateFlow = MutableStateFlow(
            AddParticipantsUiState(
                isLoadingConversationParticipants = false,
            ),
        )
        val effectsFlow = MutableSharedFlow<AddParticipantsEffect>(
            extraBufferCapacity = 1,
        )
        var resolvedConversationId: String? = null

        every { screenModel.effects } returns effectsFlow
        every { screenModel.uiState } returns screenUiStateFlow
        every { screenModel.onConfirmClick() } just runs
        every { screenModel.onConversationIdChanged(conversationId = any()) } just runs
        every { screenModel.onLoadMore() } just runs
        every { screenModel.onQueryChanged(query = any()) } just runs
        every { screenModel.onRecipientClicked(destination = any()) } just runs

        composeTestRule.setContent {
            AppTheme {
                AddParticipantsScreen(
                    conversationId = "conversation-1",
                    onNavigateToConversation = { conversationId ->
                        resolvedConversationId = conversationId
                    },
                    screenModel = screenModel,
                )
            }
        }

        composeTestRule.runOnIdle {
            effectsFlow.tryEmit(
                AddParticipantsEffect.NavigateToConversation(
                    conversationId = "conversation-2",
                ),
            )
        }
        composeTestRule.waitForIdle()

        org.junit.Assert.assertEquals("conversation-2", resolvedConversationId)
    }

    private fun setScreenContent(
        screenModel: AddParticipantsModel,
    ) {
        composeTestRule.setContent {
            AppTheme {
                AddParticipantsScreen(
                    conversationId = "conversation-1",
                    screenModel = screenModel,
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
