@file:OptIn(ExperimentalMaterial3Api::class)

package com.android.messaging.ui.conversation.v2.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.android.messaging.R
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import com.android.messaging.ui.conversation.v2.NEW_CHAT_CONTACT_RESOLVING_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.v2.recipientpicker.RecipientPickerModel
import com.android.messaging.ui.conversation.v2.recipientpicker.RecipientPickerViewModel
import com.android.messaging.ui.conversation.v2.recipientpicker.model.RecipientPickerUiState
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.persistentListOf

private val CONTACT_CORNER_RADIUS = 18.dp
private val CONTACT_MIDDLE_CORNER_RADIUS = 2.dp

private val SearchFieldShape = RoundedCornerShape(size = 22.dp)

private val TopContactShape = RoundedCornerShape(
    topStart = CONTACT_CORNER_RADIUS,
    topEnd = CONTACT_CORNER_RADIUS,
    bottomStart = CONTACT_MIDDLE_CORNER_RADIUS,
    bottomEnd = CONTACT_MIDDLE_CORNER_RADIUS,
)
private val BottomContactShape = RoundedCornerShape(
    topStart = CONTACT_MIDDLE_CORNER_RADIUS,
    topEnd = CONTACT_MIDDLE_CORNER_RADIUS,
    bottomStart = CONTACT_CORNER_RADIUS,
    bottomEnd = CONTACT_CORNER_RADIUS,
)
private val MiddleContactShape = RoundedCornerShape(size = CONTACT_MIDDLE_CORNER_RADIUS)
private val SingleContactShape = RoundedCornerShape(size = CONTACT_CORNER_RADIUS)

private const val CONTACTS_LOAD_MORE_THRESHOLD = 10
private const val NEW_CHAT_CONTACT_CONTENT_TYPE = "new_chat_contact"

@Composable
internal fun NewChatScreen(
    modifier: Modifier = Modifier,
    isResolvingConversation: Boolean = false,
    isResolvingConversationIndicatorVisible: Boolean = false,
    onContactClick: (String) -> Unit = {},
    onCreateGroupClick: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    pickerModel: RecipientPickerModel = hiltViewModel<RecipientPickerViewModel>(),
    resolvingRecipientDestination: String? = null,
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
                    Text(text = stringResource(id = R.string.start_new_conversation))
                },
            )
        },
    ) { contentPadding ->
        NewChatScreenContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = contentPadding),
            uiState = uiState,
            isResolvingConversation = isResolvingConversation,
            isResolvingConversationIndicatorVisible = isResolvingConversationIndicatorVisible,
            onContactClick = onContactClick,
            onCreateGroupClick = onCreateGroupClick,
            onLoadMore = pickerModel::onLoadMore,
            onQueryChanged = pickerModel::onQueryChanged,
            resolvingRecipientDestination = resolvingRecipientDestination,
        )
    }
}

@Composable
private fun NewChatScreenContent(
    uiState: RecipientPickerUiState,
    modifier: Modifier = Modifier,
    isResolvingConversation: Boolean = false,
    isResolvingConversationIndicatorVisible: Boolean = false,
    onContactClick: (String) -> Unit,
    onCreateGroupClick: () -> Unit,
    onLoadMore: () -> Unit,
    onQueryChanged: (String) -> Unit,
    resolvingRecipientDestination: String? = null,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        NewChatScreenBody(
            uiState = uiState,
            isResolvingConversation = isResolvingConversation,
            isResolvingConversationIndicatorVisible = isResolvingConversationIndicatorVisible,
            onContactClick = onContactClick,
            onCreateGroupClick = onCreateGroupClick,
            onLoadMore = onLoadMore,
            onQueryChanged = onQueryChanged,
            resolvingRecipientDestination = resolvingRecipientDestination,
        )
    }
}

@Composable
private fun NewChatScreenBody(
    uiState: RecipientPickerUiState,
    isResolvingConversation: Boolean,
    isResolvingConversationIndicatorVisible: Boolean,
    onContactClick: (String) -> Unit,
    onCreateGroupClick: () -> Unit,
    onLoadMore: () -> Unit,
    onQueryChanged: (String) -> Unit,
    resolvingRecipientDestination: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(height = 16.dp))

        NewChatQueryField(
            query = uiState.query,
            enabled = !isResolvingConversation,
            onQueryChanged = onQueryChanged,
        )

        Spacer(modifier = Modifier.height(height = 12.dp))

        NewChatContactsContent(
            modifier = Modifier.fillMaxSize(),
            uiState = uiState,
            contactSelectionEnabled = !isResolvingConversation,
            isResolvingConversationIndicatorVisible = isResolvingConversationIndicatorVisible,
            onContactClick = onContactClick,
            onCreateGroupClick = onCreateGroupClick,
            onLoadMore = onLoadMore,
            resolvingRecipientDestination = resolvingRecipientDestination,
        )
    }
}

@Composable
private fun NewChatQueryField(
    query: String,
    enabled: Boolean,
    onQueryChanged: (String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    TextField(
        modifier = Modifier
            .fillMaxWidth(),
        value = query,
        onValueChange = onQueryChanged,
        enabled = enabled,
        singleLine = true,
        shape = SearchFieldShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colorScheme.surface,
            unfocusedContainerColor = colorScheme.surface,
            disabledContainerColor = colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = colorScheme.onSurface,
            unfocusedTextColor = colorScheme.onSurface,
            disabledTextColor = colorScheme.onSurface,
            focusedPlaceholderColor = colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = colorScheme.onSurfaceVariant,
            disabledPlaceholderColor = colorScheme.onSurfaceVariant,
            focusedPrefixColor = colorScheme.onSurfaceVariant,
            unfocusedPrefixColor = colorScheme.onSurfaceVariant,
            disabledPrefixColor = colorScheme.onSurfaceVariant,
        ),
        prefix = {
            Text(
                modifier = Modifier
                    .padding(end = 12.dp),
                text = stringResource(id = R.string.new_chat_recipient_prefix),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        placeholder = {
            Text(
                text = stringResource(id = R.string.new_chat_query_hint),
            )
        },
    )
}

@Composable
private fun NewChatContactsContent(
    modifier: Modifier = Modifier,
    uiState: RecipientPickerUiState,
    contactSelectionEnabled: Boolean,
    isResolvingConversationIndicatorVisible: Boolean,
    onContactClick: (String) -> Unit,
    onCreateGroupClick: () -> Unit,
    onLoadMore: () -> Unit,
    resolvingRecipientDestination: String?,
) {
    val contacts = uiState.contacts
    val lastContactIndex = contacts.lastIndex
    val listState = rememberLazyListState()

    LaunchedEffect(
        listState,
        uiState.canLoadMore,
        uiState.isLoading,
        uiState.isLoadingMore,
        contacts.size,
    ) {
        snapshotFlow {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisibleIndex >= lastContactIndex - CONTACTS_LOAD_MORE_THRESHOLD
        }.collect { shouldLoadMore ->
            val isLoading = uiState.isLoading || uiState.isLoadingMore
            if (shouldLoadMore && uiState.canLoadMore && !isLoading) {
                onLoadMore()
            }
        }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            NewGroupButton(
                modifier = Modifier
                    .fillMaxWidth(),
                enabled = true,
                onClick = onCreateGroupClick,
            )
        }

        item {
            Spacer(modifier = Modifier.height(height = 12.dp))
        }

        when {
            uiState.isLoading -> {
                item {
                    NewChatLoadingState()
                }
            }

            uiState.contacts.isEmpty() || !uiState.hasContactsPermission -> {
                item {
                    NewChatEmptyState()
                }
            }

            else -> {
                itemsIndexed(
                    items = contacts,
                    key = { _, contact -> contact.id },
                    contentType = { _, _ ->
                        NEW_CHAT_CONTACT_CONTENT_TYPE
                    },
                ) { index, contact ->
                    val bottomPadding = when {
                        index == lastContactIndex -> 0.dp
                        else -> 2.dp
                    }

                    NewChatContactRow(
                        modifier = Modifier
                            .padding(bottom = bottomPadding),
                        contact = contact,
                        shape = newChatContactRowShape(
                            index = index,
                            totalCount = contacts.size,
                        ),
                        enabled = contactSelectionEnabled,
                        onContactClick = onContactClick,
                        showResolvingIndicator = isResolvingConversationIndicatorVisible &&
                            resolvingRecipientDestination == contact.destination,
                    )
                }
            }
        }

        if (uiState.isLoadingMore) {
            item {
                NewChatLoadingMoreState()
            }
        }
    }
}

@Composable
private fun NewChatLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NewChatLoadingMoreState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size = 20.dp),
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun NewChatEmptyState() {
    Text(
        text = stringResource(id = R.string.contact_list_empty_text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 24.dp, horizontal = 4.dp),
    )
}

@Composable
private fun NewGroupButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current

    FilledTonalButton(
        modifier = modifier,
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
        enabled = enabled,
        shape = RoundedCornerShape(size = 18.dp),
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

@Composable
private fun NewChatContactRow(
    modifier: Modifier = Modifier,
    contact: ConversationRecipient,
    shape: RoundedCornerShape,
    enabled: Boolean,
    onContactClick: (String) -> Unit,
    showResolvingIndicator: Boolean,
) {
    val hapticFeedback = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .then(other = modifier)
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = shape,
            )
            .clickable(
                enabled = enabled,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onContactClick(contact.destination)
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NewChatContactAvatar(contact = contact)

        Column(
            modifier = Modifier
                .padding(start = 14.dp)
                .weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 2.dp),
        ) {
            Text(
                text = contact.displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )

            contact.secondaryText?.let { secondaryText ->
                Text(
                    text = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (showResolvingIndicator) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(size = 20.dp)
                    .testTag(NEW_CHAT_CONTACT_RESOLVING_INDICATOR_TEST_TAG),
                strokeWidth = 2.dp,
            )
        }
    }
}

private fun newChatContactRowShape(
    index: Int,
    totalCount: Int,
): RoundedCornerShape {
    return when {
        totalCount <= 1 -> SingleContactShape
        index == 0 -> TopContactShape
        index == totalCount - 1 -> BottomContactShape
        else -> MiddleContactShape
    }
}

@Composable
private fun NewChatContactAvatar(
    contact: ConversationRecipient,
) {
    return when {
        contact.photoUri == null -> {
            NewChatContactTextAvatar(
                contact = contact,
            )
        }
        else -> {
            AsyncImage(
                model = contact.photoUri,
                contentDescription = contact.displayName,
                modifier = Modifier
                    .size(size = 40.dp)
                    .clip(shape = CircleShape),
            )
        }
    }
}

@Composable
private fun NewChatContactTextAvatar(
    modifier: Modifier = Modifier,
    contact: ConversationRecipient,
) {
    val label = remember(contact.displayName, contact.destination) {
        contactAvatarLabel(contact = contact)
    }

    Box(
        modifier = modifier
            .size(size = 40.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

private fun contactAvatarLabel(contact: ConversationRecipient): String {
    val labelSource = contact.displayName.ifBlank { contact.destination }
    val firstCharacter = labelSource.firstOrNull() ?: '?'

    return firstCharacter.uppercaseChar().toString()
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

@Composable
private fun NewChatScreenPreviewContent(
    uiState: RecipientPickerUiState,
    isResolvingConversation: Boolean = false,
    isResolvingConversationIndicatorVisible: Boolean = false,
    resolvingRecipientDestination: String? = null,
) {
    AppTheme {
        NewChatScreenContent(
            modifier = Modifier.fillMaxSize(),
            uiState = uiState,
            isResolvingConversation = isResolvingConversation,
            isResolvingConversationIndicatorVisible = isResolvingConversationIndicatorVisible,
            onContactClick = {},
            onCreateGroupClick = {},
            onLoadMore = {},
            onQueryChanged = {},
            resolvingRecipientDestination = resolvingRecipientDestination,
        )
    }
}

@Preview(
    name = "Contacts",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
private fun NewChatScreenContactsPreview() {
    val contacts = previewContacts()

    NewChatScreenPreviewContent(
        uiState = RecipientPickerUiState(
            contacts = persistentListOf(
                contacts[0],
                contacts[1],
                contacts[2],
            ),
        ),
    )
}

@Preview(
    name = "Single Filtered Result",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
private fun NewChatScreenFilteredResultPreview() {
    val contacts = previewContacts()

    NewChatScreenPreviewContent(
        uiState = RecipientPickerUiState(
            query = "Ada",
            contacts = persistentListOf(contacts[0]),
        ),
    )
}

@Preview(
    name = "Loading",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
private fun NewChatScreenLoadingPreview() {
    NewChatScreenPreviewContent(
        uiState = RecipientPickerUiState(
            isLoading = true,
        ),
    )
}

@Preview(
    name = "Empty Filter Result",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
private fun NewChatScreenEmptyPreview() {
    NewChatScreenPreviewContent(
        uiState = RecipientPickerUiState(
            query = "Unknown contact",
        ),
    )
}

@Preview(
    name = "No Contacts Permission",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
private fun NewChatScreenNoPermissionPreview() {
    NewChatScreenPreviewContent(
        uiState = RecipientPickerUiState(
            hasContactsPermission = false,
        ),
    )
}

@Preview(
    name = "Resolving Conversation",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
private fun NewChatScreenResolvingPreview() {
    val contacts = previewContacts()

    NewChatScreenPreviewContent(
        uiState = RecipientPickerUiState(
            query = "Ada",
            contacts = persistentListOf(
                contacts[0],
                contacts[1],
            ),
        ),
        isResolvingConversation = true,
        isResolvingConversationIndicatorVisible = true,
        resolvingRecipientDestination = contacts[0].destination,
    )
}
