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
import com.android.messaging.ui.conversation.v2.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun ConversationComposerSection(
    modifier: Modifier = Modifier,
    attachments: ImmutableList<ComposerAttachmentUiModel>,
    messageText: String,
    isMessageFieldEnabled: Boolean,
    isAttachmentActionEnabled: Boolean,
    isSendActionEnabled: Boolean,
    messageFieldFocusRequester: FocusRequester,
    onContactAttachClick: () -> Unit,
    onMediaPickerClick: () -> Unit,
    onMessageTextChange: (String) -> Unit,
    onPendingAttachmentRemove: (String) -> Unit,
    onResolvedAttachmentClick: (ComposerAttachmentUiModel.Resolved) -> Unit,
    onResolvedAttachmentRemove: (String) -> Unit,
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
            messageText = messageText,
            isMessageFieldEnabled = isMessageFieldEnabled,
            isAttachmentActionEnabled = isAttachmentActionEnabled,
            isSendActionEnabled = isSendActionEnabled,
            messageFieldFocusRequester = messageFieldFocusRequester,
            onContactAttachClick = onContactAttachClick,
            onMediaPickerClick = onMediaPickerClick,
            onMessageTextChange = onMessageTextChange,
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
                attachments = persistentListOf(),
                messageText = "",
                isMessageFieldEnabled = true,
                isAttachmentActionEnabled = true,
                isSendActionEnabled = false,
                messageFieldFocusRequester = remember { FocusRequester() },
                onContactAttachClick = {},
                onMediaPickerClick = {},
                onMessageTextChange = {},
                onPendingAttachmentRemove = {},
                onResolvedAttachmentClick = {},
                onResolvedAttachmentRemove = {},
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
                attachments = persistentListOf(
                    ComposerAttachmentUiModel.Resolved.VisualMedia.Image(
                        key = "1",
                        contentType = "image/jpeg",
                        contentUri = "content://media/1",
                        captionText = "A beautiful sunset",
                        width = 100,
                        height = 100,
                    ),
                    ComposerAttachmentUiModel.Pending(
                        key = "2",
                        contentType = "video/mp4",
                        contentUri = "content://media/2",
                        displayName = "video.mp4",
                    ),
                ),
                messageText = "Check out these attachments!",
                isMessageFieldEnabled = true,
                isAttachmentActionEnabled = true,
                isSendActionEnabled = true,
                messageFieldFocusRequester = remember { FocusRequester() },
                onContactAttachClick = {},
                onMediaPickerClick = {},
                onMessageTextChange = {},
                onPendingAttachmentRemove = {},
                onResolvedAttachmentClick = {},
                onResolvedAttachmentRemove = {},
                onSendClick = {},
            )
        }
    }
}
