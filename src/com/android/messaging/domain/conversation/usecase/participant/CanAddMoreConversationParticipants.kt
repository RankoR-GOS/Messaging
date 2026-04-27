package com.android.messaging.domain.conversation.usecase.participant

import com.android.messaging.datamodel.data.ContactPickerData
import javax.inject.Inject

internal fun interface CanAddMoreConversationParticipants {
    operator fun invoke(participantCount: Int): Boolean
}

// TODO: Get rid of legacy ContactPickerData usage
internal class CanAddMoreConversationParticipantsImpl @Inject constructor() :
    CanAddMoreConversationParticipants {

    override operator fun invoke(participantCount: Int): Boolean {
        return ContactPickerData.getCanAddMoreParticipants(participantCount)
    }
}
