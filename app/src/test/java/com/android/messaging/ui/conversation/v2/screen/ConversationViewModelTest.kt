package com.android.messaging.ui.conversation.v2.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.conversation.model.metadata.ConversationSubscription
import com.android.messaging.data.conversation.repository.ConversationSubscriptionsRepository
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.domain.conversation.usecase.CanAddMoreConversationParticipants
import com.android.messaging.domain.conversation.usecase.IsDeviceVoiceCapable
import com.android.messaging.domain.conversation.usecase.IsEmergencyPhoneNumber
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.conversation.v2.audio.delegate.ConversationAudioRecordingDelegate
import com.android.messaging.ui.conversation.v2.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationComposerAttachmentsDelegate
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.v2.composer.mapper.ConversationComposerUiStateMapper
import com.android.messaging.ui.conversation.v2.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.v2.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.v2.composer.model.ConversationDraftState
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.v2.focus.delegate.ConversationFocusDelegate
import com.android.messaging.ui.conversation.v2.mediapicker.ConversationMediaPickerDelegate
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationMediaPickerUiState
import com.android.messaging.ui.conversation.v2.messages.delegate.ConversationMessageSelectionDelegate
import com.android.messaging.ui.conversation.v2.messages.delegate.ConversationMessagesDelegate
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.v2.metadata.delegate.ConversationMetadataDelegate
import com.android.messaging.ui.conversation.v2.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMessageSelectionAction
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMessageSelectionUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenScaffoldUiState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
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

    @Test
    fun init_bindsAllDelegates() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val draftDelegate = createDraftDelegateMock()
            val audioRecordingDelegate = createAudioRecordingDelegateMock()
            val composerAttachmentsDelegate = createComposerAttachmentsDelegateMock()
            val messagesDelegate = createMessagesDelegateMock()
            val messageSelectionDelegate = createMessageSelectionDelegateMock()
            val mediaPickerDelegate = createMediaPickerDelegateMock()
            val metadataDelegate = createMetadataDelegateMock()
            val focusDelegate = createFocusDelegateMock()
            val viewModel = createViewModel(
                audioRecordingDelegate = audioRecordingDelegate.mock,
                composerAttachmentsDelegate = composerAttachmentsDelegate.mock,
                draftDelegate = draftDelegate.mock,
                messagesDelegate = messagesDelegate.mock,
                messageSelectionDelegate = messageSelectionDelegate.mock,
                mediaPickerDelegate = mediaPickerDelegate.mock,
                metadataDelegate = metadataDelegate.mock,
                focusDelegate = focusDelegate.mock,
            )

            advanceUntilIdle()

            assertEquals(1, draftDelegate.bindCalls.size)
            assertEquals(1, audioRecordingDelegate.bindCalls.size)
            assertEquals(1, composerAttachmentsDelegate.bindCalls.size)
            assertEquals(1, messagesDelegate.bindCalls.size)
            assertEquals(1, messageSelectionDelegate.bindCalls.size)
            assertEquals(1, mediaPickerDelegate.bindCalls.size)
            assertEquals(1, metadataDelegate.bindCalls.size)
            assertEquals(1, focusDelegate.bindCalls.size)
            assertSame(
                draftDelegate.bindCalls.single().conversationIdFlow,
                focusDelegate.bindCalls.single().conversationIdFlow,
            )
            assertSame(
                draftDelegate.stateFlow,
                composerAttachmentsDelegate.bindCalls.single().draftStateFlow,
            )
            assertSame(
                draftDelegate.bindCalls.single().conversationIdFlow,
                audioRecordingDelegate.bindCalls.single().conversationIdFlow,
            )
            assertSame(
                draftDelegate.bindCalls.single().conversationIdFlow,
                messagesDelegate.bindCalls.single().conversationIdFlow,
            )
            assertSame(
                draftDelegate.bindCalls.single().conversationIdFlow,
                messageSelectionDelegate.bindCalls.single().conversationIdFlow,
            )
            assertSame(
                draftDelegate.bindCalls.single().conversationIdFlow,
                mediaPickerDelegate.bindCalls.single().conversationIdFlow,
            )
            assertSame(
                draftDelegate.bindCalls.single().conversationIdFlow,
                metadataDelegate.bindCalls.single().conversationIdFlow,
            )
            assertEquals(null, draftDelegate.bindCalls.single().conversationIdFlow.value)

            viewModel.onConversationIdChanged(conversationId = "conversation-1")

            assertEquals(
                "conversation-1",
                draftDelegate.bindCalls.single().conversationIdFlow.value,
            )
            verify(exactly = 1) {
                messageSelectionDelegate.mock.dismissMessageSelection()
            }
        }
    }

    @Test
    fun scaffoldUiState_combinesDelegateStatesUsingComposerMapper() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val composerUiState = ConversationComposerUiState(
                messageText = "Mapped text",
                isSendEnabled = true,
            )
            val draftDelegate = createDraftDelegateMock()
            val messagesDelegate = createMessagesDelegateMock()
            val messageSelectionDelegate = createMessageSelectionDelegateMock()
            val metadataDelegate = createMetadataDelegateMock()
            val viewModel = createViewModel(
                draftDelegate = draftDelegate.mock,
                messagesDelegate = messagesDelegate.mock,
                messageSelectionDelegate = messageSelectionDelegate.mock,
                metadataDelegate = metadataDelegate.mock,
                composerUiStateMapper = createComposerUiStateMapperMock(
                    mappedUiState = composerUiState,
                ),
            )

            val metadataState = ConversationMetadataUiState.Present(
                title = "Weekend plan",
                selfParticipantId = "self-1",
                avatar = ConversationMetadataUiState.Avatar.Single(
                    photoUri = null,
                ),
                participantCount = 2,
                otherParticipantDisplayDestination = null,
                otherParticipantPhoneNumber = null,
                otherParticipantContactLookupKey = null,
                isArchived = false,
                composerAvailability = ConversationComposerAvailability.editable(),
            )
            val messagesState = ConversationMessagesUiState.Present(
                messages = listOf(
                    createMessageUiModel(messageId = "message-1"),
                ).toPersistentList(),
            )
            metadataDelegate.stateFlow.value = metadataState
            messagesDelegate.stateFlow.value = messagesState
            val selectionState = ConversationMessageSelectionUiState(
                selectedMessageIds = persistentSetOf("message-1"),
            )
            messageSelectionDelegate.stateFlow.value = selectionState
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
                assertEquals(false, mappedState.canAddPeople)
                assertEquals(metadataState, mappedState.metadata)
                assertEquals(messagesState, mappedState.messages)
                assertEquals(composerUiState, mappedState.composer)
                assertEquals(selectionState, mappedState.selection)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun scaffoldUiState_enablesAddPeopleWhenConversationIsBelowRecipientLimit() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val metadataDelegate = createMetadataDelegateMock()
            val canAddMoreConversationParticipants = mockk<CanAddMoreConversationParticipants>()
            every {
                canAddMoreConversationParticipants.invoke(participantCount = 2)
            } returns true
            val viewModel = createViewModel(
                metadataDelegate = metadataDelegate.mock,
                canAddMoreConversationParticipants = canAddMoreConversationParticipants,
            )

            metadataDelegate.stateFlow.value = ConversationMetadataUiState.Present(
                title = "Weekend plan",
                selfParticipantId = "self-1",
                avatar = ConversationMetadataUiState.Avatar.Group,
                participantCount = 2,
                otherParticipantDisplayDestination = null,
                otherParticipantPhoneNumber = null,
                otherParticipantContactLookupKey = null,
                isArchived = false,
                composerAvailability = ConversationComposerAvailability.editable(),
            )
            viewModel.scaffoldUiState.test {
                assertEquals(false, awaitItem().canAddPeople)
                advanceUntilIdle()
                assertEquals(true, awaitItem().canAddPeople)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun scaffoldUiState_disablesAddPeopleWhenConversationReachedRecipientLimit() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val metadataDelegate = createMetadataDelegateMock()
            val canAddMoreConversationParticipants = mockk<CanAddMoreConversationParticipants>()
            every {
                canAddMoreConversationParticipants.invoke(participantCount = 10)
            } returns false
            val viewModel = createViewModel(
                metadataDelegate = metadataDelegate.mock,
                canAddMoreConversationParticipants = canAddMoreConversationParticipants,
            )

            metadataDelegate.stateFlow.value = ConversationMetadataUiState.Present(
                title = "Weekend plan",
                selfParticipantId = "self-1",
                avatar = ConversationMetadataUiState.Avatar.Group,
                participantCount = 10,
                otherParticipantDisplayDestination = null,
                otherParticipantPhoneNumber = null,
                otherParticipantContactLookupKey = null,
                isArchived = false,
                composerAvailability = ConversationComposerAvailability.editable(),
            )
            viewModel.scaffoldUiState.test {
                assertEquals(false, awaitItem().canAddPeople)
                advanceUntilIdle()
                assertEquals(false, awaitItem().canAddPeople)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun scaffoldUiState_checksEmergencyPhoneNumberBeforeEnablingCallAction() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val metadataDelegate = createMetadataDelegateMock()
            val isEmergencyPhoneNumber = mockk<IsEmergencyPhoneNumber>()
            every { isEmergencyPhoneNumber.invoke(phoneNumber = "+15551234567") } returns false
            val viewModel = createViewModel(
                metadataDelegate = metadataDelegate.mock,
                isDeviceVoiceCapable = IsDeviceVoiceCapable { true },
                isEmergencyPhoneNumber = isEmergencyPhoneNumber,
            )

            metadataDelegate.stateFlow.value = oneOnOneMetadataState(
                phoneNumber = "+15551234567",
            )

            viewModel.scaffoldUiState.test {
                assertEquals(false, awaitItem().canCall)
                advanceUntilIdle()
                assertEquals(true, awaitItem().canCall)
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 1) {
                isEmergencyPhoneNumber.invoke(phoneNumber = "+15551234567")
            }
        }
    }

    @Test
    fun onCallClick_checksEmergencyPhoneNumberBeforeEmittingCallEffect() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val metadataDelegate = createMetadataDelegateMock()
            metadataDelegate.stateFlow.value = oneOnOneMetadataState(
                phoneNumber = "+15551234567",
            )
            val isEmergencyPhoneNumber = mockk<IsEmergencyPhoneNumber>()
            every { isEmergencyPhoneNumber.invoke(phoneNumber = "+15551234567") } returns false
            val viewModel = createViewModel(
                metadataDelegate = metadataDelegate.mock,
                isEmergencyPhoneNumber = isEmergencyPhoneNumber,
            )

            viewModel.effects.test {
                viewModel.onCallClick()
                advanceUntilIdle()

                assertEquals(
                    ConversationScreenEffect.PlacePhoneCall(
                        phoneNumber = "+15551234567",
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 1) {
                isEmergencyPhoneNumber.invoke(phoneNumber = "+15551234567")
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
    fun messageSelectionEffects_areExposedAsScreenEffects() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val messageSelectionDelegate = createMessageSelectionDelegateMock()
            val viewModel = createViewModel(
                messageSelectionDelegate = messageSelectionDelegate.mock,
            )
            advanceUntilIdle()

            viewModel.effects.test {
                messageSelectionDelegate.effectsFlow.emit(
                    ConversationScreenEffect.ShowMessage(
                        messageResId = 456,
                    ),
                )

                assertEquals(
                    ConversationScreenEffect.ShowMessage(
                        messageResId = 456,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onSeedDraft_forwardsToDraftDelegate() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val draftDelegate = createDraftDelegateMock()
            val viewModel = createViewModel(
                draftDelegate = draftDelegate.mock,
            )
            val draft = ConversationDraft(
                messageText = "Hello",
                selfParticipantId = "self-1",
            )

            viewModel.onSeedDraft(
                conversationId = "conversation-1",
                draft = draft,
            )

            verify(exactly = 1) {
                draftDelegate.mock.seedDraft(
                    conversationId = "conversation-1",
                    draft = draft,
                )
            }
        }
    }

    @Test
    fun onOpenStartupAttachment_emitsAttachmentPreviewForConversationImages() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.effects.test {
                viewModel.onOpenStartupAttachment(
                    conversationId = "conversation-1",
                    startupAttachment = ConversationEntryStartupAttachment(
                        contentType = "image/jpeg",
                        contentUri = "content://media/image/1",
                    ),
                )
                advanceUntilIdle()

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
    fun attachmentPreviewEvents_useDraftAndConversationImageCollections() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onConversationIdChanged(conversationId = "conversation-1")

            viewModel.effects.test {
                viewModel.onAttachmentClicked(
                    attachment = ComposerAttachmentUiModel.Resolved.VisualMedia.Image(
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
            val audioRecordingDelegate = createAudioRecordingDelegateMock()
            val messageSelectionDelegate = createMessageSelectionDelegateMock()
            val mediaPickerDelegate = createMediaPickerDelegateMock()
            val viewModel = createViewModel(
                audioRecordingDelegate = audioRecordingDelegate.mock,
                draftDelegate = draftDelegate.mock,
                messageSelectionDelegate = messageSelectionDelegate.mock,
                mediaPickerDelegate = mediaPickerDelegate.mock,
            )

            viewModel.onMessageClick(messageId = "message-1")
            viewModel.onMessageLongClick(messageId = "message-2")
            viewModel.onMessageSelectionActionClick(
                action = ConversationMessageSelectionAction.Delete,
            )
            viewModel.onMessageTextChanged(text = "Hello")
            viewModel.onAudioRecordingStart()
            viewModel.onLockedAudioRecordingStart()
            viewModel.onAudioRecordingFinish()
            viewModel.onAudioRecordingCancel()
            viewModel.onGalleryVisibilityChanged(isVisible = true)
            viewModel.onSendClick()
            viewModel.dismissDeleteMessageConfirmation()
            viewModel.dismissMessageSelection()
            viewModel.confirmDeleteSelectedMessages()
            viewModel.persistDraft()

            verify(exactly = 1) {
                messageSelectionDelegate.mock.onMessageClick(messageId = "message-1")
            }
            verify(exactly = 1) {
                messageSelectionDelegate.mock.onMessageLongClick(messageId = "message-2")
            }
            verify(exactly = 1) {
                messageSelectionDelegate.mock.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Delete,
                )
            }
            verify(exactly = 1) {
                draftDelegate.mock.onMessageTextChanged(messageText = "Hello")
            }
            verify(exactly = 1) {
                audioRecordingDelegate.mock.startRecording(selfParticipantId = "")
            }
            verify(exactly = 1) {
                audioRecordingDelegate.mock.startLockedRecording(selfParticipantId = "")
            }
            verify(exactly = 1) {
                audioRecordingDelegate.mock.finishRecording()
            }
            verify(exactly = 1) {
                audioRecordingDelegate.mock.cancelRecording()
            }
            verify(exactly = 1) {
                mediaPickerDelegate.mock.onGalleryVisibilityChanged(isVisible = true)
            }
            verify(exactly = 1) {
                draftDelegate.mock.onSendClick()
            }
            verify(exactly = 1) {
                messageSelectionDelegate.mock.dismissDeleteMessageConfirmation()
            }
            verify(exactly = 1) {
                messageSelectionDelegate.mock.dismissMessageSelection()
            }
            verify(exactly = 1) {
                messageSelectionDelegate.mock.confirmDeleteSelectedMessages()
            }
            verify(exactly = 1) {
                draftDelegate.mock.persistDraft()
            }
        }
    }

    @Test
    fun metadataEffects_areExposedAsScreenEffects() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val metadataDelegate = createMetadataDelegateMock()
            val viewModel = createViewModel(
                metadataDelegate = metadataDelegate.mock,
            )
            advanceUntilIdle()

            viewModel.effects.test {
                metadataDelegate.effectsFlow.emit(ConversationScreenEffect.CloseConversation)

                assertEquals(ConversationScreenEffect.CloseConversation, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun scaffoldUiState_reflectsMetadataDeleteConfirmationVisibility() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val metadataDelegate = createMetadataDelegateMock()
            val viewModel = createViewModel(metadataDelegate = metadataDelegate.mock)

            viewModel.scaffoldUiState.test {
                assertEquals(false, awaitItem().isDeleteConversationConfirmationVisible)

                metadataDelegate.deleteConfirmationVisibleFlow.value = true
                advanceUntilIdle()
                assertEquals(true, awaitItem().isDeleteConversationConfirmationVisible)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun conversationActionMethods_forwardToMetadataDelegate() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val metadataDelegate = createMetadataDelegateMock()
            val viewModel = createViewModel(metadataDelegate = metadataDelegate.mock)

            viewModel.onArchiveConversationClick()
            viewModel.onUnarchiveConversationClick()
            viewModel.onAddContactClick()
            viewModel.onDeleteConversationClick()
            viewModel.confirmDeleteConversation()
            viewModel.dismissDeleteConversationConfirmation()

            verify(exactly = 1) { metadataDelegate.mock.onArchiveConversationClick() }
            verify(exactly = 1) { metadataDelegate.mock.onUnarchiveConversationClick() }
            verify(exactly = 1) { metadataDelegate.mock.onAddContactClick() }
            verify(exactly = 1) { metadataDelegate.mock.onDeleteConversationClick() }
            verify(exactly = 1) { metadataDelegate.mock.confirmDeleteConversation() }
            verify(exactly = 1) { metadataDelegate.mock.dismissDeleteConversationConfirmation() }
        }
    }

    @Test
    fun onCleared_flushesDraftDelegateAndMediaPickerDelegate() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val draftDelegate = createDraftDelegateMock()
            val audioRecordingDelegate = createAudioRecordingDelegateMock()
            val mediaPickerDelegate = createMediaPickerDelegateMock()
            val focusDelegate = createFocusDelegateMock()
            val viewModelStore = ViewModelStore()
            createViewModelInStore(
                viewModelStore = viewModelStore,
                audioRecordingDelegate = audioRecordingDelegate.mock,
                draftDelegate = draftDelegate.mock,
                mediaPickerDelegate = mediaPickerDelegate.mock,
                focusDelegate = focusDelegate.mock,
            )

            viewModelStore.clear()

            verify(exactly = 1) {
                audioRecordingDelegate.mock.onScreenCleared()
            }
            verify(exactly = 1) {
                draftDelegate.mock.flushDraft()
            }
            verify(exactly = 1) {
                mediaPickerDelegate.mock.onScreenCleared()
            }
            verify(exactly = 1) {
                focusDelegate.mock.setScreenFocused(focused = false)
            }
        }
    }

    @Test
    fun onScreenForegrounded_forwardsCancelNotificationFlagToFocusDelegate() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val focusDelegate = createFocusDelegateMock()
            val viewModel = createViewModel(focusDelegate = focusDelegate.mock)

            viewModel.onScreenForegrounded(cancelNotification = true)
            viewModel.onScreenForegrounded(cancelNotification = false)

            verify(exactly = 1) {
                focusDelegate.mock.setScreenFocused(
                    focused = true,
                    cancelNotification = true,
                )
            }
            verify(exactly = 1) {
                focusDelegate.mock.setScreenFocused(
                    focused = true,
                    cancelNotification = false,
                )
            }
        }
    }

    @Test
    fun onScreenBackgrounded_unsetsFocusOnDelegate() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val focusDelegate = createFocusDelegateMock()
            val viewModel = createViewModel(focusDelegate = focusDelegate.mock)

            viewModel.onScreenBackgrounded()

            verify(exactly = 1) {
                focusDelegate.mock.setScreenFocused(focused = false)
            }
        }
    }

    private fun createViewModel(
        audioRecordingDelegate: ConversationAudioRecordingDelegate =
            createAudioRecordingDelegateMock().mock,
        composerAttachmentsDelegate: ConversationComposerAttachmentsDelegate =
            createComposerAttachmentsDelegateMock().mock,
        draftDelegate: ConversationDraftDelegate = createDraftDelegateMock().mock,
        messagesDelegate: ConversationMessagesDelegate = createMessagesDelegateMock().mock,
        messageSelectionDelegate: ConversationMessageSelectionDelegate =
            createMessageSelectionDelegateMock().mock,
        mediaPickerDelegate: ConversationMediaPickerDelegate = createMediaPickerDelegateMock().mock,
        metadataDelegate: ConversationMetadataDelegate = createMetadataDelegateMock().mock,
        focusDelegate: ConversationFocusDelegate = createFocusDelegateMock().mock,
        canAddMoreConversationParticipants: CanAddMoreConversationParticipants = mockk {
            every { invoke(participantCount = any()) } returns false
        },
        isDeviceVoiceCapable: IsDeviceVoiceCapable = IsDeviceVoiceCapable { false },
        isEmergencyPhoneNumber: IsEmergencyPhoneNumber = IsEmergencyPhoneNumber { false },
        composerUiStateMapper: ConversationComposerUiStateMapper =
            createComposerUiStateMapperMock(mappedUiState = ConversationComposerUiState()),
        subscriptionsRepository: ConversationSubscriptionsRepository =
            createSubscriptionsRepositoryMock(subscriptions = persistentListOf()),
    ): ConversationViewModel {
        return ConversationViewModel(
            conversationAudioRecordingDelegate = audioRecordingDelegate,
            conversationComposerAttachmentsDelegate = composerAttachmentsDelegate,
            conversationDraftDelegate = draftDelegate,
            conversationMessagesDelegate = messagesDelegate,
            conversationMessageSelectionDelegate = messageSelectionDelegate,
            conversationMediaPickerDelegate = mediaPickerDelegate,
            conversationMetadataDelegate = metadataDelegate,
            conversationFocusDelegate = focusDelegate,
            conversationComposerUiStateMapper = composerUiStateMapper,
            conversationSubscriptionsRepository = subscriptionsRepository,
            canAddMoreConversationParticipants = canAddMoreConversationParticipants,
            isDeviceVoiceCapable = isDeviceVoiceCapable,
            isEmergencyPhoneNumber = isEmergencyPhoneNumber,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
            savedStateHandle = SavedStateHandle(),
        )
    }

    private fun createViewModelInStore(
        viewModelStore: ViewModelStore,
        audioRecordingDelegate: ConversationAudioRecordingDelegate =
            createAudioRecordingDelegateMock().mock,
        composerAttachmentsDelegate: ConversationComposerAttachmentsDelegate =
            createComposerAttachmentsDelegateMock().mock,
        draftDelegate: ConversationDraftDelegate = createDraftDelegateMock().mock,
        messagesDelegate: ConversationMessagesDelegate = createMessagesDelegateMock().mock,
        messageSelectionDelegate: ConversationMessageSelectionDelegate =
            createMessageSelectionDelegateMock().mock,
        mediaPickerDelegate: ConversationMediaPickerDelegate = createMediaPickerDelegateMock().mock,
        metadataDelegate: ConversationMetadataDelegate = createMetadataDelegateMock().mock,
        focusDelegate: ConversationFocusDelegate = createFocusDelegateMock().mock,
        canAddMoreConversationParticipants: CanAddMoreConversationParticipants = mockk {
            every { invoke(participantCount = any()) } returns false
        },
        isDeviceVoiceCapable: IsDeviceVoiceCapable = IsDeviceVoiceCapable { false },
        isEmergencyPhoneNumber: IsEmergencyPhoneNumber = IsEmergencyPhoneNumber { false },
        composerUiStateMapper: ConversationComposerUiStateMapper =
            createComposerUiStateMapperMock(mappedUiState = ConversationComposerUiState()),
        subscriptionsRepository: ConversationSubscriptionsRepository =
            createSubscriptionsRepositoryMock(subscriptions = persistentListOf()),
    ): ConversationViewModel {
        return ViewModelProvider(
            store = viewModelStore,
            factory = object : ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return createViewModel(
                        audioRecordingDelegate = audioRecordingDelegate,
                        composerAttachmentsDelegate = composerAttachmentsDelegate,
                        draftDelegate = draftDelegate,
                        messagesDelegate = messagesDelegate,
                        messageSelectionDelegate = messageSelectionDelegate,
                        mediaPickerDelegate = mediaPickerDelegate,
                        metadataDelegate = metadataDelegate,
                        focusDelegate = focusDelegate,
                        canAddMoreConversationParticipants = canAddMoreConversationParticipants,
                        isDeviceVoiceCapable = isDeviceVoiceCapable,
                        isEmergencyPhoneNumber = isEmergencyPhoneNumber,
                        composerUiStateMapper = composerUiStateMapper,
                        subscriptionsRepository = subscriptionsRepository,
                    ) as T
                }
            },
        )[ConversationViewModel::class.java]
    }

    private fun oneOnOneMetadataState(phoneNumber: String): ConversationMetadataUiState.Present {
        return ConversationMetadataUiState.Present(
            title = "Alice",
            selfParticipantId = "self-1",
            avatar = ConversationMetadataUiState.Avatar.Single(photoUri = null),
            participantCount = 1,
            otherParticipantDisplayDestination = phoneNumber,
            otherParticipantPhoneNumber = phoneNumber,
            otherParticipantContactLookupKey = null,
            isArchived = false,
            composerAvailability = ConversationComposerAvailability.editable(),
        )
    }

    private fun createComposerAttachmentsDelegateMock(): ComposerAttachmentsDelegateMock {
        val bindCalls = mutableListOf<ComposerAttachmentsBindCall>()
        val stateFlow = MutableStateFlow<ImmutableList<ComposerAttachmentUiModel>>(
            persistentListOf(),
        )
        val mock = mockk<ConversationComposerAttachmentsDelegate>(relaxed = true)
        every { mock.state } returns stateFlow
        every {
            mock.bind(any(), any())
        } answers {
            bindCalls += ComposerAttachmentsBindCall(
                scope = firstArg(),
                draftStateFlow = secondArg(),
            )
        }
        return ComposerAttachmentsDelegateMock(
            mock = mock,
            stateFlow = stateFlow,
            bindCalls = bindCalls,
        )
    }

    private fun createAudioRecordingDelegateMock(): AudioRecordingDelegateMock {
        val bindCalls = mutableListOf<BindCall<ConversationAudioRecordingUiState>>()
        val stateFlow = MutableStateFlow(ConversationAudioRecordingUiState())
        val mock = mockk<ConversationAudioRecordingDelegate>(relaxed = true)
        every { mock.state } returns stateFlow
        every {
            mock.bind(any(), any())
        } answers {
            bindCalls += BindCall(
                scope = firstArg(),
                conversationIdFlow = secondArg(),
            )
        }
        return AudioRecordingDelegateMock(
            mock = mock,
            stateFlow = stateFlow,
            bindCalls = bindCalls,
        )
    }

    private fun createSubscriptionsRepositoryMock(
        subscriptions: ImmutableList<ConversationSubscription>,
    ): ConversationSubscriptionsRepository {
        val repository = mockk<ConversationSubscriptionsRepository>()
        every {
            repository.observeActiveSubscriptions()
        } returns MutableStateFlow(subscriptions)
        return repository
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

    private fun createMessageSelectionDelegateMock(): MessageSelectionDelegateMock {
        val bindCalls = mutableListOf<BindCall<ConversationMessageSelectionUiState>>()
        val stateFlow = MutableStateFlow(ConversationMessageSelectionUiState())
        val effectsFlow = MutableSharedFlow<ConversationScreenEffect>()
        val mock = mockk<ConversationMessageSelectionDelegate>(relaxed = true)
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
        return MessageSelectionDelegateMock(
            mock = mock,
            stateFlow = stateFlow,
            effectsFlow = effectsFlow,
            bindCalls = bindCalls,
        )
    }

    private fun createMetadataDelegateMock(): MetadataDelegateMock {
        val bindCalls = mutableListOf<BindCall<ConversationMetadataUiState>>()
        val stateFlow = MutableStateFlow<ConversationMetadataUiState>(
            ConversationMetadataUiState.Loading,
        )
        val effectsFlow = MutableSharedFlow<ConversationScreenEffect>()
        val deleteConfirmationVisibleFlow = MutableStateFlow(value = false)
        val mock = mockk<ConversationMetadataDelegate>(relaxed = true)
        every { mock.state } returns stateFlow
        every { mock.effects } returns effectsFlow
        every {
            mock.isDeleteConversationConfirmationVisible
        } returns deleteConfirmationVisibleFlow
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
            effectsFlow = effectsFlow,
            deleteConfirmationVisibleFlow = deleteConfirmationVisibleFlow,
            bindCalls = bindCalls,
        )
    }

    private fun createFocusDelegateMock(): FocusDelegateMock {
        val bindCalls = mutableListOf<FocusBindCall>()
        val mock = mockk<ConversationFocusDelegate>(relaxed = true)
        every {
            mock.bind(any(), any())
        } answers {
            bindCalls += FocusBindCall(
                scope = firstArg(),
                conversationIdFlow = secondArg(),
            )
        }
        return FocusDelegateMock(
            mock = mock,
            bindCalls = bindCalls,
        )
    }

    private fun createComposerUiStateMapperMock(
        mappedUiState: ConversationComposerUiState,
    ): ConversationComposerUiStateMapper {
        val mapper = mockk<ConversationComposerUiStateMapper>()
        every {
            mapper.map(any(), any(), any(), any(), any())
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

    private data class ComposerAttachmentsBindCall(
        val scope: CoroutineScope,
        val draftStateFlow: StateFlow<ConversationDraftState>,
    )

    private data class ComposerAttachmentsDelegateMock(
        val mock: ConversationComposerAttachmentsDelegate,
        val stateFlow: MutableStateFlow<ImmutableList<ComposerAttachmentUiModel>>,
        val bindCalls: List<ComposerAttachmentsBindCall>,
    )

    private data class AudioRecordingDelegateMock(
        val mock: ConversationAudioRecordingDelegate,
        val stateFlow: MutableStateFlow<ConversationAudioRecordingUiState>,
        val bindCalls: List<BindCall<ConversationAudioRecordingUiState>>,
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

    private data class MessageSelectionDelegateMock(
        val mock: ConversationMessageSelectionDelegate,
        val stateFlow: MutableStateFlow<ConversationMessageSelectionUiState>,
        val effectsFlow: MutableSharedFlow<ConversationScreenEffect>,
        val bindCalls: List<BindCall<ConversationMessageSelectionUiState>>,
    )

    private data class MetadataDelegateMock(
        val mock: ConversationMetadataDelegate,
        val stateFlow: MutableStateFlow<ConversationMetadataUiState>,
        val effectsFlow: MutableSharedFlow<ConversationScreenEffect>,
        val deleteConfirmationVisibleFlow: MutableStateFlow<Boolean>,
        val bindCalls: List<BindCall<ConversationMetadataUiState>>,
    )

    private data class FocusBindCall(
        val scope: CoroutineScope,
        val conversationIdFlow: StateFlow<String?>,
    )

    private data class FocusDelegateMock(
        val mock: ConversationFocusDelegate,
        val bindCalls: List<FocusBindCall>,
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
            canCopyMessageToClipboard = false,
            canDownloadMessage = false,
            canForwardMessage = false,
            canResendMessage = false,
            canSaveAttachments = false,
            mmsSubject = null,
            protocol = ConversationMessageUiModel.Protocol.SMS,
        )
    }
}
