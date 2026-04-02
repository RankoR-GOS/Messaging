package com.android.messaging.ui.conversation.v2.mediapicker.component.capture

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.v2.mediapicker.ConversationCaptureMode
import com.android.messaging.ui.conversation.v2.mediapicker.component.capture.ConversationMediaCaptureShutterPhase.Photo
import com.android.messaging.ui.conversation.v2.mediapicker.component.capture.ConversationMediaCaptureShutterPhase.VideoIdle
import com.android.messaging.ui.conversation.v2.mediapicker.component.capture.ConversationMediaCaptureShutterPhase.VideoRecording
import com.android.messaging.ui.core.AppTheme

private val PICKER_SHUTTER_BORDER_WIDTH = 3.dp
private val PICKER_SHUTTER_OUTER_SIZE = 78.dp
private val PICKER_SHUTTER_PHOTO_INNER_SIZE = 62.dp
private val PICKER_SHUTTER_FULL_INNER_SIZE = PICKER_SHUTTER_OUTER_SIZE -
    (PICKER_SHUTTER_BORDER_WIDTH * 2)
private const val PICKER_SHUTTER_STATE_TRANSITION_SPRING_DAMPING_RATIO = 0.7f
private const val PICKER_SHUTTER_STATE_TRANSITION_SPRING_STIFFNESS = 500f
private val PICKER_SHUTTER_COLOR_ANIMATION_SPEC = tween<Color>(durationMillis = 180)
private val PICKER_SHUTTER_FLOAT_SPRING_ANIMATION_SPEC = spring<Float>(
    dampingRatio = PICKER_SHUTTER_STATE_TRANSITION_SPRING_DAMPING_RATIO,
    stiffness = PICKER_SHUTTER_STATE_TRANSITION_SPRING_STIFFNESS,
)

private enum class ConversationMediaCaptureShutterPhase {
    Photo,
    VideoIdle,
    VideoRecording,
}

@Composable
internal fun ConversationMediaCaptureShutterButton(
    captureMode: ConversationCaptureMode,
    isPhotoCaptureInProgress: Boolean,
    isRecording: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isEnabled = captureMode != ConversationCaptureMode.Photo || !isPhotoCaptureInProgress
    val shutterPhase = resolveConversationMediaCaptureShutterPhase(
        captureMode = captureMode,
        isRecording = isRecording,
    )
    ConversationMediaCaptureShutterButtonAnimatedContent(
        colorScheme = colorScheme,
        isEnabled = isEnabled,
        onClick = onClick,
        shutterPhase = shutterPhase,
    )
}

@Composable
private fun ConversationMediaCaptureShutterButtonAnimatedContent(
    colorScheme: ColorScheme,
    isEnabled: Boolean,
    onClick: () -> Unit,
    shutterPhase: ConversationMediaCaptureShutterPhase,
) {
    val transition = updateTransition(
        targetState = shutterPhase,
        label = "picker_shutter_phase",
    )
    val outerContainerColor by transition.animateOuterContainerColor(colorScheme)
    val innerShutterColor by transition.animateInnerShutterColor(colorScheme)
    val innerShutterSize by transition.animateInnerShutterSize()
    val outerScale by transition.animateOuterScale()
    val videoCenterDotAlpha by transition.animateVideoCenterDotAlpha()
    val videoCenterDotScale by transition.animateVideoCenterDotScale()
    val recordingStopAlpha by transition.animateRecordingStopAlpha()
    val recordingStopScale by transition.animateRecordingStopScale()

    ConversationMediaCaptureShutterButtonShell(
        borderColor = colorScheme.inverseOnSurface,
        isEnabled = isEnabled,
        onClick = onClick,
        outerContainerColor = outerContainerColor,
        outerScale = outerScale,
    ) {
        ConversationMediaCaptureShutterInnerDisc(
            innerShutterColor = innerShutterColor,
            innerShutterSize = innerShutterSize,
        ) {
            if (shutterPhase != Photo) {
                ConversationMediaCaptureVideoOverlay(
                    recordingStopAlpha = recordingStopAlpha,
                    recordingStopBackgroundColor = colorScheme.error.copy(alpha = 0.3f),
                    recordingStopScale = recordingStopScale,
                    videoCenterDotAlpha = videoCenterDotAlpha,
                    videoCenterDotColor = colorScheme.inverseOnSurface,
                    videoCenterDotScale = videoCenterDotScale,
                )
            }
        }
    }
}

@Composable
private fun Transition<ConversationMediaCaptureShutterPhase>.animateInnerShutterColor(
    colorScheme: ColorScheme,
): State<Color> {
    return animateColor(
        transitionSpec = {
            PICKER_SHUTTER_COLOR_ANIMATION_SPEC
        },
        label = "picker_shutter_inner_color",
        targetValueByState = { phase ->
            phase.resolveInnerShutterColor(
                colorScheme = colorScheme,
            )
        },
    )
}

@Composable
private fun Transition<ConversationMediaCaptureShutterPhase>.animateInnerShutterSize(): State<Dp> {
    return animateDp(
        transitionSpec = {
            spring(
                dampingRatio = PICKER_SHUTTER_STATE_TRANSITION_SPRING_DAMPING_RATIO,
                stiffness = PICKER_SHUTTER_STATE_TRANSITION_SPRING_STIFFNESS,
            )
        },
        label = "picker_shutter_inner_size",
        targetValueByState = { phase ->
            phase.resolveInnerShutterSize()
        },
    )
}

@Composable
private fun Transition<ConversationMediaCaptureShutterPhase>.animateOuterContainerColor(
    colorScheme: ColorScheme,
): State<Color> {
    return animateColor(
        transitionSpec = {
            PICKER_SHUTTER_COLOR_ANIMATION_SPEC
        },
        label = "picker_shutter_outer_color",
        targetValueByState = { phase ->
            phase.resolveOuterContainerColor(
                colorScheme = colorScheme,
            )
        },
    )
}

@Composable
private fun Transition<ConversationMediaCaptureShutterPhase>.animateOuterScale(): State<Float> {
    return animateFloat(
        transitionSpec = {
            PICKER_SHUTTER_FLOAT_SPRING_ANIMATION_SPEC
        },
        label = "picker_shutter_outer_scale",
        targetValueByState = { phase ->
            phase.resolveOuterScale()
        },
    )
}

@Composable
private fun Transition<ConversationMediaCaptureShutterPhase>.animateRecordingStopAlpha():
    State<Float> {
    return animateFloat(
        transitionSpec = {
            tween(durationMillis = 130)
        },
        label = "picker_shutter_recording_stop_alpha",
        targetValueByState = { phase ->
            phase.resolveRecordingStopAlpha()
        },
    )
}

@Composable
private fun Transition<ConversationMediaCaptureShutterPhase>.animateRecordingStopScale():
    State<Float> {
    return animateFloat(
        transitionSpec = {
            PICKER_SHUTTER_FLOAT_SPRING_ANIMATION_SPEC
        },
        label = "picker_shutter_recording_stop_scale",
        targetValueByState = { phase ->
            phase.resolveRecordingStopScale()
        },
    )
}

@Composable
private fun Transition<ConversationMediaCaptureShutterPhase>.animateVideoCenterDotAlpha():
    State<Float> {
    return animateFloat(
        transitionSpec = {
            tween(durationMillis = 110)
        },
        label = "picker_shutter_video_center_dot_alpha",
        targetValueByState = { phase ->
            phase.resolveVideoCenterDotAlpha()
        },
    )
}

@Composable
private fun Transition<ConversationMediaCaptureShutterPhase>.animateVideoCenterDotScale():
    State<Float> {
    return animateFloat(
        transitionSpec = {
            PICKER_SHUTTER_FLOAT_SPRING_ANIMATION_SPEC
        },
        label = "picker_shutter_video_center_dot_scale",
        targetValueByState = { phase ->
            phase.resolveVideoCenterDotScale()
        },
    )
}

@Composable
private fun ConversationMediaCaptureShutterButtonShell(
    borderColor: Color,
    isEnabled: Boolean,
    onClick: () -> Unit,
    outerContainerColor: Color,
    outerScale: Float,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(PICKER_SHUTTER_OUTER_SIZE)
            .graphicsLayer {
                alpha = if (isEnabled) 1f else 0.7f
                scaleX = outerScale
                scaleY = outerScale
            },
        enabled = isEnabled,
        onClick = onClick,
        shape = CircleShape,
        color = outerContainerColor,
        border = BorderStroke(
            width = PICKER_SHUTTER_BORDER_WIDTH,
            color = borderColor,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun ConversationMediaCaptureShutterInnerDisc(
    innerShutterColor: Color,
    innerShutterSize: Dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.size(innerShutterSize),
        shape = CircleShape,
        color = innerShutterColor,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

@Composable
private fun ConversationMediaCaptureVideoOverlay(
    recordingStopAlpha: Float,
    recordingStopBackgroundColor: Color,
    recordingStopScale: Float,
    videoCenterDotAlpha: Float,
    videoCenterDotColor: Color,
    videoCenterDotScale: Float,
) {
    ConversationMediaCaptureRecordingStopGlyph(
        alpha = recordingStopAlpha,
        backgroundColor = recordingStopBackgroundColor,
        scale = recordingStopScale,
    )

    ConversationMediaCaptureVideoIdleDotGlyph(
        alpha = videoCenterDotAlpha,
        color = videoCenterDotColor,
        scale = videoCenterDotScale,
    )
}

@Composable
private fun ConversationMediaCaptureRecordingStopGlyph(
    alpha: Float,
    backgroundColor: Color,
    scale: Float,
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(size = 10.dp),
            ),
    )
}

@Composable
private fun ConversationMediaCaptureVideoIdleDotGlyph(
    alpha: Float,
    color: Color,
    scale: Float,
) {
    Surface(
        modifier = Modifier
            .size(16.dp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            },
        shape = CircleShape,
        color = color,
    ) {}
}

private fun resolveConversationMediaCaptureShutterPhase(
    captureMode: ConversationCaptureMode,
    isRecording: Boolean,
): ConversationMediaCaptureShutterPhase {
    return when {
        isRecording -> VideoRecording
        captureMode == ConversationCaptureMode.Video -> VideoIdle
        else -> Photo
    }
}

private fun ConversationMediaCaptureShutterPhase.resolveInnerShutterColor(
    colorScheme: ColorScheme,
): Color {
    return when (this) {
        Photo -> colorScheme.inverseOnSurface
        VideoIdle -> colorScheme.scrim.copy(alpha = 0.5f)
        VideoRecording -> colorScheme.errorContainer
    }
}

private fun ConversationMediaCaptureShutterPhase.resolveInnerShutterSize(): Dp {
    return when (this) {
        Photo -> PICKER_SHUTTER_PHOTO_INNER_SIZE

        VideoIdle,
        VideoRecording,
        -> PICKER_SHUTTER_FULL_INNER_SIZE
    }
}

private fun ConversationMediaCaptureShutterPhase.resolveOuterContainerColor(
    colorScheme: ColorScheme,
): Color {
    return when (this) {
        Photo -> colorScheme.scrim.copy(alpha = 0.2f)

        VideoIdle,
        VideoRecording,
        -> Color.Transparent
    }
}

private fun ConversationMediaCaptureShutterPhase.resolveOuterScale(): Float {
    return when (this) {
        Photo,
        VideoIdle,
        -> 1f

        VideoRecording -> 0.97f
    }
}

private fun ConversationMediaCaptureShutterPhase.resolveRecordingStopAlpha(): Float {
    return when (this) {
        Photo,
        VideoIdle,
        -> 0f

        VideoRecording -> 1f
    }
}

private fun ConversationMediaCaptureShutterPhase.resolveRecordingStopScale(): Float {
    return when (this) {
        Photo,
        VideoIdle,
        -> 0.8f

        VideoRecording -> 1f
    }
}

private fun ConversationMediaCaptureShutterPhase.resolveVideoCenterDotAlpha(): Float {
    return when (this) {
        Photo,
        VideoRecording,
        -> 0f

        VideoIdle -> 1f
    }
}

private fun ConversationMediaCaptureShutterPhase.resolveVideoCenterDotScale(): Float {
    return when (this) {
        Photo,
        VideoRecording,
        -> 0.72f

        VideoIdle -> 1f
    }
}

@Composable
private fun ConversationMediaCaptureShutterButtonPreviewContainer(
    captureMode: ConversationCaptureMode,
    isPhotoCaptureInProgress: Boolean = false,
    isRecording: Boolean = false,
) {
    AppTheme {
        Surface(color = Color.Black.copy(alpha = 0.5f)) {
            Box(
                modifier = Modifier.padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                ConversationMediaCaptureShutterButton(
                    captureMode = captureMode,
                    isPhotoCaptureInProgress = isPhotoCaptureInProgress,
                    isRecording = isRecording,
                    onClick = {},
                )
            }
        }
    }
}

@Preview(name = "Photo Idle")
@Composable
private fun ConversationMediaCaptureShutterButtonPhotoPreview() {
    ConversationMediaCaptureShutterButtonPreviewContainer(
        captureMode = ConversationCaptureMode.Photo,
    )
}

@Preview(name = "Photo Capturing")
@Composable
private fun ConversationMediaCaptureShutterButtonPhotoCapturingPreview() {
    ConversationMediaCaptureShutterButtonPreviewContainer(
        captureMode = ConversationCaptureMode.Photo,
        isPhotoCaptureInProgress = true,
    )
}

@Preview(name = "Video Idle")
@Composable
private fun ConversationMediaCaptureShutterButtonVideoIdlePreview() {
    ConversationMediaCaptureShutterButtonPreviewContainer(
        captureMode = ConversationCaptureMode.Video,
    )
}

@Preview(name = "Video Recording")
@Composable
private fun ConversationMediaCaptureShutterButtonVideoRecordingPreview() {
    ConversationMediaCaptureShutterButtonPreviewContainer(
        captureMode = ConversationCaptureMode.Video,
        isRecording = true,
    )
}
