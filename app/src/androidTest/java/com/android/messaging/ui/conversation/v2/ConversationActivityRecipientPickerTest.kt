package com.android.messaging.ui.conversation.v2

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.messaging.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ConversationActivityRecipientPickerTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ConversationActivity>()

    @Test
    fun createGroupAction_navigatesToRecipientPickerScreen() {
        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(R.string.conversation_new_group),
            )
            .performClick()

        composeTestRule
            .onAllNodesWithText(
                composeTestRule.activity.getString(R.string.start_new_conversation),
            )
            .assertCountEquals(0)
        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(R.string.conversation_new_group),
            )
            .assertIsDisplayed()
    }
}
