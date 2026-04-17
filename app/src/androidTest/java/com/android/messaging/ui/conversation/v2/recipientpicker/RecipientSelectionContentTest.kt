package com.android.messaging.ui.conversation.v2.recipientpicker

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import com.android.messaging.R
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import com.android.messaging.ui.conversation.v2.recipientpicker.model.RecipientPickerUiState
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RecipientSelectionContentTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loadingState_showsProgressIndicatorAndHidesEmptyMessage() {
        setScreenContent(
            uiState = RecipientSelectionContentUiState(
                picker = RecipientPickerUiState(
                    isLoading = true,
                ),
            ),
        )

        composeTestRule
            .onAllNodes(
                matcher = hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate),
            )
            .assertCountEquals(expectedSize = 1)
        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(R.string.contact_list_empty_text),
            )
            .assertDoesNotExist()
    }

    @Test
    fun emptyState_showsEmptyMessage() {
        setScreenContent(
            uiState = RecipientSelectionContentUiState(
                picker = RecipientPickerUiState(
                    hasContactsPermission = false,
                ),
            ),
        )

        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(R.string.contact_list_empty_text),
            )
            .assertIsDisplayed()
    }

    @Test
    fun primaryAction_isShownAndForwardsClicks() {
        var primaryActionClicks = 0

        setScreenContent(
            uiState = RecipientSelectionContentUiState(
                picker = RecipientPickerUiState(
                    contacts = listOf(
                        recipient(
                            id = "1",
                            displayName = "Ada Lovelace",
                            destination = "+1 555 0100",
                        ),
                    ).toImmutableList(),
                ),
                primaryAction = RecipientSelectionPrimaryActionUiState(
                    text = "Continue",
                    isEnabled = true,
                    testTag = PRIMARY_ACTION_TEST_TAG,
                ),
                selectedRecipientDestinations = persistentSetOf("+1 555 0100"),
            ),
            onPrimaryActionClick = {
                primaryActionClicks += 1
            },
        )

        composeTestRule
            .onNodeWithTag(PRIMARY_ACTION_TEST_TAG)
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, primaryActionClicks)
    }

    @Test
    fun longPressAndTrailingIndicator_areSupportedByRowDecorators() {
        var clickCount = 0
        var longClickedDestination: String? = null

        setScreenContent(
            uiState = RecipientSelectionContentUiState(
                picker = RecipientPickerUiState(
                    contacts = listOf(
                        recipient(
                            id = "1",
                            displayName = "Ada Lovelace",
                            destination = "+1 555 0100",
                        ),
                    ).toImmutableList(),
                ),
            ),
            rowDecorators = RecipientSelectionRowDecorators(
                recipientRowTestTag = { contact ->
                    recipientRowTestTag(contactId = contact.id)
                },
                showRecipientTrailingIndicator = { true },
                trailingIndicatorTestTag = TRAILING_INDICATOR_TEST_TAG,
            ),
            onRecipientClick = {
                clickCount += 1
            },
            onRecipientLongClick = { contact ->
                longClickedDestination = contact.destination
            },
        )

        composeTestRule
            .onNodeWithTag(recipientRowTestTag(contactId = "1"))
            .performTouchInput {
                down(center)
                advanceEventTime(1_000)
                up()
            }

        composeTestRule
            .onNodeWithTag(TRAILING_INDICATOR_TEST_TAG)
            .assertIsDisplayed()
        assertEquals("+1 555 0100", longClickedDestination)
        assertEquals(0, clickCount)
    }

    @Test
    fun scrollingNearEnd_requestsLoadMoreWhenAllowed() {
        var loadMoreCount = 0

        setScreenContent(
            uiState = RecipientSelectionContentUiState(
                picker = RecipientPickerUiState(
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
            ),
            onLoadMore = {
                loadMoreCount += 1
            },
        )

        composeTestRule
            .onNode(hasScrollToIndexAction())
            .performScrollToIndex(index = 29)
        composeTestRule.waitForIdle()

        assertEquals(1, loadMoreCount)
    }

    private fun setScreenContent(
        uiState: RecipientSelectionContentUiState,
        rowDecorators: RecipientSelectionRowDecorators = RecipientSelectionRowDecorators(
            recipientRowTestTag = { contact ->
                recipientRowTestTag(contactId = contact.id)
            },
        ),
        onLoadMore: () -> Unit = {},
        onPrimaryActionClick: () -> Unit = {},
        onRecipientClick: (ConversationRecipient) -> Unit = {},
        onRecipientLongClick: ((ConversationRecipient) -> Unit)? = null,
    ) {
        composeTestRule.setContent {
            AppTheme {
                RecipientSelectionContent(
                    uiState = uiState,
                    strings = RecipientSelectionStrings(
                        queryPrefixText = composeTestRule.activity.getString(
                            R.string.to_address_label,
                        ),
                        queryPlaceholderText = composeTestRule.activity.getString(
                            R.string.new_chat_query_hint,
                        ),
                    ),
                    rowDecorators = rowDecorators,
                    onLoadMore = onLoadMore,
                    onPrimaryActionClick = onPrimaryActionClick,
                    onQueryChanged = {},
                    onRecipientClick = onRecipientClick,
                    onRecipientLongClick = onRecipientLongClick,
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

    private fun recipientRowTestTag(contactId: String): String {
        return "recipient_selection_contact_$contactId"
    }

    private companion object {
        private const val PRIMARY_ACTION_TEST_TAG = "recipient_selection_primary_action"
        private const val TRAILING_INDICATOR_TEST_TAG = "recipient_selection_trailing_indicator"
    }
}
