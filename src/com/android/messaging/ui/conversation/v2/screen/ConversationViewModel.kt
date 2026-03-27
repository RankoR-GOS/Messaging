package com.android.messaging.ui.conversation.v2.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.v2.composer.mapper.ConversationComposerUiStateMapper
import com.android.messaging.ui.conversation.v2.messages.delegate.ConversationMessagesDelegate
import com.android.messaging.ui.conversation.v2.metadata.delegate.ConversationMetadataDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class ConversationViewModel @Inject constructor(
    private val conversationDraftDelegate: ConversationDraftDelegate,
    private val conversationMessagesDelegate: ConversationMessagesDelegate,
    private val conversationMetadataDelegate: ConversationMetadataDelegate,
    private val conversationComposerUiStateMapper: ConversationComposerUiStateMapper,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val conversationIdFlow: StateFlow<String?> = savedStateHandle.getStateFlow(
        key = CONVERSATION_ID_KEY,
        initialValue = null,
    )

    val uiState: StateFlow<ConversationUiState> = combine(
        conversationMetadataDelegate.state,
        conversationMessagesDelegate.state,
        conversationDraftDelegate.state,
    ) { metadataState, messagesUiState, draft ->
        return@combine ConversationUiState(
            metadata = metadataState,
            messages = messagesUiState,
            composer = conversationComposerUiStateMapper.map(
                draft = draft,
                composerAvailability = metadataState.composerAvailability,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
        ),
        initialValue = ConversationUiState(),
    )

    init {
        initializeDelegates()
    }

    private fun initializeDelegates() {
        conversationDraftDelegate.bind(
            scope = viewModelScope,
            conversationIdFlow = conversationIdFlow,
        )
        conversationMessagesDelegate.bind(
            scope = viewModelScope,
            conversationIdFlow = conversationIdFlow,
        )
        conversationMetadataDelegate.bind(
            scope = viewModelScope,
            conversationIdFlow = conversationIdFlow,
        )
    }

    fun onConversationChanged(conversationId: String?) {
        if (conversationId != conversationIdFlow.value) {
            savedStateHandle[CONVERSATION_ID_KEY] = conversationId
        }
    }

    fun onMessageTextChanged(text: String) {
        conversationDraftDelegate.onMessageTextChanged(messageText = text)
    }

    fun onAttachmentClick() {
        // TODO
    }

    fun onSendClick() {
        // TODO
    }

    fun persistDraft() {
        conversationDraftDelegate.persistDraft()
    }

    override fun onCleared() {
        conversationDraftDelegate.flushDraft()

        super.onCleared()
    }

    private companion object {
        private const val CONVERSATION_ID_KEY = "conversation_id"
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
