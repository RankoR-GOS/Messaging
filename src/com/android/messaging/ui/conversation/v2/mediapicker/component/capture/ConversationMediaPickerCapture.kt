package com.android.messaging.ui.conversation.v2.mediapicker.component.capture

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.SurfaceRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.mediapicker.ConversationCaptureMode
import com.android.messaging.ui.conversation.v2.mediapicker.camera.ConversationPhotoFlashMode
import com.android.messaging.ui.conversation.v2.mediapicker.component.PermissionFallback
import com.android.messaging.ui.core.AppTheme

@Composable
internal fun ConversationMediaCameraPreviewSurface(
    modifier: Modifier = Modifier,
    cameraPermissionGranted: Boolean,
    surfaceRequest: SurfaceRequest?,
    onRequestCameraPermission: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.scrim),
    ) {
        when {
            !cameraPermissionGranted -> {
                ConversationMediaCameraPermissionFallback(
                    onRequestCameraPermission = onRequestCameraPermission,
                )
            }

            surfaceRequest == null -> {
                ConversationMediaCameraLoadingState()
            }

            else -> {
                ConversationMediaCameraViewfinder(
                    surfaceRequest = surfaceRequest,
                )
            }
        }
    }
}

@Composable
private fun ConversationMediaCameraPermissionFallback(
    onRequestCameraPermission: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        PermissionFallback(
            icon = {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = null,
                )
            },
            message = stringResource(
                id = R.string.conversation_media_picker_camera_permission_message,
            ),
            actionLabel = stringResource(
                id = R.string.conversation_media_picker_allow_camera,
            ),
            onActionClick = onRequestCameraPermission,
        )
    }
}

@Composable
private fun ConversationMediaCameraLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ConversationMediaCameraViewfinder(
    surfaceRequest: SurfaceRequest,
) {
    CameraXViewfinder(
        modifier = Modifier
            .fillMaxSize(),
        surfaceRequest = surfaceRequest,
    )
}

@Composable
internal fun ConversationMediaCaptureContent(
    modifier: Modifier = Modifier,
    audioPermissionGranted: Boolean,
    captureMode: ConversationCaptureMode,
    hasFlashUnit: Boolean,
    isPhotoCaptureInProgress: Boolean,
    isRecording: Boolean,
    photoFlashMode: ConversationPhotoFlashMode,
    onCloseClick: () -> Unit,
    onRequestAudioPermission: () -> Unit,
    onPhotoCaptureClick: () -> Unit,
    onPhotoModeClick: () -> Unit,
    onSwitchCameraClick: () -> Unit,
    onToggleFlashClick: () -> Unit,
    onVideoCaptureClick: () -> Unit,
    onVideoModeClick: () -> Unit,
    recordingDurationMillis: Long,
) {
    Box(
        modifier = modifier,
    ) {
        ConversationMediaCaptureTopBar(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            captureMode = captureMode,
            hasFlashUnit = hasFlashUnit,
            isPhotoCaptureInProgress = isPhotoCaptureInProgress,
            isRecording = isRecording,
            photoFlashMode = photoFlashMode,
            onCloseClick = onCloseClick,
            onFlashClick = onToggleFlashClick,
        )

        ConversationMediaCaptureControls(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            captureMode = captureMode,
            isPhotoCaptureInProgress = isPhotoCaptureInProgress,
            isRecording = isRecording,
            recordingDurationMillis = recordingDurationMillis,
            onCaptureClick = {
                when (captureMode) {
                    ConversationCaptureMode.Video -> {
                        when {
                            !isRecording && !audioPermissionGranted -> {
                                onRequestAudioPermission()
                            }

                            else -> onVideoCaptureClick()
                        }
                    }

                    else -> onPhotoCaptureClick()
                }
            },
            onPhotoModeClick = onPhotoModeClick,
            onSwitchCameraClick = onSwitchCameraClick,
            onVideoModeClick = onVideoModeClick,
        )
    }
}

@Preview(name = "Camera Permission Fallback")
@Composable
private fun ConversationMediaCameraPermissionFallbackPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.scrim) {
            ConversationMediaCameraPermissionFallback(
                onRequestCameraPermission = {},
            )
        }
    }
}

@Preview(name = "Camera Loading State")
@Composable
private fun ConversationMediaCameraLoadingStatePreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.scrim) {
            ConversationMediaCameraLoadingState()
        }
    }
}

@Preview(name = "Capture Content - Photo")
@Composable
private fun ConversationMediaCaptureContentPhotoPreview() {
    AppTheme {
        Surface(color = Color.Black) {
            ConversationMediaCaptureContent(
                modifier = Modifier.fillMaxSize(),
                audioPermissionGranted = true,
                captureMode = ConversationCaptureMode.Photo,
                hasFlashUnit = true,
                isPhotoCaptureInProgress = false,
                isRecording = false,
                photoFlashMode = ConversationPhotoFlashMode.Auto,
                onCloseClick = {},
                onRequestAudioPermission = {},
                onPhotoCaptureClick = {},
                onPhotoModeClick = {},
                onSwitchCameraClick = {},
                onToggleFlashClick = {},
                onVideoCaptureClick = {},
                onVideoModeClick = {},
                recordingDurationMillis = 0L,
            )
        }
    }
}

@Preview(name = "Capture Content - Video Idle")
@Composable
private fun ConversationMediaCaptureContentVideoIdlePreview() {
    AppTheme {
        Surface(color = Color.Black) {
            ConversationMediaCaptureContent(
                modifier = Modifier.fillMaxSize(),
                audioPermissionGranted = true,
                captureMode = ConversationCaptureMode.Video,
                hasFlashUnit = true,
                isPhotoCaptureInProgress = false,
                isRecording = false,
                photoFlashMode = ConversationPhotoFlashMode.Auto,
                onCloseClick = {},
                onRequestAudioPermission = {},
                onPhotoCaptureClick = {},
                onPhotoModeClick = {},
                onSwitchCameraClick = {},
                onToggleFlashClick = {},
                onVideoCaptureClick = {},
                onVideoModeClick = {},
                recordingDurationMillis = 0L,
            )
        }
    }
}

@Preview(name = "Capture Content - Video Recording")
@Composable
private fun ConversationMediaCaptureContentVideoRecordingPreview() {
    AppTheme {
        Surface(color = Color.Black) {
            ConversationMediaCaptureContent(
                modifier = Modifier.fillMaxSize(),
                audioPermissionGranted = true,
                captureMode = ConversationCaptureMode.Video,
                hasFlashUnit = true,
                isPhotoCaptureInProgress = false,
                isRecording = true,
                photoFlashMode = ConversationPhotoFlashMode.Auto,
                onCloseClick = {},
                onRequestAudioPermission = {},
                onPhotoCaptureClick = {},
                onPhotoModeClick = {},
                onSwitchCameraClick = {},
                onToggleFlashClick = {},
                onVideoCaptureClick = {},
                onVideoModeClick = {},
                recordingDurationMillis = 65000L,
            )
        }
    }
}
