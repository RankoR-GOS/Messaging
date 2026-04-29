package com.android.messaging.ui.conversation.v2.metadata.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.BidiFormatter
import androidx.core.text.TextDirectionHeuristicsCompat
import coil3.compose.AsyncImage
import com.android.messaging.R
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.conversation.model.metadata.ConversationSubscription
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.ui.conversation.v2.CONVERSATION_ADD_CONTACT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_ADD_PEOPLE_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_ARCHIVE_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_CALL_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_DELETE_CONVERSATION_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_OVERFLOW_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_SIM_SELECTOR_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_UNARCHIVE_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.composer.model.ConversationSimSelectorUiState
import com.android.messaging.ui.conversation.v2.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.v2.resolveDisplayName
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.util.AccessibilityUtil
import kotlinx.collections.immutable.persistentListOf

private val CONVERSATION_TOP_APP_BAR_TITLE_SPACING = 12.dp
private val CONVERSATION_TOP_APP_BAR_AVATAR_SIZE = 36.dp
private val CONVERSATION_TOP_APP_BAR_AVATAR_ICON_SIZE = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationTopAppBar(
    modifier: Modifier = Modifier,
    metadata: ConversationMetadataUiState,
    isAddPeopleVisible: Boolean = false,
    isCallVisible: Boolean = false,
    isArchiveVisible: Boolean = false,
    isUnarchiveVisible: Boolean = false,
    isAddContactVisible: Boolean = false,
    isDeleteConversationVisible: Boolean = false,
    simSelector: ConversationSimSelectorUiState = ConversationSimSelectorUiState(),
    onAddPeopleClick: () -> Unit,
    onCallClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onUnarchiveClick: () -> Unit = {},
    onAddContactClick: () -> Unit = {},
    onDeleteConversationClick: () -> Unit = {},
    onSimSelectorClick: () -> Unit = {},
    onTitleClick: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val presentation = rememberConversationTopAppBarPresentation(
        metadata = metadata,
    )
    val isTitleClickable = metadata is ConversationMetadataUiState.Present

    TopAppBar(
        modifier = modifier.fillMaxWidth(),
        colors = conversationTopAppBarColors(),
        title = {
            ConversationTopAppBarTitle(
                isClickable = isTitleClickable,
                onClick = onTitleClick,
                presentation = presentation,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onNavigateBack,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(id = R.string.back),
                )
            }
        },
        actions = {
            if (isCallVisible) {
                IconButton(
                    modifier = Modifier.testTag(CONVERSATION_CALL_BUTTON_TEST_TAG),
                    onClick = onCallClick,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = stringResource(id = R.string.action_call),
                    )
                }
            }
            val isSimSelectorVisible = simSelector.isAvailable

            val isOverflowVisible = isAddPeopleVisible ||
                isArchiveVisible ||
                isUnarchiveVisible ||
                isAddContactVisible ||
                isDeleteConversationVisible ||
                isSimSelectorVisible

            if (isOverflowVisible) {
                ConversationTopAppBarOverflowMenu(
                    isAddPeopleVisible = isAddPeopleVisible,
                    isArchiveVisible = isArchiveVisible,
                    isUnarchiveVisible = isUnarchiveVisible,
                    isAddContactVisible = isAddContactVisible,
                    isDeleteConversationVisible = isDeleteConversationVisible,
                    isSimSelectorVisible = isSimSelectorVisible,
                    simSelectorLabel = simSelector.selectedSubscription
                        ?.label
                        ?.resolveDisplayName()
                        .orEmpty(),
                    onAddPeopleClick = onAddPeopleClick,
                    onArchiveClick = onArchiveClick,
                    onUnarchiveClick = onUnarchiveClick,
                    onAddContactClick = onAddContactClick,
                    onDeleteConversationClick = onDeleteConversationClick,
                    onSimSelectorClick = onSimSelectorClick,
                )
            }
        },
    )
}

@Composable
private fun conversationTopAppBarColors(): TopAppBarColors {
    return TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun rememberConversationTopAppBarPresentation(
    metadata: ConversationMetadataUiState,
): ConversationTopAppBarPresentation {
    val title = conversationTitle(metadata)
    val subtitle = conversationSubtitle(metadata)
    val subtitleContentDescription = conversationSubtitleContentDescription(
        metadata = metadata,
    )

    val avatar = conversationAvatar(metadata)

    return remember(
        metadata,
        title,
        subtitle,
        subtitleContentDescription,
        avatar,
    ) {
        ConversationTopAppBarPresentation(
            title = title,
            subtitle = subtitle,
            subtitleContentDescription = subtitleContentDescription,
            avatar = avatar,
        )
    }
}

@Composable
private fun ConversationTopAppBarTitle(
    isClickable: Boolean,
    onClick: () -> Unit,
    presentation: ConversationTopAppBarPresentation,
) {
    Row(
        modifier = Modifier.clickable(
            enabled = isClickable,
            onClick = onClick,
        ),
        horizontalArrangement = Arrangement.spacedBy(
            space = CONVERSATION_TOP_APP_BAR_TITLE_SPACING,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConversationAvatar(
            avatar = presentation.avatar,
        )

        ConversationTopAppBarText(
            presentation = presentation,
        )
    }
}

@Composable
private fun ConversationTopAppBarText(
    presentation: ConversationTopAppBarPresentation,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = presentation.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (presentation.subtitle != null) {
            Text(
                modifier = Modifier.semantics {
                    presentation.subtitleContentDescription?.let { subtitleContentDescription ->
                        contentDescription = subtitleContentDescription
                    }
                },
                text = presentation.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ConversationTopAppBarOverflowMenu(
    isAddPeopleVisible: Boolean,
    isArchiveVisible: Boolean,
    isUnarchiveVisible: Boolean,
    isAddContactVisible: Boolean,
    isDeleteConversationVisible: Boolean,
    isSimSelectorVisible: Boolean,
    simSelectorLabel: String,
    onAddPeopleClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onUnarchiveClick: () -> Unit,
    onAddContactClick: () -> Unit,
    onDeleteConversationClick: () -> Unit,
    onSimSelectorClick: () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(value = false) }

    IconButton(
        modifier = Modifier.testTag(CONVERSATION_OVERFLOW_BUTTON_TEST_TAG),
        onClick = { isExpanded = true },
    ) {
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = stringResource(id = R.string.action_more_options),
        )
    }

    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = {
            @Suppress("AssignedValueIsNeverRead")
            isExpanded = false
        },
    ) {
        val dismissAndInvoke: (() -> Unit) -> Unit = { action ->
            @Suppress("AssignedValueIsNeverRead")
            isExpanded = false
            action()
        }

        ConversationTopAppBarOverflowMenuItem(
            isVisible = isSimSelectorVisible,
            testTag = CONVERSATION_SIM_SELECTOR_MENU_ITEM_TEST_TAG,
            label = simSelectorLabel,
            icon = Icons.Rounded.SimCard,
            onClick = { dismissAndInvoke(onSimSelectorClick) },
        )

        ConversationTopAppBarOverflowMenuItem(
            isVisible = isAddPeopleVisible,
            testTag = CONVERSATION_ADD_PEOPLE_BUTTON_TEST_TAG,
            label = stringResource(id = R.string.conversation_add_people),
            icon = Icons.Rounded.GroupAdd,
            onClick = { dismissAndInvoke(onAddPeopleClick) },
        )

        ConversationTopAppBarOverflowMenuItem(
            isVisible = isAddContactVisible,
            testTag = CONVERSATION_ADD_CONTACT_BUTTON_TEST_TAG,
            label = stringResource(id = R.string.action_add_contact),
            icon = Icons.Rounded.PersonAdd,
            onClick = { dismissAndInvoke(onAddContactClick) },
        )

        ConversationTopAppBarOverflowMenuItem(
            isVisible = isArchiveVisible,
            testTag = CONVERSATION_ARCHIVE_BUTTON_TEST_TAG,
            label = stringResource(id = R.string.action_archive),
            icon = Icons.Rounded.Archive,
            onClick = { dismissAndInvoke(onArchiveClick) },
        )

        ConversationTopAppBarOverflowMenuItem(
            isVisible = isUnarchiveVisible,
            testTag = CONVERSATION_UNARCHIVE_BUTTON_TEST_TAG,
            label = stringResource(id = R.string.action_unarchive),
            icon = Icons.Rounded.Unarchive,
            onClick = { dismissAndInvoke(onUnarchiveClick) },
        )

        ConversationTopAppBarOverflowMenuItem(
            isVisible = isDeleteConversationVisible,
            testTag = CONVERSATION_DELETE_CONVERSATION_BUTTON_TEST_TAG,
            label = stringResource(id = R.string.action_delete),
            icon = Icons.Rounded.Delete,
            onClick = { dismissAndInvoke(onDeleteConversationClick) },
        )
    }
}

@Composable
private fun ConversationTopAppBarOverflowMenuItem(
    isVisible: Boolean,
    testTag: String,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    if (!isVisible) {
        return
    }

    DropdownMenuItem(
        modifier = Modifier.testTag(tag = testTag),
        text = {
            Text(text = label)
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun ConversationAvatar(
    avatar: ConversationMetadataUiState.Avatar,
) {
    when (avatar) {
        ConversationMetadataUiState.Avatar.Group -> {
            ConversationAvatarFallback(
                icon = Icons.Rounded.Group,
            )
        }

        is ConversationMetadataUiState.Avatar.Single -> {
            when {
                avatar.photoUri.isNullOrBlank() -> {
                    ConversationAvatarFallback(
                        icon = Icons.Rounded.Person,
                    )
                }

                else -> {
                    AsyncImage(
                        model = avatar.photoUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(size = CONVERSATION_TOP_APP_BAR_AVATAR_SIZE)
                            .clip(shape = CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationAvatarFallback(
    icon: ImageVector,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = CircleShape,
        modifier = Modifier.size(size = CONVERSATION_TOP_APP_BAR_AVATAR_SIZE),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(size = CONVERSATION_TOP_APP_BAR_AVATAR_ICON_SIZE),
            )
        }
    }
}

private fun conversationAvatar(
    metadata: ConversationMetadataUiState,
): ConversationMetadataUiState.Avatar {
    return when (metadata) {
        ConversationMetadataUiState.Loading -> {
            ConversationMetadataUiState.Avatar.Single(
                photoUri = null,
            )
        }

        ConversationMetadataUiState.Unavailable -> {
            ConversationMetadataUiState.Avatar.Single(
                photoUri = null,
            )
        }

        is ConversationMetadataUiState.Present -> metadata.avatar
    }
}

@Composable
private fun conversationTitle(
    metadata: ConversationMetadataUiState,
): String {
    return when (metadata) {
        ConversationMetadataUiState.Loading -> stringResource(id = R.string.app_name)

        ConversationMetadataUiState.Unavailable -> stringResource(id = R.string.app_name)

        is ConversationMetadataUiState.Present -> {
            metadata
                .title
                .takeIf { it.isNotBlank() }
                ?: stringResource(id = R.string.app_name)
        }
    }
}

@Composable
private fun conversationSubtitle(
    metadata: ConversationMetadataUiState,
): String? {
    return when (metadata) {
        ConversationMetadataUiState.Loading -> stringResource(id = R.string.loading_messages)

        ConversationMetadataUiState.Unavailable -> null

        is ConversationMetadataUiState.Present -> {
            when {
                shouldShowOneOnOneSubtitle(metadata = metadata) -> {
                    BidiFormatter
                        .getInstance()
                        .unicodeWrap(
                            metadata.otherParticipantDisplayDestination,
                            TextDirectionHeuristicsCompat.LTR,
                        )
                }

                metadata.participantCount > 1 -> {
                    pluralStringResource(
                        id = R.plurals.wearable_participant_count,
                        count = metadata.participantCount,
                        metadata.participantCount,
                    )
                }

                else -> null
            }
        }
    }
}

@Composable
private fun conversationSubtitleContentDescription(
    metadata: ConversationMetadataUiState,
): String? {
    return when (metadata) {
        ConversationMetadataUiState.Loading -> null
        ConversationMetadataUiState.Unavailable -> null
        is ConversationMetadataUiState.Present -> {
            metadata.otherParticipantDisplayDestination
                ?.takeIf {
                    shouldShowOneOnOneSubtitle(metadata = metadata) &&
                        metadata.otherParticipantPhoneNumber != null
                }
                ?.let { displayDestination ->
                    AccessibilityUtil.getVocalizedPhoneNumber(
                        LocalResources.current,
                        displayDestination,
                    )
                }
                ?.takeIf { it.isNotBlank() }
        }
    }
}

private fun shouldShowOneOnOneSubtitle(
    metadata: ConversationMetadataUiState.Present,
): Boolean {
    val displayDestination = metadata.otherParticipantDisplayDestination
        ?.takeIf { it.isNotBlank() }
        ?: return false

    return !displayDestination.equals(other = metadata.title, ignoreCase = false)
}

@Immutable
private data class ConversationTopAppBarPresentation(
    val title: String,
    val subtitle: String?,
    val subtitleContentDescription: String?,
    val avatar: ConversationMetadataUiState.Avatar,
)

@Preview(showBackground = true)
@Composable
private fun ConversationTopAppBarLoadingPreview() {
    AppTheme {
        ConversationTopAppBar(
            metadata = ConversationMetadataUiState.Loading,
            onAddPeopleClick = {},
            onTitleClick = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConversationTopAppBarLoadedPreview() {
    AppTheme {
        ConversationTopAppBar(
            metadata = ConversationMetadataUiState.Present(
                title = "Carol",
                selfParticipantId = "self",
                avatar = ConversationMetadataUiState.Avatar.Single(
                    photoUri = null,
                ),
                participantCount = 1,
                otherParticipantDisplayDestination = "(555) 123-4567",
                otherParticipantPhoneNumber = "+15551234567",
                otherParticipantContactLookupKey = null,
                isArchived = false,
                composerAvailability = ConversationComposerAvailability.editable(),
            ),
            isCallVisible = true,
            onAddPeopleClick = {},
            onCallClick = {},
            onTitleClick = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConversationTopAppBarGroupPreview() {
    AppTheme {
        ConversationTopAppBar(
            metadata = ConversationMetadataUiState.Present(
                title = "Carol, Dave, Erin",
                selfParticipantId = "self",
                avatar = ConversationMetadataUiState.Avatar.Group,
                participantCount = 3,
                otherParticipantDisplayDestination = null,
                otherParticipantPhoneNumber = null,
                otherParticipantContactLookupKey = null,
                isArchived = false,
                composerAvailability = ConversationComposerAvailability.editable(),
            ),
            isAddPeopleVisible = true,
            onAddPeopleClick = {},
            onTitleClick = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConversationTopAppBarWithSimSelectorPreview() {
    val subscriptions = persistentListOf(
        ConversationSubscription(
            selfParticipantId = "1",
            label = ConversationSubscriptionLabel.Named(name = "Verizon"),
            displayDestination = "+1 555-867-5309",
            displaySlotId = 1,
            color = 0xFF5E9BE8.toInt(),
        ),
        ConversationSubscription(
            selfParticipantId = "2",
            label = ConversationSubscriptionLabel.Named(name = "AT&T Business"),
            displayDestination = "+1 555-111-2222",
            displaySlotId = 2,
            color = 0xFFE97E6A.toInt(),
        ),
    )

    AppTheme {
        ConversationTopAppBar(
            metadata = ConversationMetadataUiState.Present(
                title = "Carol",
                selfParticipantId = "self",
                avatar = ConversationMetadataUiState.Avatar.Single(
                    photoUri = null,
                ),
                participantCount = 1,
                otherParticipantDisplayDestination = "(555) 123-4567",
                otherParticipantPhoneNumber = "+15551234567",
                otherParticipantContactLookupKey = null,
                isArchived = false,
                composerAvailability = ConversationComposerAvailability.editable(),
            ),
            isCallVisible = true,
            simSelector = ConversationSimSelectorUiState(
                subscriptions = subscriptions,
                selectedSubscription = subscriptions.first(),
            ),
            onAddPeopleClick = {},
            onCallClick = {},
            onSimSelectorClick = {},
            onTitleClick = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConversationTopAppBarOneOnOnePreview() {
    AppTheme {
        ConversationTopAppBar(
            metadata = ConversationMetadataUiState.Present(
                title = "Carol",
                selfParticipantId = "self",
                avatar = ConversationMetadataUiState.Avatar.Single(
                    photoUri = null,
                ),
                participantCount = 1,
                otherParticipantDisplayDestination = "(555) 123-4567",
                otherParticipantPhoneNumber = "+15551234567",
                otherParticipantContactLookupKey = null,
                isArchived = false,
                composerAvailability = ConversationComposerAvailability.editable(),
            ),
            isAddPeopleVisible = true,
            isCallVisible = true,
            onAddPeopleClick = {},
            onCallClick = {},
            onTitleClick = {},
            onNavigateBack = {},
        )
    }
}
