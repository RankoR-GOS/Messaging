package com.android.messaging.ui.conversation.v2.screen

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftEffect
import com.android.messaging.ui.conversation.v2.composer.mapper.ConversationComposerUiStateMapper
import com.android.messaging.ui.conversation.v2.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.v2.messages.delegate.ConversationMessagesDelegate
import com.android.messaging.ui.conversation.v2.messages.model.ConversationMessageUiModel
import com.android.messaging.ui.conversation.v2.messages.model.ConversationMessagesUiState
import com.android.messaging.ui.conversation.v2.metadata.delegate.ConversationMetadataDelegate
import com.android.messaging.ui.conversation.v2.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import com.android.messaging.ui.conversation.v2.screen.model.ConversationUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_bindsAllDelegates() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val conversationDraftDelegate = FakeConversationDraftDelegate()
            val conversationMessagesDelegate = FakeConversationMessagesDelegate()
            val conversationMetadataDelegate = FakeConversationMetadataDelegate()
            val viewModel = createViewModel(
                conversationDraftDelegate = conversationDraftDelegate,
                conversationMessagesDelegate = conversationMessagesDelegate,
                conversationMetadataDelegate = conversationMetadataDelegate,
            )

            advanceUntilIdle()

            assertEquals(1, conversationDraftDelegate.bindCalls.size)
            assertEquals(1, conversationMessagesDelegate.bindCalls.size)
            assertEquals(1, conversationMetadataDelegate.bindCalls.size)
            assertSame(
                conversationDraftDelegate.bindCalls.single().conversationIdFlow,
                conversationMessagesDelegate.bindCalls.single().conversationIdFlow,
            )
            assertSame(
                conversationDraftDelegate.bindCalls.single().conversationIdFlow,
                conversationMetadataDelegate.bindCalls.single().conversationIdFlow,
            )
            assertEquals(
                null,
                conversationDraftDelegate.bindCalls.single().conversationIdFlow.value
            )

            viewModel.onConversationChanged(conversationId = "conversation-1")
            assertEquals(
                "conversation-1",
                conversationDraftDelegate.bindCalls.single().conversationIdFlow.value
            )
        }
    }

    @Test
    fun uiState_combinesDelegateStatesUsingComposerMapper() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val conversationDraftDelegate = FakeConversationDraftDelegate()
            val conversationMessagesDelegate = FakeConversationMessagesDelegate()
            val conversationMetadataDelegate = FakeConversationMetadataDelegate()
            val composerUiState = ConversationComposerUiState(
                messageText = "Mapped text",
                isSendEnabled = true,
            )
            val viewModel = createViewModel(
                conversationDraftDelegate = conversationDraftDelegate,
                conversationMessagesDelegate = conversationMessagesDelegate,
                conversationMetadataDelegate = conversationMetadataDelegate,
                conversationComposerUiStateMapper = FakeConversationComposerUiStateMapper(
                    mappedUiState = composerUiState,
                ),
            )

            val metadataState = ConversationMetadataUiState.Present(
                title = "Weekend plan",
                selfParticipantId = "self-1",
                isGroupConversation = false,
                participantCount = 2,
                composerAvailability = ConversationComposerAvailability.editable(),
            )
            val messagesState = ConversationMessagesUiState.Present(
                messages = listOf(
                    createMessageUiModel(messageId = "message-1"),
                ),
            )
            conversationMetadataDelegate.stateFlow.value = metadataState
            conversationMessagesDelegate.stateFlow.value = messagesState
            conversationDraftDelegate.stateFlow.value = ConversationDraft(
                messageText = "Draft text",
            )

            viewModel.uiState.test {
                assertEquals(ConversationUiState(), awaitItem())

                val mappedState = awaitItem()
                assertEquals(metadataState, mappedState.metadata)
                assertEquals(messagesState, mappedState.messages)
                assertEquals(composerUiState, mappedState.composer)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun draftEffects_areMappedToScreenEffects() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val conversationDraftDelegate = FakeConversationDraftDelegate()
            val viewModel = createViewModel(
                conversationDraftDelegate = conversationDraftDelegate,
            )
            advanceUntilIdle()

            viewModel.effects.test {
                conversationDraftDelegate.effectsFlow.emit(
                    ConversationDraftEffect.LaunchAttachmentChooser(
                        conversationId = "conversation-1",
                    ),
                )

                assertEquals(
                    ConversationScreenEffect.LaunchAttachmentChooser(
                        conversationId = "conversation-1",
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun eventMethods_forwardToDraftDelegate() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val conversationDraftDelegate = FakeConversationDraftDelegate()
            val viewModel = createViewModel(
                conversationDraftDelegate = conversationDraftDelegate,
            )

            viewModel.onMessageTextChanged(text = "Hello")
            viewModel.onAttachmentClick()
            viewModel.onSendClick()
            viewModel.persistDraft()

            assertEquals(listOf("Hello"), conversationDraftDelegate.messageTextChanges)
            assertEquals(1, conversationDraftDelegate.attachmentClicks)
            assertEquals(1, conversationDraftDelegate.sendClicks)
            assertEquals(1, conversationDraftDelegate.persistCalls)
        }
    }

    @Test
    fun onCleared_flushesDraftDelegate() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val conversationDraftDelegate = FakeConversationDraftDelegate()
            val viewModel = createViewModel(
                conversationDraftDelegate = conversationDraftDelegate,
            )

            val onClearedMethod = ConversationViewModel::class.java.getDeclaredMethod("onCleared")
            onClearedMethod.isAccessible = true
            onClearedMethod.invoke(viewModel)

            assertEquals(1, conversationDraftDelegate.flushCalls)
        }
    }

    private fun createViewModel(
        conversationDraftDelegate: FakeConversationDraftDelegate = FakeConversationDraftDelegate(),
        conversationMessagesDelegate: FakeConversationMessagesDelegate =
            FakeConversationMessagesDelegate(),
        conversationMetadataDelegate: FakeConversationMetadataDelegate =
            FakeConversationMetadataDelegate(),
        conversationComposerUiStateMapper: ConversationComposerUiStateMapper =
            FakeConversationComposerUiStateMapper(
                mappedUiState = ConversationComposerUiState(),
            ),
    ): ConversationViewModel {
        return ConversationViewModel(
            conversationDraftDelegate = conversationDraftDelegate,
            conversationMessagesDelegate = conversationMessagesDelegate,
            conversationMetadataDelegate = conversationMetadataDelegate,
            conversationComposerUiStateMapper = conversationComposerUiStateMapper,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
            savedStateHandle = SavedStateHandle(),
        )
    }

    private data class BindCall<T>(
        val scope: CoroutineScope,
        val conversationIdFlow: StateFlow<String?>,
    )

    private class FakeConversationDraftDelegate : ConversationDraftDelegate {
        private val bindCallsInternal = mutableListOf<BindCall<ConversationDraft>>()

        val effectsFlow = MutableSharedFlow<ConversationDraftEffect>()
        val stateFlow = MutableStateFlow(ConversationDraft())
        override val effects: Flow<ConversationDraftEffect> = effectsFlow
        override val state: StateFlow<ConversationDraft> = stateFlow
        val bindCalls: List<BindCall<ConversationDraft>>
            get() = bindCallsInternal
        val messageTextChanges = mutableListOf<String>()

        var attachmentClicks = 0
        var flushCalls = 0
        var persistCalls = 0
        var sendClicks = 0

        override fun bind(
            scope: CoroutineScope,
            conversationIdFlow: StateFlow<String?>,
        ) {
            bindCallsInternal += BindCall(
                scope = scope,
                conversationIdFlow = conversationIdFlow,
            )
        }

        override fun onMessageTextChanged(messageText: String) {
            messageTextChanges += messageText
        }

        override fun onAttachmentClick() {
            attachmentClicks += 1
        }

        override fun onSendClick() {
            sendClicks += 1
        }

        override fun persistDraft() {
            persistCalls += 1
        }

        override fun flushDraft() {
            flushCalls += 1
        }
    }

    private class FakeConversationMessagesDelegate : ConversationMessagesDelegate {
        private val bindCallsInternal = mutableListOf<BindCall<ConversationMessagesUiState>>()

        val bindCalls: List<BindCall<ConversationMessagesUiState>>
            get() = bindCallsInternal
        val stateFlow = MutableStateFlow<ConversationMessagesUiState>(
            ConversationMessagesUiState.Loading,
        )
        override val state: StateFlow<ConversationMessagesUiState> = stateFlow

        override fun bind(
            scope: CoroutineScope,
            conversationIdFlow: StateFlow<String?>,
        ) {
            bindCallsInternal += BindCall(
                scope = scope,
                conversationIdFlow = conversationIdFlow,
            )
        }
    }

    private class FakeConversationMetadataDelegate : ConversationMetadataDelegate {
        private val bindCallsInternal = mutableListOf<BindCall<ConversationMetadataUiState>>()

        val bindCalls: List<BindCall<ConversationMetadataUiState>>
            get() = bindCallsInternal
        val stateFlow = MutableStateFlow<ConversationMetadataUiState>(
            ConversationMetadataUiState.Loading,
        )
        override val state: StateFlow<ConversationMetadataUiState> = stateFlow

        override fun bind(
            scope: CoroutineScope,
            conversationIdFlow: StateFlow<String?>,
        ) {
            bindCallsInternal += BindCall(
                scope = scope,
                conversationIdFlow = conversationIdFlow,
            )
        }
    }

    private class FakeConversationComposerUiStateMapper(
        private val mappedUiState: ConversationComposerUiState,
    ) : ConversationComposerUiStateMapper {
        override fun map(
            draft: ConversationDraft,
            composerAvailability: ConversationComposerAvailability,
        ): ConversationComposerUiState {
            return mappedUiState
        }
    }

    private fun createMessageUiModel(messageId: String): ConversationMessageUiModel {
        return ConversationMessageUiModel(
            messageId = messageId,
            conversationId = "conversation-1",
            text = "Hello",
            parts = emptyList(),
            sentTimestamp = 1L,
            receivedTimestamp = 1L,
            displayTimestamp = 1L,
            status = ConversationMessageUiModel.Status.Outgoing.Complete,
            isIncoming = false,
            senderDisplayName = null,
            senderAvatarUri = null,
            senderContactLookupKey = null,
            canClusterWithPrevious = false,
            canClusterWithNext = false,
            mmsSubject = null,
            protocol = ConversationMessageUiModel.Protocol.SMS,
        )
    }
}
