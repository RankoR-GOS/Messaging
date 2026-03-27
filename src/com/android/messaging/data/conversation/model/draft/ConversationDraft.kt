package com.android.messaging.data.conversation.model.draft

internal data class ConversationDraft(
    val messageText: String = "",
    val subjectText: String = "",
    val selfParticipantId: String = "",
    val attachments: List<ConversationDraftAttachment> = emptyList(),
    val isCheckingDraft: Boolean = false,
    val isSending: Boolean = false,
    val messageCount: Int = 1,
    val codePointsRemainingInCurrentMessage: Int = 0,
) {
    val hasContent: Boolean
        get() = messageText.isNotBlank() ||
            subjectText.isNotBlank() ||
            attachments.isNotEmpty()

    val isMms: Boolean
        get() = subjectText.isNotBlank() || attachments.isNotEmpty()
}
