package com.android.messaging.ui.conversation.v2.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.android.messaging.data.conversation.mapper.ConversationMessageDataDraftMapper
import com.android.messaging.data.conversation.mapper.ConversationMessageDataDraftMapperImpl
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.v2.composer.mapper.ConversationComposerUiStateMapper
import com.android.messaging.ui.conversation.v2.composer.model.ConversationComposerAttachmentUiState
import com.android.messaging.ui.conversation.v2.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.v2.composer.model.ConversationDraftState
import com.android.messaging.ui.conversation.v2.mediapicker.ConversationMediaPickerDelegate
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationMediaPickerUiState
import com.android.messaging.ui.conversation.v2.messages.delegate.ConversationMessagesDelegate
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.v2.metadata.delegate.ConversationMetadataDelegate
import com.android.messaging.ui.conversation.v2.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationLaunchRequest
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenScaffoldUiState
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        unmockkAll()
        clearAllMocks()
    }

    @Test
    fun init_bindsAllDelegates() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val draftDelegate = createDraftDelegateMock()
            val messagesDelegate = createMessagesDelegateMock()
            val mediaPickerDelegate = createMediaPickerDelegateMock()
            val metadataDelegate = createMetadataDelegateMock()
            val viewModel = createViewModel(
                draftDelegate = draftDelegate.mock,
                messagesDelegate = messagesDelegate.mock,
                mediaPickerDelegate = mediaPickerDelegate.mock,
                metadataDelegate = metadataDelegate.mock,
            )

            advanceUntilIdle()

            assertEquals(1, draftDelegate.bindCalls.size)
            assertEquals(1, messagesDelegate.bindCalls.size)
            assertEquals(1, metadataDelegate.bindCalls.size)
            assertSame(
                draftDelegate.bindCalls.single().conversationIdFlow,
                messagesDelegate.bindCalls.single().conversationIdFlow,
            )
            assertSame(
                draftDelegate.bindCalls.single().conversationIdFlow,
                mediaPickerDelegate.bindCalls.single().conversationIdFlow,
            )
            assertSame(
                draftDelegate.bindCalls.single().conversationIdFlow,
                metadataDelegate.bindCalls.single().conversationIdFlow,
            )
            assertEquals(
                null,
                draftDelegate.bindCalls.single().conversationIdFlow.value,
            )

            viewModel.onLaunchRequest(
                launchRequest = ConversationLaunchRequest(
                    launchGeneration = 1,
                    conversationId = "conversation-1",
                ),
            )
            assertEquals(
                "conversation-1",
                draftDelegate.bindCalls.single().conversationIdFlow.value,
            )
        }
    }

    @Test
    fun uiState_combinesDelegateStatesUsingComposerMapper() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val composerUiState = ConversationComposerUiState(
                messageText = "Mapped text",
                isSendEnabled = true,
            )
            val draftDelegate = createDraftDelegateMock()
            val messagesDelegate = createMessagesDelegateMock()
            val metadataDelegate = createMetadataDelegateMock()
            val viewModel = createViewModel(
                draftDelegate = draftDelegate.mock,
                messagesDelegate = messagesDelegate.mock,
                metadataDelegate = metadataDelegate.mock,
                composerUiStateMapper = createComposerUiStateMapperMock(
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
                ).toPersistentList(),
            )
            metadataDelegate.stateFlow.value = metadataState
            messagesDelegate.stateFlow.value = messagesState
            draftDelegate.stateFlow.value = ConversationDraftState(
                draft = ConversationDraft(
                    messageText = "Draft text",
                ),
            )

            viewModel.scaffoldUiState.test {
                assertEquals(
                    ConversationScreenScaffoldUiState(
                        composer = composerUiState,
                    ),
                    awaitItem(),
                )

                val mappedState = awaitItem()
                assertEquals(metadataState, mappedState.metadata)
                assertEquals(messagesState, mappedState.messages)
                assertEquals(composerUiState, mappedState.composer)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun mediaPickerEffects_areExposedAsScreenEffects() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val mediaPickerDelegate = createMediaPickerDelegateMock()
            val viewModel = createViewModel(
                mediaPickerDelegate = mediaPickerDelegate.mock,
            )
            advanceUntilIdle()

            viewModel.effects.test {
                mediaPickerDelegate.effectsFlow.emit(
                    ConversationScreenEffect.ShowMessage(
                        messageResId = 123,
                    ),
                )

                assertEquals(
                    ConversationScreenEffect.ShowMessage(
                        messageResId = 123,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onLaunchRequest_seedsDraftAndOpensStartupAttachment() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val draftDelegate = createDraftDelegateMock()
            val messageDataDraftMapper = mockk<ConversationMessageDataDraftMapper>()
            val viewModel = createViewModel(
                draftDelegate = draftDelegate.mock,
                conversationMessageDataDraftMapper = messageDataDraftMapper,
            )
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
                messageDataDraftMapper.map(messageData = draftData)
            } returns mappedDraft

            viewModel.effects.test {
                viewModel.onLaunchRequest(
                    launchRequest = createLaunchRequest(
                        conversationId = "conversation-1",
                        launchGeneration = 1,
                        draftData = draftData,
                        startupAttachmentUri = "content://media/image/1",
                        startupAttachmentType = "image/jpeg",
                    ),
                )
                advanceUntilIdle()

                verify(exactly = 1) {
                    messageDataDraftMapper.map(messageData = draftData)
                }
                verify(exactly = 1) {
                    draftDelegate.mock.seedDraft(
                        conversationId = "conversation-1",
                        draft = mappedDraft,
                    )
                }
                assertEquals(
                    ConversationScreenEffect.OpenAttachmentPreview(
                        contentType = "image/jpeg",
                        contentUri = "content://media/image/1",
                        imageCollectionUri = MessagingContentProvider
                            .buildConversationImagesUri("conversation-1")
                            .toString(),
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onLaunchRequest_sameGenerationSkipsReseedingButStillUpdatesConversationId() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val draftDelegate = createDraftDelegateMock()
            val messageDataDraftMapper = mockk<ConversationMessageDataDraftMapper>()
            val viewModel = createViewModel(
                draftDelegate = draftDelegate.mock,
                conversationMessageDataDraftMapper = messageDataDraftMapper,
            )
            val firstDraftData = MessageData.createDraftSmsMessage(
                "conversation-1",
                "self-1",
                "First",
            )
            val secondDraftData = MessageData.createDraftSmsMessage(
                "conversation-2",
                "self-2",
                "Second",
            )
            every {
                messageDataDraftMapper.map(messageData = firstDraftData)
            } returns ConversationDraft(
                messageText = "First",
                selfParticipantId = "self-1",
            )

            viewModel.effects.test {
                viewModel.onLaunchRequest(
                    launchRequest = createLaunchRequest(
                        conversationId = "conversation-1",
                        launchGeneration = 1,
                        draftData = firstDraftData,
                        startupAttachmentUri = "content://media/image/1",
                        startupAttachmentType = "image/jpeg",
                    ),
                )
                advanceUntilIdle()
                awaitItem()

                viewModel.onLaunchRequest(
                    launchRequest = createLaunchRequest(
                        conversationId = "conversation-2",
                        launchGeneration = 1,
                        draftData = secondDraftData,
                        startupAttachmentUri = "content://media/image/2",
                        startupAttachmentType = "image/png",
                    ),
                )
                advanceUntilIdle()

                assertEquals(
                    "conversation-2",
                    draftDelegate.bindCalls.single().conversationIdFlow.value,
                )
                verify(exactly = 1) {
                    messageDataDraftMapper.map(messageData = any())
                }
                verify(exactly = 1) {
                    draftDelegate.mock.seedDraft(
                        conversationId = any(),
                        draft = any(),
                    )
                }
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onLaunchRequest_withoutConversationIdSkipsDraftSeedAndUsesNullImageCollection() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val draftDelegate = createDraftDelegateMock()
            val messageDataDraftMapper = mockk<ConversationMessageDataDraftMapper>()
            val viewModel = createViewModel(
                draftDelegate = draftDelegate.mock,
                conversationMessageDataDraftMapper = messageDataDraftMapper,
            )
            val draftData = MessageData.createDraftSmsMessage(
                "conversation-1",
                "self-1",
                "Hello",
            )

            viewModel.effects.test {
                viewModel.onLaunchRequest(
                    launchRequest = createLaunchRequest(
                        conversationId = null,
                        launchGeneration = 1,
                        draftData = draftData,
                        startupAttachmentUri = "content://media/image/1",
                        startupAttachmentType = "image/jpeg",
                    ),
                )
                advanceUntilIdle()

                verify(exactly = 0) {
                    messageDataDraftMapper.map(messageData = any())
                }
                verify(exactly = 0) {
                    draftDelegate.mock.seedDraft(
                        conversationId = any(),
                        draft = any(),
                    )
                }
                assertEquals(
                    ConversationScreenEffect.OpenAttachmentPreview(
                        contentType = "image/jpeg",
                        contentUri = "content://media/image/1",
                        imageCollectionUri = null,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onLaunchRequest_requiresBothStartupAttachmentFields() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.effects.test {
                viewModel.onLaunchRequest(
                    launchRequest = createLaunchRequest(
                        conversationId = "conversation-1",
                        launchGeneration = 1,
                        startupAttachmentUri = "content://media/image/1",
                        startupAttachmentType = null,
                    ),
                )
                advanceUntilIdle()
                expectNoEvents()

                viewModel.onLaunchRequest(
                    launchRequest = createLaunchRequest(
                        conversationId = "conversation-1",
                        launchGeneration = 2,
                        startupAttachmentUri = null,
                        startupAttachmentType = "image/jpeg",
                    ),
                )
                advanceUntilIdle()
                expectNoEvents()

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun attachmentPreviewEvents_useDraftAndConversationImageCollections() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val draftDelegate = createDraftDelegateMock()
            val viewModel = createViewModel(
                draftDelegate = draftDelegate.mock,
            )
            viewModel.onLaunchRequest(
                launchRequest = createLaunchRequest(
                    conversationId = "conversation-1",
                    launchGeneration = 1,
                ),
            )

            viewModel.effects.test {
                viewModel.onAttachmentClicked(
                    attachment = ConversationComposerAttachmentUiState.Resolved(
                        key = "attachment-1",
                        contentType = "image/jpeg",
                        contentUri = "content://media/image/1",
                        captionText = "",
                        width = 640,
                        height = 480,
                    ),
                )
                advanceUntilIdle()
                assertEquals(
                    ConversationScreenEffect.OpenAttachmentPreview(
                        contentType = "image/jpeg",
                        contentUri = "content://media/image/1",
                        imageCollectionUri = MessagingContentProvider
                            .buildDraftImagesUri("conversation-1")
                            .toString(),
                    ),
                    awaitItem(),
                )

                viewModel.onMessageAttachmentClicked(
                    contentType = "image/png",
                    contentUri = "content://media/image/2",
                )
                advanceUntilIdle()
                assertEquals(
                    ConversationScreenEffect.OpenAttachmentPreview(
                        contentType = "image/png",
                        contentUri = "content://media/image/2",
                        imageCollectionUri = MessagingContentProvider
                            .buildConversationImagesUri("conversation-1")
                            .toString(),
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun eventMethods_forwardToDelegates() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val draftDelegate = createDraftDelegateMock()
            val mediaPickerDelegate = createMediaPickerDelegateMock()
            val viewModel = createViewModel(
                draftDelegate = draftDelegate.mock,
                mediaPickerDelegate = mediaPickerDelegate.mock,
            )

            viewModel.onMessageTextChanged(text = "Hello")
            viewModel.onGalleryVisibilityChanged(isVisible = true)
            viewModel.onSendClick()
            viewModel.persistDraft()

            verify(exactly = 1) {
                draftDelegate.mock.onMessageTextChanged(messageText = "Hello")
            }
            verify(exactly = 1) {
                mediaPickerDelegate.mock.onGalleryVisibilityChanged(isVisible = true)
            }
            verify(exactly = 1) { draftDelegate.mock.onSendClick() }
            verify(exactly = 1) { draftDelegate.mock.persistDraft() }
        }
    }

    @Test
    fun onCleared_flushesDraftDelegate() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val draftDelegate = createDraftDelegateMock()
            val mediaPickerDelegate = createMediaPickerDelegateMock()
            val viewModelStore = ViewModelStore()
            createViewModelInStore(
                viewModelStore = viewModelStore,
                draftDelegate = draftDelegate.mock,
                mediaPickerDelegate = mediaPickerDelegate.mock,
            )

            viewModelStore.clear()

            verify(exactly = 1) { draftDelegate.mock.flushDraft() }
            verify(exactly = 1) { mediaPickerDelegate.mock.onScreenCleared() }
        }
    }

    private fun createViewModel(
        draftDelegate: ConversationDraftDelegate = createDraftDelegateMock().mock,
        messagesDelegate: ConversationMessagesDelegate = createMessagesDelegateMock().mock,
        conversationMessageDataDraftMapper: ConversationMessageDataDraftMapper =
            ConversationMessageDataDraftMapperImpl(),
        mediaPickerDelegate: ConversationMediaPickerDelegate = createMediaPickerDelegateMock().mock,
        metadataDelegate: ConversationMetadataDelegate = createMetadataDelegateMock().mock,
        composerUiStateMapper: ConversationComposerUiStateMapper =
            createComposerUiStateMapperMock(mappedUiState = ConversationComposerUiState()),
    ): ConversationViewModel {
        return ConversationViewModel(
            conversationDraftDelegate = draftDelegate,
            conversationMessagesDelegate = messagesDelegate,
            conversationMessageDataDraftMapper = conversationMessageDataDraftMapper,
            conversationMediaPickerDelegate = mediaPickerDelegate,
            conversationMetadataDelegate = metadataDelegate,
            conversationComposerUiStateMapper = composerUiStateMapper,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
            savedStateHandle = SavedStateHandle(),
        )
    }

    private fun createViewModelInStore(
        viewModelStore: ViewModelStore,
        draftDelegate: ConversationDraftDelegate = createDraftDelegateMock().mock,
        messagesDelegate: ConversationMessagesDelegate = createMessagesDelegateMock().mock,
        conversationMessageDataDraftMapper: ConversationMessageDataDraftMapper =
            ConversationMessageDataDraftMapperImpl(),
        mediaPickerDelegate: ConversationMediaPickerDelegate = createMediaPickerDelegateMock().mock,
        metadataDelegate: ConversationMetadataDelegate = createMetadataDelegateMock().mock,
        composerUiStateMapper: ConversationComposerUiStateMapper =
            createComposerUiStateMapperMock(mappedUiState = ConversationComposerUiState()),
    ): ConversationViewModel {
        return ViewModelProvider(
            store = viewModelStore,
            factory = object : ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return createViewModel(
                        draftDelegate = draftDelegate,
                        messagesDelegate = messagesDelegate,
                        conversationMessageDataDraftMapper = conversationMessageDataDraftMapper,
                        mediaPickerDelegate = mediaPickerDelegate,
                        metadataDelegate = metadataDelegate,
                        composerUiStateMapper = composerUiStateMapper,
                    ) as T
                }
            },
        )[ConversationViewModel::class.java]
    }

    private fun createDraftDelegateMock(): DraftDelegateMock {
        val bindCalls = mutableListOf<BindCall<ConversationDraftState>>()
        val stateFlow = MutableStateFlow(ConversationDraftState())
        val mock = mockk<ConversationDraftDelegate>(relaxed = true)
        every { mock.state } returns stateFlow
        every {
            mock.bind(any(), any())
        } answers {
            bindCalls += BindCall(
                scope = firstArg(),
                conversationIdFlow = secondArg(),
            )
        }
        return DraftDelegateMock(
            mock = mock,
            stateFlow = stateFlow,
            bindCalls = bindCalls,
        )
    }

    private fun createMediaPickerDelegateMock(): MediaPickerDelegateMock {
        val bindCalls = mutableListOf<BindCall<ConversationMediaPickerUiState>>()
        val stateFlow = MutableStateFlow(ConversationMediaPickerUiState())
        val effectsFlow = MutableSharedFlow<ConversationScreenEffect>()
        val mock = mockk<ConversationMediaPickerDelegate>(relaxed = true)
        every { mock.state } returns stateFlow
        every { mock.effects } returns effectsFlow
        every {
            mock.bind(any(), any())
        } answers {
            bindCalls += BindCall(
                scope = firstArg(),
                conversationIdFlow = secondArg(),
            )
        }
        return MediaPickerDelegateMock(
            mock = mock,
            stateFlow = stateFlow,
            effectsFlow = effectsFlow,
            bindCalls = bindCalls,
        )
    }

    private fun createMessagesDelegateMock(): MessagesDelegateMock {
        val bindCalls = mutableListOf<BindCall<ConversationMessagesUiState>>()
        val stateFlow = MutableStateFlow<ConversationMessagesUiState>(
            ConversationMessagesUiState.Loading,
        )
        val mock = mockk<ConversationMessagesDelegate>()
        every { mock.state } returns stateFlow
        every {
            mock.bind(any(), any())
        } answers {
            bindCalls += BindCall(
                scope = firstArg(),
                conversationIdFlow = secondArg(),
            )
        }
        return MessagesDelegateMock(
            mock = mock,
            stateFlow = stateFlow,
            bindCalls = bindCalls,
        )
    }

    private fun createMetadataDelegateMock(): MetadataDelegateMock {
        val bindCalls = mutableListOf<BindCall<ConversationMetadataUiState>>()
        val stateFlow = MutableStateFlow<ConversationMetadataUiState>(
            ConversationMetadataUiState.Loading,
        )
        val mock = mockk<ConversationMetadataDelegate>()
        every { mock.state } returns stateFlow
        every {
            mock.bind(any(), any())
        } answers {
            bindCalls += BindCall(
                scope = firstArg(),
                conversationIdFlow = secondArg(),
            )
        }
        return MetadataDelegateMock(
            mock = mock,
            stateFlow = stateFlow,
            bindCalls = bindCalls,
        )
    }

    private fun createComposerUiStateMapperMock(
        mappedUiState: ConversationComposerUiState,
    ): ConversationComposerUiStateMapper {
        val mapper = mockk<ConversationComposerUiStateMapper>()
        every {
            mapper.map(any(), any())
        } returns mappedUiState
        return mapper
    }

    private data class BindCall<T>(
        val scope: CoroutineScope,
        val conversationIdFlow: StateFlow<String?>,
    )

    private data class DraftDelegateMock(
        val mock: ConversationDraftDelegate,
        val stateFlow: MutableStateFlow<ConversationDraftState>,
        val bindCalls: List<BindCall<ConversationDraftState>>,
    )

    private data class MediaPickerDelegateMock(
        val mock: ConversationMediaPickerDelegate,
        val stateFlow: MutableStateFlow<ConversationMediaPickerUiState>,
        val effectsFlow: MutableSharedFlow<ConversationScreenEffect>,
        val bindCalls: List<BindCall<ConversationMediaPickerUiState>>,
    )

    private data class MessagesDelegateMock(
        val mock: ConversationMessagesDelegate,
        val stateFlow: MutableStateFlow<ConversationMessagesUiState>,
        val bindCalls: List<BindCall<ConversationMessagesUiState>>,
    )

    private data class MetadataDelegateMock(
        val mock: ConversationMetadataDelegate,
        val stateFlow: MutableStateFlow<ConversationMetadataUiState>,
        val bindCalls: List<BindCall<ConversationMetadataUiState>>,
    )

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

    private fun createLaunchRequest(
        conversationId: String?,
        launchGeneration: Int,
        draftData: MessageData? = null,
        startupAttachmentUri: String? = null,
        startupAttachmentType: String? = null,
    ): ConversationLaunchRequest {
        return ConversationLaunchRequest(
            launchGeneration = launchGeneration,
            conversationId = conversationId,
            draftData = draftData,
            startupAttachmentUri = startupAttachmentUri,
            startupAttachmentType = startupAttachmentType,
        )
    }
}
