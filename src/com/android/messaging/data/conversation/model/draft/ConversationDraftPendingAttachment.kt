package com.android.messaging.data.conversation.model.draft

internal data class ConversationDraftPendingAttachment(
    val pendingAttachmentId: String,
    val contentUri: String,
    val contentType: String,
    val displayName: String = "",
    val kind: ConversationDraftPendingAttachmentKind =
        ConversationDraftPendingAttachmentKind.Generic,
)

internal enum class ConversationDraftPendingAttachmentKind {
    Generic,
    AudioFinalizing,
}
