package com.android.messaging.ui.conversation.v2.messages.ui.attachment

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationInlineAttachment
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.ui.core.AppTheme
import org.junit.Rule
import org.junit.Test

class ConversationVCardInlineAttachmentRowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loadedContactMetadata_rendersDisplayNameAndDetails() {
        setRowContent(
            attachment = ConversationInlineAttachment.VCard(
                key = "attachment-1",
                contentUri = "content://mms/part/vcard-1",
                openAction = null,
                subtitleTextResId = R.string.vcard_tap_hint,
                titleText = null,
                titleTextResId = R.string.notification_vcard,
                metadata = ConversationVCardAttachmentMetadata.Loaded(
                    type = ConversationVCardAttachmentType.CONTACT,
                    displayName = "Sam Rivera",
                    details = "sam@example.com",
                    locationAddress = null,
                ),
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
    fun loadedLocationMetadata_withoutName_usesLocationFallbackTitle() {
        setRowContent(
            attachment = ConversationInlineAttachment.VCard(
                key = "attachment-1",
                contentUri = "content://mms/part/vcard-1",
                openAction = null,
                subtitleTextResId = R.string.vcard_tap_hint,
                titleText = null,
                titleTextResId = R.string.notification_vcard,
                metadata = ConversationVCardAttachmentMetadata.Loaded(
                    type = ConversationVCardAttachmentType.LOCATION,
                    displayName = null,
                    details = "New York",
                    locationAddress = "25 11th Ave New York NY 10011 United States",
                ),
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
    fun missingMetadata_rendersDefaultStringsFromResources() {
        setRowContent(
            attachment = ConversationInlineAttachment.VCard(
                key = "attachment-1",
                contentUri = "content://mms/part/vcard-1",
                openAction = null,
                subtitleTextResId = R.string.vcard_tap_hint,
                titleText = null,
                titleTextResId = R.string.notification_vcard,
                metadata = ConversationVCardAttachmentMetadata.Missing,
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
