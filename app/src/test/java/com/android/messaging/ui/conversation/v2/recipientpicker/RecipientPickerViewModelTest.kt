package com.android.messaging.ui.conversation.v2.recipientpicker

import androidx.lifecycle.SavedStateHandle
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import com.android.messaging.data.conversation.repository.ConversationRecipientsPage
import com.android.messaging.data.conversation.repository.ConversationRecipientsRepository
import com.android.messaging.domain.contacts.usecase.IsReadContactsPermissionGranted
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.conversation.v2.recipientpicker.delegate.RecipientPickerDelegateImpl
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RecipientPickerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var conversationRecipientsRepository: ConversationRecipientsRepository
    private lateinit var isReadContactsPermissionGranted: IsReadContactsPermissionGranted

    @Before
    fun setUp() {
        conversationRecipientsRepository = mockk()
        isReadContactsPermissionGranted = mockk()
    }

    @Test
    fun init_withoutContactsPermission_showsPermissionDeniedStateAndSkipsRepository() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        every {
            isReadContactsPermissionGranted.invoke()
        } returns false
        val savedStateHandle = SavedStateHandle(
            mapOf(SEARCH_QUERY_KEY to "Ada"),
        )

        val viewModel = createViewModel(savedStateHandle = savedStateHandle)
        advanceUntilIdle()

        assertEquals("Ada", viewModel.uiState.value.query)
        assertFalse(viewModel.uiState.value.hasContactsPermission)
        assertTrue(viewModel.uiState.value.contacts.isEmpty())
        assertFalse(viewModel.uiState.value.canLoadMore)
        verify(exactly = 1) {
            isReadContactsPermissionGranted.invoke()
        }
        verify(exactly = 0) {
            @Suppress("UnusedFlow")
            conversationRecipientsRepository.searchRecipients(query = any(), offset = any())
        }
    }

    @Test
    fun init_withNoMatchesForRequestedQuery_fallsBackToDefaultContacts() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        every {
            isReadContactsPermissionGranted.invoke()
        } returns true
        every {
            conversationRecipientsRepository.searchRecipients(
                query = "Ada",
                offset = 0,
            )
        } returns flowOf(
            page(),
        )
        every {
            conversationRecipientsRepository.searchRecipients(
                query = "",
                offset = 0,
            )
        } returns flowOf(
            page(
                recipients = listOf(
                    recipient(
                        id = "1",
                        displayName = "Grace Hopper",
                        destination = "+1 555 0100",
                    ),
                ),
            ),
        )
        val savedStateHandle = SavedStateHandle(
            mapOf(SEARCH_QUERY_KEY to "Ada"),
        )

        val viewModel = createViewModel(savedStateHandle = savedStateHandle)
        runCurrent()

        assertTrue(viewModel.uiState.value.isLoading)

        advanceTimeBy(150L)
        advanceUntilIdle()

        assertEquals("Ada", viewModel.uiState.value.query)
        assertEquals(
            listOf(
                recipient(
                    id = "1",
                    displayName = "Grace Hopper",
                    destination = "+1 555 0100",
                ),
            ),
            viewModel.uiState.value.contacts,
        )
        assertTrue(viewModel.uiState.value.hasContactsPermission)
        assertFalse(viewModel.uiState.value.isLoading)
        verify(exactly = 1) {
            @Suppress("UnusedFlow")
            conversationRecipientsRepository.searchRecipients(
                query = "Ada",
                offset = 0,
            )
        }
        verify(exactly = 1) {
            @Suppress("UnusedFlow")
            conversationRecipientsRepository.searchRecipients(
                query = "",
                offset = 0,
            )
        }
    }

    @Test
    fun onQueryChanged_updatesSavedStateImmediatelyAndSearchesAfterDebounce() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        every {
            isReadContactsPermissionGranted.invoke()
        } returns true
        every {
            conversationRecipientsRepository.searchRecipients(
                query = "",
                offset = 0,
            )
        } returns flowOf(page())
        every {
            conversationRecipientsRepository.searchRecipients(
                query = "Bob",
                offset = 0,
            )
        } returns flowOf(
            page(
                recipients = listOf(
                    recipient(
                        id = "2",
                        displayName = "Bob",
                        destination = "+1 555 0101",
                    ),
                ),
            ),
        )
        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle = savedStateHandle)
        advanceUntilIdle()
        clearAllMocks(
            answers = false,
            recordedCalls = true,
        )

        viewModel.onQueryChanged(query = "Bob")

        assertEquals("Bob", viewModel.uiState.value.query)
        assertEquals("Bob", savedStateHandle[SEARCH_QUERY_KEY])
        advanceTimeBy(149L)

        verify(exactly = 0) {
            @Suppress("UnusedFlow")
            conversationRecipientsRepository.searchRecipients(
                query = "Bob",
                offset = 0,
            )
        }

        advanceTimeBy(1L)
        advanceUntilIdle()

        assertEquals(
            listOf(
                recipient(
                    id = "2",
                    displayName = "Bob",
                    destination = "+1 555 0101",
                ),
            ),
            viewModel.uiState.value.contacts,
        )
        verify(exactly = 1) {
            @Suppress("UnusedFlow")
            conversationRecipientsRepository.searchRecipients(
                query = "Bob",
                offset = 0,
            )
        }
    }

    @Test
    fun onExcludedDestinationsChanged_filtersExcludedContactsAndContinuesPaging() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        every {
            isReadContactsPermissionGranted.invoke()
        } returns true
        every {
            conversationRecipientsRepository.searchRecipients(
                query = "",
                offset = 0,
            )
        } returnsMany listOf(
            flowOf(
                page(
                    recipients = listOf(
                        recipient(
                            id = "1",
                            displayName = "Ada",
                            destination = "+1 555 0100",
                        ),
                    ),
                ),
            ),
            flowOf(
                page(
                    recipients = listOf(
                        recipient(
                            id = "1",
                            displayName = "Ada",
                            destination = "+1 555 0100",
                        ),
                    ),
                    nextOffset = 1,
                ),
            ),
        )
        every {
            conversationRecipientsRepository.searchRecipients(
                query = "",
                offset = 1,
            )
        } returns flowOf(
            page(
                recipients = listOf(
                    recipient(
                        id = "2",
                        displayName = "Bob",
                        destination = "+1 555 0101",
                    ),
                ),
            ),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        clearAllMocks(
            answers = false,
            recordedCalls = true,
        )

        viewModel.onExcludedDestinationsChanged(
            destinations = setOf("+1 555 0100"),
        )
        advanceTimeBy(150L)
        advanceUntilIdle()

        assertEquals(
            listOf(
                recipient(
                    id = "2",
                    displayName = "Bob",
                    destination = "+1 555 0101",
                ),
            ),
            viewModel.uiState.value.contacts,
        )
        verify(exactly = 1) {
            @Suppress("UnusedFlow")
            conversationRecipientsRepository.searchRecipients(
                query = "",
                offset = 0,
            )
        }
        verify(exactly = 1) {
            @Suppress("UnusedFlow")
            conversationRecipientsRepository.searchRecipients(
                query = "",
                offset = 1,
            )
        }
    }

    @Test
    fun onLoadMore_appendsRecipientsDeduplicatesByDestinationAndUpdatesPagination() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        every {
            isReadContactsPermissionGranted.invoke()
        } returns true
        every {
            conversationRecipientsRepository.searchRecipients(
                query = "",
                offset = 0,
            )
        } returns flowOf(
            page(
                recipients = listOf(
                    recipient(
                        id = "1",
                        displayName = "Ada",
                        destination = "+1 555 0100",
                    ),
                    recipient(
                        id = "2",
                        displayName = "Bob",
                        destination = "+1 555 0101",
                    ),
                ),
                nextOffset = 2,
            ),
        )
        every {
            conversationRecipientsRepository.searchRecipients(
                query = "",
                offset = 2,
            )
        } returns flowOf(
            page(
                recipients = listOf(
                    recipient(
                        id = "3",
                        displayName = "Bob Duplicate",
                        destination = "+1 555 0101",
                    ),
                    recipient(
                        id = "4",
                        displayName = "Carol",
                        destination = "+1 555 0102",
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onLoadMore()
        advanceUntilIdle()

        assertEquals(
            listOf(
                recipient(
                    id = "1",
                    displayName = "Ada",
                    destination = "+1 555 0100",
                ),
                recipient(
                    id = "2",
                    displayName = "Bob",
                    destination = "+1 555 0101",
                ),
                recipient(
                    id = "4",
                    displayName = "Carol",
                    destination = "+1 555 0102",
                ),
            ),
            viewModel.uiState.value.contacts,
        )
        assertFalse(viewModel.uiState.value.isLoadingMore)
        assertFalse(viewModel.uiState.value.canLoadMore)
        verify(exactly = 1) {
            @Suppress("UnusedFlow")
            conversationRecipientsRepository.searchRecipients(
                query = "",
                offset = 2,
            )
        }
    }

    @Test
    fun onLoadMore_ignoresRequestsWhenNoNextPageExists() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        every {
            isReadContactsPermissionGranted.invoke()
        } returns true
        every {
            conversationRecipientsRepository.searchRecipients(
                query = "",
                offset = 0,
            )
        } returns flowOf(
            page(
                recipients = listOf(
                    recipient(
                        id = "1",
                        displayName = "Ada",
                        destination = "+1 555 0100",
                    ),
                ),
                nextOffset = null,
            ),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        clearAllMocks(
            answers = false,
            recordedCalls = true,
        )

        viewModel.onLoadMore()
        advanceUntilIdle()

        verify(exactly = 0) {
            @Suppress("UnusedFlow")
            conversationRecipientsRepository.searchRecipients(
                query = any(),
                offset = any(),
            )
        }
    }

    @Test
    fun onLoadMore_dropsStalePageWhenQueryChangesDuringPagination() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val stalePageFlow = MutableSharedFlow<ConversationRecipientsPage>()
        every {
            isReadContactsPermissionGranted.invoke()
        } returns true
        every {
            conversationRecipientsRepository.searchRecipients(
                query = "",
                offset = 0,
            )
        } returns flowOf(
            page(
                recipients = listOf(
                    recipient(
                        id = "1",
                        displayName = "Ada",
                        destination = "+1 555 0100",
                    ),
                ),
                nextOffset = 1,
            ),
        )
        every {
            conversationRecipientsRepository.searchRecipients(
                query = "",
                offset = 1,
            )
        } returns stalePageFlow
        every {
            conversationRecipientsRepository.searchRecipients(
                query = "Bob",
                offset = 0,
            )
        } returns flowOf(
            page(
                recipients = listOf(
                    recipient(
                        id = "2",
                        displayName = "Bob",
                        destination = "+1 555 0101",
                    ),
                ),
            ),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onLoadMore()
        runCurrent()
        assertTrue(viewModel.uiState.value.isLoadingMore)

        viewModel.onQueryChanged(query = "Bob")
        advanceTimeBy(150L)
        advanceUntilIdle()
        stalePageFlow.emit(
            page(
                recipients = listOf(
                    recipient(
                        id = "3",
                        displayName = "Stale",
                        destination = "+1 555 0999",
                    ),
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(
            listOf(
                recipient(
                    id = "2",
                    displayName = "Bob",
                    destination = "+1 555 0101",
                ),
            ),
            viewModel.uiState.value.contacts,
        )
        assertFalse(viewModel.uiState.value.isLoadingMore)
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): RecipientPickerViewModel {
        val recipientPickerDelegate = RecipientPickerDelegateImpl(
            conversationRecipientsRepository = conversationRecipientsRepository,
            isReadContactsPermissionGranted = isReadContactsPermissionGranted,
            savedStateHandle = savedStateHandle,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )

        return RecipientPickerViewModel(
            recipientPickerDelegate = recipientPickerDelegate,
        )
    }

    private fun page(
        recipients: List<ConversationRecipient> = emptyList(),
        nextOffset: Int? = null,
    ): ConversationRecipientsPage {
        return ConversationRecipientsPage(
            recipients = recipients.toImmutableList(),
            nextOffset = nextOffset,
        )
    }

    private fun recipient(
        id: String,
        displayName: String,
        destination: String,
    ): ConversationRecipient {
        return ConversationRecipient(
            id = id,
            displayName = displayName,
            destination = destination,
            secondaryText = destination,
        )
    }

    private companion object {
        private const val SEARCH_QUERY_KEY = "search_query"
    }
}
