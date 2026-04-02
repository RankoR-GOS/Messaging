package com.android.messaging.ui.conversation.v2.composer.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.composer.model.ConversationComposerAttachmentUiState
import com.android.messaging.ui.conversation.v2.mediapicker.component.ConversationMediaThumbnail
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.util.ContentType

private val ATTACHMENT_PREVIEW_CORNER_RADIUS = 20.dp
private const val ATTACHMENT_PREVIEW_SIZE_PX = 256

@Composable
internal fun ConversationAttachmentPreview(
    modifier: Modifier = Modifier,
    attachments: List<ConversationComposerAttachmentUiState>,
    onPendingAttachmentRemove: (String) -> Unit,
    onResolvedAttachmentClick: (ConversationComposerAttachmentUiState.Resolved) -> Unit,
    onResolvedAttachmentRemove: (String) -> Unit,
) {
    if (attachments.isEmpty()) {
        return
    }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 12.dp,
            top = 4.dp,
            end = 12.dp,
            bottom = 4.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = attachments,
            key = { attachment -> attachment.key },
        ) { attachment ->
            when (attachment) {
                is ConversationComposerAttachmentUiState.Pending -> {
                    PendingAttachmentPreviewItem(
                        onRemoveClick = {
                            onPendingAttachmentRemove(attachment.key)
                        },
                    )
                }

                is ConversationComposerAttachmentUiState.Resolved -> {
                    ResolvedAttachmentPreviewItem(
                        attachment = attachment,
                        onAttachmentClick = {
                            onResolvedAttachmentClick(attachment)
                        },
                        onRemoveClick = {
                            onResolvedAttachmentRemove(attachment.contentUri)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingAttachmentPreviewItem(
    onRemoveClick: () -> Unit,
) {
    AttachmentPreviewItemContainer(
        onClick = {},
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp,
            )
        }

        RemoveAttachmentButton(onClick = onRemoveClick)
    }
}

@Composable
private fun ResolvedAttachmentPreviewItem(
    attachment: ConversationComposerAttachmentUiState.Resolved,
    onAttachmentClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    val thumbnailSize = IntSize(
        width = ATTACHMENT_PREVIEW_SIZE_PX,
        height = ATTACHMENT_PREVIEW_SIZE_PX,
    )

    AttachmentPreviewItemContainer(
        onClick = onAttachmentClick,
    ) {
        ConversationMediaThumbnail(
            modifier = Modifier.fillMaxSize(),
            contentUri = attachment.contentUri,
            contentType = attachment.contentType,
            size = thumbnailSize,
        )

        if (ContentType.isVideoType(attachment.contentType)) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        RemoveAttachmentButton(onClick = onRemoveClick)
    }
}

@Composable
private fun AttachmentPreviewItemContainer(
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(ATTACHMENT_PREVIEW_CORNER_RADIUS))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(ATTACHMENT_PREVIEW_CORNER_RADIUS),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(content = content)
    }
}

@Composable
private fun BoxScope.RemoveAttachmentButton(onClick: () -> Unit) {
    FilledIconButton(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .size(28.dp),
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = pluralStringResource(
                id = R.plurals.attachment_preview_close_content_description,
                count = 1,
            ),
        )
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
private fun ConversationAttachmentPreviewPreview() {
    AppTheme {
        Surface {
            ConversationAttachmentPreview(
                attachments = listOf(
                    ConversationComposerAttachmentUiState.Pending(
                        key = "1",
                        contentType = "image/jpeg",
                        contentUri = "content://media/external/images/media/1",
                        displayName = "image.jpg",
                    ),
                    ConversationComposerAttachmentUiState.Resolved(
                        key = "2",
                        contentType = "image/jpeg",
                        contentUri = "content://media/external/images/media/2",
                        captionText = "",
                        width = 100,
                        height = 100,
                    ),
                    ConversationComposerAttachmentUiState.Resolved(
                        key = "3",
                        contentType = "video/mp4",
                        contentUri = "content://media/external/video/media/3",
                        captionText = "",
                        width = 100,
                        height = 100,
                    ),
                ),
                onPendingAttachmentRemove = {},
                onResolvedAttachmentClick = {},
                onResolvedAttachmentRemove = {},
            )
        }
    }
}
