package com.android.messaging.ui.conversation.v2.composer.delegate

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.repository.ConversationDraftsRepository
import com.android.messaging.domain.conversation.usecase.SendConversationDraft
import com.android.messaging.ui.conversation.v2.composer.model.ConversationDraftState
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationDraftDelegateImplTest {

    @Before
    fun setUp() {
        unmockkAll()
        clearAllMocks()
    }

    @Test
    fun bind_setsCheckingStateUntilPersistedDraftArrives() {
        runTest {
            val repository = createConversationDraftsRepositoryMock()
            val harness = createDelegateHarness(
                repository = repository,
                sendConversationDraft = createSendConversationDraftMock(),
            )
            try {
                harness.conversationIdFlow.value = CONVERSATION_ID
                advanceUntilIdle()

                assertTrue(harness.delegate.state.value.draft.isCheckingDraft)

                repository.emitDraft(
                    conversationId = CONVERSATION_ID,
                    draft = ConversationDraft(
                        messageText = "Persisted",
                    ),
                )
                advanceUntilIdle()

                assertEquals("Persisted", harness.delegate.state.value.draft.messageText)
                assertFalse(harness.delegate.state.value.draft.isCheckingDraft)
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onMessageTextChanged_autosavesAfterDebounce() {
        runTest {
            val repository = createConversationDraftsRepositoryMock()
            val harness = createBoundLoadedDelegateHarness(repository = repository)

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")

                advanceTimeBy(299)
                assertTrue(repository.savedDrafts.isEmpty())

                advanceTimeBy(1)
                advanceUntilIdle()

                assertEquals(1, repository.savedDrafts.size)
                assertEquals("Hello", repository.savedDrafts.single().draft.messageText)
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun persistDraft_catchesSaveFailuresAndLeavesStateUsable() {
        runTest {
            val repository = createConversationDraftsRepositoryMock()
            val harness = createBoundLoadedDelegateHarness(repository = repository)

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")
                repository.saveFailure = IllegalStateException("boom")

                harness.delegate.persistDraft()
                advanceUntilIdle()

                assertEquals("Hello", harness.delegate.state.value.draft.messageText)
                assertTrue(repository.savedDrafts.isEmpty())

                repository.saveFailure = null
                harness.delegate.persistDraft()
                advanceUntilIdle()

                assertEquals(1, repository.savedDrafts.size)
                assertEquals("Hello", repository.savedDrafts.single().draft.messageText)
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun bind_catchesObservationFailuresAndPublishesSafeEmptyDraft() {
        runTest {
            val repository = createConversationDraftsRepositoryMock(
                observeFailure = IllegalStateException("boom"),
            )
            val harness = createDelegateHarness(
                repository = repository,
                sendConversationDraft = createSendConversationDraftMock(),
            )
            try {
                harness.conversationIdFlow.value = CONVERSATION_ID
                advanceUntilIdle()

                assertEquals(
                    ConversationDraftState(
                        draft = ConversationDraft(),
                    ),
                    harness.delegate.state.value,
                )
                assertFalse(harness.delegate.state.value.draft.isCheckingDraft)
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun conversationSwitch_flushesPreviousDraftBeforeResettingState() {
        runTest {
            val repository = createConversationDraftsRepositoryMock()
            val harness = createDelegateHarness(
                repository = repository,
                sendConversationDraft = createSendConversationDraftMock(),
            )
            try {
                harness.conversationIdFlow.value = CONVERSATION_ID
                repository.emitDraft(
                    conversationId = CONVERSATION_ID,
                    draft = ConversationDraft(),
                )
                advanceUntilIdle()

                harness.delegate.onMessageTextChanged(messageText = "Hello")
                harness.conversationIdFlow.value = "conversation-2"
                advanceUntilIdle()

                assertEquals(1, repository.savedDrafts.size)
                assertEquals(CONVERSATION_ID, repository.savedDrafts.single().conversationId)
                assertTrue(harness.delegate.state.value.draft.isCheckingDraft)
                assertEquals("", harness.delegate.state.value.draft.messageText)
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun sendSuccess_allowsAutosavingNewDraftBeforeRepositoryClearsSentDraft() {
        runTest {
            val repository = createConversationDraftsRepositoryMock()
            val sendConversationDraft = createSendConversationDraftMock()
            val harness = createBoundLoadedDelegateHarness(
                repository = repository,
                sendConversationDraft = sendConversationDraft,
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")
                harness.delegate.onSendClick()
                advanceUntilIdle()

                assertFalse(harness.delegate.state.value.draft.isSending)
                assertEquals("", harness.delegate.state.value.draft.messageText)

                harness.delegate.onMessageTextChanged(messageText = "Next")
                advanceTimeBy(300)
                advanceUntilIdle()

                assertEquals(1, repository.savedDrafts.size)
                assertEquals("Next", repository.savedDrafts.single().draft.messageText)
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun sendFailure_restoresIdleStateAndKeepsDraft() {
        runTest {
            val repository = createConversationDraftsRepositoryMock()
            val sendConversationDraft = createSendConversationDraftMock(
                flowFactory = SendFlowFactory {
                    flow {
                        throw IllegalStateException("boom")
                    }
                },
            )
            val harness = createBoundLoadedDelegateHarness(
                repository = repository,
                sendConversationDraft = sendConversationDraft,
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")
                harness.delegate.onSendClick()
                advanceUntilIdle()

                assertFalse(harness.delegate.state.value.draft.isSending)
                assertEquals("Hello", harness.delegate.state.value.draft.messageText)
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun sendCancellation_restoresIdleState() {
        runTest {
            val repository = createConversationDraftsRepositoryMock()
            val sendConversationDraft = createSendConversationDraftMock(
                flowFactory = SendFlowFactory {
                    flow {
                        throw CancellationException("cancelled")
                    }
                },
            )
            val harness = createBoundLoadedDelegateHarness(
                repository = repository,
                sendConversationDraft = sendConversationDraft,
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")
                harness.delegate.onSendClick()
                advanceUntilIdle()

                assertFalse(harness.delegate.state.value.draft.isSending)
                assertEquals("Hello", harness.delegate.state.value.draft.messageText)
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun typingDuringSendIsPreservedWhenDispatchCompletes() {
        runTest {
            val repository = createConversationDraftsRepositoryMock()
            val sendGate = CompletableDeferred<Unit>()
            val sendConversationDraft = createSendConversationDraftMock(
                flowFactory = SendFlowFactory {
                    flow {
                        sendGate.await()
                        emit(Unit)
                    }
                },
            )
            val harness = createBoundLoadedDelegateHarness(
                repository = repository,
                sendConversationDraft = sendConversationDraft,
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")
                harness.delegate.onSendClick()
                advanceUntilIdle()
                assertTrue(harness.delegate.state.value.draft.isSending)

                harness.delegate.onMessageTextChanged(messageText = "Next")
                sendGate.complete(Unit)
                advanceUntilIdle()

                assertFalse(harness.delegate.state.value.draft.isSending)
                assertEquals("Next", harness.delegate.state.value.draft.messageText)

                advanceTimeBy(300)
                advanceUntilIdle()

                assertEquals(1, repository.savedDrafts.size)
                assertEquals("Next", repository.savedDrafts.single().draft.messageText)
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun sendFlowCompletingWithoutEmissionRestoresIdleState() {
        runTest {
            val repository = createConversationDraftsRepositoryMock()
            val sendConversationDraft = createSendConversationDraftMock(
                flowFactory = SendFlowFactory {
                    emptyFlow()
                },
            )
            val harness = createBoundLoadedDelegateHarness(
                repository = repository,
                sendConversationDraft = sendConversationDraft,
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")
                harness.delegate.onSendClick()
                advanceUntilIdle()

                assertFalse(harness.delegate.state.value.draft.isSending)
                assertEquals("Hello", harness.delegate.state.value.draft.messageText)
            } finally {
                harness.cancel()
            }
        }
    }

    private suspend fun TestScope.createBoundLoadedDelegateHarness(
        repository: RepositoryMock,
        sendConversationDraft: SendConversationDraftMock = createSendConversationDraftMock(),
    ): DelegateHarness {
        val harness = createDelegateHarness(
            repository = repository,
            sendConversationDraft = sendConversationDraft,
        )
        harness.conversationIdFlow.value = CONVERSATION_ID
        repository.emitDraft(
            conversationId = CONVERSATION_ID,
            draft = ConversationDraft(),
        )
        advanceUntilIdle()

        return harness
    }

    private fun TestScope.createDelegateHarness(
        repository: RepositoryMock,
        sendConversationDraft: SendConversationDraftMock,
    ): DelegateHarness {
        val dispatcher = StandardTestDispatcher(scheduler = testScheduler)
        val applicationScope = TestScope(dispatcher)
        val delegateScope = TestScope(dispatcher)
        val delegate = ConversationDraftDelegateImpl(
            applicationScope = applicationScope,
            conversationDraftsRepository = repository.mock,
            sendConversationDraft = sendConversationDraft.mock,
            defaultDispatcher = dispatcher,
        )
        val conversationIdFlow = MutableStateFlow<String?>(null)

        delegate.bind(
            scope = delegateScope,
            conversationIdFlow = conversationIdFlow,
        )

        return DelegateHarness(
            delegate = delegate,
            conversationIdFlow = conversationIdFlow,
            delegateScope = delegateScope,
            applicationScope = applicationScope,
        )
    }

    private fun createConversationDraftsRepositoryMock(
        observeFailure: Exception? = null,
    ): RepositoryMock {
        val draftUpdatesByConversationId =
            mutableMapOf<String, MutableSharedFlow<ConversationDraft>>()
        val savedDrafts = mutableListOf<SaveDraftCall>()
        val mock = mockk<ConversationDraftsRepository>()
        val result = RepositoryMock(
            mock = mock,
            draftUpdatesByConversationId = draftUpdatesByConversationId,
            savedDrafts = savedDrafts,
        )

        every {
            mock.observeConversationDraft(any())
        } answers {
            observeFailure?.let { exception ->
                return@answers flow {
                    throw exception
                }
            }

            val conversationId = firstArg<String>()
            draftUpdatesByConversationId.getOrPut(conversationId) {
                MutableSharedFlow(
                    replay = 1,
                    extraBufferCapacity = 16,
                )
            }
        }
        coEvery {
            mock.saveDraft(any(), any())
        } answers {
            result.saveFailure?.let { exception ->
                throw exception
            }

            savedDrafts += SaveDraftCall(
                conversationId = firstArg(),
                draft = secondArg(),
            )
        }

        return result
    }

    private fun createSendConversationDraftMock(
        flowFactory: SendFlowFactory = createDefaultSendFlowFactory(),
    ): SendConversationDraftMock {
        val requests = mutableListOf<ConversationDraft>()
        val mock = mockk<SendConversationDraft>()
        every {
            mock.invoke(any(), any())
        } answers {
            val draft = secondArg<ConversationDraft>()
            requests += draft
            flowFactory.create(draft = draft)
        }
        return SendConversationDraftMock(
            mock = mock,
            requests = requests,
        )
    }

    private fun createDefaultSendFlowFactory(): SendFlowFactory {
        return SendFlowFactory {
            flow {
                emit(Unit)
            }
        }
    }

    private data class SaveDraftCall(
        val conversationId: String,
        val draft: ConversationDraft,
    )

    private data class DelegateHarness(
        val delegate: ConversationDraftDelegateImpl,
        val conversationIdFlow: MutableStateFlow<String?>,
        val delegateScope: TestScope,
        val applicationScope: TestScope,
    ) {
        fun cancel() {
            delegateScope.cancel()
            applicationScope.cancel()
        }
    }

    private data class RepositoryMock(
        val mock: ConversationDraftsRepository,
        val draftUpdatesByConversationId: MutableMap<String, MutableSharedFlow<ConversationDraft>>,
        val savedDrafts: MutableList<SaveDraftCall>,
        var saveFailure: Exception? = null,
    ) {
        suspend fun emitDraft(
            conversationId: String,
            draft: ConversationDraft,
        ) {
            draftUpdatesByConversationId.getOrPut(conversationId) {
                MutableSharedFlow(
                    replay = 1,
                    extraBufferCapacity = 16,
                )
            }.emit(draft)
        }
    }

    private data class SendConversationDraftMock(
        val mock: SendConversationDraft,
        val requests: MutableList<ConversationDraft>,
    )

    private fun interface SendFlowFactory {
        fun create(draft: ConversationDraft): Flow<Unit>
    }

    private companion object {
        private const val CONVERSATION_ID = "conversation-1"
    }
}
