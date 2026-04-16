package com.android.messaging.ui.conversation.v2.recipientpicker

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.navigation.RecipientPickerMode
import com.android.messaging.ui.core.AppTheme
import org.junit.Rule
import org.junit.Test

class RecipientPickerScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun createGroupMode_showsCreateGroupTitle() {
        setScreenContent(mode = RecipientPickerMode.CREATE_GROUP)

        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(R.string.conversation_new_group),
            )
            .assertIsDisplayed()
    }

    @Test
    fun addParticipantsMode_showsAddPeopleTitle() {
        setScreenContent(mode = RecipientPickerMode.ADD_PARTICIPANTS)

        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(R.string.conversation_add_people),
            )
            .assertIsDisplayed()
    }

    private fun setScreenContent(mode: RecipientPickerMode) {
        composeTestRule.setContent {
            AppTheme {
                RecipientPickerScreen(mode = mode)
            }
        }
    }
}
