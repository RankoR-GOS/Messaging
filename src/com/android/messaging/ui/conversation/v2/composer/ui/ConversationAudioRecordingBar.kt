package com.android.messaging.ui.conversation.v2.composer.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.CONVERSATION_AUDIO_RECORDING_BAR_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_AUDIO_RECORDING_CANCEL_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_AUDIO_RECORDING_LOCK_AFFORDANCE_TEST_TAG
import com.android.messaging.ui.conversation.v2.audio.formatConversationAudioDuration
import com.android.messaging.ui.core.AppTheme

private const val AUDIO_RECORDING_COLOR_ANIMATION_THRESHOLD = 0.7f

@Composable
internal fun ConversationAudioRecordingBar(
    modifier: Modifier = Modifier,
    durationMillis: Long,
    cancelProgress: Float,
    isCancellationArmed: Boolean,
) {
    val visualState = animateAudioRecordingBarVisualState(
        cancelProgress = cancelProgress,
        isCancellationArmed = isCancellationArmed,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height = 56.dp)
            .testTag(CONVERSATION_AUDIO_RECORDING_BAR_TEST_TAG),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = 56.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(size = 28.dp),
                )
                .padding(
                    horizontal = 12.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AudioRecordingDeleteIcon(
                isVisible = isCancellationArmed,
                tint = visualState.deleteIconTint,
            )

            Spacer(modifier = Modifier.width(width = 4.dp))

            AudioRecordingDurationLabel(
                durationMillis = durationMillis,
                contentColor = visualState.contentColor,
            )

            AudioRecordingCancelHint(
                modifier = Modifier
                    .weight(weight = 1f)
                    .padding(end = 8.dp),
                contentColor = visualState.contentColor,
                hintAlpha = visualState.hintAlpha,
            )
        }
    }
}

@Composable
private fun animateAudioRecordingBarVisualState(
    cancelProgress: Float,
    isCancellationArmed: Boolean,
): AudioRecordingBarVisualState {
    val visualProgress = when {
        isCancellationArmed -> 1f
        else -> {
            (cancelProgress / AUDIO_RECORDING_COLOR_ANIMATION_THRESHOLD)
                .coerceIn(minimumValue = 0f, maximumValue = 1f)
        }
    }

    val contentColor = lerp(
        start = MaterialTheme.colorScheme.onSurfaceVariant,
        stop = when {
            isCancellationArmed -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
        },
        fraction = visualProgress,
    )

    val deleteIconTint = animateColorAsRecordingState(
        isCancellationArmed = isCancellationArmed,
        visualProgress = visualProgress,
    )

    val hintAlpha = animateFloatAsState(
        targetValue = 1f - (visualProgress * 0.45f),
        animationSpec = tween(durationMillis = 180),
        label = "conversation_audio_hint_alpha",
    ).value

    return AudioRecordingBarVisualState(
        contentColor = contentColor,
        deleteIconTint = deleteIconTint,
        hintAlpha = hintAlpha,
    )
}

@Composable
private fun AudioRecordingDeleteIcon(
    isVisible: Boolean,
    tint: Color,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
            expandHorizontally(
                animationSpec = tween(durationMillis = 260),
                expandFrom = Alignment.Start,
            ),
        exit = fadeOut(animationSpec = tween(durationMillis = 140)) +
            shrinkHorizontally(
                animationSpec = tween(durationMillis = 180),
                shrinkTowards = Alignment.Start,
            ),
    ) {
        Icon(
            modifier = Modifier.testTag(CONVERSATION_AUDIO_RECORDING_CANCEL_BUTTON_TEST_TAG),
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = null,
            tint = tint,
        )
    }
}

@Composable
private fun AudioRecordingDurationLabel(
    durationMillis: Long,
    contentColor: Color,
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(durationMillis = 140)) +
            slideInHorizontally(
                animationSpec = tween(durationMillis = 200),
                initialOffsetX = { fullWidth ->
                    -(fullWidth / 6)
                },
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecordingIndicatorDot()

            Text(
                modifier = Modifier.padding(
                    start = 8.dp,
                    end = 12.dp,
                ),
                text = formatConversationAudioDuration(durationMillis = durationMillis),
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun AudioRecordingCancelHint(
    modifier: Modifier = Modifier,
    contentColor: Color,
    hintAlpha: Float,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = null,
            tint = contentColor.copy(alpha = hintAlpha),
        )

        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = stringResource(id = R.string.conversation_audio_recording_slide_to_cancel),
            style = MaterialTheme.typography.titleMedium,
            color = contentColor.copy(alpha = hintAlpha),
        )
    }
}

@Composable
private fun animateColorAsRecordingState(
    isCancellationArmed: Boolean,
    visualProgress: Float,
): Color {
    return animateColorAsState(
        targetValue = lerp(
            start = MaterialTheme.colorScheme.onSurfaceVariant,
            stop = when {
                isCancellationArmed -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
            fraction = visualProgress,
        ),
        animationSpec = tween(durationMillis = 180),
        label = "conversation_audio_delete_icon_color",
    ).value
}

@Composable
private fun RecordingIndicatorDot() {
    val pulseTransition = rememberInfiniteTransition(
        label = "conversation_audio_recording_dot",
    )

    val dotScale = pulseTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "conversation_audio_recording_dot_scale",
    ).value
    val dotAlpha = pulseTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "conversation_audio_recording_dot_alpha",
    ).value

    Box(
        modifier = Modifier
            .size(size = 10.dp)
            .graphicsLayer {
                scaleX = dotScale
                scaleY = dotScale
                alpha = dotAlpha
            }
            .background(
                color = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(size = 100.dp),
            ),
    )
}

@Composable
internal fun ConversationAudioRecordingLockAffordance(
    modifier: Modifier = Modifier,
    lockProgress: Float,
) {
    val visualState = animateConversationAudioRecordingLockAffordanceVisualState(
        lockProgress = lockProgress,
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = visualState.scale
                scaleY = visualState.scale
                translationY = visualState.verticalTranslation
            }
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(size = 24.dp),
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(size = 24.dp),
            )
            .padding(
                paddingValues = PaddingValues(
                    horizontal = 10.dp,
                    vertical = 8.dp,
                ),
            )
            .testTag(CONVERSATION_AUDIO_RECORDING_LOCK_AFFORDANCE_TEST_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier.size(size = 18.dp),
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = visualState.contentColor,
        )

        ConversationAudioRecordingLockAffordanceDivider(
            color = visualState.contentColor,
        )

        Icon(
            modifier = Modifier.size(size = 18.dp),
            imageVector = Icons.Rounded.KeyboardArrowUp,
            contentDescription = null,
            tint = visualState.contentColor,
        )
    }
}

@Composable
private fun animateConversationAudioRecordingLockAffordanceVisualState(
    lockProgress: Float,
): ConversationAudioRecordingLockAffordanceVisualState {
    val resolvedLockProgress = lockProgress.coerceIn(minimumValue = 0f, maximumValue = 1f)

    val contentColor = animateColorAsState(
        targetValue = lerp(
            start = MaterialTheme.colorScheme.onSurfaceVariant,
            stop = MaterialTheme.colorScheme.onSurface,
            fraction = resolvedLockProgress,
        ),
        animationSpec = tween(durationMillis = 180),
        label = "conversation_audio_lock_content_color",
    ).value

    val scale = animateFloatAsState(
        targetValue = 0.96f + (resolvedLockProgress * 0.06f),
        animationSpec = tween(durationMillis = 180),
        label = "conversation_audio_lock_scale",
    ).value

    return ConversationAudioRecordingLockAffordanceVisualState(
        contentColor = contentColor,
        scale = scale,
        verticalTranslation = -8f * resolvedLockProgress,
    )
}

@Composable
private fun ConversationAudioRecordingLockAffordanceDivider(color: Color) {
    Spacer(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .size(
                width = 18.dp,
                height = 1.dp,
            )
            .background(
                color = color.copy(alpha = 0.2f),
                shape = CircleShape,
            ),
    )
}

private data class AudioRecordingBarVisualState(
    val contentColor: Color,
    val deleteIconTint: Color,
    val hintAlpha: Float,
)

private data class ConversationAudioRecordingLockAffordanceVisualState(
    val contentColor: Color,
    val scale: Float,
    val verticalTranslation: Float,
)

@Immutable
private data class ConversationAudioRecordingBarPreviewState(
    val label: String,
    val durationMillis: Long,
    val cancelProgress: Float,
    val isCancellationArmed: Boolean,
)

private val conversationAudioRecordingBarPreviewStates = listOf(
    ConversationAudioRecordingBarPreviewState(
        label = "Recording started",
        durationMillis = 3_000L,
        cancelProgress = 0f,
        isCancellationArmed = false,
    ),
    ConversationAudioRecordingBarPreviewState(
        label = "Cancel drag 25%",
        durationMillis = 12_000L,
        cancelProgress = 0.25f,
        isCancellationArmed = false,
    ),
    ConversationAudioRecordingBarPreviewState(
        label = "Cancel drag 50%",
        durationMillis = 38_000L,
        cancelProgress = 0.5f,
        isCancellationArmed = false,
    ),
    ConversationAudioRecordingBarPreviewState(
        label = "Cancel threshold",
        durationMillis = 65_000L,
        cancelProgress = AUDIO_RECORDING_COLOR_ANIMATION_THRESHOLD,
        isCancellationArmed = false,
    ),
    ConversationAudioRecordingBarPreviewState(
        label = "Cancel armed",
        durationMillis = 72_000L,
        cancelProgress = 1f,
        isCancellationArmed = true,
    ),
    ConversationAudioRecordingBarPreviewState(
        label = "Long duration",
        durationMillis = 3_665_000L,
        cancelProgress = 0f,
        isCancellationArmed = false,
    ),
)

@Preview(
    name = "Audio Recording Bar States - Light",
    showBackground = true,
    widthDp = 840,
)
@Preview(
    name = "Audio Recording Bar States - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 840,
)
@Composable
private fun ConversationAudioRecordingBarStatesPreview() {
    ConversationAudioRecordingBarPreviewGrid()
}

@Composable
private fun ConversationAudioRecordingBarPreviewGrid() {
    AppTheme {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 24.dp),
                verticalArrangement = Arrangement.spacedBy(space = 16.dp),
            ) {
                conversationAudioRecordingBarPreviewStates
                    .chunked(size = 2)
                    .forEach { rowStates ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
                        ) {
                            rowStates.forEach { previewState ->
                                ConversationAudioRecordingBarPreviewItem(
                                    modifier = Modifier.weight(weight = 1f),
                                    previewState = previewState,
                                )
                            }

                            if (rowStates.size == 1) {
                                Spacer(modifier = Modifier.weight(weight = 1f))
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun ConversationAudioRecordingBarPreviewItem(
    modifier: Modifier = Modifier,
    previewState: ConversationAudioRecordingBarPreviewState,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Text(
            text = previewState.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ConversationAudioRecordingBar(
            durationMillis = previewState.durationMillis,
            cancelProgress = previewState.cancelProgress,
            isCancellationArmed = previewState.isCancellationArmed,
        )
    }
}
