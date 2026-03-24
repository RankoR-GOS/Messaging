package com.android.messaging.ui.conversation.v2.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class ConversationUiState(
    val metadata: ConversationMetadataUiState = ConversationMetadataUiState.Loading,
    val messages: ConversationMessagesUiState = ConversationMessagesUiState.Loading,
)
