package com.android.messaging.ui.conversation.v2.messages.ui.attachment

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationInlineAttachment
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentUiState
import com.android.messaging.ui.core.AppTheme

@Composable
internal fun ConversationVCardInlineAttachmentRow(
    attachment: ConversationInlineAttachment.VCard,
    isSelectionMode: Boolean,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onLongClick: () -> Unit,
) {
    val uiState = attachment.toConversationVCardAttachmentUiState()

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
        uiState = uiState,
        isSelectionMode = isSelectionMode,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@Composable
private fun ConversationInlineAttachment.VCard.toConversationVCardAttachmentUiState():
    ConversationVCardAttachmentUiState {
    return metadata.toConversationVCardAttachmentUiState(
        defaultUiText = resolveConversationVCardDefaultUiText(),
    )
}

@Composable
private fun ConversationInlineAttachment.VCard.resolveConversationVCardDefaultUiText():
    ConversationVCardDefaultUiText {
    val defaultTitle = titleText
        ?: titleTextResId?.let { titleTextResId ->
            stringResource(id = titleTextResId)
        }
        ?: stringResource(id = R.string.notification_vcard)

    val defaultSubtitle = subtitleTextResId?.let { subtitleTextResId ->
        stringResource(id = subtitleTextResId)
    } ?: stringResource(id = R.string.vcard_tap_hint)

    return ConversationVCardDefaultUiText(
        defaultTitle = defaultTitle,
        defaultSubtitle = defaultSubtitle,
        loadingSubtitle = stringResource(id = R.string.loading_vcard),
        failedSubtitle = stringResource(id = R.string.failed_loading_vcard),
        locationTitle = stringResource(id = R.string.notification_location),
    )
}

private fun ConversationVCardAttachmentMetadata?.toConversationVCardAttachmentUiState(
    defaultUiText: ConversationVCardDefaultUiText,
): ConversationVCardAttachmentUiState {
    return when (this) {
        ConversationVCardAttachmentMetadata.Failed -> {
            createConversationContactUiState(
                title = defaultUiText.defaultTitle,
                subtitle = defaultUiText.failedSubtitle,
            )
        }

        ConversationVCardAttachmentMetadata.Loading -> {
            createConversationContactUiState(
                title = defaultUiText.defaultTitle,
                subtitle = defaultUiText.loadingSubtitle,
            )
        }

        ConversationVCardAttachmentMetadata.Missing,
        null,
        -> {
            createConversationContactUiState(
                title = defaultUiText.defaultTitle,
                subtitle = defaultUiText.defaultSubtitle,
            )
        }

        is ConversationVCardAttachmentMetadata.Loaded -> {
            toConversationLoadedVCardAttachmentUiState(
                defaultUiText = defaultUiText,
            )
        }
    }
}

private fun ConversationVCardAttachmentMetadata.Loaded.toConversationLoadedVCardAttachmentUiState(
    defaultUiText: ConversationVCardDefaultUiText,
): ConversationVCardAttachmentUiState {
    return when (type) {
        ConversationVCardAttachmentType.CONTACT -> {
            createConversationContactUiState(
                title = displayName ?: defaultUiText.defaultTitle,
                subtitle = details ?: defaultUiText.defaultSubtitle,
            )
        }

        ConversationVCardAttachmentType.LOCATION -> {
            ConversationVCardAttachmentUiState(
                type = ConversationVCardAttachmentType.LOCATION,
                title = displayName ?: defaultUiText.locationTitle,
                subtitle = locationAddress ?: details,
            )
        }
    }
}

private fun createConversationContactUiState(
    title: String,
    subtitle: String?,
): ConversationVCardAttachmentUiState {
    return ConversationVCardAttachmentUiState(
        type = ConversationVCardAttachmentType.CONTACT,
        title = title,
        subtitle = subtitle,
    )
}

@Composable
internal fun ConversationVCardInlineAttachmentRowContent(
    uiState: ConversationVCardAttachmentUiState,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(size = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when (uiState.type) {
                        ConversationVCardAttachmentType.CONTACT -> Icons.Rounded.Person
                        ConversationVCardAttachmentType.LOCATION -> Icons.Rounded.Place
                    },
                    contentDescription = null,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(space = 2.dp),
            ) {
                Text(
                    text = uiState.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                uiState.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
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

private data class ConversationVCardDefaultUiText(
    val defaultTitle: String,
    val defaultSubtitle: String,
    val loadingSubtitle: String,
    val failedSubtitle: String,
    val locationTitle: String,
)

@Preview(showBackground = true, name = "VCard Attachment")
@Composable
private fun ConversationVCardInlineAttachmentRowPreview() {
    ConversationVCardInlineAttachmentPreviewContainer {
        ConversationVCardInlineAttachmentRowContent(
            uiState = ConversationVCardAttachmentUiState(
                type = ConversationVCardAttachmentType.CONTACT,
                title = "Sam Rivera",
                subtitle = "View contact card",
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
            uiState = ConversationVCardAttachmentUiState(
                type = ConversationVCardAttachmentType.LOCATION,
                title = "Pier 57",
                subtitle = "25 11th Ave New York NY 10011 United States",
            ),
            isSelectionMode = false,
            onClick = {},
            onLongClick = {},
        )
    }
}
