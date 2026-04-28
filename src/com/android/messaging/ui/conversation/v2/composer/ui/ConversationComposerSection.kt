package com.android.messaging.ui.conversation.v2.composer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.ui.conversation.v2.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.v2.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun ConversationComposerSection(
    modifier: Modifier = Modifier,
    audioRecording: ConversationAudioRecordingUiState,
    attachments: ImmutableList<ComposerAttachmentUiModel>,
    messageText: String,
    sendProtocol: ConversationDraftSendProtocol,
    isMessageFieldEnabled: Boolean,
    isAttachmentActionEnabled: Boolean,
    isRecordActionEnabled: Boolean,
    isSendActionEnabled: Boolean,
    shouldShowRecordAction: Boolean,
    messageFieldFocusRequester: FocusRequester,
    onContactAttachClick: () -> Unit,
    onMediaPickerClick: () -> Unit,
    onMessageTextChange: (String) -> Unit,
    onPendingAttachmentRemove: (String) -> Unit,
    onResolvedAttachmentClick: (ComposerAttachmentUiModel.Resolved) -> Unit,
    onResolvedAttachmentRemove: (String) -> Unit,
    onAudioRecordingStartRequest: () -> Unit,
    onLockedAudioRecordingStartRequest: () -> Unit,
    onAudioRecordingFinish: () -> Unit,
    onAudioRecordingLock: () -> Boolean,
    onAudioRecordingCancel: () -> Unit,
    onSendClick: () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        ConversationAttachmentPreview(
            attachments = attachments,
            onPendingAttachmentRemove = onPendingAttachmentRemove,
            onResolvedAttachmentClick = onResolvedAttachmentClick,
            onResolvedAttachmentRemove = onResolvedAttachmentRemove,
        )

        ConversationComposeBar(
            audioRecording = audioRecording,
            messageText = messageText,
            sendProtocol = sendProtocol,
            isMessageFieldEnabled = isMessageFieldEnabled,
            isAttachmentActionEnabled = isAttachmentActionEnabled,
            isRecordActionEnabled = isRecordActionEnabled,
            isSendActionEnabled = isSendActionEnabled,
            shouldShowRecordAction = shouldShowRecordAction,
            messageFieldFocusRequester = messageFieldFocusRequester,
            onContactAttachClick = onContactAttachClick,
            onMediaPickerClick = onMediaPickerClick,
            onLockedAudioRecordingStartRequest = onLockedAudioRecordingStartRequest,
            onMessageTextChange = onMessageTextChange,
            onAudioRecordingStartRequest = onAudioRecordingStartRequest,
            onAudioRecordingFinish = onAudioRecordingFinish,
            onAudioRecordingLock = onAudioRecordingLock,
            onAudioRecordingCancel = onAudioRecordingCancel,
            onSendClick = onSendClick,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConversationComposerSectionEmptyPreview() {
    AppTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            ConversationComposerSection(
                audioRecording = ConversationAudioRecordingUiState(),
                attachments = persistentListOf(),
                messageText = "",
                sendProtocol = ConversationDraftSendProtocol.SMS,
                isMessageFieldEnabled = true,
                isAttachmentActionEnabled = true,
                isRecordActionEnabled = true,
                isSendActionEnabled = false,
                shouldShowRecordAction = true,
                messageFieldFocusRequester = remember { FocusRequester() },
                onContactAttachClick = {},
                onMediaPickerClick = {},
                onMessageTextChange = {},
                onPendingAttachmentRemove = {},
                onResolvedAttachmentClick = {},
                onResolvedAttachmentRemove = {},
                onAudioRecordingStartRequest = {},
                onLockedAudioRecordingStartRequest = {},
                onAudioRecordingFinish = {},
                onAudioRecordingLock = { false },
                onAudioRecordingCancel = {},
                onSendClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConversationComposerSectionWithAttachmentsPreview() {
    AppTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            ConversationComposerSection(
                audioRecording = ConversationAudioRecordingUiState(),
                attachments = persistentListOf(
                    ComposerAttachmentUiModel.Resolved.VisualMedia.Image(
                        key = "1",
                        contentType = "image/jpeg",
                        contentUri = "content://media/1",
                        captionText = "A beautiful sunset",
                        width = 100,
                        height = 100,
                    ),
                    ComposerAttachmentUiModel.Pending.Generic(
                        key = "2",
                        contentType = "video/mp4",
                        contentUri = "content://media/2",
                        displayName = "video.mp4",
                    ),
                ),
                messageText = "Check out these attachments!",
                sendProtocol = ConversationDraftSendProtocol.MMS,
                isMessageFieldEnabled = true,
                isAttachmentActionEnabled = true,
                isRecordActionEnabled = true,
                isSendActionEnabled = true,
                shouldShowRecordAction = false,
                messageFieldFocusRequester = remember { FocusRequester() },
                onContactAttachClick = {},
                onMediaPickerClick = {},
                onMessageTextChange = {},
                onPendingAttachmentRemove = {},
                onResolvedAttachmentClick = {},
                onResolvedAttachmentRemove = {},
                onAudioRecordingStartRequest = {},
                onLockedAudioRecordingStartRequest = {},
                onAudioRecordingFinish = {},
                onAudioRecordingLock = { false },
                onAudioRecordingCancel = {},
                onSendClick = {},
            )
        }
    }
}
