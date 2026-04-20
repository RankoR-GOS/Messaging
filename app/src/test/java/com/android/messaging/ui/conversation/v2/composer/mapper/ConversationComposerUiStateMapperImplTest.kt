package com.android.messaging.ui.conversation.v2.composer.mapper

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.conversation.model.metadata.ConversationComposerDisabledReason
import com.android.messaging.data.conversation.model.metadata.ConversationSubscription
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.ui.conversation.v2.composer.model.ConversationDraftState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationComposerUiStateMapperImplTest {

    private val mapper = ConversationComposerUiStateMapperImpl()

    @Test
    fun map_enablesSendOnlyWhenContentIsAvailableAndDraftIsIdle() {
        val uiState = mapper.map(
            draftState = ConversationDraftState(
                draft = ConversationDraft(
                    messageText = "Hello",
                ),
            ),
            composerAvailability = ConversationComposerAvailability.editable(),
            subscriptions = persistentListOf(),
        )

        assertTrue(uiState.isSendEnabled)
    }

    @Test
    fun map_disablesSendWhenDraftIsEmpty() {
        val uiState = mapper.map(
            draftState = ConversationDraftState(
                draft = ConversationDraft(),
            ),
            composerAvailability = ConversationComposerAvailability.editable(),
            subscriptions = persistentListOf(),
        )

        assertFalse(uiState.isSendEnabled)
    }

    @Test
    fun map_disablesSendAndAttachmentWhenDraftIsBusy() {
        val uiState = mapper.map(
            draftState = ConversationDraftState(
                draft = ConversationDraft(
                    messageText = "Hello",
                    isCheckingDraft = true,
                    isSending = true,
                ),
            ),
            composerAvailability = ConversationComposerAvailability.editable(),
            subscriptions = persistentListOf(),
        )

        assertFalse(uiState.isAttachmentActionEnabled)
        assertFalse(uiState.isSendEnabled)
    }

    @Test
    fun map_keepsMessageFieldTiedToAvailabilityOnly() {
        val unavailableAvailability = ConversationComposerAvailability.unavailable(
            reason = ConversationComposerDisabledReason.CONVERSATION_UNAVAILABLE,
        )

        val unavailableUiState = mapper.map(
            draftState = ConversationDraftState(
                draft = ConversationDraft(
                    messageText = "Hello",
                    isCheckingDraft = true,
                    isSending = true,
                ),
            ),
            composerAvailability = unavailableAvailability,
            subscriptions = persistentListOf(),
        )
        val availableUiState = mapper.map(
            draftState = ConversationDraftState(
                draft = ConversationDraft(
                    messageText = "Hello",
                    isCheckingDraft = true,
                    isSending = true,
                ),
            ),
            composerAvailability = ConversationComposerAvailability.editable(),
            subscriptions = persistentListOf(),
        )

        assertFalse(unavailableUiState.isMessageFieldEnabled)
        assertTrue(availableUiState.isMessageFieldEnabled)
    }

    @Test
    fun map_selectsSubscriptionMatchingDraftSelfParticipantId() {
        val matchingSubscription = createSubscription(
            selfParticipantId = "sub-b",
            slotId = 2,
        )
        val subscriptions = persistentListOf(
            createSubscription(selfParticipantId = "sub-a", slotId = 1),
            matchingSubscription,
        )

        val uiState = mapper.map(
            draftState = ConversationDraftState(
                draft = ConversationDraft(
                    selfParticipantId = "sub-b",
                ),
            ),
            composerAvailability = ConversationComposerAvailability.editable(),
            subscriptions = subscriptions,
        )

        assertEquals(matchingSubscription, uiState.simSelector.selectedSubscription)
        assertEquals(subscriptions, uiState.simSelector.subscriptions)
        assertTrue(uiState.simSelector.isAvailable)
    }

    @Test
    fun map_fallsBackToFirstSubscriptionWhenDraftSelfParticipantIdDoesNotMatch() {
        val firstSubscription = createSubscription(
            selfParticipantId = "sub-a",
            slotId = 1,
        )
        val subscriptions = persistentListOf(
            firstSubscription,
            createSubscription(selfParticipantId = "sub-b", slotId = 2),
        )

        val uiState = mapper.map(
            draftState = ConversationDraftState(
                draft = ConversationDraft(
                    selfParticipantId = "non-existent",
                ),
            ),
            composerAvailability = ConversationComposerAvailability.editable(),
            subscriptions = subscriptions,
        )

        assertEquals(firstSubscription, uiState.simSelector.selectedSubscription)
    }

    @Test
    fun map_leavesSimSelectorUnavailableForSingleOrEmptySubscriptionList() {
        val emptyUiState = mapper.map(
            draftState = ConversationDraftState(
                draft = ConversationDraft(),
            ),
            composerAvailability = ConversationComposerAvailability.editable(),
            subscriptions = persistentListOf(),
        )
        val singleUiState = mapper.map(
            draftState = ConversationDraftState(
                draft = ConversationDraft(),
            ),
            composerAvailability = ConversationComposerAvailability.editable(),
            subscriptions = persistentListOf(
                createSubscription(selfParticipantId = "sub-a", slotId = 1),
            ),
        )

        assertFalse(emptyUiState.simSelector.isAvailable)
        assertNull(emptyUiState.simSelector.selectedSubscription)
        assertFalse(singleUiState.simSelector.isAvailable)
    }

    private fun createSubscription(
        selfParticipantId: String,
        slotId: Int,
    ): ConversationSubscription {
        return ConversationSubscription(
            selfParticipantId = selfParticipantId,
            label = ConversationSubscriptionLabel.Slot(slotId = slotId),
            displayDestination = null,
            displaySlotId = slotId,
            color = 0,
        )
    }
}
