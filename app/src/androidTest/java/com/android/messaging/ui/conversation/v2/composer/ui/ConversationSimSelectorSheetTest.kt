package com.android.messaging.ui.conversation.v2.composer.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.android.messaging.R
import com.android.messaging.data.conversation.model.metadata.ConversationSubscription
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.ui.conversation.v2.CONVERSATION_SIM_SELECTOR_SHEET_TEST_TAG
import com.android.messaging.ui.conversation.v2.composer.model.ConversationSimSelectorUiState
import com.android.messaging.ui.conversation.v2.conversationSimSelectorItemTestTag
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConversationSimSelectorSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val verizonName = "Verizon"
    private val attName = "AT&T Business"

    private val verizon = ConversationSubscription(
        selfParticipantId = "self-1",
        label = ConversationSubscriptionLabel.Named(name = verizonName),
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

    @Test
    fun sheet_rendersTitleAndAllSubscriptions() {
        setContent(
            uiState = ConversationSimSelectorUiState(
                subscriptions = persistentListOf(verizon, att),
                selectedSubscription = verizon,
            ),
        )

        val title = resolveString(R.string.sim_selector_sheet_title)

        composeTestRule
            .onNodeWithTag(CONVERSATION_SIM_SELECTOR_SHEET_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(verizonName).assertIsDisplayed()
        composeTestRule.onNodeWithText(attName).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(verizon.displayDestination.orEmpty())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(att.displayDestination.orEmpty())
            .assertIsDisplayed()
    }

    @Test
    fun sheet_marksSelectedSubscriptionWithCheckIcon() {
        setContent(
            uiState = ConversationSimSelectorUiState(
                subscriptions = persistentListOf(verizon, att),
                selectedSubscription = att,
            ),
        )

        val selectedDescription = resolveString(R.string.sim_selector_item_selected)

        composeTestRule
            .onNodeWithContentDescription(selectedDescription)
            .assertIsDisplayed()
    }

    @Test
    fun sheet_doesNotShowCheckWhenNoSubscriptionIsSelected() {
        setContent(
            uiState = ConversationSimSelectorUiState(
                subscriptions = persistentListOf(verizon, att),
                selectedSubscription = null,
            ),
        )

        val selectedDescription = resolveString(R.string.sim_selector_item_selected)

        composeTestRule
            .onNodeWithContentDescription(selectedDescription)
            .assertIsNotDisplayed()
    }

    @Test
    fun sheet_invokesCallbackWithSelfParticipantIdWhenRowClicked() {
        val selections = mutableListOf<String>()

        composeTestRule.setContent {
            AppTheme {
                ConversationSimSelectorSheet(
                    uiState = ConversationSimSelectorUiState(
                        subscriptions = persistentListOf(verizon, att),
                        selectedSubscription = verizon,
                    ),
                    onSimSelected = { selections += it },
                    onDismissRequest = {},
                )
            }
        }

        composeTestRule
            .onNodeWithTag(
                conversationSimSelectorItemTestTag(selfParticipantId = att.selfParticipantId),
            )
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(att.selfParticipantId), selections)
        }
    }

    private fun setContent(uiState: ConversationSimSelectorUiState) {
        composeTestRule.setContent {
            AppTheme {
                ConversationSimSelectorSheet(
                    uiState = uiState,
                    onSimSelected = {},
                    onDismissRequest = {},
                )
            }
        }
    }

    private fun resolveString(resId: Int): String {
        return InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getString(resId)
    }
}
