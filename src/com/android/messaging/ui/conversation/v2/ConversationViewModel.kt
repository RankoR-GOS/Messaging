package com.android.messaging.ui.conversation.v2

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.conversation.v2.mapper.ConversationMessageUiModelMapper
import com.android.messaging.ui.conversation.v2.model.ConversationUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
internal class ConversationViewModel @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val conversationMessageUiModelMapper: ConversationMessageUiModelMapper,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConversationUiState>(ConversationUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private var loadConversationJob: Job? = null

    var conversationId: String? = null
        set(value) {
            if (value != field) {
                field = value
                loadConversation()
            }
    }

    private fun loadConversation() {
        val conversationId = conversationId ?: savedStateHandle[CONVERSATION_ID_KEY]

        if (conversationId == null) {
            _uiState.update { ConversationUiState.Present() }
            return
        }

        savedStateHandle[CONVERSATION_ID_KEY] = conversationId
        Log.d(LOG_TAG, "loadConversation: conversationId=$conversationId")
        _uiState.update { ConversationUiState.Loading }
        loadConversationJob?.cancel()

        loadConversationJob = viewModelScope.launch {
            conversationsRepository
                .getConversationMessages(conversationId = conversationId)
                .map { messages ->
                    withContext(context = defaultDispatcher) {
                        messages.mapNotNull { message ->
                            conversationMessageUiModelMapper.map(data = message)
                        }
                    }
                }
                .collect { messages ->
                    Log.d(LOG_TAG, "Messages loaded: count=${messages.size}")
                    _uiState.update {
                        ConversationUiState.Present(
                            messages = messages,
                        )
                    }
                }
        }
    }

    private companion object {
        private const val CONVERSATION_ID_KEY = "conversation_id"
        private const val LOG_TAG = "ConversationViewModel"
    }
}
