package com.android.messaging.ui.conversation.v2.entry

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.android.messaging.R
import com.android.messaging.data.conversation.mapper.ConversationMessageDataDraftMapper
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.domain.conversation.usecase.participant.IsConversationRecipientLimitExceeded
import com.android.messaging.domain.conversation.usecase.participant.ResolveConversationId
import com.android.messaging.domain.conversation.usecase.participant.model.ResolveConversationIdResult
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryEffect
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryLaunchRequest
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryStartupAttachment
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var conversationMessageDataDraftMapper: ConversationMessageDataDraftMapper
    private lateinit var isConversationRecipientLimitExceeded: IsConversationRecipientLimitExceeded
    private lateinit var resolveConversationId: ResolveConversationId

    @Before
    fun setUp() {
        conversationMessageDataDraftMapper = mockk()
        isConversationRecipientLimitExceeded = mockk()
        resolveConversationId = mockk()
        every {
            isConversationRecipientLimitExceeded.invoke(participantCount = any())
        } returns false
    }

    @Test
    fun onCreateGroupRequested_emitsRecipientPickerNavigationEffect() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val viewModel = createViewModel()

        viewModel.onCreateGroupRequested()

        assertEquals(true, viewModel.uiState.value.isCreatingGroup)
        assertEquals(
            emptyList<String>(),
            viewModel.uiState.value.selectedGroupRecipientDestinations,
        )
    }

    @Test
    fun onCreateGroupRequested_cancelsPendingConversationResolution() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val firstResolutionResult = CompletableDeferred<ResolveConversationIdResult>()
        coEvery {
            resolveConversationId.invoke(
                destinations = listOf("+1 555 0100"),
            )
        } coAnswers {
            firstResolutionResult.await()
        }
        val viewModel = createViewModel()

        viewModel.onNewChatRecipientSelected(destination = "+1 555 0100")
        runCurrent()
        viewModel.onCreateGroupRequested()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isCreatingGroup)
        assertEquals(false, viewModel.uiState.value.isResolvingConversation)
        assertEquals(
            false,
            viewModel.uiState.value.isResolvingConversationIndicatorVisible,
        )
        assertNull(viewModel.uiState.value.resolvingRecipientDestination)
    }

    @Test
    fun onCreateGroupCanceled_exitsInlineModeAndClearsSelections() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val viewModel = createViewModel()

        viewModel.onCreateGroupRequested()
        viewModel.onCreateGroupRecipientClicked(destination = "+1 555 0100")
        viewModel.onCreateGroupCanceled()

        assertEquals(false, viewModel.uiState.value.isCreatingGroup)
        assertEquals(
            emptyList<String>(),
            viewModel.uiState.value.selectedGroupRecipientDestinations,
        )
    }

    @Test
    fun onCreateGroupRecipientClicked_togglesSelection() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val viewModel = createViewModel()

        viewModel.onCreateGroupRequested()
        viewModel.onCreateGroupRecipientClicked(destination = "+1 555 0100")
        viewModel.onCreateGroupRecipientClicked(destination = "+1 555 0101")

        assertEquals(
            listOf("+1 555 0100", "+1 555 0101"),
            viewModel.uiState.value.selectedGroupRecipientDestinations,
        )

        viewModel.onCreateGroupRecipientClicked(destination = "+1 555 0100")

        assertEquals(
            listOf("+1 555 0101"),
            viewModel.uiState.value.selectedGroupRecipientDestinations,
        )
    }

    @Test
    fun onCreateGroupConfirmed_resolvesSelectedRecipientsAndNavigates() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        coEvery {
            resolveConversationId.invoke(
                destinations = listOf("+1 555 0100", "+1 555 0101"),
            )
        } returns ResolveConversationIdResult.Resolved(
            conversationId = "conversation-group",
        )
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onCreateGroupRequested()
            viewModel.onCreateGroupRecipientClicked(destination = "+1 555 0100")
            viewModel.onCreateGroupRecipientClicked(destination = "+1 555 0101")
            viewModel.onCreateGroupConfirmed()
            advanceUntilIdle()

            assertEquals(
                ConversationEntryEffect.NavigateToConversation(
                    conversationId = "conversation-group",
                ),
                awaitItem(),
            )
            assertEquals(false, viewModel.uiState.value.isCreatingGroup)
            assertEquals(
                emptyList<String>(),
                viewModel.uiState.value.selectedGroupRecipientDestinations,
            )
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) {
            resolveConversationId.invoke(
                destinations = listOf("+1 555 0100", "+1 555 0101"),
            )
        }
    }

    @Test
    fun onNewChatRecipientLongPressed_entersCreateGroupModeWithSelectedRecipient() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val viewModel = createViewModel()

        viewModel.onNewChatRecipientLongPressed(destination = "+1 555 0100")

        assertEquals(true, viewModel.uiState.value.isCreatingGroup)
        assertEquals(
            listOf("+1 555 0100"),
            viewModel.uiState.value.selectedGroupRecipientDestinations,
        )
        assertEquals(false, viewModel.uiState.value.isResolvingConversation)
    }

    @Test
    fun onLaunchRequest_mapsDraftAndPersistsLaunchState() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val draftData = MessageData.createDraftSmsMessage(
            "conversation-1",
            "self-1",
            "Hello",
        )
        val mappedDraft = ConversationDraft(
            messageText = "Hello",
            selfParticipantId = "self-1",
        )
        every {
            conversationMessageDataDraftMapper.map(messageData = draftData)
        } returns mappedDraft

        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle = savedStateHandle)

        viewModel.onLaunchRequest(
            launchRequest = ConversationEntryLaunchRequest(
                launchGeneration = 3,
                conversationId = "conversation-1",
                draftData = draftData,
                startupAttachmentUri = "content://media/image/1",
                startupAttachmentType = "image/jpeg",
                messagePosition = 12,
            ),
        )

        assertEquals(3, viewModel.uiState.value.launchGeneration)
        assertEquals("conversation-1", viewModel.uiState.value.conversationId)
        assertEquals(mappedDraft, viewModel.uiState.value.pendingDraft)
        assertEquals(
            ConversationEntryStartupAttachment(
                contentType = "image/jpeg",
                contentUri = "content://media/image/1",
            ),
            viewModel.uiState.value.pendingStartupAttachment,
        )
        assertEquals(12, viewModel.uiState.value.pendingScrollPosition)
        assertEquals(draftData, savedStateHandle[PENDING_DRAFT_DATA_KEY])
        assertEquals(12, savedStateHandle[PENDING_SCROLL_POSITION_KEY])
        assertEquals(3, savedStateHandle[PROCESSED_LAUNCH_GENERATION_KEY])
        assertEquals("conversation-1", savedStateHandle[CONVERSATION_ID_KEY])
        verify(exactly = 1) {
            conversationMessageDataDraftMapper.map(messageData = draftData)
        }
    }

    @Test
    fun onLaunchRequest_skipsProcessingWhenGenerationWasAlreadyHandled() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                PROCESSED_LAUNCH_GENERATION_KEY to 7,
                CONVERSATION_ID_KEY to "conversation-existing",
            ),
        )
        val viewModel = createViewModel(savedStateHandle = savedStateHandle)

        viewModel.onLaunchRequest(
            launchRequest = ConversationEntryLaunchRequest(
                launchGeneration = 7,
                conversationId = "conversation-new",
                draftData = MessageData.createDraftSmsMessage(
                    "conversation-new",
                    "self-1",
                    "Hello",
                ),
                messagePosition = 4,
            ),
        )

        assertEquals("conversation-existing", viewModel.uiState.value.conversationId)
        assertNull(viewModel.uiState.value.pendingScrollPosition)
        assertNull(savedStateHandle[PENDING_SCROLL_POSITION_KEY])
        verify(exactly = 0) {
            conversationMessageDataDraftMapper.map(messageData = any())
        }
    }

    @Test
    fun restoreUiState_readsPendingDraftAndStartupAttachmentFromSavedState() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val draftData = MessageData.createDraftSmsMessage(
            "conversation-2",
            "self-2",
            "Draft",
        )
        val mappedDraft = ConversationDraft(
            messageText = "Draft",
            selfParticipantId = "self-2",
        )
        every {
            conversationMessageDataDraftMapper.map(messageData = draftData)
        } returns mappedDraft
        val savedStateHandle = SavedStateHandle(
            mapOf(
                LAUNCH_GENERATION_KEY to 5,
                CONVERSATION_ID_KEY to "conversation-2",
                IS_CREATING_GROUP_KEY to true,
                LEGACY_IS_RESOLVING_CONVERSATION_KEY to true,
                PENDING_DRAFT_DATA_KEY to draftData,
                PENDING_SCROLL_POSITION_KEY to 17,
                PENDING_STARTUP_ATTACHMENT_URI_KEY to "content://media/image/2",
                PENDING_STARTUP_ATTACHMENT_TYPE_KEY to "image/png",
                SELECTED_GROUP_RECIPIENT_DESTINATIONS_KEY to arrayListOf(
                    "+1 555 0100",
                    "+1 555 0101",
                ),
            ),
        )

        val viewModel = createViewModel(savedStateHandle = savedStateHandle)

        assertEquals(5, viewModel.uiState.value.launchGeneration)
        assertEquals("conversation-2", viewModel.uiState.value.conversationId)
        assertEquals(true, viewModel.uiState.value.isCreatingGroup)
        assertEquals(false, viewModel.uiState.value.isResolvingConversation)
        assertEquals(
            false,
            viewModel.uiState.value.isResolvingConversationIndicatorVisible,
        )
        assertEquals(mappedDraft, viewModel.uiState.value.pendingDraft)
        assertEquals(17, viewModel.uiState.value.pendingScrollPosition)
        assertEquals(
            ConversationEntryStartupAttachment(
                contentType = "image/png",
                contentUri = "content://media/image/2",
            ),
            viewModel.uiState.value.pendingStartupAttachment,
        )
        assertEquals(
            listOf("+1 555 0100", "+1 555 0101"),
            viewModel.uiState.value.selectedGroupRecipientDestinations,
        )
        assertNull(viewModel.uiState.value.resolvingRecipientDestination)
        verify(exactly = 1) {
            conversationMessageDataDraftMapper.map(messageData = draftData)
        }
    }

    @Test
    fun onNewChatRecipientSelected_delaysIndicatorUntilThreshold() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val firstResolutionResult = CompletableDeferred<ResolveConversationIdResult>()
        coEvery {
            resolveConversationId.invoke(
                destinations = listOf("+1 555 0100"),
            )
        } coAnswers {
            firstResolutionResult.await()
        }
        val viewModel = createViewModel()

        viewModel.onNewChatRecipientSelected(destination = "+1 555 0100")
        runCurrent()

        assertEquals(true, viewModel.uiState.value.isResolvingConversation)
        assertEquals(
            false,
            viewModel.uiState.value.isResolvingConversationIndicatorVisible,
        )
        assertEquals("+1 555 0100", viewModel.uiState.value.resolvingRecipientDestination)

        advanceTimeBy(delayTimeMillis = RESOLVING_CONVERSATION_INDICATOR_DELAY_MILLIS - 1)
        runCurrent()

        assertEquals(
            false,
            viewModel.uiState.value.isResolvingConversationIndicatorVisible,
        )

        advanceTimeBy(delayTimeMillis = 1)
        runCurrent()

        assertEquals(
            true,
            viewModel.uiState.value.isResolvingConversationIndicatorVisible,
        )

        firstResolutionResult.complete(ResolveConversationIdResult.NotResolved)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isResolvingConversation)
        assertEquals(
            false,
            viewModel.uiState.value.isResolvingConversationIndicatorVisible,
        )
        assertNull(viewModel.uiState.value.resolvingRecipientDestination)
    }

    @Test
    fun onNewChatRecipientSelected_navigatesToResolvedConversation() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        coEvery {
            resolveConversationId.invoke(
                destinations = listOf("+1 555 0100"),
            )
        } returns ResolveConversationIdResult.Resolved(
            conversationId = "conversation-3",
        )
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onNewChatRecipientSelected(destination = "+1 555 0100")
            advanceUntilIdle()

            assertEquals(
                ConversationEntryEffect.NavigateToConversation(
                    conversationId = "conversation-3",
                ),
                awaitItem(),
            )
            assertEquals("conversation-3", viewModel.uiState.value.conversationId)
            assertEquals(false, viewModel.uiState.value.isResolvingConversation)
            assertEquals(
                false,
                viewModel.uiState.value.isResolvingConversationIndicatorVisible,
            )
            assertNull(viewModel.uiState.value.resolvingRecipientDestination)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) {
            resolveConversationId.invoke(
                destinations = listOf("+1 555 0100"),
            )
        }
    }

    @Test
    fun onNewChatRecipientSelected_whenResolutionFails_showsFailureMessage() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        coEvery {
            resolveConversationId.invoke(
                destinations = listOf("+1 555 0100"),
            )
        } returns ResolveConversationIdResult.NotResolved
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onNewChatRecipientSelected(destination = "+1 555 0100")
            advanceUntilIdle()

            assertEquals(
                ConversationEntryEffect.ShowMessage(
                    messageResId = R.string.conversation_creation_failure,
                ),
                awaitItem(),
            )
            assertEquals(false, viewModel.uiState.value.isResolvingConversation)
            assertEquals(
                false,
                viewModel.uiState.value.isResolvingConversationIndicatorVisible,
            )
            assertNull(viewModel.uiState.value.resolvingRecipientDestination)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onNewChatRecipientSelected_whenUseCaseReturnsEmptyDestinations_showsFailureMessage() =
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            coEvery {
                resolveConversationId.invoke(
                    destinations = listOf("+1 555 0100"),
                )
            } returns ResolveConversationIdResult.EmptyDestinations
            val viewModel = createViewModel()

            viewModel.effects.test {
                viewModel.onNewChatRecipientSelected(destination = "+1 555 0100")
                advanceUntilIdle()

                assertEquals(
                    ConversationEntryEffect.ShowMessage(
                        messageResId = R.string.conversation_creation_failure,
                    ),
                    awaitItem(),
                )
                assertEquals(false, viewModel.uiState.value.isResolvingConversation)
                assertEquals(
                    false,
                    viewModel.uiState.value.isResolvingConversationIndicatorVisible,
                )
                assertNull(viewModel.uiState.value.resolvingRecipientDestination)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun onNewChatRecipientSelected_ignoresSelectionWhileAlreadyResolving() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val firstResolutionResult = CompletableDeferred<ResolveConversationIdResult>()
        coEvery {
            resolveConversationId.invoke(
                destinations = listOf("+1 555 0100"),
            )
        } coAnswers {
            firstResolutionResult.await()
        }
        val viewModel = createViewModel()

        viewModel.onNewChatRecipientSelected(destination = "+1 555 0100")
        runCurrent()
        viewModel.onNewChatRecipientSelected(destination = "+1 555 0101")
        runCurrent()

        coVerify(exactly = 1) {
            resolveConversationId.invoke(
                destinations = listOf("+1 555 0100"),
            )
        }
        coVerify(exactly = 0) {
            resolveConversationId.invoke(
                destinations = listOf("+1 555 0101"),
            )
        }
        assertEquals(true, viewModel.uiState.value.isResolvingConversation)
        assertEquals("+1 555 0100", viewModel.uiState.value.resolvingRecipientDestination)

        firstResolutionResult.complete(ResolveConversationIdResult.NotResolved)
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.isResolvingConversation)
        assertEquals(
            false,
            viewModel.uiState.value.isResolvingConversationIndicatorVisible,
        )
        assertNull(viewModel.uiState.value.resolvingRecipientDestination)
    }

    @Test
    fun onNewChatRecipientSelected_ignoresSelectionWhileCreatingGroup() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val viewModel = createViewModel()

        viewModel.onCreateGroupRequested()
        viewModel.onNewChatRecipientSelected(destination = "+1 555 0100")
        advanceUntilIdle()

        coVerify(exactly = 0) {
            resolveConversationId.invoke(
                destinations = any(),
            )
        }
        assertEquals(true, viewModel.uiState.value.isCreatingGroup)
        assertEquals(false, viewModel.uiState.value.isResolvingConversation)
    }

    @Test
    fun onDraftPayloadConsumed_clearsOnlyMatchingConversationDraft() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val draftData = MessageData.createDraftSmsMessage(
            "conversation-4",
            "self-4",
            "Draft",
        )
        every {
            conversationMessageDataDraftMapper.map(messageData = draftData)
        } returns ConversationDraft(messageText = "Draft")
        val savedStateHandle = SavedStateHandle(
            mapOf(
                CONVERSATION_ID_KEY to "conversation-4",
                PENDING_DRAFT_DATA_KEY to draftData,
            ),
        )
        val viewModel = createViewModel(savedStateHandle = savedStateHandle)

        viewModel.onDraftPayloadConsumed(conversationId = "other")
        assertEquals("Draft", viewModel.uiState.value.pendingDraft?.messageText)

        viewModel.onDraftPayloadConsumed(conversationId = "conversation-4")

        assertNull(viewModel.uiState.value.pendingDraft)
        assertNull(savedStateHandle[PENDING_DRAFT_DATA_KEY])
    }

    @Test
    fun onScrollPositionConsumed_clearsOnlyMatchingConversationPosition() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                CONVERSATION_ID_KEY to "conversation-6",
                PENDING_SCROLL_POSITION_KEY to 9,
            ),
        )
        val viewModel = createViewModel(savedStateHandle = savedStateHandle)

        viewModel.onScrollPositionConsumed(conversationId = "other")
        assertEquals(9, viewModel.uiState.value.pendingScrollPosition)
        assertEquals(9, savedStateHandle[PENDING_SCROLL_POSITION_KEY])

        viewModel.onScrollPositionConsumed(conversationId = "conversation-6")

        assertNull(viewModel.uiState.value.pendingScrollPosition)
        assertNull(savedStateHandle[PENDING_SCROLL_POSITION_KEY])
    }

    @Test
    fun onStartupAttachmentConsumed_clearsOnlyMatchingConversationAttachment() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                CONVERSATION_ID_KEY to "conversation-5",
                PENDING_STARTUP_ATTACHMENT_URI_KEY to "content://media/image/5",
                PENDING_STARTUP_ATTACHMENT_TYPE_KEY to "image/jpeg",
            ),
        )
        val viewModel = createViewModel(savedStateHandle = savedStateHandle)

        viewModel.onStartupAttachmentConsumed(conversationId = "other")
        assertEquals(
            "content://media/image/5",
            viewModel.uiState.value.pendingStartupAttachment?.contentUri,
        )

        viewModel.onStartupAttachmentConsumed(conversationId = "conversation-5")

        assertNull(viewModel.uiState.value.pendingStartupAttachment)
    }

    @Test
    fun navigateBack_emitsBackEffect() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.navigateBack()

            assertEquals(
                ConversationEntryEffect.NavigateBack,
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): ConversationEntryViewModel {
        return ConversationEntryViewModel(
            conversationMessageDataDraftMapper = conversationMessageDataDraftMapper,
            isConversationRecipientLimitExceeded = isConversationRecipientLimitExceeded,
            resolveConversationId = resolveConversationId,
            savedStateHandle = savedStateHandle,
            mainDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    private companion object {
        private const val CONVERSATION_ID_KEY = "conversation_id"
        private const val IS_CREATING_GROUP_KEY = "is_creating_group"
        private const val LEGACY_IS_RESOLVING_CONVERSATION_KEY = "is_resolving_conversation"
        private const val LAUNCH_GENERATION_KEY = "launch_generation"
        private const val PENDING_DRAFT_DATA_KEY = "pending_draft_data"
        private const val PENDING_SCROLL_POSITION_KEY = "pending_scroll_position"
        private const val PENDING_STARTUP_ATTACHMENT_TYPE_KEY = "pending_startup_attachment_type"
        private const val PENDING_STARTUP_ATTACHMENT_URI_KEY = "pending_startup_attachment_uri"
        private const val PROCESSED_LAUNCH_GENERATION_KEY = "processed_launch_generation"
        private const val SELECTED_GROUP_RECIPIENT_DESTINATIONS_KEY =
            "selected_group_recipient_destinations"
    }
}
