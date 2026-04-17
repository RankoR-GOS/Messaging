package com.android.messaging.data.conversation.repository

import android.content.ContentResolver
import android.database.ContentObserver
import app.cash.turbine.test
import com.android.messaging.data.conversation.mapper.ConversationDraftMessageDataMapperImpl
import com.android.messaging.data.conversation.mapper.ConversationMessageDataDraftMapperImpl
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.MessageData
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationDraftsRepositoryImplTest {

    private lateinit var contentResolver: ContentResolver
    private lateinit var conversationDraftStore: ConversationDraftStore
    private lateinit var conversationMetadataNotifier: ConversationMetadataNotifier

    @Before
    fun setUp() {
        contentResolver = mockk()
        conversationDraftStore = mockk()
        conversationMetadataNotifier = mockk()
        every {
            conversationMetadataNotifier.notifyConversationMetadataChanged(any())
        } just runs
    }

    @Test
    fun observeConversationDraft_registersAndUnregistersObserverForCollection() {
        runTest {
            val registeredObserver = slot<ContentObserver>()
            val expectedUri = MessagingContentProvider.buildConversationMetadataUri(CONVERSATION_ID)
            val repository = createRepository()

            every {
                conversationDraftStore.getConversation(CONVERSATION_ID)
            } returns ConversationDraftConversation(
                selfParticipantId = "self-1",
            )
            every {
                conversationDraftStore.readDraftMessage(
                    conversationId = CONVERSATION_ID,
                    selfParticipantId = "self-1",
                )
            } returns MessageData.createDraftSmsMessage(
                CONVERSATION_ID,
                "self-1",
                "Hello",
            )
            stubObserverRegistration(
                expectedUri = expectedUri,
                registeredObserver = registeredObserver,
            )

            repository.observeConversationDraft(conversationId = CONVERSATION_ID).test {
                assertEquals("Hello", awaitItem().messageText)
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 1) {
                contentResolver.registerContentObserver(
                    expectedUri,
                    true,
                    registeredObserver.captured,
                )
            }
            verify(exactly = 1) {
                contentResolver.unregisterContentObserver(registeredObserver.captured)
            }
        }
    }

    @Test
    fun observeConversationDraft_reloadsDraftWhenObserverChanges() {
        runTest {
            val registeredObserver = slot<ContentObserver>()
            val expectedUri = MessagingContentProvider.buildConversationMetadataUri(CONVERSATION_ID)
            val repository = createRepository()
            var currentDraftMessage: MessageData? = MessageData.createDraftSmsMessage(
                CONVERSATION_ID,
                "self-1",
                "Before",
            )

            every {
                conversationDraftStore.getConversation(CONVERSATION_ID)
            } returns ConversationDraftConversation(
                selfParticipantId = "self-1",
            )
            every {
                conversationDraftStore.readDraftMessage(
                    conversationId = CONVERSATION_ID,
                    selfParticipantId = "self-1",
                )
            } answers {
                currentDraftMessage
            }
            stubObserverRegistration(
                expectedUri = expectedUri,
                registeredObserver = registeredObserver,
            )

            repository.observeConversationDraft(conversationId = CONVERSATION_ID).test {
                assertEquals("Before", awaitItem().messageText)

                currentDraftMessage = MessageData.createDraftMmsMessage(
                    CONVERSATION_ID,
                    "self-1",
                    "",
                    "Updated subject",
                )
                registeredObserver.captured.onChange(false)

                val updatedDraft = awaitItem()
                assertEquals("Updated subject", updatedDraft.subjectText)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun observeConversationDraft_emitsEmptyDraftWhenConversationDoesNotExist() {
        runTest {
            val registeredObserver = slot<ContentObserver>()
            val expectedUri = MessagingContentProvider.buildConversationMetadataUri(CONVERSATION_ID)
            val repository = createRepository()

            every {
                conversationDraftStore.getConversation(CONVERSATION_ID)
            } returns null
            stubObserverRegistration(
                expectedUri = expectedUri,
                registeredObserver = registeredObserver,
            )

            repository.observeConversationDraft(conversationId = CONVERSATION_ID).test {
                assertEquals(ConversationDraft(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun observeConversationDraft_emitsSafeEmptyDraftWhenLoadingFails() {
        runTest {
            val registeredObserver = slot<ContentObserver>()
            val expectedUri = MessagingContentProvider.buildConversationMetadataUri(CONVERSATION_ID)
            val repository = createRepository()

            every {
                conversationDraftStore.getConversation(CONVERSATION_ID)
            } throws IllegalStateException("boom")
            stubObserverRegistration(
                expectedUri = expectedUri,
                registeredObserver = registeredObserver,
            )

            repository.observeConversationDraft(conversationId = CONVERSATION_ID).test {
                assertEquals(ConversationDraft(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun saveDraft_bindsMissingParticipantsAndNotifiesMetadata() {
        runTest {
            val updatedMessage = slot<MessageData>()
            val repository = createRepository()

            every {
                conversationDraftStore.getConversation(CONVERSATION_ID)
            } returns ConversationDraftConversation(
                selfParticipantId = "self-1",
            )
            every {
                conversationDraftStore.updateDraftMessage(
                    conversationId = CONVERSATION_ID,
                    message = capture(updatedMessage),
                )
            } just runs

            repository.saveDraft(
                conversationId = CONVERSATION_ID,
                draft = ConversationDraft(
                    messageText = "Hello",
                    selfParticipantId = "",
                ),
            )

            assertEquals("self-1", updatedMessage.captured.selfId)
            assertEquals("self-1", updatedMessage.captured.participantId)
            verify(exactly = 1) {
                conversationMetadataNotifier.notifyConversationMetadataChanged(CONVERSATION_ID)
            }
        }
    }

    @Test
    fun saveDraft_returnsWithoutPersistingWhenConversationWasDeletedBeforeBinding() {
        runTest {
            val repository = createRepository()

            every {
                conversationDraftStore.getConversation(CONVERSATION_ID)
            } returns null

            repository.saveDraft(
                conversationId = CONVERSATION_ID,
                draft = ConversationDraft(
                    messageText = "Hello",
                    selfParticipantId = "",
                ),
            )

            verify(exactly = 0) {
                conversationDraftStore.updateDraftMessage(any(), any())
            }
            verify(exactly = 0) {
                conversationMetadataNotifier.notifyConversationMetadataChanged(any())
            }
        }
    }

    @Test
    fun saveDraft_preservesProvidedSelfParticipantId() {
        runTest {
            val updatedMessage = slot<MessageData>()
            val repository = createRepository()

            every {
                conversationDraftStore.getConversation(CONVERSATION_ID)
            } returns ConversationDraftConversation(
                selfParticipantId = "self-1",
            )
            every {
                conversationDraftStore.updateDraftMessage(
                    conversationId = CONVERSATION_ID,
                    message = capture(updatedMessage),
                )
            } just runs

            repository.saveDraft(
                conversationId = CONVERSATION_ID,
                draft = ConversationDraft(
                    messageText = "Hello",
                    selfParticipantId = "self-2",
                ),
            )

            assertEquals("self-2", updatedMessage.captured.selfId)
            assertEquals("self-2", updatedMessage.captured.participantId)
        }
    }

    private fun createRepository(): ConversationDraftsRepositoryImpl {
        return ConversationDraftsRepositoryImpl(
            contentResolver = contentResolver,
            conversationDraftMessageDataMapper = ConversationDraftMessageDataMapperImpl(),
            conversationMessageDataDraftMapper = ConversationMessageDataDraftMapperImpl(),
            conversationDraftStore = conversationDraftStore,
            conversationMetadataNotifier = conversationMetadataNotifier,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    private fun stubObserverRegistration(
        expectedUri: android.net.Uri,
        registeredObserver: io.mockk.CapturingSlot<ContentObserver>,
    ) {
        every {
            contentResolver.registerContentObserver(
                expectedUri,
                true,
                capture(registeredObserver),
            )
        } just runs
        every {
            contentResolver.unregisterContentObserver(any())
        } just runs
    }

    private companion object {
        private const val CONVERSATION_ID = "conversation-1"
    }
}
