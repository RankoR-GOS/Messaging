package com.android.messaging.ui.conversation.v2.messages.model.attachment

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface ConversationInlineAttachment {
    val key: String
    val openAction: ConversationAttachmentOpenAction?

    @Immutable
    data class Audio(
        override val key: String,
        val contentUri: String,
        override val openAction: ConversationAttachmentOpenAction?,
        val titleText: String?,
        val titleTextResId: Int?,
    ) : ConversationInlineAttachment

    @Immutable
    data class File(
        override val key: String,
        override val openAction: ConversationAttachmentOpenAction?,
        val subtitleTextResId: Int?,
        val titleText: String?,
        val titleTextResId: Int?,
    ) : ConversationInlineAttachment

    @Immutable
    data class VCard(
        override val key: String,
        val contentUri: String,
        override val openAction: ConversationAttachmentOpenAction?,
        val type: ConversationVCardAttachmentType,
        val titleText: String?,
        val titleTextResId: Int?,
        val subtitleText: String?,
        val subtitleTextResId: Int?,
    ) : ConversationInlineAttachment
}
