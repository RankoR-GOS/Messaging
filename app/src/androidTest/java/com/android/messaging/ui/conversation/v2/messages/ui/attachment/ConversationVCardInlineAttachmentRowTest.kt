package com.android.messaging.ui.conversation.v2.messages.ui.attachment

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationInlineAttachment
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.ui.core.AppTheme
import org.junit.Rule
import org.junit.Test

class ConversationVCardInlineAttachmentRowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loadedContactUiModel_rendersDisplayNameAndDetails() {
        setRowContent(
            attachment = ConversationInlineAttachment.VCard(
                key = "attachment-1",
                contentUri = "content://mms/part/vcard-1",
                openAction = null,
                type = ConversationVCardAttachmentType.CONTACT,
                titleText = "Sam Rivera",
                titleTextResId = null,
                subtitleText = "sam@example.com",
                subtitleTextResId = null,
            ),
        )

        composeTestRule
            .onNodeWithText("Sam Rivera")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("sam@example.com")
            .assertIsDisplayed()
    }

    @Test
    fun loadedLocationUiModel_withoutName_usesLocationFallbackTitle() {
        setRowContent(
            attachment = ConversationInlineAttachment.VCard(
                key = "attachment-1",
                contentUri = "content://mms/part/vcard-1",
                openAction = null,
                type = ConversationVCardAttachmentType.LOCATION,
                titleText = null,
                titleTextResId = R.string.notification_location,
                subtitleText = "25 11th Ave New York NY 10011 United States",
                subtitleTextResId = null,
            ),
        )

        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(R.string.notification_location),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("25 11th Ave New York NY 10011 United States")
            .assertIsDisplayed()
    }

    @Test
    fun missingUiModelDetails_rendersDefaultStringsFromResources() {
        setRowContent(
            attachment = ConversationInlineAttachment.VCard(
                key = "attachment-1",
                contentUri = "content://mms/part/vcard-1",
                openAction = null,
                type = ConversationVCardAttachmentType.CONTACT,
                titleText = null,
                titleTextResId = R.string.notification_vcard,
                subtitleText = null,
                subtitleTextResId = R.string.vcard_tap_hint,
            ),
        )

        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(R.string.notification_vcard),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(R.string.vcard_tap_hint),
            )
            .assertIsDisplayed()
    }

    private fun setRowContent(
        attachment: ConversationInlineAttachment.VCard,
    ) {
        composeTestRule.setContent {
            AppTheme {
                ConversationVCardInlineAttachmentRow(
                    attachment = attachment,
                    isSelectionMode = false,
                    onAttachmentClick = { _, _ -> },
                    onExternalUriClick = {},
                    onLongClick = {},
                )
            }
        }
    }
}
