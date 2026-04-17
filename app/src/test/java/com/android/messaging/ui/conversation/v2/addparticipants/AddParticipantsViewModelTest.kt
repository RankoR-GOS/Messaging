package com.android.messaging.ui.conversation.v2.addparticipants

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.android.messaging.R
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import com.android.messaging.data.conversation.repository.ConversationParticipantsRepository
import com.android.messaging.data.conversation.repository.ConversationRecipientsPage
import com.android.messaging.data.conversation.repository.ConversationRecipientsRepository
import com.android.messaging.domain.contacts.usecase.IsReadContactsPermissionGranted
import com.android.messaging.domain.conversation.usecase.IsConversationRecipientLimitExceeded
import com.android.messaging.domain.conversation.usecase.ResolveConversationId
import com.android.messaging.domain.conversation.usecase.model.ResolveConversationIdResult
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.conversation.v2.addparticipants.model.AddParticipantsEffect
import com.android.messaging.ui.conversation.v2.recipientpicker.delegate.RecipientPickerDelegateImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
class AddParticipantsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var conversationRecipientsRepository: ConversationRecipientsRepository
    private lateinit var conversationParticipantsRepository: ConversationParticipantsRepository
    private lateinit var isReadContactsPermissionGranted: IsReadContactsPermissionGranted
    private lateinit var isConversationRecipientLimitExceeded: IsConversationRecipientLimitExceeded
    private lateinit var resolveConversationId: ResolveConversationId

    @Before
    fun setUp() {
        conversationRecipientsRepository = mockk()
        conversationParticipantsRepository = mockk()
        isReadContactsPermissionGranted = mockk()
        isConversationRecipientLimitExceeded = mockk()
        resolveConversationId = mockk()

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
            isConversationRecipientLimitExceeded.invoke(participantCount = any())
        } returns false
    }

    @Test
    fun onRecipientClicked_ignoresExistingParticipantsAndTogglesNewSelection() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        every {
            conversationParticipantsRepository.getParticipants(conversationId = "conversation-1")
        } returns flowOf(
            listOf(
                recipient(
                    id = "1",
                    displayName = "Ada",
                    destination = "+1 555 0100",
                ),
            ).toImmutableList(),
        )

        val viewModel = createViewModel()
        val uiStateCollectionJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        viewModel.onConversationIdChanged(conversationId = "conversation-1")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadingConversationParticipants)
        assertEquals(1, viewModel.uiState.value.existingParticipants.size)

        viewModel.onRecipientClicked(destination = "+1 555 0100")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.selectedRecipientDestinations.isEmpty())

        viewModel.onRecipientClicked(destination = "+1 555 0101")
        advanceUntilIdle()
        assertEquals(
            listOf("+1 555 0101"),
            viewModel.uiState.value.selectedRecipientDestinations,
        )

        viewModel.onRecipientClicked(destination = "+1 555 0101")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.selectedRecipientDestinations.isEmpty())
        uiStateCollectionJob.cancel()
    }

    @Test
    fun onConfirmClick_resolvesConversationWithExistingAndSelectedParticipants() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        every {
            conversationParticipantsRepository.getParticipants(conversationId = "conversation-1")
        } returns flowOf(
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
            ).toImmutableList(),
        )
        coEvery {
            resolveConversationId.invoke(
                destinations = listOf(
                    "+1 555 0100",
                    "+1 555 0101",
                    "+1 555 0102",
                ),
            )
        } returns ResolveConversationIdResult.Resolved(
            conversationId = "conversation-2",
        )

        val viewModel = createViewModel()
        val uiStateCollectionJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        viewModel.onConversationIdChanged(conversationId = "conversation-1")
        advanceUntilIdle()
        viewModel.onRecipientClicked(destination = "+1 555 0102")

        viewModel.effects.test {
            viewModel.onConfirmClick()
            advanceUntilIdle()

            assertEquals(
                AddParticipantsEffect.NavigateToConversation(
                    conversationId = "conversation-2",
                ),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(viewModel.uiState.value.isResolvingConversation)
        assertTrue(viewModel.uiState.value.selectedRecipientDestinations.isEmpty())
        uiStateCollectionJob.cancel()
    }

    @Test
    fun onConfirmClick_showsTooManyParticipantsWhenLimitWouldBeExceeded() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        every {
            conversationParticipantsRepository.getParticipants(conversationId = "conversation-1")
        } returns flowOf(
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
            ).toImmutableList(),
        )
        every {
            isConversationRecipientLimitExceeded.invoke(participantCount = 3)
        } returns true

        val viewModel = createViewModel()
        val uiStateCollectionJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        viewModel.onConversationIdChanged(conversationId = "conversation-1")
        advanceUntilIdle()
        viewModel.onRecipientClicked(destination = "+1 555 0102")

        viewModel.effects.test {
            viewModel.onConfirmClick()

            assertEquals(
                AddParticipantsEffect.ShowMessage(
                    messageResId = R.string.too_many_participants,
                ),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) {
            resolveConversationId.invoke(destinations = any())
        }
        uiStateCollectionJob.cancel()
    }

    @Test
    fun onConversationIdChanged_null_clearsParticipantsSelectionAndRecipientExclusions() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val participantsFlow = MutableStateFlow(
            value = listOf(
                recipient(
                    id = "1",
                    displayName = "Ada",
                    destination = "+1 555 0100",
                ),
            ).toImmutableList(),
        )
        every {
            conversationParticipantsRepository.getParticipants(conversationId = "conversation-1")
        } returns participantsFlow
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
            ),
        )

        val viewModel = createViewModel()
        val uiStateCollectionJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onConversationIdChanged(conversationId = "conversation-1")
        advanceTimeBy(delayTimeMillis = 150L)
        advanceUntilIdle()
        viewModel.onRecipientClicked(destination = "+1 555 0101")
        advanceUntilIdle()

        assertEquals(
            listOf(
                recipient(
                    id = "2",
                    displayName = "Bob",
                    destination = "+1 555 0101",
                ),
            ),
            viewModel.uiState.value.recipientPickerUiState.contacts,
        )
        assertEquals(
            listOf("+1 555 0101"),
            viewModel.uiState.value.selectedRecipientDestinations,
        )

        viewModel.onConversationIdChanged(conversationId = null)
        advanceTimeBy(delayTimeMillis = 150L)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.existingParticipants.isEmpty())
        assertFalse(viewModel.uiState.value.isLoadingConversationParticipants)
        assertTrue(viewModel.uiState.value.selectedRecipientDestinations.isEmpty())
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
            ),
            viewModel.uiState.value.recipientPickerUiState.contacts,
        )
        uiStateCollectionJob.cancel()
    }

    @Test
    fun participantUpdates_pruneSelectionsAndRefreshRecipientExclusions() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val participantsFlow = MutableStateFlow(
            value = listOf(
                recipient(
                    id = "1",
                    displayName = "Ada",
                    destination = "+1 555 0100",
                ),
            ).toImmutableList(),
        )
        every {
            conversationParticipantsRepository.getParticipants(conversationId = "conversation-1")
        } returns participantsFlow
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
                    recipient(
                        id = "3",
                        displayName = "Carol",
                        destination = "+1 555 0102",
                    ),
                ),
            ),
        )

        val viewModel = createViewModel()
        val uiStateCollectionJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onConversationIdChanged(conversationId = "conversation-1")
        advanceTimeBy(delayTimeMillis = 150L)
        advanceUntilIdle()
        viewModel.onRecipientClicked(destination = "+1 555 0101")
        advanceUntilIdle()

        assertEquals(
            listOf("+1 555 0101"),
            viewModel.uiState.value.selectedRecipientDestinations,
        )
        assertEquals(
            listOf(
                recipient(
                    id = "2",
                    displayName = "Bob",
                    destination = "+1 555 0101",
                ),
                recipient(
                    id = "3",
                    displayName = "Carol",
                    destination = "+1 555 0102",
                ),
            ),
            viewModel.uiState.value.recipientPickerUiState.contacts,
        )

        participantsFlow.value = listOf(
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
        ).toImmutableList()
        advanceTimeBy(delayTimeMillis = 150L)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.selectedRecipientDestinations.isEmpty())
        assertEquals(
            listOf(
                recipient(
                    id = "3",
                    displayName = "Carol",
                    destination = "+1 555 0102",
                ),
            ),
            viewModel.uiState.value.recipientPickerUiState.contacts,
        )
        uiStateCollectionJob.cancel()
    }

    @Test
    fun onRecipientClicked_ignoresBlankAndLoadingDestinations() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val viewModel = createViewModel()

        viewModel.onRecipientClicked(destination = "   ")
        viewModel.onRecipientClicked(destination = "+1 555 0101")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.selectedRecipientDestinations.isEmpty())
    }

    @Test
    fun onRecipientClicked_ignoresSelectionsWhileConversationResolutionIsInProgress() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val participantsFlow = MutableStateFlow(
            value = listOf(
                recipient(
                    id = "1",
                    displayName = "Ada",
                    destination = "+1 555 0100",
                ),
            ).toImmutableList(),
        )
        val resolveConversationResult = CompletableDeferred<ResolveConversationIdResult>()
        every {
            conversationParticipantsRepository.getParticipants(conversationId = "conversation-1")
        } returns participantsFlow
        coEvery {
            resolveConversationId.invoke(
                destinations = listOf(
                    "+1 555 0100",
                    "+1 555 0101",
                ),
            )
        } coAnswers {
            resolveConversationResult.await()
        }

        val viewModel = createViewModel()
        val uiStateCollectionJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        viewModel.onConversationIdChanged(conversationId = "conversation-1")
        advanceUntilIdle()
        viewModel.onRecipientClicked(destination = "+1 555 0101")
        advanceUntilIdle()

        viewModel.onConfirmClick()
        runCurrent()
        assertTrue(viewModel.uiState.value.isResolvingConversation)

        viewModel.onRecipientClicked(destination = "+1 555 0102")
        advanceUntilIdle()

        assertEquals(
            listOf("+1 555 0101"),
            viewModel.uiState.value.selectedRecipientDestinations,
        )
        resolveConversationResult.complete(
            value = ResolveConversationIdResult.Resolved(
                conversationId = "conversation-2",
            ),
        )
        advanceUntilIdle()
        uiStateCollectionJob.cancel()
    }

    @Test
    fun onConfirmClick_ignoresRequestsWhileLoadingOrWithoutSelection() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val participantsFlow = MutableStateFlow<ImmutableList<ConversationRecipient>>(
            value = emptyList<ConversationRecipient>().toImmutableList(),
        )
        every {
            conversationParticipantsRepository.getParticipants(conversationId = "conversation-1")
        } returns participantsFlow

        val viewModel = createViewModel()

        viewModel.onConfirmClick()
        viewModel.onConversationIdChanged(conversationId = "conversation-1")
        runCurrent()
        viewModel.onConfirmClick()
        advanceUntilIdle()

        coVerify(exactly = 0) {
            resolveConversationId.invoke(destinations = any())
        }
    }

    @Test
    fun onConfirmClick_doesNotStartSecondResolutionWhileCurrentResolutionIsRunning() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val participantsFlow = MutableStateFlow(
            value = listOf(
                recipient(
                    id = "1",
                    displayName = "Ada",
                    destination = "+1 555 0100",
                ),
            ).toImmutableList(),
        )
        val resolveConversationResult = CompletableDeferred<ResolveConversationIdResult>()
        every {
            conversationParticipantsRepository.getParticipants(conversationId = "conversation-1")
        } returns participantsFlow
        coEvery {
            resolveConversationId.invoke(
                destinations = listOf(
                    "+1 555 0100",
                    "+1 555 0101",
                ),
            )
        } coAnswers {
            resolveConversationResult.await()
        }

        val viewModel = createViewModel()
        val uiStateCollectionJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        viewModel.onConversationIdChanged(conversationId = "conversation-1")
        advanceUntilIdle()
        viewModel.onRecipientClicked(destination = "+1 555 0101")
        advanceUntilIdle()

        viewModel.onConfirmClick()
        runCurrent()
        viewModel.onConfirmClick()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            resolveConversationId.invoke(
                destinations = listOf(
                    "+1 555 0100",
                    "+1 555 0101",
                ),
            )
        }
        resolveConversationResult.complete(
            value = ResolveConversationIdResult.Resolved(
                conversationId = "conversation-2",
            ),
        )
        advanceUntilIdle()
        uiStateCollectionJob.cancel()
    }

    @Test
    fun onConfirmClick_showsFailureMessageWhenConversationIsNotResolved() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val participantsFlow = MutableStateFlow(
            value = listOf(
                recipient(
                    id = "1",
                    displayName = "Ada",
                    destination = "+1 555 0100",
                ),
            ).toImmutableList(),
        )
        every {
            conversationParticipantsRepository.getParticipants(conversationId = "conversation-1")
        } returns participantsFlow
        coEvery {
            resolveConversationId.invoke(
                destinations = listOf(
                    "+1 555 0100",
                    "+1 555 0101",
                ),
            )
        } returns ResolveConversationIdResult.NotResolved

        val viewModel = createViewModel()
        val uiStateCollectionJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        viewModel.onConversationIdChanged(conversationId = "conversation-1")
        advanceUntilIdle()
        viewModel.onRecipientClicked(destination = "+1 555 0101")

        viewModel.effects.test {
            viewModel.onConfirmClick()
            advanceUntilIdle()

            assertEquals(
                AddParticipantsEffect.ShowMessage(
                    messageResId = R.string.conversation_creation_failure,
                ),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(viewModel.uiState.value.isResolvingConversation)
        assertEquals(
            listOf("+1 555 0101"),
            viewModel.uiState.value.selectedRecipientDestinations,
        )
        uiStateCollectionJob.cancel()
    }

    @Test
    fun onConfirmClick_showsFailureMessageWhenResolutionReturnsEmptyDestinations() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val participantsFlow = MutableStateFlow(
            value = listOf(
                recipient(
                    id = "1",
                    displayName = "Ada",
                    destination = "+1 555 0100",
                ),
            ).toImmutableList(),
        )
        every {
            conversationParticipantsRepository.getParticipants(conversationId = "conversation-1")
        } returns participantsFlow
        coEvery {
            resolveConversationId.invoke(
                destinations = listOf(
                    "+1 555 0100",
                    "+1 555 0101",
                ),
            )
        } returns ResolveConversationIdResult.EmptyDestinations

        val viewModel = createViewModel()
        val uiStateCollectionJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        viewModel.onConversationIdChanged(conversationId = "conversation-1")
        advanceUntilIdle()
        viewModel.onRecipientClicked(destination = "+1 555 0101")

        viewModel.effects.test {
            viewModel.onConfirmClick()
            advanceUntilIdle()

            assertEquals(
                AddParticipantsEffect.ShowMessage(
                    messageResId = R.string.conversation_creation_failure,
                ),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(viewModel.uiState.value.isResolvingConversation)
        uiStateCollectionJob.cancel()
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): AddParticipantsViewModel {
        val recipientPickerDelegate = RecipientPickerDelegateImpl(
            conversationRecipientsRepository = conversationRecipientsRepository,
            isReadContactsPermissionGranted = isReadContactsPermissionGranted,
            savedStateHandle = savedStateHandle,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )

        return AddParticipantsViewModel(
            conversationParticipantsRepository = conversationParticipantsRepository,
            isConversationRecipientLimitExceeded = isConversationRecipientLimitExceeded,
            recipientPickerDelegate = recipientPickerDelegate,
            resolveConversationId = resolveConversationId,
            savedStateHandle = savedStateHandle,
            mainDispatcher = mainDispatcherRule.testDispatcher,
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
}
