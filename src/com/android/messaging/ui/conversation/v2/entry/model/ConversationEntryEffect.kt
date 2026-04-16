package com.android.messaging.ui.conversation.v2.entry.model

import com.android.messaging.ui.conversation.v2.navigation.RecipientPickerMode

internal sealed interface ConversationEntryEffect {

    data class NavigateToConversation(
        val conversationId: String,
    ) : ConversationEntryEffect

    data class NavigateToRecipientPicker(
        val mode: RecipientPickerMode,
    ) : ConversationEntryEffect

    data object NavigateBack : ConversationEntryEffect

    data class ShowMessage(
        val messageResId: Int,
    ) : ConversationEntryEffect
}
