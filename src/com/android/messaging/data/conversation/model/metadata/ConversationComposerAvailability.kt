package com.android.messaging.data.conversation.model.metadata

internal data class ConversationComposerAvailability(
    val isMessageFieldEnabled: Boolean,
    val isAttachmentActionEnabled: Boolean,
    val isSendAvailable: Boolean,
    val disabledReason: ConversationComposerDisabledReason?,
) {

    companion object {
        fun editable(): ConversationComposerAvailability {
            return ConversationComposerAvailability(
                isMessageFieldEnabled = true,
                isAttachmentActionEnabled = true,
                isSendAvailable = true,
                disabledReason = null,
            )
        }

        fun unavailable(
            reason: ConversationComposerDisabledReason,
        ): ConversationComposerAvailability {
            return ConversationComposerAvailability(
                isMessageFieldEnabled = false,
                isAttachmentActionEnabled = false,
                isSendAvailable = false,
                disabledReason = reason,
            )
        }
    }
}
