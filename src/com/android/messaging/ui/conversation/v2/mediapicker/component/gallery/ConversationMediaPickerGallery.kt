package com.android.messaging.ui.conversation.v2.mediapicker.component.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.media.model.ConversationMediaItem
import com.android.messaging.data.media.model.ConversationMediaType
import com.android.messaging.ui.conversation.v2.mediapicker.component.ConversationMediaThumbnail
import com.android.messaging.ui.conversation.v2.mediapicker.component.PermissionFallback
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationMediaPickerUiState
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.persistentListOf

private val GALLERY_GRID_SPACING = 8.dp
private val GALLERY_ITEM_CORNER_RADIUS = 20.dp
private const val GALLERY_ITEM_SIZE_PX = 384

@Composable
internal fun ConversationGallerySheet(
    uiState: ConversationMediaPickerUiState,
    galleryPermissionGranted: Boolean,
    onMediaClick: (ConversationMediaItem) -> Unit,
    onRequestGalleryPermission: () -> Unit,
) {
    LazyVerticalGrid(
        modifier = Modifier.navigationBarsPadding(),
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = 20.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(GALLERY_GRID_SPACING),
        verticalArrangement = Arrangement.spacedBy(GALLERY_GRID_SPACING),
    ) {
        item(
            span = {
                GridItemSpan(maxLineSpan)
            },
        ) {
            GallerySheetDragHandle()
        }

        when {
            !galleryPermissionGranted -> {
                galleryPermissionItem(
                    onRequestGalleryPermission = onRequestGalleryPermission,
                )
            }

            uiState.isLoadingGallery -> {
                galleryLoadingItem()
            }

            else -> {
                galleryItems(
                    items = uiState.galleryItems,
                    onMediaClick = onMediaClick,
                )
            }
        }
    }
}

private fun LazyGridScope.galleryPermissionItem(
    onRequestGalleryPermission: () -> Unit,
) {
    item(
        span = {
            GridItemSpan(maxLineSpan)
        },
    ) {
        PermissionFallback(
            icon = {
                Icon(
                    imageVector = Icons.Rounded.PhotoLibrary,
                    contentDescription = null,
                )
            },
            message = stringResource(
                id = R.string.conversation_media_picker_gallery_permission_message,
            ),
            actionLabel = stringResource(
                id = R.string.conversation_media_picker_allow_gallery,
            ),
            onActionClick = onRequestGalleryPermission,
        )
    }
}

private fun LazyGridScope.galleryLoadingItem() {
    item(
        span = {
            GridItemSpan(maxLineSpan)
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

private fun LazyGridScope.galleryItems(
    items: List<ConversationMediaItem>,
    onMediaClick: (ConversationMediaItem) -> Unit,
) {
    items(
        items = items,
        key = { item -> item.mediaId },
    ) { item ->
        GalleryGridItem(
            item = item,
            onClick = {
                onMediaClick(item)
            },
        )
    }
}

@Composable
private fun GallerySheetDragHandle(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = 32.dp,
                    height = 4.dp,
                )
                .clip(CircleShape)
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                ),
        )
    }
}

@Composable
private fun GalleryGridItem(
    item: ConversationMediaItem,
    onClick: () -> Unit,
) {
    val thumbnailSize = IntSize(
        width = GALLERY_ITEM_SIZE_PX,
        height = GALLERY_ITEM_SIZE_PX,
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(GALLERY_ITEM_CORNER_RADIUS))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(GALLERY_ITEM_CORNER_RADIUS),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Box {
            ConversationMediaThumbnail(
                modifier = Modifier.fillMaxSize(),
                contentUri = item.contentUri,
                contentType = item.contentType,
                size = thumbnailSize,
            )

            if (item.mediaType == ConversationMediaType.Video) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Preview(name = "Gallery Permission Required", showBackground = true)
@Composable
private fun ConversationGallerySheetPermissionPreview() {
    AppTheme {
        ConversationGallerySheet(
            uiState = ConversationMediaPickerUiState(),
            galleryPermissionGranted = false,
            onMediaClick = {},
            onRequestGalleryPermission = {},
        )
    }
}

@Preview(name = "Gallery Loading", showBackground = true)
@Composable
private fun ConversationGallerySheetLoadingPreview() {
    AppTheme {
        ConversationGallerySheet(
            uiState = ConversationMediaPickerUiState(isLoadingGallery = true),
            galleryPermissionGranted = true,
            onMediaClick = {},
            onRequestGalleryPermission = {},
        )
    }
}

@Preview(name = "Gallery Items", showBackground = true)
@Composable
private fun ConversationGallerySheetItemsPreview() {
    val items = persistentListOf(
        previewMediaItem("1", ConversationMediaType.Image),
        previewMediaItem("2", ConversationMediaType.Video),
        previewMediaItem("3", ConversationMediaType.Image),
        previewMediaItem("4", ConversationMediaType.Image),
        previewMediaItem("5", ConversationMediaType.Video),
    )
    AppTheme {
        ConversationGallerySheet(
            uiState = ConversationMediaPickerUiState(galleryItems = items),
            galleryPermissionGranted = true,
            onMediaClick = {},
            onRequestGalleryPermission = {},
        )
    }
}

private fun previewMediaItem(
    id: String,
    type: ConversationMediaType,
): ConversationMediaItem {
    return ConversationMediaItem(
        mediaId = id,
        contentUri = "content://media/external/images/media/$id",
        contentType = if (type == ConversationMediaType.Image) "image/jpeg" else "video/mp4",
        mediaType = type,
        width = 1080,
        height = 1920,
        durationMillis = if (type == ConversationMediaType.Video) 30000L else null,
    )
}
