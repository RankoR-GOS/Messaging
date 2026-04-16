package com.android.messaging.ui.conversation.v2.entry.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversation.model.draft.ConversationDraft

@Immutable
internal data class ConversationEntryUiState(
    val launchGeneration: Int? = null,
    val conversationId: String? = null,
    val isResolvingConversation: Boolean = false,
    val isResolvingConversationIndicatorVisible: Boolean = false,
    val pendingDraft: ConversationDraft? = null,
    val pendingStartupAttachment: ConversationEntryStartupAttachment? = null,
    val resolvingRecipientDestination: String? = null,
)

@Immutable
internal data class ConversationEntryStartupAttachment(
    val contentType: String,
    val contentUri: String,
)
