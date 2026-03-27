package com.android.messaging.ui.conversation.v2.composer.delegate

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.repository.ConversationDraftsRepository
import com.android.messaging.di.core.ApplicationCoroutineScope
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.conversation.v2.common.ConversationScreenDelegate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal interface ConversationDraftDelegate : ConversationScreenDelegate<ConversationDraft> {

    fun onMessageTextChanged(messageText: String)

    fun persistDraft()

    fun flushDraft()
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
internal class ConversationDraftDelegateImpl @Inject constructor(
    @param:ApplicationCoroutineScope
    private val applicationScope: CoroutineScope,
    private val conversationDraftsRepository: ConversationDraftsRepository,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ConversationDraftDelegate {

    private val _state = MutableStateFlow(ConversationDraft())
    override val state = _state.asStateFlow()

    private val draftEditorState = MutableStateFlow(DraftEditorState())
    private val draftSaveMutex = Mutex()

    private var boundScope: CoroutineScope? = null

    override fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<String?>,
    ) {
        if (boundScope != null) {
            return
        }

        boundScope = scope

        bindConversationDraftObservation(
            scope = scope,
            conversationIdFlow = conversationIdFlow,
        )
        bindDraftAutosave(scope = scope)
    }

    override fun onMessageTextChanged(messageText: String) {
        updateDraftEditorState { currentDraftEditorState ->
            return@updateDraftEditorState currentDraftEditorState.withMessageText(
                messageText = messageText,
            )
        }
    }

    override fun persistDraft() {
        val currentDraftEditorState = draftEditorState.value
        val scope = boundScope ?: return

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val saveRequest = currentDraftEditorState.toSaveRequestOrNull() ?: return@launch

            saveDraft(
                saveRequest = saveRequest,
                shouldMarkCurrentDraftAsPersisted = true,
            )
        }
    }

    override fun flushDraft() {
        val saveRequest = draftEditorState.value.toSaveRequestOrNull() ?: return

        applicationScope.launch {
            flushDraft(saveRequest = saveRequest)
        }
    }

    private suspend fun flushDraft(saveRequest: DraftSaveRequest) {
        withContext(context = NonCancellable) {
            saveDraft(
                saveRequest = saveRequest,
                shouldMarkCurrentDraftAsPersisted = false,
            )
        }
    }

    private suspend fun saveDraft(
        saveRequest: DraftSaveRequest,
        shouldMarkCurrentDraftAsPersisted: Boolean,
    ) {
        draftSaveMutex.withLock {
            conversationDraftsRepository.saveDraft(
                conversationId = saveRequest.conversationId,
                draft = saveRequest.draft,
            )

            if (!shouldMarkCurrentDraftAsPersisted) {
                return@withLock
            }

            updateDraftEditorState { currentDraftEditorState ->
                return@updateDraftEditorState currentDraftEditorState.markPersistedIfUnchanged(
                    saveRequest = saveRequest,
                )
            }
        }
    }

    private fun bindConversationDraftObservation(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<String?>,
    ) {
        scope.launch(defaultDispatcher) {
            conversationIdFlow.collectLatest { conversationId ->
                resetDraftEditorState(conversationId = conversationId)

                if (conversationId == null) {
                    return@collectLatest
                }

                observePersistedDraft(conversationId = conversationId)
            }
        }
    }

    private fun bindDraftAutosave(scope: CoroutineScope) {
        scope.launch(defaultDispatcher) {
            draftEditorState
                .map { currentDraftEditorState ->
                    currentDraftEditorState.toSaveRequestOrNull()
                }
                .distinctUntilChanged()
                .debounce(timeoutMillis = DRAFT_AUTOSAVE_DELAY_MILLIS)
                .filterNotNull()
                .collect { saveRequest ->
                    saveDraft(
                        saveRequest = saveRequest,
                        shouldMarkCurrentDraftAsPersisted = true,
                    )
                }
        }
    }

    private suspend fun resetDraftEditorState(conversationId: String?) {
        val previousDraftEditorState = draftEditorState.value
        updateDraftEditorState(
            draftEditorState = DraftEditorState(
                conversationId = conversationId,
            ),
        )

        previousDraftEditorState
            .toSaveRequestOrNull()
            ?.let { saveRequest ->
                flushDraft(saveRequest = saveRequest)
            }
    }

    private suspend fun observePersistedDraft(conversationId: String) {
        conversationDraftsRepository
            .observeConversationDraft(conversationId = conversationId)
            .collect { persistedDraft ->
                updateDraftEditorState { currentDraftEditorState ->
                    if (currentDraftEditorState.conversationId != conversationId) {
                        return@updateDraftEditorState currentDraftEditorState
                    }

                    return@updateDraftEditorState currentDraftEditorState.withPersistedDraft(
                        persistedDraft = persistedDraft,
                    )
                }
            }
    }

    private fun updateDraftEditorState(draftEditorState: DraftEditorState) {
        this.draftEditorState.value = draftEditorState
        _state.value = draftEditorState.visibleDraft
    }

    private fun updateDraftEditorState(transform: (DraftEditorState) -> DraftEditorState) {
        draftEditorState.update { currentDraftEditorState ->
            val updatedDraftEditorState = transform(currentDraftEditorState)
            _state.value = updatedDraftEditorState.visibleDraft

            updatedDraftEditorState
        }
    }

    private companion object {
        private const val DRAFT_AUTOSAVE_DELAY_MILLIS = 300L
    }
}

private data class DraftEditorState(
    val conversationId: String? = null,
    val persistedDraft: ConversationDraft = ConversationDraft(),
    val localEdits: ConversationDraftEdits = ConversationDraftEdits(),
    val isLoaded: Boolean = false,
) {
    val effectiveDraft: ConversationDraft
        get() {
            return localEdits.applyTo(baseDraft = persistedDraft)
        }

    val visibleDraft: ConversationDraft
        get() {
            if (conversationId == null) {
                return ConversationDraft()
            }

            return effectiveDraft
        }

    fun withPersistedDraft(persistedDraft: ConversationDraft): DraftEditorState {
        return copy(
            persistedDraft = persistedDraft,
            localEdits = localEdits.normalizedAgainst(
                baseDraft = persistedDraft,
            ),
            isLoaded = true,
        )
    }

    fun withMessageText(messageText: String): DraftEditorState {
        if (conversationId == null) {
            return this
        }

        return copy(
            localEdits = localEdits
                .copy(messageText = messageText)
                .normalizedAgainst(baseDraft = persistedDraft),
        )
    }

    fun toSaveRequestOrNull(): DraftSaveRequest? {
        val currentConversationId = conversationId ?: return null

        if (!isLoaded || !localEdits.hasChanges) {
            return null
        }

        return DraftSaveRequest(
            conversationId = currentConversationId,
            draft = effectiveDraft,
        )
    }

    fun markPersistedIfUnchanged(saveRequest: DraftSaveRequest): DraftEditorState {
        if (conversationId != saveRequest.conversationId) {
            return this
        }

        if (effectiveDraft != saveRequest.draft) {
            return this
        }

        return copy(
            persistedDraft = saveRequest.draft,
            localEdits = ConversationDraftEdits(),
            isLoaded = true,
        )
    }
}

private data class ConversationDraftEdits(
    val messageText: String? = null,
    val subjectText: String? = null,
    val selfParticipantId: String? = null,
    val attachments: List<ConversationDraftAttachment>? = null,
) {
    val hasChanges: Boolean
        get() {
            return messageText != null ||
                subjectText != null ||
                selfParticipantId != null ||
                attachments != null
        }

    fun applyTo(baseDraft: ConversationDraft): ConversationDraft {
        return baseDraft.copy(
            messageText = messageText ?: baseDraft.messageText,
            subjectText = subjectText ?: baseDraft.subjectText,
            selfParticipantId = selfParticipantId ?: baseDraft.selfParticipantId,
            attachments = attachments ?: baseDraft.attachments,
        )
    }

    fun normalizedAgainst(baseDraft: ConversationDraft): ConversationDraftEdits {
        return ConversationDraftEdits(
            messageText = messageText?.takeUnless { value ->
                value == baseDraft.messageText
            },
            subjectText = subjectText?.takeUnless { value ->
                value == baseDraft.subjectText
            },
            selfParticipantId = selfParticipantId?.takeUnless { value ->
                value == baseDraft.selfParticipantId
            },
            attachments = attachments?.takeUnless { value ->
                value == baseDraft.attachments
            },
        )
    }
}

private data class DraftSaveRequest(
    val conversationId: String,
    val draft: ConversationDraft,
)
