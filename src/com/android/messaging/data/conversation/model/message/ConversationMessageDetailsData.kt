package com.android.messaging.data.conversation.model.message

import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.datamodel.data.ConversationParticipantsData
import com.android.messaging.datamodel.data.ParticipantData

internal data class ConversationMessageDetailsData(
    val message: ConversationMessageData,
    val participants: ConversationParticipantsData,
    val selfParticipant: ParticipantData?,
)
