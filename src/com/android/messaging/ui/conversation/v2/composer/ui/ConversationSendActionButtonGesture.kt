package com.android.messaging.ui.conversation.v2.composer.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput

@Immutable
internal data class ConversationSendActionButtonGestureState(
    val cancelDragDistancePx: Float = 0f,
    val lockDragDistancePx: Float = 0f,
)

@Composable
internal fun Modifier.conversationSendActionButtonGesture(
    mode: ConversationSendActionButtonMode,
    enabled: Boolean,
    cancelThresholdPx: Float,
    lockThresholdPx: Float,
    isRecordingActive: Boolean,
    isRecordingLocked: Boolean,
    onGestureActiveChange: (Boolean) -> Unit,
    onRecordGestureStart: () -> Unit,
    onRecordGestureMove: (ConversationSendActionButtonGestureState) -> Unit,
    onRecordGestureLock: () -> Boolean,
    onRecordGestureFinish: (Boolean) -> Unit,
    onLockedStopClick: () -> Unit,
): Modifier {
    val currentIsRecordingActive by rememberUpdatedState(newValue = isRecordingActive)
    val currentIsRecordingLocked by rememberUpdatedState(newValue = isRecordingLocked)
    val currentOnGestureActiveChange by rememberUpdatedState(newValue = onGestureActiveChange)
    val currentOnRecordGestureStart by rememberUpdatedState(newValue = onRecordGestureStart)
    val currentOnRecordGestureMove by rememberUpdatedState(newValue = onRecordGestureMove)
    val currentOnRecordGestureLock by rememberUpdatedState(newValue = onRecordGestureLock)
    val currentOnRecordGestureFinish by rememberUpdatedState(newValue = onRecordGestureFinish)
    val currentOnLockedStopClick by rememberUpdatedState(newValue = onLockedStopClick)

    return when {
        mode != ConversationSendActionButtonMode.Send && enabled -> {
            pointerInput(
                mode,
                enabled,
                cancelThresholdPx,
                lockThresholdPx,
            ) {
                awaitEachGesture {
                    when {
                        currentIsRecordingActive && currentIsRecordingLocked -> {
                            handleLockedRecordGesture(
                                cancelThresholdPx = cancelThresholdPx,
                                onGestureActiveChange = currentOnGestureActiveChange,
                                onRecordGestureMove = currentOnRecordGestureMove,
                                onRecordGestureFinish = currentOnRecordGestureFinish,
                                onLockedStopClick = currentOnLockedStopClick,
                            )
                        }

                        else -> {
                            handleRecordGesture(
                                cancelThresholdPx = cancelThresholdPx,
                                lockThresholdPx = lockThresholdPx,
                                onGestureActiveChange = currentOnGestureActiveChange,
                                onRecordGestureStart = currentOnRecordGestureStart,
                                onRecordGestureMove = currentOnRecordGestureMove,
                                onRecordGestureLock = currentOnRecordGestureLock,
                                onRecordGestureFinish = currentOnRecordGestureFinish,
                            )
                        }
                    }
                }
            }
        }

        else -> this
    }
}

private suspend fun AwaitPointerEventScope.handleRecordGesture(
    cancelThresholdPx: Float,
    lockThresholdPx: Float,
    onGestureActiveChange: (Boolean) -> Unit,
    onRecordGestureStart: () -> Unit,
    onRecordGestureMove: (ConversationSendActionButtonGestureState) -> Unit,
    onRecordGestureLock: () -> Boolean,
    onRecordGestureFinish: (Boolean) -> Unit,
) {
    val initialDown = awaitFirstDown(requireUnconsumed = false)

    val longPressChange = awaitLongPressOrCancellation(pointerId = initialDown.id)
        ?: return

    onGestureActiveChange(true)
    onRecordGestureStart()

    trackRecordGestureDrag(
        initialDown = initialDown,
        longPressChange = longPressChange,
        cancelThresholdPx = cancelThresholdPx,
        lockThresholdPx = lockThresholdPx,
        onGestureActiveChange = onGestureActiveChange,
        onRecordGestureMove = onRecordGestureMove,
        onRecordGestureLock = onRecordGestureLock,
        onRecordGestureFinish = onRecordGestureFinish,
    )
}

private suspend fun AwaitPointerEventScope.trackRecordGestureDrag(
    initialDown: PointerInputChange,
    longPressChange: PointerInputChange,
    cancelThresholdPx: Float,
    lockThresholdPx: Float,
    onGestureActiveChange: (Boolean) -> Unit,
    onRecordGestureMove: (ConversationSendActionButtonGestureState) -> Unit,
    onRecordGestureLock: () -> Boolean,
    onRecordGestureFinish: (Boolean) -> Unit,
) {
    var isRecordingLocked = false

    longPressChange.consume()

    while (true) {
        val pointerChange = awaitRecordGestureChange(pointerId = initialDown.id) ?: break
        val gestureState = calculateRecordGestureState(
            initialDown = initialDown,
            pointerChange = pointerChange,
        )

        if (!isRecordingLocked) {
            onRecordGestureMove(gestureState)

            if (gestureState.lockDragDistancePx >= lockThresholdPx) {
                isRecordingLocked = onRecordGestureLock()

                if (isRecordingLocked) {
                    onRecordGestureMove(ConversationSendActionButtonGestureState())
                }
            }
        }

        pointerChange.consume()

        if (pointerChange.pressed) {
            continue
        }

        resetRecordGestureDragUi(
            onGestureActiveChange = onGestureActiveChange,
            onRecordGestureMove = onRecordGestureMove,
        )

        if (!isRecordingLocked) {
            onRecordGestureFinish(gestureState.cancelDragDistancePx >= cancelThresholdPx)
        }

        return
    }

    resetRecordGestureDragUi(
        onGestureActiveChange = onGestureActiveChange,
        onRecordGestureMove = onRecordGestureMove,
    )
}

private suspend fun AwaitPointerEventScope.handleLockedRecordGesture(
    cancelThresholdPx: Float,
    onGestureActiveChange: (Boolean) -> Unit,
    onRecordGestureMove: (ConversationSendActionButtonGestureState) -> Unit,
    onRecordGestureFinish: (Boolean) -> Unit,
    onLockedStopClick: () -> Unit,
) {
    val initialDown = awaitFirstDown(requireUnconsumed = false)

    onGestureActiveChange(true)
    initialDown.consume()

    while (true) {
        val pointerChange = awaitRecordGestureChange(pointerId = initialDown.id) ?: break
        val gestureState = calculateRecordGestureState(
            initialDown = initialDown,
            pointerChange = pointerChange,
        )

        onRecordGestureMove(
            ConversationSendActionButtonGestureState(
                cancelDragDistancePx = gestureState.cancelDragDistancePx,
            ),
        )
        pointerChange.consume()

        if (!pointerChange.pressed) {
            resetRecordGestureDragUi(
                onGestureActiveChange = onGestureActiveChange,
                onRecordGestureMove = onRecordGestureMove,
            )
            when {
                gestureState.cancelDragDistancePx >= cancelThresholdPx -> {
                    onRecordGestureFinish(true)
                }

                else -> {
                    onLockedStopClick()
                }
            }
            return
        }
    }

    resetRecordGestureDragUi(
        onGestureActiveChange = onGestureActiveChange,
        onRecordGestureMove = onRecordGestureMove,
    )
}

private fun resetRecordGestureDragUi(
    onGestureActiveChange: (Boolean) -> Unit,
    onRecordGestureMove: (ConversationSendActionButtonGestureState) -> Unit,
) {
    onGestureActiveChange(false)
    onRecordGestureMove(ConversationSendActionButtonGestureState())
}

private suspend fun AwaitPointerEventScope.awaitRecordGestureChange(
    pointerId: PointerId,
): PointerInputChange? {
    return awaitPointerEvent()
        .changes
        .firstOrNull { change ->
            change.id == pointerId
        }
}

private fun calculateRecordGestureState(
    initialDown: PointerInputChange,
    pointerChange: PointerInputChange,
): ConversationSendActionButtonGestureState {
    return ConversationSendActionButtonGestureState(
        cancelDragDistancePx = (initialDown.position.x - pointerChange.position.x)
            .coerceAtLeast(minimumValue = 0f),
        lockDragDistancePx = (initialDown.position.y - pointerChange.position.y)
            .coerceAtLeast(minimumValue = 0f),
    )
}
