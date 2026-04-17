@file:OptIn(
    ExperimentalMaterial3Api::class,
)

package com.android.messaging.ui.conversation.v2.entry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.messaging.R
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import com.android.messaging.ui.conversation.v2.NEW_CHAT_CONTACT_RESOLVING_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.v2.NEW_CHAT_CREATE_GROUP_NEXT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.newChatContactRowTestTag
import com.android.messaging.ui.conversation.v2.recipientpicker.RecipientPickerModel
import com.android.messaging.ui.conversation.v2.recipientpicker.RecipientPickerViewModel
import com.android.messaging.ui.conversation.v2.recipientpicker.RecipientSelectionContent
import com.android.messaging.ui.conversation.v2.recipientpicker.RecipientSelectionContentUiState
import com.android.messaging.ui.conversation.v2.recipientpicker.RecipientSelectionPrimaryActionUiState
import com.android.messaging.ui.conversation.v2.recipientpicker.RecipientSelectionRowDecorators
import com.android.messaging.ui.conversation.v2.recipientpicker.RecipientSelectionStrings
import com.android.messaging.ui.conversation.v2.recipientpicker.model.RecipientPickerUiState
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet

@Composable
internal fun NewChatScreen(
    modifier: Modifier = Modifier,
    isCreatingGroup: Boolean = false,
    isResolvingConversation: Boolean = false,
    isResolvingConversationIndicatorVisible: Boolean = false,
    onContactClick: (String) -> Unit = {},
    onContactLongClick: (String) -> Unit = {},
    onCreateGroupClick: () -> Unit = {},
    onCreateGroupConfirmed: () -> Unit = {},
    onCreateGroupRecipientClick: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    pickerModel: RecipientPickerModel = hiltViewModel<RecipientPickerViewModel>(),
    resolvingRecipientDestination: String? = null,
    selectedGroupRecipientDestinations: ImmutableList<String> = persistentListOf(),
) {
    val uiState by pickerModel.uiState.collectAsStateWithLifecycle()
    val screenContainerColor = MaterialTheme.colorScheme.surfaceVariant

    Scaffold(
        modifier = modifier,
        containerColor = screenContainerColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = screenContainerColor,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
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
                title = {
                    Text(text = newChatTitle(isCreatingGroup = isCreatingGroup))
                },
            )
        },
    ) { contentPadding ->
        NewChatRecipientSelectionContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = contentPadding),
            pickerUiState = uiState,
            isCreatingGroup = isCreatingGroup,
            isResolvingConversation = isResolvingConversation,
            isResolvingConversationIndicatorVisible = isResolvingConversationIndicatorVisible,
            resolvingRecipientDestination = resolvingRecipientDestination,
            selectedGroupRecipientDestinations = selectedGroupRecipientDestinations,
            onLoadMore = pickerModel::onLoadMore,
            onQueryChanged = pickerModel::onQueryChanged,
            onContactClick = onContactClick,
            onContactLongClick = onContactLongClick,
            onCreateGroupClick = onCreateGroupClick,
            onCreateGroupConfirmed = onCreateGroupConfirmed,
            onCreateGroupRecipientClick = onCreateGroupRecipientClick,
        )
    }
}

@Composable
private fun NewChatRecipientSelectionContent(
    pickerUiState: RecipientPickerUiState,
    isCreatingGroup: Boolean,
    isResolvingConversation: Boolean,
    isResolvingConversationIndicatorVisible: Boolean,
    resolvingRecipientDestination: String?,
    selectedGroupRecipientDestinations: ImmutableList<String>,
    onLoadMore: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onContactClick: (String) -> Unit,
    onContactLongClick: (String) -> Unit,
    onCreateGroupClick: () -> Unit,
    onCreateGroupConfirmed: () -> Unit,
    onCreateGroupRecipientClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryAction = when {
        isCreatingGroup && selectedGroupRecipientDestinations.isNotEmpty() -> {
            RecipientSelectionPrimaryActionUiState(
                text = stringResource(id = R.string.next),
                isEnabled = !pickerUiState.isLoading && !isResolvingConversation,
                isLoading = isResolvingConversationIndicatorVisible,
                testTag = NEW_CHAT_CREATE_GROUP_NEXT_BUTTON_TEST_TAG,
            )
        }

        else -> null
    }

    RecipientSelectionContent(
        uiState = RecipientSelectionContentUiState(
            picker = pickerUiState,
            primaryAction = primaryAction,
            selectedRecipientDestinations = when {
                isCreatingGroup -> selectedGroupRecipientDestinations.toImmutableSet()
                else -> persistentSetOf()
            },
            isQueryEnabled = !isResolvingConversation,
        ),
        strings = RecipientSelectionStrings(
            queryPrefixText = stringResource(id = R.string.new_chat_recipient_prefix),
            queryPlaceholderText = stringResource(id = R.string.new_chat_query_hint),
        ),
        rowDecorators = RecipientSelectionRowDecorators(
            recipientRowTestTag = { contact ->
                newChatContactRowTestTag(contactId = contact.id)
            },
            showRecipientTrailingIndicator = { contact ->
                !isCreatingGroup &&
                    isResolvingConversationIndicatorVisible &&
                    resolvingRecipientDestination == contact.destination
            },
            trailingIndicatorTestTag = NEW_CHAT_CONTACT_RESOLVING_INDICATOR_TEST_TAG,
        ),
        onRecipientClick = { contact ->
            when {
                isCreatingGroup -> {
                    onCreateGroupRecipientClick(contact.destination)
                }

                else -> {
                    onContactClick(contact.destination)
                }
            }
        },
        modifier = modifier,
        onLoadMore = onLoadMore,
        onPrimaryActionClick = onCreateGroupConfirmed,
        onQueryChanged = onQueryChanged,
        onRecipientLongClick = { contact ->
            when {
                isCreatingGroup -> {
                    onCreateGroupRecipientClick(contact.destination)
                }

                else -> {
                    onContactLongClick(contact.destination)
                }
            }
        },
        topListContent = {
            AnimatedVisibility(
                visible = !isCreatingGroup,
                enter = newGroupButtonEnterTransition(),
                exit = newGroupButtonExitTransition(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(space = 12.dp),
                ) {
                    NewGroupButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onCreateGroupClick,
                    )
                    Spacer(modifier = Modifier.height(height = 12.dp))
                }
            }
        },
    )
}

@Composable
private fun newChatTitle(
    isCreatingGroup: Boolean,
): String {
    return when {
        isCreatingGroup -> stringResource(id = R.string.conversation_new_group)
        else -> stringResource(id = R.string.start_new_conversation)
    }
}

@Composable
private fun NewGroupButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current

    FilledTonalButton(
        modifier = modifier,
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(size = 18.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(
                alpha = 0.5f,
            ),
            disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                alpha = 0.5f,
            ),
        ),
    ) {
        Icon(
            imageVector = Icons.Rounded.Group,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.size(size = 8.dp))
        Text(text = stringResource(id = R.string.conversation_new_group))
    }
}

private fun newGroupButtonEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = newChatDefaultEffectsAnimationSpec(),
    ) + slideInVertically(
        animationSpec = newChatSpatialAnimationSpec(),
        initialOffsetY = { fullHeight ->
            -fullHeight / 4
        },
    )
}

private fun newGroupButtonExitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = newChatFastEffectsAnimationSpec(),
    ) + shrinkVertically(
        animationSpec = newChatSpatialAnimationSpec(),
        shrinkTowards = androidx.compose.ui.Alignment.Top,
    )
}

private fun <T> newChatDefaultEffectsAnimationSpec(): FiniteAnimationSpec<T> {
    return tween(
        durationMillis = 200,
        easing = LinearOutSlowInEasing,
    )
}

private fun <T> newChatFastEffectsAnimationSpec(): FiniteAnimationSpec<T> {
    return tween(
        durationMillis = 150,
        easing = FastOutSlowInEasing,
    )
}

private fun <T> newChatSpatialAnimationSpec(): FiniteAnimationSpec<T> {
    return spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}

private fun previewContacts(): List<ConversationRecipient> {
    return listOf(
        ConversationRecipient(
            id = "1",
            displayName = "Ada Lovelace",
            destination = "+1 555 0100",
            secondaryText = "+1 555 0100",
        ),
        ConversationRecipient(
            id = "2",
            displayName = "Grace Hopper",
            destination = "+1 555 0101",
            secondaryText = "+1 555 0101",
        ),
        ConversationRecipient(
            id = "3",
            displayName = "Katherine Johnson",
            destination = "+1 555 0102",
            secondaryText = "+1 555 0102",
        ),
    )
}

@Preview(
    name = "Contacts",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
private fun NewChatScreenContactsPreview() {
    val contacts = previewContacts()

    AppTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            NewChatRecipientSelectionContent(
                modifier = Modifier.fillMaxSize(),
                pickerUiState = RecipientPickerUiState(
                    contacts = contacts.toImmutableList(),
                ),
                isCreatingGroup = false,
                isResolvingConversation = false,
                isResolvingConversationIndicatorVisible = false,
                resolvingRecipientDestination = null,
                selectedGroupRecipientDestinations = persistentListOf(),
                onLoadMore = {},
                onQueryChanged = {},
                onContactClick = {},
                onContactLongClick = {},
                onCreateGroupClick = {},
                onCreateGroupConfirmed = {},
                onCreateGroupRecipientClick = {},
            )
        }
    }
}

@Preview(
    name = "Create Group Selection",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
private fun NewChatScreenCreateGroupPreview() {
    val contacts = previewContacts()

    AppTheme {
        NewChatRecipientSelectionContent(
            modifier = Modifier.fillMaxSize(),
            pickerUiState = RecipientPickerUiState(
                contacts = contacts.toImmutableList(),
            ),
            isCreatingGroup = true,
            isResolvingConversation = false,
            isResolvingConversationIndicatorVisible = false,
            resolvingRecipientDestination = null,
            selectedGroupRecipientDestinations = persistentListOf(
                contacts[0].destination,
                contacts[2].destination,
            ),
            onLoadMore = {},
            onQueryChanged = {},
            onContactClick = {},
            onContactLongClick = {},
            onCreateGroupClick = {},
            onCreateGroupConfirmed = {},
            onCreateGroupRecipientClick = {},
        )
    }
}

@Preview(
    name = "Resolving Conversation",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
private fun NewChatScreenResolvingPreview() {
    val contacts = previewContacts()

    AppTheme {
        NewChatRecipientSelectionContent(
            modifier = Modifier.fillMaxSize(),
            pickerUiState = RecipientPickerUiState(
                query = "Ada",
                contacts = contacts.take(2).toImmutableList(),
            ),
            isCreatingGroup = false,
            isResolvingConversation = true,
            isResolvingConversationIndicatorVisible = true,
            resolvingRecipientDestination = contacts[0].destination,
            selectedGroupRecipientDestinations = persistentListOf(),
            onLoadMore = {},
            onQueryChanged = {},
            onContactClick = {},
            onContactLongClick = {},
            onCreateGroupClick = {},
            onCreateGroupConfirmed = {},
            onCreateGroupRecipientClick = {},
        )
    }
}
