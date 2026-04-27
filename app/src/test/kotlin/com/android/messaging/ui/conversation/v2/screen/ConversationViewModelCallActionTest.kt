package com.android.messaging.ui.conversation.v2.screen

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.conversation.repository.ConversationSubscriptionsRepository
import com.android.messaging.domain.conversation.usecase.action.CreateDefaultSmsRoleRequest
import com.android.messaging.domain.conversation.usecase.participant.CanAddMoreConversationParticipants
import com.android.messaging.domain.conversation.usecase.telephony.IsDeviceVoiceCapable
import com.android.messaging.domain.conversation.usecase.telephony.IsEmergencyPhoneNumber
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.conversation.v2.audio.delegate.ConversationAudioRecordingDelegate
import com.android.messaging.ui.conversation.v2.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationComposerAttachmentsDelegate
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.v2.composer.mapper.ConversationComposerUiStateMapperImpl
import com.android.messaging.ui.conversation.v2.composer.model.ConversationDraftState
import com.android.messaging.ui.conversation.v2.focus.delegate.ConversationFocusDelegate
import com.android.messaging.ui.conversation.v2.mediapicker.ConversationMediaPickerDelegate
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationMediaPickerUiState
import com.android.messaging.ui.conversation.v2.messages.delegate.ConversationMessageSelectionDelegate
import com.android.messaging.ui.conversation.v2.messages.delegate.ConversationMessagesDelegate
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.v2.metadata.delegate.ConversationMetadataDelegate
import com.android.messaging.ui.conversation.v2.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMessageSelectionUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ConversationViewModelCallActionTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun scaffoldUiState_allowsCallForVoiceCapableOneOnOneNonEmergencyConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(
                metadataState = oneOnOneMetadata(phoneNumber = "+15551234567"),
                isDeviceVoiceCapable = true,
                emergencyPhoneNumbers = setOf("911"),
            )

            assertTrue(viewModel.scaffoldUiState.value.canCall)
        }
    }

    @Test
    fun scaffoldUiState_hidesCallForEmergencyConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(
                metadataState = oneOnOneMetadata(phoneNumber = "911"),
                isDeviceVoiceCapable = true,
                emergencyPhoneNumbers = setOf("911"),
            )

            assertFalse(viewModel.scaffoldUiState.value.canCall)
        }
    }

    @Test
    fun onCallClick_doesNotEmitCallEffectForEmergencyConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(
                metadataState = oneOnOneMetadata(phoneNumber = "911"),
                isDeviceVoiceCapable = true,
                emergencyPhoneNumbers = setOf("911"),
            )

            viewModel.effects.test {
                viewModel.onCallClick()
                advanceUntilIdle()

                expectNoEvents()
            }
        }
    }

    @Test
    fun onCallClick_emitsCallEffectForNonEmergencyConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(
                metadataState = oneOnOneMetadata(phoneNumber = "+15551234567"),
                isDeviceVoiceCapable = true,
                emergencyPhoneNumbers = setOf("911"),
            )

            viewModel.effects.test {
                viewModel.onCallClick()
                advanceUntilIdle()

                val effect = awaitItem()
                assertTrue(effect is ConversationScreenEffect.PlacePhoneCall)
                assertEquals(
                    "+15551234567",
                    (effect as ConversationScreenEffect.PlacePhoneCall).phoneNumber,
                )
            }
        }
    }

    private fun createViewModel(
        metadataState: ConversationMetadataUiState,
        isDeviceVoiceCapable: Boolean,
        emergencyPhoneNumbers: Set<String>,
    ): ConversationViewModel {
        val audioRecordingDelegate = mockk<ConversationAudioRecordingDelegate>(relaxed = true)
        val composerAttachmentsDelegate =
            mockk<ConversationComposerAttachmentsDelegate>(relaxed = true)
        val draftDelegate = mockk<ConversationDraftDelegate>(relaxed = true)
        val messagesDelegate = mockk<ConversationMessagesDelegate>(relaxed = true)
        val messageSelectionDelegate = mockk<ConversationMessageSelectionDelegate>(relaxed = true)
        val mediaPickerDelegate = mockk<ConversationMediaPickerDelegate>(relaxed = true)
        val metadataDelegate = mockk<ConversationMetadataDelegate>(relaxed = true)
        val focusDelegate = mockk<ConversationFocusDelegate>(relaxed = true)
        val subscriptionsRepository = mockk<ConversationSubscriptionsRepository>()

        every { audioRecordingDelegate.state } returns MutableStateFlow(
            ConversationAudioRecordingUiState(),
        )
        every { composerAttachmentsDelegate.state } returns MutableStateFlow(persistentListOf())
        every { draftDelegate.effects } returns emptyFlow()
        every { draftDelegate.state } returns MutableStateFlow(ConversationDraftState())
        every { messagesDelegate.state } returns
            MutableStateFlow(ConversationMessagesUiState.Loading)
        every { messageSelectionDelegate.effects } returns emptyFlow()
        every { messageSelectionDelegate.state } returns MutableStateFlow(
            ConversationMessageSelectionUiState(),
        )
        every { mediaPickerDelegate.effects } returns emptyFlow()
        every { mediaPickerDelegate.state } returns
            MutableStateFlow(ConversationMediaPickerUiState())
        every { metadataDelegate.effects } returns emptyFlow()
        every { metadataDelegate.isDeleteConversationConfirmationVisible } returns
            MutableStateFlow(false)
        every { metadataDelegate.state } returns MutableStateFlow(metadataState)
        every { subscriptionsRepository.observeActiveSubscriptions() } returns flowOf(
            persistentListOf(),
        )

        val canAddMoreConversationParticipants = CanAddMoreConversationParticipants {
            true
        }
        val deviceVoiceCapable = IsDeviceVoiceCapable {
            isDeviceVoiceCapable
        }
        val emergencyPhoneNumber = IsEmergencyPhoneNumber { phoneNumber ->
            emergencyPhoneNumbers.contains(phoneNumber)
        }

        return ConversationViewModel(
            conversationAudioRecordingDelegate = audioRecordingDelegate,
            conversationComposerAttachmentsDelegate = composerAttachmentsDelegate,
            conversationDraftDelegate = draftDelegate,
            conversationMessagesDelegate = messagesDelegate,
            conversationMessageSelectionDelegate = messageSelectionDelegate,
            conversationMediaPickerDelegate = mediaPickerDelegate,
            conversationMetadataDelegate = metadataDelegate,
            conversationFocusDelegate = focusDelegate,
            conversationComposerUiStateMapper = ConversationComposerUiStateMapperImpl(),
            conversationSubscriptionsRepository = subscriptionsRepository,
            canAddMoreConversationParticipants = canAddMoreConversationParticipants,
            createDefaultSmsRoleRequest = CreateDefaultSmsRoleRequest {
                null
            },
            isDeviceVoiceCapable = deviceVoiceCapable,
            isEmergencyPhoneNumber = emergencyPhoneNumber,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
            savedStateHandle = SavedStateHandle(),
        )
    }

    private fun oneOnOneMetadata(phoneNumber: String): ConversationMetadataUiState.Present {
        return ConversationMetadataUiState.Present(
            title = "Alice",
            selfParticipantId = "self",
            avatar = ConversationMetadataUiState.Avatar.Single(photoUri = null),
            participantCount = 1,
            otherParticipantDisplayDestination = phoneNumber,
            otherParticipantPhoneNumber = phoneNumber,
            otherParticipantContactLookupKey = null,
            isArchived = false,
            composerAvailability = ConversationComposerAvailability.editable(),
        )
    }
}
