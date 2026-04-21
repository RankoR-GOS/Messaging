package com.android.messaging.ui.conversation.v2.messages.model.attachment

import androidx.compose.runtime.Immutable

@Immutable
internal data class ConversationVCardAttachmentUiState(
    val type: ConversationVCardAttachmentType,
    val title: String,
    val subtitle: String?,
)

@Immutable
internal enum class ConversationVCardAttachmentType {
    CONTACT,
    LOCATION,
}
