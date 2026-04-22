package com.android.messaging.ui.conversation.v2.mediapicker

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.messaging.data.media.model.ConversationMediaItem
import com.android.messaging.ui.conversation.v2.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.v2.mediapicker.camera.BindConversationCameraLifecycleEffect
import com.android.messaging.ui.conversation.v2.mediapicker.camera.rememberConversationCameraController
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationCapturedMedia
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationMediaPickerUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationMediaPicker(
    modifier: Modifier = Modifier,
    uiState: ConversationMediaPickerUiState,
    attachments: ImmutableList<ComposerAttachmentUiModel>,
    conversationTitle: String?,
    isSendActionEnabled: Boolean,
    state: ConversationMediaPickerState,
    cameraPermissionGranted: Boolean,
    audioPermissionGranted: Boolean,
    galleryPermissionGranted: Boolean,
    onClose: () -> Unit,
    onAttachmentPreviewClick: (ComposerAttachmentUiModel.Resolved.VisualMedia) -> Unit,
    onAttachmentCaptionChange: (String, String) -> Unit,
    onAttachmentRemove: (String) -> Unit,
    onGalleryMediaConfirmed: (List<ConversationMediaItem>) -> Unit,
    onRequestAudioPermission: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRequestGalleryPermission: () -> Unit,
    onCapturedMediaReady: (ConversationCapturedMedia) -> Unit,
    onSendClick: () -> Unit,
) {
    val cameraController = rememberConversationCameraController()
    val lifecycleOwner = LocalLifecycleOwner.current

    val visualAttachments = remember(attachments) {
        attachments
            .asSequence()
            .filterIsInstance<ComposerAttachmentUiModel.Resolved.VisualMedia>()
            .toImmutableList()
    }

    val isReviewVisible = state.isReviewRequested && visualAttachments.isNotEmpty()
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true,
    )

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState,
    )

    var pendingSelectedMediaItem by remember {
        mutableStateOf<ConversationMediaItem?>(value = null)
    }

    HandlePendingGallerySelectionEffect(
        pendingSelectedMediaItem = pendingSelectedMediaItem,
        sheetState = sheetState,
        onGalleryMediaConfirmed = onGalleryMediaConfirmed,
        onShowReview = state::showReview,
        onSelectionHandled = {
            pendingSelectedMediaItem = null
        },
    )
    BindConversationCameraLifecycleEffect(
        cameraController = cameraController,
        cameraPermissionGranted = cameraPermissionGranted,
        isCameraPreviewVisible = !isReviewVisible,
        lifecycleOwner = lifecycleOwner,
    )

    ConversationMediaPickerScaffold(
        modifier = modifier,
        cameraController = cameraController,
        scaffoldState = scaffoldState,
        uiState = uiState,
        visualAttachments = visualAttachments,
        conversationTitle = conversationTitle,
        captureMode = state.captureMode,
        reviewContentUri = state.reviewContentUri,
        reviewRequestSequence = state.reviewRequestSequence,
        isReviewVisible = isReviewVisible,
        isSendActionEnabled = isSendActionEnabled,
        cameraPermissionGranted = cameraPermissionGranted,
        audioPermissionGranted = audioPermissionGranted,
        galleryPermissionGranted = galleryPermissionGranted,
        onClose = onClose,
        onAttachmentPreviewClick = onAttachmentPreviewClick,
        onAttachmentCaptionChange = onAttachmentCaptionChange,
        onAttachmentRemove = onAttachmentRemove,
        onGalleryMediaClick = { mediaItem ->
            pendingSelectedMediaItem = mediaItem
        },
        onRequestAudioPermission = onRequestAudioPermission,
        onRequestCameraPermission = onRequestCameraPermission,
        onRequestGalleryPermission = onRequestGalleryPermission,
        onCapturedMediaReady = onCapturedMediaReady,
        onSendClick = onSendClick,
        onShowReview = state::showReview,
        onClearReview = state::clearReview,
        onCaptureModeChange = state::updateCaptureMode,
    )
}
