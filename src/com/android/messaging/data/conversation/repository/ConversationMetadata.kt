package com.android.messaging.data.conversation.repository

internal data class ConversationMetadata(
    val conversationName: String,
    val selfParticipantId: String,
    val isGroupConversation: Boolean,
    val participantCount: Int,
)
