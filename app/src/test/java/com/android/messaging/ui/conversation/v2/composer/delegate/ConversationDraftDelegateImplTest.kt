package com.android.messaging.ui.conversation.v2.composer.delegate

import android.app.Activity
import app.cash.turbine.test
import com.android.messaging.R
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.model.send.ConversationSendData
import com.android.messaging.data.conversation.repository.ConversationDraftsRepository
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.domain.conversation.usecase.action.CheckConversationActionRequirements
import com.android.messaging.domain.conversation.usecase.action.ConversationActionRequirementsResult
import com.android.messaging.domain.conversation.usecase.draft.GetConversationDraftSendProtocol
import com.android.messaging.domain.conversation.usecase.draft.SendConversationDraft
import com.android.messaging.domain.conversation.usecase.draft.exception.ConversationSimNotReadyException
import com.android.messaging.domain.conversation.usecase.draft.exception.DraftDispatchFailedException
import com.android.messaging.domain.conversation.usecase.draft.exception.TooManyVideoAttachmentsException
import com.android.messaging.domain.conversation.usecase.draft.exception.UnknownConversationRecipientException
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.ui.conversation.v2.composer.model.ConversationDraftState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration.Companion.milliseconds
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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationDraftDelegateImplTest {

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

                advanceTimeBy(299.milliseconds)
                assertTrue(repository.savedDrafts.isEmpty())

                advanceTimeBy(1.milliseconds)
                advanceUntilIdle()

                assertEquals(1, repository.savedDrafts.size)
                assertEquals("Hello", repository.savedDrafts.single().draft.messageText)
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onMessageTextChanged_resolvesDraftSendProtocolAfterDebounce() {
        runTest {
            val repository = createConversationDraftsRepositoryMock()
            val conversationsRepository = createConversationsRepositoryMock()
            val getDraftSendProtocol = createDraftSendProtocolMock(
                initialResult = ConversationDraftSendProtocol.MMS,
            )
            val harness = createBoundLoadedDelegateHarness(
                repository = repository,
                conversationsRepository = conversationsRepository,
                getDraftSendProtocol = getDraftSendProtocol,
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")

                advanceTimeBy(249.milliseconds)

                assertEquals(0, conversationsRepository.sendDataRequests.size)
                assertEquals(
                    ConversationDraftSendProtocol.SMS,
                    harness.delegate.state.value.sendProtocol,
                )

                advanceTimeBy(1.milliseconds)
                advanceUntilIdle()

                assertEquals(1, conversationsRepository.sendDataRequests.size)
                assertEquals(
                    ConversationSendDataRequest(
                        conversationId = CONVERSATION_ID,
                        selfParticipantId = "",
                    ),
                    conversationsRepository.sendDataRequests.single(),
                )
                assertEquals("Hello", getDraftSendProtocol.requests.single().messageText)
                assertEquals(
                    ConversationDraftSendProtocol.MMS,
                    harness.delegate.state.value.sendProtocol,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onMessageTextChanged_debouncesDraftSendProtocolUntilTypingSettles() {
        runTest {
            val repository = createConversationDraftsRepositoryMock()
            val conversationsRepository = createConversationsRepositoryMock()
            val getDraftSendProtocol = createDraftSendProtocolMock(
                initialResult = ConversationDraftSendProtocol.MMS,
            )
            val harness = createBoundLoadedDelegateHarness(
                repository = repository,
                conversationsRepository = conversationsRepository,
                getDraftSendProtocol = getDraftSendProtocol,
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "H")
                advanceTimeBy(100.milliseconds)
                harness.delegate.onMessageTextChanged(messageText = "He")
                advanceTimeBy(100.milliseconds)
                harness.delegate.onMessageTextChanged(messageText = "Hel")
                advanceTimeBy(249.milliseconds)

                assertEquals(0, conversationsRepository.sendDataRequests.size)
                assertTrue(getDraftSendProtocol.requests.isEmpty())

                advanceTimeBy(1.milliseconds)
                advanceUntilIdle()

                assertEquals(1, conversationsRepository.sendDataRequests.size)
                assertEquals(1, getDraftSendProtocol.requests.size)
                assertEquals("Hel", getDraftSendProtocol.requests.single().messageText)
                assertEquals(
                    ConversationDraftSendProtocol.MMS,
                    harness.delegate.state.value.sendProtocol,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onMessageTextChanged_whenDraftBecomesEmpty_resetsDraftSendProtocolToSms() {
        runTest {
            val repository = createConversationDraftsRepositoryMock()
            val conversationsRepository = createConversationsRepositoryMock()
            val getDraftSendProtocol = createDraftSendProtocolMock(
                initialResult = ConversationDraftSendProtocol.MMS,
            )
            val harness = createBoundLoadedDelegateHarness(
                repository = repository,
                conversationsRepository = conversationsRepository,
                getDraftSendProtocol = getDraftSendProtocol,
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")
                advanceTimeBy(250.milliseconds)
                advanceUntilIdle()

                assertEquals(
                    ConversationDraftSendProtocol.MMS,
                    harness.delegate.state.value.sendProtocol,
                )

                harness.delegate.onMessageTextChanged(messageText = "")

                assertEquals(
                    ConversationDraftSendProtocol.SMS,
                    harness.delegate.state.value.sendProtocol,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun addAttachments_whenSendDataIsUnavailable_fallsBackToMmsDraftProtocol() {
        runTest {
            val repository = createConversationDraftsRepositoryMock()
            val conversationsRepository = createConversationsRepositoryMock(
                sendData = null,
            )
            val getDraftSendProtocol = createDraftSendProtocolMock(
                initialResult = ConversationDraftSendProtocol.SMS,
            )
            val harness = createBoundLoadedDelegateHarness(
                repository = repository,
                conversationsRepository = conversationsRepository,
                getDraftSendProtocol = getDraftSendProtocol,
            )

            try {
                harness.delegate.addAttachments(
                    attachments = listOf(
                        ConversationDraftAttachment(
                            contentType = "image/jpeg",
                            contentUri = "content://images/1",
                        ),
                    ),
                )
                advanceTimeBy(250.milliseconds)
                advanceUntilIdle()

                assertEquals(1, conversationsRepository.sendDataRequests.size)
                assertTrue(getDraftSendProtocol.requests.isEmpty())
                assertEquals(
                    ConversationDraftSendProtocol.MMS,
                    harness.delegate.state.value.sendProtocol,
                )
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
                advanceTimeBy(300.milliseconds)
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
                flowFactory = {
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
    fun sendValidationFailure_whenRecipientIsUnknown_emitsUnknownSenderMessage() {
        runTest {
            assertSendFailureMessage(
                exception = UnknownConversationRecipientException(
                    conversationId = CONVERSATION_ID,
                ),
                expectedMessageResId = R.string.unknown_sender,
            )
        }
    }

    @Test
    fun sendValidationFailure_whenSimIsNotReady_emitsNetworkNotReadyMessage() {
        runTest {
            assertSendFailureMessage(
                exception = ConversationSimNotReadyException(
                    conversationId = CONVERSATION_ID,
                    selfSubId = 1,
                    cause = IllegalStateException("SIM unavailable"),
                ),
                expectedMessageResId = R.string.cant_send_message_without_active_subscription,
            )
        }
    }

    @Test
    fun sendValidationFailure_whenTooManyVideos_emitsVideoLimitMessage() {
        runTest {
            assertSendFailureMessage(
                exception = TooManyVideoAttachmentsException(
                    conversationId = CONVERSATION_ID,
                    videoAttachmentCount = 2,
                ),
                expectedMessageResId = R.string.cant_send_message_with_multiple_videos,
            )
        }
    }

    @Test
    fun sendValidationFailure_whenDispatchFails_emitsGenericSendFailureMessage() {
        runTest {
            assertSendFailureMessage(
                exception = DraftDispatchFailedException(
                    conversationId = CONVERSATION_ID,
                    cause = IllegalStateException("boom"),
                ),
                expectedMessageResId = R.string.send_message_failure,
            )
        }
    }

    @Test
    fun sendCancellation_restoresIdleState() {
        runTest {
            val repository = createConversationDraftsRepositoryMock()
            val sendConversationDraft = createSendConversationDraftMock(
                flowFactory = {
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

    private suspend fun TestScope.assertSendFailureMessage(
        exception: Throwable,
        expectedMessageResId: Int,
    ) {
        val repository = createConversationDraftsRepositoryMock()
        val sendConversationDraft = createSendConversationDraftMock(
            flowFactory = {
                flow {
                    throw exception
                }
            },
        )
        val harness = createBoundLoadedDelegateHarness(
            repository = repository,
            sendConversationDraft = sendConversationDraft,
        )

        try {
            harness.delegate.onMessageTextChanged(messageText = "Hello")

            harness.delegate.effects.test {
                harness.delegate.onSendClick()
                advanceUntilIdle()

                assertFalse(harness.delegate.state.value.draft.isSending)
                assertEquals("Hello", harness.delegate.state.value.draft.messageText)
                assertEquals(
                    ConversationScreenEffect.ShowMessage(
                        messageResId = expectedMessageResId,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            harness.cancel()
        }
    }

    @Test
    fun typingDuringSendIsPreservedWhenDispatchCompletes() {
        runTest {
            val repository = createConversationDraftsRepositoryMock()
            val sendGate = CompletableDeferred<Unit>()
            val sendConversationDraft = createSendConversationDraftMock(
                flowFactory = {
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

                advanceTimeBy(300.milliseconds)
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
                flowFactory = {
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

    @Test
    fun onSendClick_whenSmsIsNotCapable_emitsSmsDisabledMessage() {
        runTest {
            val repository = createConversationDraftsRepositoryMock()
            val harness = createBoundLoadedDelegateHarness(
                repository = repository,
                actionRequirements = createActionRequirementsMock(
                    initialResult = ConversationActionRequirementsResult.SmsNotCapable,
                ),
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")

                harness.delegate.effects.test {
                    harness.delegate.onSendClick()
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.ShowMessage(
                            messageResId = R.string.sms_disabled,
                        ),
                        awaitItem(),
                    )
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onSendClick_whenPreferredSmsSimIsMissing_emitsNoPreferredSimMessage() {
        runTest {
            val repository = createConversationDraftsRepositoryMock()
            val harness = createBoundLoadedDelegateHarness(
                repository = repository,
                actionRequirements = createActionRequirementsMock(
                    initialResult = ConversationActionRequirementsResult.NoPreferredSmsSim,
                ),
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")

                harness.delegate.effects.test {
                    harness.delegate.onSendClick()
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.ShowMessage(
                            messageResId = R.string.no_preferred_sim_selected,
                        ),
                        awaitItem(),
                    )
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onSendClick_whenDefaultSmsRoleIsMissing_promptsAndSendsAfterRoleRequestSucceeds() {
        runTest {
            val repository = createConversationDraftsRepositoryMock()
            val sendConversationDraft = createSendConversationDraftMock()
            val actionRequirements = createActionRequirementsMock(
                initialResult = ConversationActionRequirementsResult.MissingDefaultSmsRole,
            )
            val harness = createBoundLoadedDelegateHarness(
                repository = repository,
                sendConversationDraft = sendConversationDraft,
                actionRequirements = actionRequirements,
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")

                harness.delegate.effects.test {
                    harness.delegate.onSendClick()
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.RequestDefaultSmsRole(isSending = true),
                        awaitItem(),
                    )
                    assertTrue(sendConversationDraft.requests.isEmpty())

                    actionRequirements.result = ConversationActionRequirementsResult.Ready
                    assertTrue(
                        harness.delegate.onDefaultSmsRoleRequestResult(
                            resultCode = Activity.RESULT_OK,
                        ),
                    )
                    advanceUntilIdle()

                    assertEquals(1, sendConversationDraft.requests.size)
                    assertEquals("Hello", sendConversationDraft.requests.single().messageText)
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onDefaultSmsRoleRequestResult_withoutPendingSend_returnsFalse() {
        runTest {
            val repository = createConversationDraftsRepositoryMock()
            val harness = createBoundLoadedDelegateHarness(repository = repository)

            try {
                assertFalse(
                    harness.delegate.onDefaultSmsRoleRequestResult(
                        resultCode = Activity.RESULT_OK,
                    ),
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onDefaultSmsRoleRequestResult_whenCanceled_clearsPendingSendWithoutSending() {
        runTest {
            val repository = createConversationDraftsRepositoryMock()
            val sendConversationDraft = createSendConversationDraftMock()
            val actionRequirements = createActionRequirementsMock(
                initialResult = ConversationActionRequirementsResult.MissingDefaultSmsRole,
            )
            val harness = createBoundLoadedDelegateHarness(
                repository = repository,
                sendConversationDraft = sendConversationDraft,
                actionRequirements = actionRequirements,
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")

                harness.delegate.effects.test {
                    harness.delegate.onSendClick()
                    advanceUntilIdle()
                    awaitItem()

                    assertFalse(
                        harness.delegate.onDefaultSmsRoleRequestResult(
                            resultCode = Activity.RESULT_CANCELED,
                        ),
                    )
                    advanceUntilIdle()

                    assertTrue(sendConversationDraft.requests.isEmpty())
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                harness.cancel()
            }
        }
    }

    private suspend fun TestScope.createBoundLoadedDelegateHarness(
        repository: RepositoryMock,
        sendConversationDraft: SendConversationDraftMock = createSendConversationDraftMock(),
        actionRequirements: ActionRequirementsMock = createActionRequirementsMock(),
        conversationsRepository: ConversationsRepositoryMock = createConversationsRepositoryMock(),
        getDraftSendProtocol: DraftSendProtocolMock = createDraftSendProtocolMock(),
    ): DelegateHarness {
        val harness = createDelegateHarness(
            repository = repository,
            sendConversationDraft = sendConversationDraft,
            actionRequirements = actionRequirements,
            conversationsRepository = conversationsRepository,
            getDraftSendProtocol = getDraftSendProtocol,
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
        actionRequirements: ActionRequirementsMock = createActionRequirementsMock(),
        conversationsRepository: ConversationsRepositoryMock = createConversationsRepositoryMock(),
        getDraftSendProtocol: DraftSendProtocolMock = createDraftSendProtocolMock(),
    ): DelegateHarness {
        val dispatcher = StandardTestDispatcher(scheduler = testScheduler)
        val applicationScope = TestScope(dispatcher)
        val delegateScope = TestScope(dispatcher)
        val delegate = ConversationDraftDelegateImpl(
            applicationScope = applicationScope,
            checkConversationActionRequirements = actionRequirements.mock,
            conversationDraftsRepository = repository.mock,
            conversationsRepository = conversationsRepository.mock,
            getConversationDraftSendProtocol = getDraftSendProtocol.mock,
            sendConversationDraft = sendConversationDraft.mock,
            defaultDispatcher = dispatcher,
            ioDispatcher = dispatcher,
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

    private fun createActionRequirementsMock(
        initialResult: ConversationActionRequirementsResult =
            ConversationActionRequirementsResult.Ready,
    ): ActionRequirementsMock {
        val mock = mockk<CheckConversationActionRequirements>()
        val result = ActionRequirementsMock(
            mock = mock,
            result = initialResult,
        )
        every { mock.invoke() } answers { result.result }
        return result
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

    private fun createConversationsRepositoryMock(
        sendData: ConversationSendData? = mockk(relaxed = true),
    ): ConversationsRepositoryMock {
        val mock = mockk<ConversationsRepository>(relaxed = true)
        val sendDataRequests = mutableListOf<ConversationSendDataRequest>()

        every {
            mock.getConversationSendData(any(), any())
        } answers {
            sendDataRequests += ConversationSendDataRequest(
                conversationId = firstArg(),
                selfParticipantId = secondArg(),
            )
            sendData
        }

        return ConversationsRepositoryMock(
            mock = mock,
            sendDataRequests = sendDataRequests,
        )
    }

    private fun createDraftSendProtocolMock(
        initialResult: ConversationDraftSendProtocol = ConversationDraftSendProtocol.SMS,
    ): DraftSendProtocolMock {
        val mock = mockk<GetConversationDraftSendProtocol>()
        val requests = mutableListOf<ConversationDraft>()
        val result = DraftSendProtocolMock(
            mock = mock,
            requests = requests,
            protocol = initialResult,
        )

        every {
            mock.invoke(any(), any())
        } answers {
            val draft = firstArg<ConversationDraft>()
            requests += draft
            result.protocol
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

    private data class ConversationSendDataRequest(
        val conversationId: String,
        val selfParticipantId: String,
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

    private data class ConversationsRepositoryMock(
        val mock: ConversationsRepository,
        val sendDataRequests: MutableList<ConversationSendDataRequest>,
    )

    private data class DraftSendProtocolMock(
        val mock: GetConversationDraftSendProtocol,
        val requests: MutableList<ConversationDraft>,
        var protocol: ConversationDraftSendProtocol,
    )

    private data class SendConversationDraftMock(
        val mock: SendConversationDraft,
        val requests: MutableList<ConversationDraft>,
    )

    private data class ActionRequirementsMock(
        val mock: CheckConversationActionRequirements,
        var result: ConversationActionRequirementsResult,
    )

    private fun interface SendFlowFactory {
        fun create(draft: ConversationDraft): Flow<Unit>
    }

    private companion object {
        private const val CONVERSATION_ID = "conversation-1"
    }
}
