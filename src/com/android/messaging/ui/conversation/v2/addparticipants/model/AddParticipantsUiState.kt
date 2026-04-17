package com.android.messaging.ui.conversation.v2.addparticipants.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import com.android.messaging.ui.conversation.v2.recipientpicker.model.RecipientPickerUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class AddParticipantsUiState(
    val existingParticipants: ImmutableList<ConversationRecipient> = persistentListOf(),
    val isLoadingConversationParticipants: Boolean = true,
    val isResolvingConversation: Boolean = false,
    val recipientPickerUiState: RecipientPickerUiState = RecipientPickerUiState(),
    val selectedRecipientDestinations: ImmutableList<String> = persistentListOf(),
)
