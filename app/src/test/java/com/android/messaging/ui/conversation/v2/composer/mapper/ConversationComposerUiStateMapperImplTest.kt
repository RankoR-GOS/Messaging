package com.android.messaging.ui.conversation.v2.composer.mapper

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.conversation.model.metadata.ConversationComposerDisabledReason
import com.android.messaging.ui.conversation.v2.composer.model.ConversationDraftState
import org.junit.Assert.assertFalse
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
        )

        assertFalse(unavailableUiState.isMessageFieldEnabled)
        assertTrue(availableUiState.isMessageFieldEnabled)
    }
}
