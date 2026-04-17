package com.android.messaging.ui.conversation.v2.addparticipants.model

internal sealed interface AddParticipantsEffect {

    data class NavigateToConversation(
        val conversationId: String,
    ) : AddParticipantsEffect

    data class ShowMessage(
        val messageResId: Int,
    ) : AddParticipantsEffect
}
