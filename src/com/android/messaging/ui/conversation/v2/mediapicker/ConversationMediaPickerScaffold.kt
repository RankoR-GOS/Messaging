package com.android.messaging.ui.conversation.v2.mediapicker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.messaging.data.media.model.ConversationMediaItem
import com.android.messaging.ui.conversation.v2.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.v2.mediapicker.camera.ConversationCameraController
import com.android.messaging.ui.conversation.v2.mediapicker.component.capture.ConversationMediaCameraPreviewSurface
import com.android.messaging.ui.conversation.v2.mediapicker.component.gallery.ConversationGallerySheet
import com.android.messaging.ui.conversation.v2.mediapicker.component.review.ConversationMediaReviewScene
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationCapturedMedia
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationMediaPickerUiState
import kotlinx.collections.immutable.ImmutableList

private const val CAMERA_PREVIEW_HEIGHT_FRACTION = 2f / 3f
private val PICKER_GALLERY_SHEET_HEIGHT_REDUCTION = 16.dp

private enum class ConversationMediaPickerOverlayMode {
    Capture,
    Review,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationMediaPickerScaffold(
    modifier: Modifier = Modifier,
    cameraController: ConversationCameraController,
    scaffoldState: BottomSheetScaffoldState,
    uiState: ConversationMediaPickerUiState,
    visualAttachments: ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia>,
    conversationTitle: String?,
    captureMode: ConversationCaptureMode,
    reviewContentUri: String?,
    reviewRequestSequence: Int,
    isReviewVisible: Boolean,
    isSendActionEnabled: Boolean,
    cameraPermissionGranted: Boolean,
    audioPermissionGranted: Boolean,
    galleryPermissionGranted: Boolean,
    onClose: () -> Unit,
    onAttachmentPreviewClick: (ComposerAttachmentUiModel.Resolved.VisualMedia) -> Unit,
    onAttachmentCaptionChange: (String, String) -> Unit,
    onAttachmentRemove: (String) -> Unit,
    onGalleryMediaClick: (ConversationMediaItem) -> Unit,
    onRequestAudioPermission: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRequestGalleryPermission: () -> Unit,
    onCapturedMediaReady: (ConversationCapturedMedia) -> Unit,
    onSendClick: () -> Unit,
    onShowReview: (String) -> Unit,
    onClearReview: () -> Unit,
    onCaptureModeChange: (ConversationCaptureMode) -> Unit,
) {
    val overlayMode = when {
        isReviewVisible -> ConversationMediaPickerOverlayMode.Review
        else -> ConversationMediaPickerOverlayMode.Capture
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize(),
    ) {
        val previewHeight = maxHeight * CAMERA_PREVIEW_HEIGHT_FRACTION
        val defaultSheetPeekHeight = maxHeight - previewHeight

        val sheetPeekHeight = when {
            defaultSheetPeekHeight > PICKER_GALLERY_SHEET_HEIGHT_REDUCTION -> {
                defaultSheetPeekHeight - PICKER_GALLERY_SHEET_HEIGHT_REDUCTION
            }

            else -> defaultSheetPeekHeight
        }

        AnimatedContent(
            modifier = Modifier
                .fillMaxSize(),
            targetState = overlayMode,
            transitionSpec = {
                pickerOverlayTransition()
            },
            label = "pickerOverlayMode",
        ) { currentOverlayMode ->
            when (currentOverlayMode) {
                ConversationMediaPickerOverlayMode.Capture -> {
                    ConversationMediaPickerCaptureScene(
                        cameraController = cameraController,
                        scaffoldState = scaffoldState,
                        cameraPermissionGranted = cameraPermissionGranted,
                        onRequestCameraPermission = onRequestCameraPermission,
                        uiState = uiState,
                        galleryPermissionGranted = galleryPermissionGranted,
                        onGalleryMediaClick = onGalleryMediaClick,
                        onRequestGalleryPermission = onRequestGalleryPermission,
                        sheetPeekHeight = sheetPeekHeight,
                        audioPermissionGranted = audioPermissionGranted,
                        captureMode = captureMode,
                        onClose = onClose,
                        onRequestAudioPermission = onRequestAudioPermission,
                        onShowReview = onShowReview,
                        onCapturedMediaReady = onCapturedMediaReady,
                        onCaptureModeChange = onCaptureModeChange,
                    )
                }

                ConversationMediaPickerOverlayMode.Review -> {
                    ConversationMediaPickerReviewScene(
                        scaffoldState = scaffoldState,
                        uiState = uiState,
                        galleryPermissionGranted = galleryPermissionGranted,
                        onGalleryMediaClick = onGalleryMediaClick,
                        onRequestGalleryPermission = onRequestGalleryPermission,
                        sheetPeekHeight = sheetPeekHeight,
                        attachments = visualAttachments,
                        conversationTitle = conversationTitle,
                        initiallyReviewedContentUri = reviewContentUri,
                        reviewRequestSequence = reviewRequestSequence,
                        isSendActionEnabled = isSendActionEnabled,
                        onAttachmentPreviewClick = onAttachmentPreviewClick,
                        onCaptionChange = onAttachmentCaptionChange,
                        onAttachmentRemove = onAttachmentRemove,
                        onAddMoreClick = onClearReview,
                        onClearReview = onClearReview,
                        onCloseClick = onClose,
                        onSendClick = {
                            onSendClick()
                            onClose()
                        },
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ConversationMediaPickerReviewScene(
    scaffoldState: BottomSheetScaffoldState,
    uiState: ConversationMediaPickerUiState,
    galleryPermissionGranted: Boolean,
    onGalleryMediaClick: (ConversationMediaItem) -> Unit,
    onRequestGalleryPermission: () -> Unit,
    sheetPeekHeight: Dp,
    attachments: ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia>,
    conversationTitle: String?,
    initiallyReviewedContentUri: String?,
    reviewRequestSequence: Int,
    isSendActionEnabled: Boolean,
    onAttachmentPreviewClick: (ComposerAttachmentUiModel.Resolved.VisualMedia) -> Unit,
    onCaptionChange: (String, String) -> Unit,
    onAttachmentRemove: (String) -> Unit,
    onAddMoreClick: () -> Unit,
    onClearReview: () -> Unit,
    onCloseClick: () -> Unit,
    onSendClick: () -> Unit,
) {
    BottomSheetScaffold(
        modifier = Modifier
            .fillMaxSize(),
        scaffoldState = scaffoldState,
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.98f),
        sheetContentColor = MaterialTheme.colorScheme.onSurface,
        sheetShape = RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 28.dp,
        ),
        containerColor = Color.Transparent,
        sheetDragHandle = null,
        sheetPeekHeight = sheetPeekHeight,
        sheetContent = {
            ConversationGallerySheet(
                uiState = uiState,
                galleryPermissionGranted = galleryPermissionGranted,
                onMediaClick = onGalleryMediaClick,
                onRequestGalleryPermission = onRequestGalleryPermission,
            )
        },
    ) { innerPadding ->
        ConversationMediaReviewScene(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
            attachments = attachments,
            conversationTitle = conversationTitle,
            initiallyReviewedContentUri = initiallyReviewedContentUri,
            reviewRequestSequence = reviewRequestSequence,
            isSendActionEnabled = isSendActionEnabled,
            onAttachmentPreviewClick = onAttachmentPreviewClick,
            onCaptionChange = onCaptionChange,
            onAttachmentRemove = onAttachmentRemove,
            onAddMoreClick = onAddMoreClick,
            onClearReview = onClearReview,
            onCloseClick = onCloseClick,
            onSendClick = onSendClick,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ConversationMediaPickerCaptureScene(
    cameraController: ConversationCameraController,
    scaffoldState: BottomSheetScaffoldState,
    cameraPermissionGranted: Boolean,
    onRequestCameraPermission: () -> Unit,
    uiState: ConversationMediaPickerUiState,
    galleryPermissionGranted: Boolean,
    onGalleryMediaClick: (ConversationMediaItem) -> Unit,
    onRequestGalleryPermission: () -> Unit,
    sheetPeekHeight: Dp,
    audioPermissionGranted: Boolean,
    captureMode: ConversationCaptureMode,
    onClose: () -> Unit,
    onRequestAudioPermission: () -> Unit,
    onShowReview: (String) -> Unit,
    onCapturedMediaReady: (ConversationCapturedMedia) -> Unit,
    onCaptureModeChange: (ConversationCaptureMode) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        ConversationMediaCameraPreviewRoute(
            modifier = Modifier
                .fillMaxSize(),
            cameraController = cameraController,
            cameraPermissionGranted = cameraPermissionGranted,
            onRequestCameraPermission = onRequestCameraPermission,
        )

        BottomSheetScaffold(
            modifier = Modifier
                .fillMaxSize(),
            scaffoldState = scaffoldState,
            sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            sheetContentColor = MaterialTheme.colorScheme.onSurface,
            sheetShape = RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp,
            ),
            containerColor = Color.Transparent,
            sheetDragHandle = null,
            sheetPeekHeight = sheetPeekHeight,
            sheetContent = {
                ConversationGallerySheet(
                    uiState = uiState,
                    galleryPermissionGranted = galleryPermissionGranted,
                    onMediaClick = onGalleryMediaClick,
                    onRequestGalleryPermission = onRequestGalleryPermission,
                )
            },
        ) { innerPadding ->
            ConversationMediaCaptureRoute(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues = innerPadding),
                cameraController = cameraController,
                audioPermissionGranted = audioPermissionGranted,
                captureMode = captureMode,
                onClose = onClose,
                onRequestAudioPermission = onRequestAudioPermission,
                onShowReview = onShowReview,
                onCapturedMediaReady = onCapturedMediaReady,
                onCaptureModeChange = onCaptureModeChange,
            )
        }
    }
}

@Composable
private fun ConversationMediaCameraPreviewRoute(
    modifier: Modifier = Modifier,
    cameraController: ConversationCameraController,
    cameraPermissionGranted: Boolean,
    onRequestCameraPermission: () -> Unit,
) {
    val surfaceRequest = cameraController.surfaceRequest.collectAsStateWithLifecycle()

    ConversationMediaCameraPreviewSurface(
        modifier = modifier,
        cameraPermissionGranted = cameraPermissionGranted,
        surfaceRequest = surfaceRequest.value,
        onRequestCameraPermission = onRequestCameraPermission,
    )
}

private fun pickerOverlayTransition(): ContentTransform {
    return (
        fadeIn(
            animationSpec = tween(
                durationMillis = 180,
                delayMillis = 40,
            ),
        ) + scaleIn(
            initialScale = 0.98f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
        ).togetherWith(
        fadeOut(
            animationSpec = tween(durationMillis = 100),
        ) + scaleOut(
            targetScale = 0.985f,
            animationSpec = tween(durationMillis = 100),
        ),
    )
}
