package com.android.messaging.ui.conversation.v2.composer.model

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface ConversationComposerAttachmentUiState {
    val key: String
    val contentType: String
    val contentUri: String

    @Immutable
    data class Pending(
        override val key: String,
        override val contentType: String,
        override val contentUri: String,
        val displayName: String,
    ) : ConversationComposerAttachmentUiState

    @Immutable
    data class Resolved(
        override val key: String,
        override val contentType: String,
        override val contentUri: String,
        val captionText: String,
        val width: Int?,
        val height: Int?,
    ) : ConversationComposerAttachmentUiState
}
