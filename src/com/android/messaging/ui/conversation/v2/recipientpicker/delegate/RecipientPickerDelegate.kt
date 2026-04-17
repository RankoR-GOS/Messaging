package com.android.messaging.ui.conversation.v2.recipientpicker.delegate

import androidx.lifecycle.SavedStateHandle
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import com.android.messaging.data.conversation.repository.ConversationRecipientsPage
import com.android.messaging.data.conversation.repository.ConversationRecipientsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.domain.contacts.usecase.IsReadContactsPermissionGranted
import com.android.messaging.ui.conversation.v2.recipientpicker.model.RecipientPickerUiState
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal interface RecipientPickerDelegate {
    val state: StateFlow<RecipientPickerUiState>

    fun bind(scope: CoroutineScope)

    fun onLoadMore()

    fun onExcludedDestinationsChanged(destinations: Set<String>)

    fun onQueryChanged(query: String)
}

internal class RecipientPickerDelegateImpl @Inject constructor(
    private val conversationRecipientsRepository: ConversationRecipientsRepository,
    private val isReadContactsPermissionGranted: IsReadContactsPermissionGranted,
    private val savedStateHandle: SavedStateHandle,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : RecipientPickerDelegate {

    private val queryFlow = MutableStateFlow(
        value = savedStateHandle.get<String>(SEARCH_QUERY_KEY).orEmpty(),
    )
    private val excludedDestinationsFlow = MutableStateFlow<Set<String>>(
        value = emptySet(),
    )

    private val _state = MutableStateFlow(
        value = RecipientPickerUiState(
            query = queryFlow.value,
            isLoading = false,
        ),
    )

    override val state = _state.asStateFlow()

    private var boundScope: CoroutineScope? = null

    private var searchSession = RecipientSearchSession(
        effectiveQuery = queryFlow.value,
        hasCompletedInitialLoad = false,
        nextPageOffset = null,
    )

    private val searchSessionMutex = Mutex()

    override fun bind(scope: CoroutineScope) {
        if (boundScope != null) {
            return
        }

        boundScope = scope

        scope.launch(defaultDispatcher) {
            combine(
                queryFlow,
                excludedDestinationsFlow,
            ) { query, excludedDestinations ->
                SearchInputs(
                    query = query,
                    excludedDestinations = excludedDestinations,
                )
            }.collectLatest { searchInputs ->
                handleSearchInputsChanged(searchInputs = searchInputs)
            }
        }
    }

    override fun onLoadMore() {
        boundScope?.launch(defaultDispatcher) {
            val loadMoreRequest = createLoadMoreRequest() ?: return@launch
            loadMore(request = loadMoreRequest)
        }
    }

    override fun onExcludedDestinationsChanged(destinations: Set<String>) {
        val normalizedDestinations = destinations
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        excludedDestinationsFlow.value = normalizedDestinations
    }

    override fun onQueryChanged(query: String) {
        updateQueryInState(query = query)

        if (query != queryFlow.value) {
            queryFlow.value = query
            savedStateHandle[SEARCH_QUERY_KEY] = query
        }
    }

    private suspend fun handleSearchInputsChanged(searchInputs: SearchInputs) {
        if (!isReadContactsPermissionGranted()) {
            applyPermissionDeniedState(query = searchInputs.query)
            return
        }

        startSearch(searchInputs = searchInputs)
    }

    private fun mergeRecipients(
        existingRecipients: List<ConversationRecipient>,
        additionalRecipients: List<ConversationRecipient>,
    ): ImmutableList<ConversationRecipient> {
        val seenDestinations = LinkedHashSet<String>()

        return (existingRecipients + additionalRecipients)
            .asSequence()
            .filter { recipient ->
                seenDestinations.add(recipient.destination)
            }
            .toImmutableList()
    }

    private suspend fun startSearch(searchInputs: SearchInputs) {
        applySearchStartedState()
        delay(timeMillis = SEARCH_DEBOUNCE_MILLIS)

        val initialSearchResult = resolveInitialSearch(searchInputs = searchInputs)
        updateSearchSession { currentSearchSession ->
            currentSearchSession.copy(
                effectiveQuery = initialSearchResult.effectiveQuery,
                hasCompletedInitialLoad = true,
                nextPageOffset = initialSearchResult.page.nextOffset,
            )
        }

        applyInitialSearchResult(result = initialSearchResult)
    }

    private suspend fun applyPermissionDeniedState(query: String) {
        updateSearchSession { currentSearchSession ->
            currentSearchSession.copy(
                effectiveQuery = query,
                nextPageOffset = null,
            )
        }

        _state.update { currentState ->
            currentState.copy(
                canLoadMore = false,
                contacts = persistentListOf(),
                hasContactsPermission = false,
                isLoading = false,
                isLoadingMore = false,
            )
        }
    }

    private suspend fun applySearchStartedState() {
        val shouldShowInitialLoader = searchSessionMutex.withLock {
            !searchSession.hasCompletedInitialLoad
        }

        _state.update { currentState ->
            currentState.copy(
                canLoadMore = false,
                hasContactsPermission = true,
                isLoading = shouldShowInitialLoader,
                isLoadingMore = false,
            )
        }
    }

    private suspend fun resolveInitialSearch(
        searchInputs: SearchInputs,
    ): InitialSearchResult {
        val requestedPage = loadRecipientsPage(
            query = searchInputs.query,
            offset = 0,
            excludedDestinations = searchInputs.excludedDestinations,
        )

        if (shouldUseRequestedPage(query = searchInputs.query, page = requestedPage)) {
            return InitialSearchResult(
                effectiveQuery = searchInputs.query,
                page = requestedPage,
            )
        }

        val defaultPage = loadRecipientsPage(
            query = "",
            offset = 0,
            excludedDestinations = searchInputs.excludedDestinations,
        )

        return InitialSearchResult(
            effectiveQuery = "",
            page = defaultPage,
        )
    }

    private fun shouldUseRequestedPage(
        query: String,
        page: ConversationRecipientsPage,
    ): Boolean {
        return query.isBlank() || page.recipients.isNotEmpty()
    }

    private suspend fun loadRecipientsPage(
        query: String,
        offset: Int,
        excludedDestinations: Set<String>,
    ): ConversationRecipientsPage {
        var nextOffset: Int? = offset
        val visibleRecipients = mutableListOf<ConversationRecipient>()

        while (nextOffset != null) {
            val rawPage = conversationRecipientsRepository
                .searchRecipients(
                    query = query,
                    offset = nextOffset,
                )
                .first()

            visibleRecipients.addAll(
                rawPage.recipients.filterNot { recipient ->
                    recipient.destination in excludedDestinations
                },
            )

            if (visibleRecipients.isNotEmpty() || rawPage.nextOffset == null) {
                return ConversationRecipientsPage(
                    recipients = visibleRecipients.toImmutableList(),
                    nextOffset = rawPage.nextOffset,
                )
            }

            nextOffset = rawPage.nextOffset
        }

        return ConversationRecipientsPage(
            recipients = persistentListOf(),
            nextOffset = null,
        )
    }

    private fun applyInitialSearchResult(result: InitialSearchResult) {
        _state.update { currentState ->
            currentState.copy(
                contacts = result.page.recipients,
                canLoadMore = result.page.nextOffset != null,
                hasContactsPermission = true,
                isLoading = false,
                isLoadingMore = false,
            )
        }
    }

    private suspend fun createLoadMoreRequest(): LoadMoreRequest? {
        val currentState = _state.value

        return when {
            currentState.isLoading || currentState.isLoadingMore -> null
            !currentState.hasContactsPermission -> null

            else -> {
                searchSessionMutex.withLock {
                    val nextPageOffset = searchSession.nextPageOffset ?: return@withLock null

                    LoadMoreRequest(
                        effectiveQuery = searchSession.effectiveQuery,
                        inputQuery = currentState.query,
                        excludedDestinations = excludedDestinationsFlow.value,
                        offset = nextPageOffset,
                    )
                }
            }
        }
    }

    private suspend fun loadMore(request: LoadMoreRequest) {
        applyLoadMoreStartedState()

        val nextPage = loadRecipientsPage(
            query = request.effectiveQuery,
            offset = request.offset,
            excludedDestinations = request.excludedDestinations,
        )

        if (!isLoadMoreRequestCurrent(request = request)) {
            applyLoadMoreStoppedState()
            return
        }

        updateSearchSession { currentSearchSession ->
            currentSearchSession.copy(
                nextPageOffset = nextPage.nextOffset,
            )
        }

        applyLoadMoreResult(page = nextPage)
    }

    private fun applyLoadMoreStartedState() {
        _state.update { currentState ->
            currentState.copy(
                isLoadingMore = true,
            )
        }
    }

    private suspend fun isLoadMoreRequestCurrent(request: LoadMoreRequest): Boolean {
        val currentEffectiveQuery = searchSessionMutex.withLock {
            searchSession.effectiveQuery
        }

        return currentEffectiveQuery == request.effectiveQuery &&
            _state.value.query == request.inputQuery
    }

    private fun applyLoadMoreStoppedState() {
        _state.update { currentState ->
            currentState.copy(
                isLoadingMore = false,
            )
        }
    }

    private fun applyLoadMoreResult(page: ConversationRecipientsPage) {
        _state.update { currentState ->
            currentState.copy(
                contacts = mergeRecipients(
                    existingRecipients = currentState.contacts,
                    additionalRecipients = page.recipients,
                ),
                canLoadMore = page.nextOffset != null,
                isLoadingMore = false,
            )
        }
    }

    private fun updateQueryInState(query: String) {
        _state.update { currentState ->
            currentState.copy(
                query = query,
            )
        }
    }

    private suspend fun updateSearchSession(
        transform: (RecipientSearchSession) -> RecipientSearchSession,
    ) {
        searchSessionMutex.withLock {
            searchSession = transform(searchSession)
        }
    }

    private data class InitialSearchResult(
        val effectiveQuery: String,
        val page: ConversationRecipientsPage,
    )

    private data class LoadMoreRequest(
        val effectiveQuery: String,
        val inputQuery: String,
        val excludedDestinations: Set<String>,
        val offset: Int,
    )

    private data class RecipientSearchSession(
        val effectiveQuery: String,
        val hasCompletedInitialLoad: Boolean,
        val nextPageOffset: Int?,
    )

    private data class SearchInputs(
        val query: String,
        val excludedDestinations: Set<String>,
    )

    private companion object {
        private const val SEARCH_DEBOUNCE_MILLIS = 150L
        private const val SEARCH_QUERY_KEY = "search_query"
    }
}
