package com.android.messaging.ui.conversation.v2.messages.model.attachment

import androidx.compose.runtime.Immutable

@Immutable
internal data class ConversationVCardAttachmentUiModel(
    val type: ConversationVCardAttachmentType,
    val titleText: String? = null,
    val titleTextResId: Int? = null,
    val subtitleText: String? = null,
    val subtitleTextResId: Int? = null,
)

@Immutable
internal enum class ConversationVCardAttachmentType {
    CONTACT,
    LOCATION,
}
