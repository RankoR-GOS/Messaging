package com.android.messaging.ui.conversation.v2.metadata.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.conversation.model.metadata.ConversationSubscription
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.ui.conversation.v2.CONVERSATION_OVERFLOW_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_SIM_SELECTOR_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.v2.composer.model.ConversationSimSelectorUiState
import com.android.messaging.ui.conversation.v2.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConversationTopAppBarSimSelectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val attName = "AT&T Business"

    private val verizon = ConversationSubscription(
        selfParticipantId = "self-1",
        label = ConversationSubscriptionLabel.Named(name = "Verizon"),
        displayDestination = "+1 555-867-5309",
        displaySlotId = 1,
        color = 0xFF5E9BE8.toInt(),
    )

    private val att = ConversationSubscription(
        selfParticipantId = "self-2",
        label = ConversationSubscriptionLabel.Named(name = attName),
        displayDestination = "+1 555-111-2222",
        displaySlotId = 2,
        color = 0xFFE97E6A.toInt(),
    )

    private val presentMetadata = ConversationMetadataUiState.Present(
        title = "Carol",
        selfParticipantId = "self-1",
        isGroupConversation = false,
        participantCount = 1,
        otherParticipantPhoneNumber = "+15551234567",
        otherParticipantContactLookupKey = null,
        isArchived = false,
        composerAvailability = ConversationComposerAvailability.editable(),
    )

    @Test
    fun simSelectorMenuItem_isHiddenWhenOnlyOneSubscriptionIsAvailable() {
        setContent(
            simSelector = ConversationSimSelectorUiState(
                subscriptions = persistentListOf(verizon),
                selectedSubscription = verizon,
            ),
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_OVERFLOW_BUTTON_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun simSelectorMenuItem_showsSelectedSubscriptionLabelWhenExpanded() {
        setContent(
            simSelector = ConversationSimSelectorUiState(
                subscriptions = persistentListOf(verizon, att),
                selectedSubscription = att,
            ),
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_OVERFLOW_BUTTON_TEST_TAG)
            .performClick()

        composeTestRule
            .onNodeWithTag(CONVERSATION_SIM_SELECTOR_MENU_ITEM_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(attName)
            .assertIsDisplayed()
    }

    @Test
    fun simSelectorMenuItem_clickInvokesCallbackAndDismissesMenu() {
        var clicks = 0

        composeTestRule.setContent {
            AppTheme {
                ConversationTopAppBar(
                    metadata = presentMetadata,
                    simSelector = ConversationSimSelectorUiState(
                        subscriptions = persistentListOf(verizon, att),
                        selectedSubscription = verizon,
                    ),
                    onAddPeopleClick = {},
                    onSimSelectorClick = { clicks += 1 },
                    onTitleClick = {},
                    onNavigateBack = {},
                )
            }
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_OVERFLOW_BUTTON_TEST_TAG)
            .performClick()

        composeTestRule
            .onNodeWithTag(CONVERSATION_SIM_SELECTOR_MENU_ITEM_TEST_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, clicks)
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_SIM_SELECTOR_MENU_ITEM_TEST_TAG)
            .assertDoesNotExist()
    }

    private fun setContent(simSelector: ConversationSimSelectorUiState) {
        composeTestRule.setContent {
            AppTheme {
                ConversationTopAppBar(
                    metadata = presentMetadata,
                    simSelector = simSelector,
                    onAddPeopleClick = {},
                    onSimSelectorClick = {},
                    onTitleClick = {},
                    onNavigateBack = {},
                )
            }
        }
    }
}
