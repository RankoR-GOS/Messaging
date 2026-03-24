package com.android.messaging.ui.conversation.v2.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
internal sealed interface ConversationUiState {

    data object Loading : ConversationUiState

    @Immutable
    data class Present(
        val conversationName: String = "",
        val selfParticipantId: String = "",
        val isGroupConversation: Boolean = false,
        val messages: List<ConversationMessageUiModel> = emptyList(),
        // TODO: Draft

    ) : ConversationUiState
}