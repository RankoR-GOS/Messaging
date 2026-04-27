package com.android.messaging.ui.conversation.v2.messages.delegate

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import app.cash.turbine.test
import com.android.messaging.R
import com.android.messaging.data.conversation.model.message.ConversationMessageDetailsData
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.datamodel.data.ConversationParticipantsData
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.domain.conversation.usecase.action.CheckConversationActionRequirements
import com.android.messaging.domain.conversation.usecase.action.ConversationActionRequirementsResult
import com.android.messaging.domain.conversation.usecase.forward.CreateForwardedMessage
import com.android.messaging.ui.conversation.v2.mediapicker.repository.ConversationAttachmentRepository
import com.android.messaging.ui.conversation.v2.mediapicker.repository.SaveAttachmentsResult
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMessageSelectionAction
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMessageSelectionUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationMessageSelectionDelegateImplTest {

    @Test
    fun onMessageLongClick_selectsSingleMessageAndExposesSupportedActions() {
        runTest {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        text = "Hello",
                        canCopyMessageToClipboard = true,
                        canForwardMessage = true,
                    ),
                )
                advanceUntilIdle()

                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                assertEquals(
                    persistentSetOf("message-1"),
                    harness.delegate.state.value.selectedMessageIds,
                )
                assertEquals(
                    persistentSetOf(
                        ConversationMessageSelectionAction.Delete,
                        ConversationMessageSelectionAction.Share,
                        ConversationMessageSelectionAction.Forward,
                        ConversationMessageSelectionAction.Copy,
                        ConversationMessageSelectionAction.Details,
                    ),
                    harness.delegate.state.value.availableActions,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onMessageClick_doesNothingOutsideSelectionMode() {
        runTest {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(messageId = "message-1"),
                )
                advanceUntilIdle()

                harness.delegate.onMessageClick(messageId = "message-1")
                advanceUntilIdle()

                assertEquals(
                    ConversationMessageSelectionUiState(),
                    harness.delegate.state.value,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onMessageClick_togglesSelectionWhenSelectionModeIsActive() {
        runTest {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(messageId = "message-1"),
                    createMessageUiModel(messageId = "message-2"),
                )
                advanceUntilIdle()

                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()
                harness.delegate.onMessageClick(messageId = "message-2")
                advanceUntilIdle()

                assertEquals(
                    persistentSetOf("message-1", "message-2"),
                    harness.delegate.state.value.selectedMessageIds,
                )
                assertEquals(
                    persistentSetOf(ConversationMessageSelectionAction.Delete),
                    harness.delegate.state.value.availableActions,
                )

                harness.delegate.onMessageClick(messageId = "message-1")
                advanceUntilIdle()

                assertEquals(
                    persistentSetOf("message-2"),
                    harness.delegate.state.value.selectedMessageIds,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun bind_clearsSelectionWhenConversationChanges() {
        runTest {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(messageId = "message-1"),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()
                harness.delegate.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Delete,
                )
                advanceUntilIdle()

                harness.conversationIdFlow.value = "conversation-2"
                advanceUntilIdle()

                assertEquals(
                    ConversationMessageSelectionUiState(),
                    harness.delegate.state.value,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun copyAction_copiesTextAndClearsSelection() {
        runTest {
            val harness = createHarness()
            val copiedClipData = slot<ClipData>()
            every {
                harness.clipboardManager.setPrimaryClip(capture(copiedClipData))
            } just runs

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        text = "Copied text",
                        canCopyMessageToClipboard = true,
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Copy,
                )
                advanceUntilIdle()

                assertEquals(
                    "Copied text",
                    copiedClipData.captured.getItemAt(0).text.toString(),
                )
                assertEquals(
                    ConversationMessageSelectionUiState(),
                    harness.delegate.state.value,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun deleteAction_showsAndDismissesConfirmation() {
        runTest {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(messageId = "message-1"),
                    createMessageUiModel(messageId = "message-2"),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()
                harness.delegate.onMessageClick(messageId = "message-2")
                advanceUntilIdle()

                harness.delegate.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Delete,
                )
                advanceUntilIdle()

                assertEquals(
                    persistentSetOf("message-1", "message-2"),
                    harness.delegate.state.value.deleteConfirmation?.messageIds,
                )

                harness.delegate.dismissDeleteMessageConfirmation()
                advanceUntilIdle()

                assertNull(harness.delegate.state.value.deleteConfirmation)
                assertEquals(
                    persistentSetOf("message-1", "message-2"),
                    harness.delegate.state.value.selectedMessageIds,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun confirmDeleteSelectedMessages_deletesSelectedMessagesAndClearsSelection() {
        runTest {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(messageId = "message-1"),
                    createMessageUiModel(messageId = "message-2"),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()
                harness.delegate.onMessageClick(messageId = "message-2")
                advanceUntilIdle()
                harness.delegate.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Delete,
                )
                advanceUntilIdle()

                harness.delegate.confirmDeleteSelectedMessages()
                advanceUntilIdle()

                verify(exactly = 1) {
                    harness.conversationsRepository.deleteMessages(
                        messageIds = persistentSetOf("message-1", "message-2"),
                    )
                }
                assertEquals(
                    ConversationMessageSelectionUiState(),
                    harness.delegate.state.value,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun downloadAction_downloadsSelectedMessageAndClearsSelection() {
        runTest {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        canDownloadMessage = true,
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Download,
                )
                advanceUntilIdle()

                verify(exactly = 1) {
                    harness.conversationsRepository.downloadMessage(messageId = "message-1")
                }
                assertEquals(
                    ConversationMessageSelectionUiState(),
                    harness.delegate.state.value,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun resendAction_resendsSelectedMessageAndClearsSelection() {
        runTest {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        canResendMessage = true,
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Resend,
                )
                advanceUntilIdle()

                verify(exactly = 1) {
                    harness.conversationsRepository.resendMessage(messageId = "message-1")
                }
                assertEquals(
                    ConversationMessageSelectionUiState(),
                    harness.delegate.state.value,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun resendAction_whenSmsIsNotCapable_emitsSmsDisabledMessage() {
        runTest {
            val harness = createHarness(
                actionRequirements = createActionRequirementsMock(
                    initialResult = ConversationActionRequirementsResult.SmsNotCapable,
                ),
            )

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        canResendMessage = true,
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onMessageSelectionActionClick(
                        action = ConversationMessageSelectionAction.Resend,
                    )
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
    fun resendAction_whenPreferredSmsSimIsMissing_emitsNoPreferredSimMessage() {
        runTest {
            val harness = createHarness(
                actionRequirements = createActionRequirementsMock(
                    initialResult = ConversationActionRequirementsResult.NoPreferredSmsSim,
                ),
            )

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        canResendMessage = true,
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onMessageSelectionActionClick(
                        action = ConversationMessageSelectionAction.Resend,
                    )
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
    fun resendAction_whenDefaultSmsRoleIsMissing_promptsAndResendsAfterRoleRequestSucceeds() {
        runTest {
            val actionRequirements = createActionRequirementsMock(
                initialResult = ConversationActionRequirementsResult.MissingDefaultSmsRole,
            )
            val harness = createHarness(actionRequirements = actionRequirements)

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        canResendMessage = true,
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onMessageSelectionActionClick(
                        action = ConversationMessageSelectionAction.Resend,
                    )
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.RequestDefaultSmsRole(isSending = true),
                        awaitItem(),
                    )
                    verify(exactly = 0) {
                        harness.conversationsRepository.resendMessage(any())
                    }

                    actionRequirements.result = ConversationActionRequirementsResult.Ready
                    assertTrue(
                        harness.delegate.onDefaultSmsRoleRequestResult(
                            resultCode = Activity.RESULT_OK,
                        ),
                    )
                    advanceUntilIdle()

                    verify(exactly = 1) {
                        harness.conversationsRepository.resendMessage(messageId = "message-1")
                    }
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onDefaultSmsRoleRequestResult_withoutPendingResend_returnsFalse() {
        runTest {
            val harness = createHarness()

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
    fun onDefaultSmsRoleRequestResult_whenCanceled_clearsPendingResend() {
        runTest {
            val actionRequirements = createActionRequirementsMock(
                initialResult = ConversationActionRequirementsResult.MissingDefaultSmsRole,
            )
            val harness = createHarness(actionRequirements = actionRequirements)

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        canResendMessage = true,
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onMessageSelectionActionClick(
                        action = ConversationMessageSelectionAction.Resend,
                    )
                    advanceUntilIdle()
                    awaitItem()

                    assertTrue(
                        harness.delegate.onDefaultSmsRoleRequestResult(
                            resultCode = Activity.RESULT_CANCELED,
                        ),
                    )
                    advanceUntilIdle()

                    verify(exactly = 0) {
                        harness.conversationsRepository.resendMessage(any())
                    }
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun forwardAction_emitsForwardEffectAndClearsSelection() {
        runTest {
            val harness = createHarness()
            val forwardedMessage = mockk<MessageData>()
            every {
                harness.createForwardedMessage.invoke(
                    conversationId = "conversation-1",
                    messageId = "message-1",
                )
            } returns forwardedMessage

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        canForwardMessage = true,
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onMessageSelectionActionClick(
                        action = ConversationMessageSelectionAction.Forward,
                    )
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.LaunchForwardMessage(
                            message = forwardedMessage,
                        ),
                        awaitItem(),
                    )
                    cancelAndIgnoreRemainingEvents()
                }
                assertEquals(
                    ConversationMessageSelectionUiState(),
                    harness.delegate.state.value,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun detailsAction_emitsDetailsEffectAndClearsSelection() {
        runTest {
            val harness = createHarness()
            val messageDetails = mockk<ConversationMessageData>()
            val participants = mockk<ConversationParticipantsData>()
            val selfParticipant = mockk<ParticipantData>()
            every {
                harness.conversationsRepository.getMessageDetailsData(
                    conversationId = "conversation-1",
                    messageId = "message-1",
                )
            } returns ConversationMessageDetailsData(
                message = messageDetails,
                participants = participants,
                selfParticipant = selfParticipant,
            )

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(messageId = "message-1"),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onMessageSelectionActionClick(
                        action = ConversationMessageSelectionAction.Details,
                    )
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.ShowMessageDetails(
                            message = messageDetails,
                            participants = participants,
                            selfParticipant = selfParticipant,
                        ),
                        awaitItem(),
                    )
                    cancelAndIgnoreRemainingEvents()
                }
                assertEquals(
                    ConversationMessageSelectionUiState(),
                    harness.delegate.state.value,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onMessageLongClick_exposesSaveAttachmentActionWhenCanSaveAttachments() {
        runTest {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        text = null,
                        canSaveAttachments = true,
                        parts = listOf(
                            createAttachmentPart(
                                contentType = "image/jpeg",
                                contentUri = "content://media/image/1",
                            ),
                        ),
                    ),
                )
                advanceUntilIdle()

                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                assertEquals(
                    persistentSetOf(
                        ConversationMessageSelectionAction.Delete,
                        ConversationMessageSelectionAction.SaveAttachment,
                        ConversationMessageSelectionAction.Details,
                    ),
                    harness.delegate.state.value.availableActions,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun saveAttachmentAction_emitsResultEffectAndClearsSelection() {
        runTest {
            val harness = createHarness()
            val attachments = listOf(
                ConversationAttachmentRepository.AttachmentToSave(
                    contentType = "image/jpeg",
                    contentUri = "content://media/image/1",
                ),
            )
            every {
                harness.conversationAttachmentRepository.saveAttachmentsToMediaStore(
                    attachments = attachments,
                )
            } returns flowOf(
                SaveAttachmentsResult(
                    imageCount = 1,
                    videoCount = 0,
                    otherCount = 0,
                    failCount = 0,
                ),
            )

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        text = null,
                        canSaveAttachments = true,
                        parts = listOf(
                            createAttachmentPart(
                                contentType = "image/jpeg",
                                contentUri = "content://media/image/1",
                            ),
                        ),
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onMessageSelectionActionClick(
                        action = ConversationMessageSelectionAction.SaveAttachment,
                    )
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.ShowSaveAttachmentsResult(
                            imageCount = 1,
                            videoCount = 0,
                            otherCount = 0,
                            failCount = 0,
                        ),
                        awaitItem(),
                    )
                    cancelAndIgnoreRemainingEvents()
                }
                assertEquals(
                    ConversationMessageSelectionUiState(),
                    harness.delegate.state.value,
                )
                verify(exactly = 1) {
                    @Suppress("UnusedFlow")
                    harness.conversationAttachmentRepository.saveAttachmentsToMediaStore(
                        attachments = attachments,
                    )
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun saveAttachmentAction_skipsAttachmentsWithBlankContentTypeOrNullUri() {
        runTest {
            val harness = createHarness()
            val attachments = listOf(
                ConversationAttachmentRepository.AttachmentToSave(
                    contentType = "image/jpeg",
                    contentUri = "content://media/image/1",
                ),
            )
            every {
                harness.conversationAttachmentRepository.saveAttachmentsToMediaStore(
                    attachments = attachments,
                )
            } returns flowOf(
                SaveAttachmentsResult(
                    imageCount = 1,
                    videoCount = 0,
                    otherCount = 0,
                    failCount = 0,
                ),
            )

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        text = null,
                        canSaveAttachments = true,
                        parts = listOf(
                            createAttachmentPart(
                                contentType = "image/jpeg",
                                contentUri = "content://media/image/1",
                            ),
                            ConversationMessagePartUiModel.Attachment.File(
                                text = null,
                                contentType = "",
                                contentUri = Uri.parse("content://media/blank"),
                                width = 0,
                                height = 0,
                            ),
                            ConversationMessagePartUiModel.Attachment.File(
                                text = null,
                                contentType = "application/pdf",
                                contentUri = null,
                                width = 0,
                                height = 0,
                            ),
                        ),
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.SaveAttachment,
                )
                advanceUntilIdle()

                verify(exactly = 1) {
                    @Suppress("UnusedFlow")
                    harness.conversationAttachmentRepository.saveAttachmentsToMediaStore(
                        attachments = attachments,
                    )
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun shareAction_emitsTextShareWhenSelectedMessageHasText() {
        runTest {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        text = "Share me",
                        canForwardMessage = true,
                        parts = listOf(
                            createAttachmentPart(
                                contentType = "image/jpeg",
                                contentUri = "content://media/image/1",
                            ),
                        ),
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onMessageSelectionActionClick(
                        action = ConversationMessageSelectionAction.Share,
                    )
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.ShareMessage(
                            attachmentContentType = null,
                            attachmentContentUri = null,
                            text = "Share me",
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
    fun shareAction_emitsAttachmentShareWhenSelectedMessageHasNoText() {
        runTest {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        text = null,
                        canForwardMessage = true,
                        parts = listOf(
                            createAttachmentPart(
                                contentType = "image/jpeg",
                                contentUri = "content://media/image/1",
                            ),
                        ),
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onMessageSelectionActionClick(
                        action = ConversationMessageSelectionAction.Share,
                    )
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.ShareMessage(
                            attachmentContentType = "image/jpeg",
                            attachmentContentUri = "content://media/image/1",
                            text = null,
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

    private fun TestScope.createHarness(
        actionRequirements: ActionRequirementsMock = createActionRequirementsMock(),
    ): DelegateHarness {
        val dispatcher = StandardTestDispatcher(scheduler = testScheduler)
        val scope = TestScope(dispatcher)
        val clipboardManager = mockk<ClipboardManager>(relaxed = true)
        val conversationAttachmentRepository =
            mockk<ConversationAttachmentRepository>(relaxed = true)
        val conversationMessagesDelegate = mockk<ConversationMessagesDelegate>()
        val createForwardedMessage = mockk<CreateForwardedMessage>()
        val conversationsRepository = mockk<ConversationsRepository>(relaxed = true)
        val messagesStateFlow = MutableStateFlow<ConversationMessagesUiState>(
            value = ConversationMessagesUiState.Loading,
        )
        val conversationIdFlow = MutableStateFlow<String?>("conversation-1")

        every { conversationMessagesDelegate.state } returns messagesStateFlow
        every {
            createForwardedMessage.invoke(any(), any())
        } returns null

        val delegate = ConversationMessageSelectionDelegateImpl(
            checkConversationActionRequirements = actionRequirements.mock,
            clipboardManager = clipboardManager,
            conversationAttachmentRepository = conversationAttachmentRepository,
            conversationMessagesDelegate = conversationMessagesDelegate,
            createForwardedMessage = createForwardedMessage,
            conversationsRepository = conversationsRepository,
            defaultDispatcher = dispatcher,
        )
        delegate.bind(
            scope = scope,
            conversationIdFlow = conversationIdFlow,
        )

        return DelegateHarness(
            delegate = delegate,
            clipboardManager = clipboardManager,
            conversationAttachmentRepository = conversationAttachmentRepository,
            conversationIdFlow = conversationIdFlow,
            conversationsRepository = conversationsRepository,
            createForwardedMessage = createForwardedMessage,
            messagesStateFlow = messagesStateFlow,
            scope = scope,
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

    private fun createAttachmentPart(
        contentType: String,
        contentUri: String,
    ): ConversationMessagePartUiModel.Attachment {
        return when {
            contentType.startsWith(prefix = "image/") -> {
                ConversationMessagePartUiModel.Attachment.Image(
                    text = null,
                    contentType = contentType,
                    contentUri = Uri.parse(contentUri),
                    width = 640,
                    height = 480,
                )
            }

            contentType.startsWith(prefix = "audio/") -> {
                ConversationMessagePartUiModel.Attachment.Audio(
                    text = null,
                    contentType = contentType,
                    contentUri = Uri.parse(contentUri),
                    width = 640,
                    height = 480,
                )
            }

            contentType.equals(other = "text/x-vCard", ignoreCase = true) -> {
                ConversationMessagePartUiModel.Attachment.VCard(
                    text = null,
                    contentType = contentType,
                    contentUri = Uri.parse(contentUri),
                    width = 640,
                    height = 480,
                    vCardUiModel = ConversationVCardAttachmentUiModel(
                        type = ConversationVCardAttachmentType.CONTACT,
                        titleText = "Sam Rivera",
                        subtitleText = "sam@example.com",
                    ),
                )
            }

            contentType.startsWith(prefix = "video/") -> {
                ConversationMessagePartUiModel.Attachment.Video(
                    text = null,
                    contentType = contentType,
                    contentUri = Uri.parse(contentUri),
                    width = 640,
                    height = 480,
                )
            }

            else -> {
                ConversationMessagePartUiModel.Attachment.File(
                    text = null,
                    contentType = contentType,
                    contentUri = Uri.parse(contentUri),
                    width = 640,
                    height = 480,
                )
            }
        }
    }

    private fun createMessageUiModel(
        messageId: String,
        text: String? = "Hello",
        parts: List<ConversationMessagePartUiModel> = emptyList(),
        canCopyMessageToClipboard: Boolean = false,
        canDownloadMessage: Boolean = false,
        canForwardMessage: Boolean = false,
        canResendMessage: Boolean = false,
        canSaveAttachments: Boolean = false,
    ): ConversationMessageUiModel {
        return ConversationMessageUiModel(
            messageId = messageId,
            conversationId = "conversation-1",
            text = text,
            parts = parts,
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
            canCopyMessageToClipboard = canCopyMessageToClipboard,
            canDownloadMessage = canDownloadMessage,
            canForwardMessage = canForwardMessage,
            canResendMessage = canResendMessage,
            canSaveAttachments = canSaveAttachments,
            mmsSubject = null,
            protocol = ConversationMessageUiModel.Protocol.SMS,
        )
    }

    private fun createMessagesUiState(
        vararg messages: ConversationMessageUiModel,
    ): ConversationMessagesUiState.Present {
        return ConversationMessagesUiState.Present(
            messages = messages.toList().toPersistentList(),
        )
    }

    private data class DelegateHarness(
        val delegate: ConversationMessageSelectionDelegateImpl,
        val clipboardManager: ClipboardManager,
        val conversationAttachmentRepository: ConversationAttachmentRepository,
        val conversationIdFlow: MutableStateFlow<String?>,
        val conversationsRepository: ConversationsRepository,
        val createForwardedMessage: CreateForwardedMessage,
        val messagesStateFlow: MutableStateFlow<ConversationMessagesUiState>,
        val scope: TestScope,
    ) {
        fun cancel() {
            scope.cancel()
        }
    }

    private data class ActionRequirementsMock(
        val mock: CheckConversationActionRequirements,
        var result: ConversationActionRequirementsResult,
    )
}
