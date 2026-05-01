package com.android.messaging.ui.conversation.messages.ui.attachment

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.ui.conversation.attachment.ui.ConversationVCardAttachmentCardContent
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationInlineAttachment
import com.android.messaging.ui.core.AppTheme

@Composable
internal fun ConversationVCardInlineAttachmentRow(
    attachment: ConversationInlineAttachment.VCard,
    isSelectionMode: Boolean,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onLongClick: () -> Unit,
) {
    val onClick = attachment.openAction?.let { action ->
        {
            dispatchConversationAttachmentOpenAction(
                action = action,
                onAttachmentClick = onAttachmentClick,
                onExternalUriClick = onExternalUriClick,
            )
        }
    }

    ConversationVCardInlineAttachmentRowContent(
        attachment = attachment,
        isSelectionMode = isSelectionMode,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@Composable
internal fun ConversationVCardInlineAttachmentRowContent(
    attachment: ConversationInlineAttachment.VCard,
    isSelectionMode: Boolean,
    onClick: (() -> Unit)?,
    onLongClick: () -> Unit,
) {
    val modifier = when {
        isSelectionMode -> Modifier
        else -> {
            Modifier.combinedClickable(
                onClick = {
                    onClick?.invoke()
                },
                onLongClick = onLongClick,
            )
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(other = modifier),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(size = MESSAGE_ATTACHMENT_CORNER_RADIUS),
    ) {
        ConversationVCardAttachmentCardContent(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            type = attachment.type,
            titleText = attachment.titleText,
            titleTextResId = attachment.titleTextResId,
            subtitleText = attachment.subtitleText,
            subtitleTextResId = attachment.subtitleTextResId,
        )
    }
}

@Composable
private fun ConversationVCardInlineAttachmentPreviewContainer(
    content: @Composable () -> Unit,
) {
    AppTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(
                modifier = Modifier.padding(all = 16.dp),
            ) {
                content()
            }
        }
    }
}

@Preview(showBackground = true, name = "VCard Attachment")
@Composable
private fun ConversationVCardInlineAttachmentRowPreview() {
    ConversationVCardInlineAttachmentPreviewContainer {
        ConversationVCardInlineAttachmentRowContent(
            attachment = ConversationInlineAttachment.VCard(
                key = "contact",
                contentUri = "content://contacts/lookup/1",
                openAction = null,
                type = ConversationVCardAttachmentType.CONTACT,
                titleText = "Sam Rivera",
                titleTextResId = null,
                subtitleText = "View contact card",
                subtitleTextResId = null,
            ),
            isSelectionMode = false,
            onClick = {},
            onLongClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Location Attachment")
@Composable
private fun ConversationLocationInlineAttachmentRowPreview() {
    ConversationVCardInlineAttachmentPreviewContainer {
        ConversationVCardInlineAttachmentRowContent(
            attachment = ConversationInlineAttachment.VCard(
                key = "location",
                contentUri = "content://locations/1",
                openAction = null,
                type = ConversationVCardAttachmentType.LOCATION,
                titleText = "Pier 57",
                titleTextResId = null,
                subtitleText = "25 11th Ave New York NY 10011 United States",
                subtitleTextResId = null,
            ),
            isSelectionMode = false,
            onClick = {},
            onLongClick = {},
        )
    }
}
