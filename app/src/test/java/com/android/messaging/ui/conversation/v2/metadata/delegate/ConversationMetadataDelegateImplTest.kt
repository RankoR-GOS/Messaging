package com.android.messaging.ui.conversation.v2.metadata.delegate

import app.cash.turbine.test
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.conversation.model.metadata.ConversationMetadata
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.ui.conversation.v2.metadata.mapper.ConversationMetadataUiStateMapper
import com.android.messaging.ui.conversation.v2.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationMetadataDelegateImplTest {

    @Test
    fun onArchiveConversationClick_archivesViaRepositoryAndEmitsCloseConversation() {
        runTest {
            val harness = createHarness(conversationId = "conversation-42")

            try {
                harness.delegate.effects.test {
                    harness.delegate.onArchiveConversationClick()
                    advanceUntilIdle()

                    assertEquals(ConversationScreenEffect.CloseConversation, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }

                verify(exactly = 1) {
                    harness.conversationsRepository
                        .archiveConversation(conversationId = "conversation-42")
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onUnarchiveConversationClick_unarchivesViaRepositoryWithoutClosing() {
        runTest {
            val harness = createHarness(conversationId = "conversation-42")

            try {
                harness.delegate.effects.test {
                    harness.delegate.onUnarchiveConversationClick()
                    advanceUntilIdle()

                    expectNoEvents()
                    cancelAndIgnoreRemainingEvents()
                }

                verify(exactly = 1) {
                    harness.conversationsRepository
                        .unarchiveConversation(conversationId = "conversation-42")
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onAddContactClick_emitsLaunchAddContactFlowWithDestination() {
        runTest {
            val harness = createHarness(conversationId = "conversation-42")

            try {
                harness.setPresentState(
                    otherParticipantPhoneNumber = "+15551234567",
                )
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onAddContactClick()
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.LaunchAddContactFlow(
                            destination = "+15551234567",
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
    fun onAddContactClick_doesNothingWhenDestinationMissing() {
        runTest {
            val harness = createHarness(conversationId = "conversation-42")

            try {
                harness.setPresentState(otherParticipantPhoneNumber = null)
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onAddContactClick()
                    advanceUntilIdle()

                    expectNoEvents()
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onDeleteConversationClick_togglesConfirmationVisibility() {
        runTest {
            val harness = createHarness(conversationId = "conversation-42")

            try {
                advanceUntilIdle()
                assertEquals(
                    false,
                    harness.delegate.isDeleteConversationConfirmationVisible.value,
                )

                harness.delegate.onDeleteConversationClick()
                advanceUntilIdle()
                assertEquals(
                    true,
                    harness.delegate.isDeleteConversationConfirmationVisible.value,
                )

                harness.delegate.dismissDeleteConversationConfirmation()
                advanceUntilIdle()
                assertEquals(
                    false,
                    harness.delegate.isDeleteConversationConfirmationVisible.value,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun confirmDeleteConversation_deletesViaRepositoryAndEmitsCloseConversation() {
        runTest {
            val harness = createHarness(conversationId = "conversation-42")

            try {
                advanceUntilIdle()
                harness.delegate.onDeleteConversationClick()
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.confirmDeleteConversation()
                    advanceUntilIdle()

                    assertEquals(ConversationScreenEffect.CloseConversation, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }

                assertEquals(
                    false,
                    harness.delegate.isDeleteConversationConfirmationVisible.value,
                )
                verify(exactly = 1) {
                    harness.conversationsRepository
                        .deleteConversation(conversationId = "conversation-42")
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun commandMethods_doNothingWhenConversationIdIsBlank() {
        runTest {
            val harness = createHarness(conversationId = null)

            try {
                harness.delegate.effects.test {
                    harness.delegate.onArchiveConversationClick()
                    harness.delegate.onUnarchiveConversationClick()
                    harness.delegate.onDeleteConversationClick()
                    harness.delegate.confirmDeleteConversation()
                    advanceUntilIdle()

                    expectNoEvents()
                    cancelAndIgnoreRemainingEvents()
                }

                assertEquals(
                    false,
                    harness.delegate.isDeleteConversationConfirmationVisible.value,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    private fun TestScope.createHarness(conversationId: String?): DelegateHarness {
        val dispatcher = StandardTestDispatcher(scheduler = testScheduler)
        val scope = TestScope(dispatcher)
        val conversationsRepository = mockk<ConversationsRepository>(relaxUnitFun = true)
        val mapper = mockk<ConversationMetadataUiStateMapper>()
        val conversationIdFlow = MutableStateFlow(conversationId)
        val metadataFlow = MutableStateFlow<ConversationMetadata?>(value = null)

        every {
            conversationsRepository.getConversationMetadata(any())
        } returns metadataFlow
        every {
            mapper.map(metadata = any())
        } answers {
            val metadata = firstArg<ConversationMetadata>()
            ConversationMetadataUiState.Present(
                title = "Carol",
                selfParticipantId = "self-1",
                isGroupConversation = false,
                participantCount = 2,
                otherParticipantPhoneNumber = metadata.otherParticipantNormalizedDestination,
                otherParticipantContactLookupKey = null,
                isArchived = false,
                composerAvailability = ConversationComposerAvailability.editable(),
            )
        }

        val delegate = ConversationMetadataDelegateImpl(
            conversationsRepository = conversationsRepository,
            conversationMetadataUiStateMapper = mapper,
            defaultDispatcher = dispatcher,
        )
        delegate.bind(
            scope = scope,
            conversationIdFlow = conversationIdFlow,
        )

        return DelegateHarness(
            delegate = delegate,
            conversationsRepository = conversationsRepository,
            metadataFlow = metadataFlow,
            scope = scope,
        )
    }

    private fun DelegateHarness.setPresentState(
        otherParticipantPhoneNumber: String?,
    ) {
        metadataFlow.value = mockk<ConversationMetadata>(relaxed = true) {
            every { otherParticipantNormalizedDestination } returns otherParticipantPhoneNumber
        }
    }

    private data class DelegateHarness(
        val delegate: ConversationMetadataDelegateImpl,
        val conversationsRepository: ConversationsRepository,
        val metadataFlow: MutableStateFlow<ConversationMetadata?>,
        val scope: TestScope,
    ) {
        fun cancel() {
            scope.cancel()
        }
    }
}
