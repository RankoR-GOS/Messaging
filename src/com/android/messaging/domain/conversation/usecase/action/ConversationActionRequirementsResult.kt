package com.android.messaging.domain.conversation.usecase.action

internal sealed interface ConversationActionRequirementsResult {
    data object Ready : ConversationActionRequirementsResult

    data object SmsNotCapable : ConversationActionRequirementsResult

    data object NoPreferredSmsSim : ConversationActionRequirementsResult

    data object MissingDefaultSmsRole : ConversationActionRequirementsResult
}
