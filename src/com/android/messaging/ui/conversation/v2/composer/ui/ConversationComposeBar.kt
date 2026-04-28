package com.android.messaging.ui.conversation.v2.composer.ui

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.android.messaging.R
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_AUDIO_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_CONTACT_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_COMPOSE_BAR_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_MMS_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_SEND_BUTTON_SHAPE_CIRCLE
import com.android.messaging.ui.conversation.v2.CONVERSATION_SEND_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_TEXT_FIELD_TEST_TAG
import com.android.messaging.ui.conversation.v2.audio.model.ConversationAudioRecordingPhase
import com.android.messaging.ui.conversation.v2.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.v2.conversationShape
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal val AUDIO_RECORD_CANCEL_THRESHOLD = 96.dp
internal val AUDIO_RECORD_LOCK_THRESHOLD = 72.dp

private const val CONTENT_SWAP_ENTER_FADE_DURATION_MILLIS = 160
private const val CONTENT_SWAP_ENTER_SLIDE_DURATION_MILLIS = 220
private const val CONTENT_SWAP_ENTER_SLIDE_OFFSET_DIVISOR = 10
private const val CONTENT_SWAP_EXIT_FADE_DURATION_MILLIS = 120
private const val CONTENT_SWAP_EXIT_SLIDE_DURATION_MILLIS = 180
private const val CONTENT_SWAP_EXIT_SLIDE_OFFSET_DIVISOR = 12

@Composable
internal fun ConversationComposeBar(
    modifier: Modifier = Modifier,
    audioRecording: ConversationAudioRecordingUiState,
    messageText: String,
    sendProtocol: ConversationDraftSendProtocol,
    isMessageFieldEnabled: Boolean,
    isAttachmentActionEnabled: Boolean,
    isRecordActionEnabled: Boolean,
    isSendActionEnabled: Boolean,
    shouldShowRecordAction: Boolean,
    messageFieldFocusRequester: FocusRequester? = null,
    onContactAttachClick: () -> Unit,
    onMediaPickerClick: () -> Unit,
    onLockedAudioRecordingStartRequest: () -> Unit,
    onMessageTextChange: (String) -> Unit,
    onAudioRecordingStartRequest: () -> Unit,
    onAudioRecordingFinish: () -> Unit,
    onAudioRecordingLock: () -> Boolean,
    onAudioRecordingCancel: () -> Unit,
    onSendClick: () -> Unit,
) {
    val presentation = rememberConversationComposeBarPresentation()
    val hapticFeedback = LocalHapticFeedback.current

    var recordingGestureState by remember {
        mutableStateOf(ConversationSendActionButtonGestureState())
    }

    LaunchedEffect(audioRecording.phase) {
        if (audioRecording.phase != ConversationAudioRecordingPhase.Recording) {
            recordingGestureState = ConversationSendActionButtonGestureState()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .testTag(CONVERSATION_COMPOSE_BAR_TEST_TAG),
    ) {
        ConversationComposeInputContent(
            audioRecording = audioRecording,
            messageText = messageText,
            sendProtocol = sendProtocol,
            isMessageFieldEnabled = isMessageFieldEnabled,
            isAttachmentActionEnabled = isAttachmentActionEnabled,
            isRecordActionEnabled = isRecordActionEnabled,
            isSendActionEnabled = isSendActionEnabled,
            shouldShowRecordAction = shouldShowRecordAction,
            recordingGestureState = recordingGestureState,
            messageFieldFocusRequester = messageFieldFocusRequester,
            presentation = presentation,
            onContactAttachClick = onContactAttachClick,
            onMediaPickerClick = onMediaPickerClick,
            onLockedAudioRecordingStartRequest = onLockedAudioRecordingStartRequest,
            onMessageTextChange = onMessageTextChange,
            onAudioRecordingStartRequest = {
                recordingGestureState = ConversationSendActionButtonGestureState()
                onAudioRecordingStartRequest()
            },
            onAudioRecordingDrag = { gestureState ->
                recordingGestureState = gestureState
            },
            onAudioRecordingLock = {
                if (audioRecording.isLocked) {
                    return@ConversationComposeInputContent false
                }

                recordingGestureState = ConversationSendActionButtonGestureState()
                val didLockRecording = onAudioRecordingLock()
                if (didLockRecording) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                didLockRecording
            },
            onAudioRecordingFinish = { shouldCancelRecording ->
                recordingGestureState = ConversationSendActionButtonGestureState()
                when {
                    shouldCancelRecording -> onAudioRecordingCancel()
                    else -> onAudioRecordingFinish()
                }
            },
            onSendClick = onSendClick,
        )
    }
}

@Composable
private fun rememberConversationComposeBarPresentation(): ConversationComposeBarPresentation {
    val fieldColors = conversationComposeBarTextFieldColors()

    return remember(fieldColors) {
        ConversationComposeBarPresentation(
            fieldShape = RoundedCornerShape(size = 28.dp),
            fieldColors = fieldColors,
        )
    }
}

@Composable
private fun conversationComposeBarTextFieldColors(): TextFieldColors {
    return TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ConversationComposeInputContent(
    audioRecording: ConversationAudioRecordingUiState,
    messageText: String,
    sendProtocol: ConversationDraftSendProtocol,
    isMessageFieldEnabled: Boolean,
    isAttachmentActionEnabled: Boolean,
    isRecordActionEnabled: Boolean,
    isSendActionEnabled: Boolean,
    shouldShowRecordAction: Boolean,
    recordingGestureState: ConversationSendActionButtonGestureState,
    messageFieldFocusRequester: FocusRequester?,
    presentation: ConversationComposeBarPresentation,
    onContactAttachClick: () -> Unit,
    onMediaPickerClick: () -> Unit,
    onLockedAudioRecordingStartRequest: () -> Unit,
    onMessageTextChange: (String) -> Unit,
    onAudioRecordingStartRequest: () -> Unit,
    onAudioRecordingDrag: (ConversationSendActionButtonGestureState) -> Unit,
    onAudioRecordingLock: () -> Boolean,
    onAudioRecordingFinish: (Boolean) -> Unit,
    onSendClick: () -> Unit,
) {
    val inputState = conversationComposeInputState(
        audioRecording = audioRecording,
        recordingGestureState = recordingGestureState,
        shouldShowRecordAction = shouldShowRecordAction,
        isRecordActionEnabled = isRecordActionEnabled,
        isSendActionEnabled = isSendActionEnabled,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
        ),
        verticalAlignment = Alignment.Bottom,
    ) {
        ConversationComposeMessageRecordingContent(
            modifier = Modifier.weight(weight = 1f),
            messageText = messageText,
            sendProtocol = sendProtocol,
            durationMillis = audioRecording.durationMillis,
            inputState = inputState,
            isMessageFieldEnabled = isMessageFieldEnabled,
            isAttachmentActionEnabled = isAttachmentActionEnabled,
            isRecordActionEnabled = isRecordActionEnabled,
            messageFieldFocusRequester = messageFieldFocusRequester,
            presentation = presentation,
            onContactAttachClick = onContactAttachClick,
            onMediaPickerClick = onMediaPickerClick,
            onLockedAudioRecordingStartRequest = onLockedAudioRecordingStartRequest,
            onMessageTextChange = onMessageTextChange,
        )

        ConversationComposeSendAction(
            modifier = Modifier
                .testTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
                .semantics {
                    conversationShape = CONVERSATION_SEND_BUTTON_SHAPE_CIRCLE
                },
            enabled = inputState.isRecordingControlEnabled,
            mode = conversationComposeSendActionMode(
                isRecordMode = inputState.isRecordMode,
                isRecordingLocked = audioRecording.isLocked,
            ),
            isRecordingActive = inputState.isActiveRecording,
            isRecordingLocked = audioRecording.isLocked,
            shouldShowLockAffordance = inputState.isActiveRecording && !audioRecording.isLocked,
            lockProgress = inputState.lockProgress,
            onClick = onSendClick,
            onLockedStopClick = {
                onAudioRecordingFinish(false)
            },
            onRecordGestureStart = onAudioRecordingStartRequest,
            onRecordGestureMove = onAudioRecordingDrag,
            onRecordGestureLock = onAudioRecordingLock,
            onRecordGestureFinish = onAudioRecordingFinish,
        )
    }
}

@Composable
private fun conversationComposeInputState(
    audioRecording: ConversationAudioRecordingUiState,
    recordingGestureState: ConversationSendActionButtonGestureState,
    shouldShowRecordAction: Boolean,
    isRecordActionEnabled: Boolean,
    isSendActionEnabled: Boolean,
): ConversationComposeInputState {
    val cancelThresholdPx = with(LocalDensity.current) {
        AUDIO_RECORD_CANCEL_THRESHOLD.toPx()
    }
    val lockThresholdPx = with(LocalDensity.current) {
        AUDIO_RECORD_LOCK_THRESHOLD.toPx()
    }
    val cancelProgress = (recordingGestureState.cancelDragDistancePx / cancelThresholdPx)
        .coerceIn(minimumValue = 0f, maximumValue = 1f)

    val lockProgress = when {
        audioRecording.isLocked -> 1f

        else -> {
            (recordingGestureState.lockDragDistancePx / lockThresholdPx)
                .coerceIn(minimumValue = 0f, maximumValue = 1f)
        }
    }
    val isActiveRecording = audioRecording.phase == ConversationAudioRecordingPhase.Recording
    val isRecordMode = shouldShowRecordAction || isActiveRecording

    val isRecordingControlEnabled = when {
        isActiveRecording -> true
        isRecordMode -> isRecordActionEnabled
        else -> isSendActionEnabled
    }

    return ConversationComposeInputState(
        cancelProgress = cancelProgress,
        lockProgress = lockProgress,
        isCancellationArmed = cancelProgress >= 1f,
        isActiveRecording = isActiveRecording,
        isRecordMode = isRecordMode,
        isRecordingControlEnabled = isRecordingControlEnabled,
    )
}

@Composable
private fun ConversationComposeMessageRecordingContent(
    modifier: Modifier = Modifier,
    messageText: String,
    sendProtocol: ConversationDraftSendProtocol,
    durationMillis: Long,
    inputState: ConversationComposeInputState,
    isMessageFieldEnabled: Boolean,
    isAttachmentActionEnabled: Boolean,
    isRecordActionEnabled: Boolean,
    messageFieldFocusRequester: FocusRequester?,
    presentation: ConversationComposeBarPresentation,
    onContactAttachClick: () -> Unit,
    onMediaPickerClick: () -> Unit,
    onLockedAudioRecordingStartRequest: () -> Unit,
    onMessageTextChange: (String) -> Unit,
) {
    Box(
        modifier = modifier,
    ) {
        ConversationComposeMessageField(
            modifier = Modifier.fillMaxWidth(),
            value = messageText,
            onValueChange = { updatedMessageText ->
                if (!inputState.isActiveRecording) {
                    onMessageTextChange(updatedMessageText)
                }
            },
            enabled = isMessageFieldEnabled,
            sendProtocol = sendProtocol,
            isVisuallyHidden = inputState.isActiveRecording,
            messageFieldFocusRequester = messageFieldFocusRequester,
            presentation = presentation,
            isAttachmentActionEnabled = isAttachmentActionEnabled,
            isAudioRecordActionEnabled = isRecordActionEnabled,
            onContactAttachClick = onContactAttachClick,
            onMediaPickerClick = onMediaPickerClick,
            onAudioAttachClick = onLockedAudioRecordingStartRequest,
        )

        ConversationAudioRecordingContentOverlay(
            modifier = Modifier.matchParentSize(),
            isActiveRecording = inputState.isActiveRecording,
            durationMillis = durationMillis,
            cancelProgress = inputState.cancelProgress,
            isCancellationArmed = inputState.isCancellationArmed,
        )
    }
}

@Composable
private fun ConversationAudioRecordingContentOverlay(
    modifier: Modifier = Modifier,
    isActiveRecording: Boolean,
    durationMillis: Long,
    cancelProgress: Float,
    isCancellationArmed: Boolean,
) {
    AnimatedContent(
        modifier = modifier,
        targetState = isActiveRecording,
        transitionSpec = {
            contentSwapTransition()
        },
        label = "conversation_compose_content",
    ) { isRecording ->
        when {
            isRecording -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    ConversationAudioRecordingBar(
                        durationMillis = durationMillis,
                        cancelProgress = cancelProgress,
                        isCancellationArmed = isCancellationArmed,
                    )
                }
            }

            else -> {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

private fun conversationComposeSendActionMode(
    isRecordMode: Boolean,
    isRecordingLocked: Boolean,
): ConversationSendActionButtonMode {
    return when {
        isRecordMode && isRecordingLocked -> ConversationSendActionButtonMode.Stop
        isRecordMode -> ConversationSendActionButtonMode.Record
        else -> ConversationSendActionButtonMode.Send
    }
}

@Composable
private fun ConversationComposeMessageField(
    modifier: Modifier = Modifier,
    value: String,
    enabled: Boolean,
    sendProtocol: ConversationDraftSendProtocol,
    isVisuallyHidden: Boolean,
    messageFieldFocusRequester: FocusRequester?,
    presentation: ConversationComposeBarPresentation,
    isAttachmentActionEnabled: Boolean,
    isAudioRecordActionEnabled: Boolean,
    onValueChange: (String) -> Unit,
    onContactAttachClick: () -> Unit,
    onMediaPickerClick: () -> Unit,
    onAudioAttachClick: () -> Unit,
) {
    val focusRequesterModifier = messageFieldFocusRequester
        ?.let(Modifier::focusRequester)
        ?: Modifier

    val recordingVisibilityModifier = when {
        isVisuallyHidden -> {
            Modifier
                .alpha(alpha = 0f)
                .clearAndSetSemantics {}
        }

        else -> Modifier
    }

    val mmsText = stringResource(id = R.string.mms_text)
    val sendProtocolSemanticsModifier = when (sendProtocol) {
        ConversationDraftSendProtocol.MMS -> {
            Modifier.semantics {
                stateDescription = mmsText
            }
        }

        ConversationDraftSendProtocol.SMS -> Modifier
    }

    TextField(
        modifier = modifier
            .then(focusRequesterModifier)
            .testTag(CONVERSATION_TEXT_FIELD_TEST_TAG)
            .heightIn(min = 56.dp)
            .then(sendProtocolSemanticsModifier)
            .then(recordingVisibilityModifier),
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        shape = presentation.fieldShape,
        colors = presentation.fieldColors,
        placeholder = ::ConversationComposePlaceholder,
        leadingIcon = {
            ConversationComposeAttachmentMenu(
                modifier = Modifier.testTag(CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG),
                enabled = isAttachmentActionEnabled,
                isAudioRecordActionEnabled = isAudioRecordActionEnabled,
                onContactAttachClick = onContactAttachClick,
                onMediaPickerClick = onMediaPickerClick,
                onAudioAttachClick = onAudioAttachClick,
            )
        },
        trailingIcon = when (sendProtocol) {
            ConversationDraftSendProtocol.MMS -> {
                {
                    MmsIndicator()
                }
            }

            ConversationDraftSendProtocol.SMS -> null
        },
        minLines = 1,
        maxLines = 4,
    )
}

@Composable
private fun MmsIndicator() {
    Text(
        modifier = Modifier
            .padding(end = 12.dp)
            .clearAndSetSemantics {}
            .testTag(CONVERSATION_MMS_INDICATOR_TEST_TAG),
        text = stringResource(id = R.string.mms_text),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.tertiary,
    )
}

@Composable
private fun ConversationComposePlaceholder() {
    Text(
        text = stringResource(id = R.string.compose_message_view_hint_text),
        style = MaterialTheme.typography.bodyLarge,
    )
}

private fun contentSwapTransition(): ContentTransform {
    val enterTransition = contentSwapEnterTransition()
    val exitTransition = contentSwapExitTransition()

    return enterTransition.togetherWith(exitTransition)
}

private fun contentSwapEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = tween(durationMillis = CONTENT_SWAP_ENTER_FADE_DURATION_MILLIS),
    ) + slideInHorizontally(
        animationSpec = tween(durationMillis = CONTENT_SWAP_ENTER_SLIDE_DURATION_MILLIS),
        initialOffsetX = ::contentSwapEnterOffset,
    )
}

private fun contentSwapExitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = tween(durationMillis = CONTENT_SWAP_EXIT_FADE_DURATION_MILLIS),
    ) + slideOutHorizontally(
        animationSpec = tween(durationMillis = CONTENT_SWAP_EXIT_SLIDE_DURATION_MILLIS),
        targetOffsetX = ::contentSwapExitOffset,
    )
}

private fun contentSwapEnterOffset(fullWidth: Int): Int {
    return fullWidth / CONTENT_SWAP_ENTER_SLIDE_OFFSET_DIVISOR
}

private fun contentSwapExitOffset(fullWidth: Int): Int {
    return -(fullWidth / CONTENT_SWAP_EXIT_SLIDE_OFFSET_DIVISOR)
}

@Composable
private fun ConversationComposeAttachmentMenu(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    isAudioRecordActionEnabled: Boolean,
    onContactAttachClick: () -> Unit,
    onMediaPickerClick: () -> Unit,
    onAudioAttachClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    var isExpanded by rememberSaveable {
        mutableStateOf(value = false)
    }

    fun closeMenuAndRun(action: () -> Unit) {
        isExpanded = false
        action()
    }

    Box(
        modifier = modifier,
    ) {
        IconButton(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                isExpanded = true
            },
            enabled = enabled,
        ) {
            Icon(
                imageVector = Icons.Rounded.AddCircleOutline,
                contentDescription = stringResource(
                    id = R.string.attachMediaButtonContentDescription,
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = {
                isExpanded = false
            },
            shape = RoundedCornerShape(size = 24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
            offset = DpOffset(
                x = 0.dp,
                y = (-8).dp,
            ),
            properties = PopupProperties(
                focusable = false,
            ),
        ) {
            ConversationComposeAttachmentMenuItem(
                modifier = Modifier.testTag(CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG),
                imageVector = Icons.Rounded.Image,
                textResId = R.string.mediapicker_gallery_title,
                onClick = {
                    closeMenuAndRun(action = onMediaPickerClick)
                },
            )
            ConversationComposeAttachmentMenuItem(
                modifier = Modifier.testTag(CONVERSATION_ATTACHMENT_AUDIO_MENU_ITEM_TEST_TAG),
                imageVector = Icons.Rounded.Mic,
                textResId = R.string.mediapicker_audio_title,
                enabled = isAudioRecordActionEnabled,
                onClick = {
                    closeMenuAndRun(action = onAudioAttachClick)
                },
            )

            ConversationComposeAttachmentMenuItem(
                modifier = Modifier.testTag(CONVERSATION_ATTACHMENT_CONTACT_MENU_ITEM_TEST_TAG),
                imageVector = Icons.Rounded.Person,
                textResId = R.string.mediapicker_contact_title,
                onClick = {
                    closeMenuAndRun(action = onContactAttachClick)
                },
            )
        }
    }
}

@Composable
private fun ConversationComposeAttachmentMenuItem(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    @StringRes textResId: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        modifier = modifier,
        text = {
            Text(text = stringResource(id = textResId))
        },
        leadingIcon = {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = Modifier.size(size = 24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun ConversationComposeSendAction(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    mode: ConversationSendActionButtonMode,
    isRecordingActive: Boolean,
    isRecordingLocked: Boolean,
    shouldShowLockAffordance: Boolean,
    lockProgress: Float,
    onClick: () -> Unit,
    onLockedStopClick: () -> Unit,
    onRecordGestureStart: () -> Unit,
    onRecordGestureMove: (ConversationSendActionButtonGestureState) -> Unit,
    onRecordGestureLock: () -> Boolean,
    onRecordGestureFinish: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier.heightIn(
            min = 56.dp,
            max = 56.dp,
        ),
    ) {
        ConversationSendActionButton(
            modifier = modifier,
            enabled = enabled,
            mode = mode,
            isRecordingActive = isRecordingActive,
            isRecordingLocked = isRecordingLocked,
            onClick = onClick,
            onLockedStopClick = onLockedStopClick,
            onRecordGestureStart = onRecordGestureStart,
            onRecordGestureMove = onRecordGestureMove,
            onRecordGestureLock = onRecordGestureLock,
            onRecordGestureFinish = onRecordGestureFinish,
        )

        if (shouldShowLockAffordance) {
            ConversationAudioRecordingLockAffordance(
                modifier = Modifier
                    .align(alignment = Alignment.TopCenter)
                    .padding(top = 2.dp)
                    .offset(y = (-74).dp),
                lockProgress = lockProgress,
            )
        }
    }
}

private data class ConversationComposeBarPresentation(
    val fieldShape: RoundedCornerShape,
    val fieldColors: TextFieldColors,
)

private data class ConversationComposeInputState(
    val cancelProgress: Float,
    val lockProgress: Float,
    val isCancellationArmed: Boolean,
    val isActiveRecording: Boolean,
    val isRecordMode: Boolean,
    val isRecordingControlEnabled: Boolean,
)

private data class ConversationComposeBarPreviewState(
    val label: String,
    val audioRecording: ConversationAudioRecordingUiState = ConversationAudioRecordingUiState(),
    val messageText: String = "",
    val sendProtocol: ConversationDraftSendProtocol = ConversationDraftSendProtocol.SMS,
    val isMessageFieldEnabled: Boolean = true,
    val isAttachmentActionEnabled: Boolean = true,
    val isRecordActionEnabled: Boolean = true,
    val isSendActionEnabled: Boolean = false,
    val shouldShowRecordAction: Boolean = true,
    val cancelProgress: Float = 0f,
    val lockProgress: Float = 0f,
)

private val conversationComposeBarPreviewStates = persistentListOf(
    ConversationComposeBarPreviewState(
        label = "Empty record mode",
    ),
    ConversationComposeBarPreviewState(
        label = "Attachments disabled",
        isAttachmentActionEnabled = false,
    ),
    ConversationComposeBarPreviewState(
        label = "Record disabled",
        isRecordActionEnabled = false,
    ),
    ConversationComposeBarPreviewState(
        label = "Send enabled",
        messageText = "See you there",
        isSendActionEnabled = true,
        shouldShowRecordAction = false,
    ),
    ConversationComposeBarPreviewState(
        label = "MMS draft",
        messageText = "Photo attached",
        sendProtocol = ConversationDraftSendProtocol.MMS,
        isSendActionEnabled = true,
        shouldShowRecordAction = false,
    ),
    ConversationComposeBarPreviewState(
        label = "Multiline draft",
        messageText = "Can you bring the invoice?\nAlso, please check the last address.",
        isSendActionEnabled = true,
        shouldShowRecordAction = false,
    ),
    ConversationComposeBarPreviewState(
        label = "Send disabled",
        messageText = "Waiting for service",
        isSendActionEnabled = false,
        shouldShowRecordAction = false,
    ),
    ConversationComposeBarPreviewState(
        label = "Composer disabled",
        isMessageFieldEnabled = false,
        isAttachmentActionEnabled = false,
        isRecordActionEnabled = false,
    ),
    ConversationComposeBarPreviewState(
        label = "Finalizing audio",
        audioRecording = ConversationAudioRecordingUiState(
            phase = ConversationAudioRecordingPhase.Finalizing,
            durationMillis = 12_400L,
        ),
        isRecordActionEnabled = false,
        shouldShowRecordAction = false,
    ),
    ConversationComposeBarPreviewState(
        label = "Recording",
        audioRecording = ConversationAudioRecordingUiState(
            phase = ConversationAudioRecordingPhase.Recording,
            durationMillis = 8_400L,
        ),
        shouldShowRecordAction = false,
    ),
    ConversationComposeBarPreviewState(
        label = "Recording cancel drag",
        audioRecording = ConversationAudioRecordingUiState(
            phase = ConversationAudioRecordingPhase.Recording,
            durationMillis = 14_250L,
        ),
        shouldShowRecordAction = false,
        cancelProgress = 0.62f,
    ),
    ConversationComposeBarPreviewState(
        label = "Recording cancel armed",
        audioRecording = ConversationAudioRecordingUiState(
            phase = ConversationAudioRecordingPhase.Recording,
            durationMillis = 19_800L,
        ),
        shouldShowRecordAction = false,
        cancelProgress = 1f,
    ),
    ConversationComposeBarPreviewState(
        label = "Recording lock drag",
        audioRecording = ConversationAudioRecordingUiState(
            phase = ConversationAudioRecordingPhase.Recording,
            durationMillis = 23_100L,
        ),
        shouldShowRecordAction = false,
        lockProgress = 0.72f,
    ),
    ConversationComposeBarPreviewState(
        label = "Recording locked",
        audioRecording = ConversationAudioRecordingUiState(
            phase = ConversationAudioRecordingPhase.Recording,
            durationMillis = 31_000L,
            isLocked = true,
        ),
        shouldShowRecordAction = false,
        lockProgress = 1f,
    ),
)

@Composable
private fun ConversationComposeBarPreviewGrid(
    modifier: Modifier = Modifier,
    previewStates: ImmutableList<ConversationComposeBarPreviewState>,
) {
    AppTheme {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp),
                verticalArrangement = Arrangement.spacedBy(space = 12.dp),
            ) {
                previewStates
                    .chunked(size = 2)
                    .forEach { rowStates ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
                        ) {
                            rowStates.forEach { previewState ->
                                ConversationComposeBarPreviewItem(
                                    modifier = Modifier.weight(weight = 1f),
                                    previewState = previewState,
                                )
                            }

                            if (rowStates.size == 1) {
                                Box(modifier = Modifier.weight(weight = 1f))
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun ConversationComposeBarPreviewItem(
    modifier: Modifier = Modifier,
    previewState: ConversationComposeBarPreviewState,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(size = 20.dp),
    ) {
        Column(
            modifier = Modifier.padding(all = 12.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            Text(
                text = previewState.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 156.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                ConversationComposeBarPreviewContent(previewState = previewState)
            }
        }
    }
}

@Composable
private fun ConversationComposeBarPreviewContent(
    previewState: ConversationComposeBarPreviewState,
) {
    val presentation = rememberConversationComposeBarPresentation()
    val cancelDragDistancePx = with(LocalDensity.current) {
        AUDIO_RECORD_CANCEL_THRESHOLD.toPx()
    } * previewState.cancelProgress
    val lockDragDistancePx = with(LocalDensity.current) {
        AUDIO_RECORD_LOCK_THRESHOLD.toPx()
    } * previewState.lockProgress

    ConversationComposeInputContent(
        audioRecording = previewState.audioRecording,
        messageText = previewState.messageText,
        sendProtocol = previewState.sendProtocol,
        isMessageFieldEnabled = previewState.isMessageFieldEnabled,
        isAttachmentActionEnabled = previewState.isAttachmentActionEnabled,
        isRecordActionEnabled = previewState.isRecordActionEnabled,
        isSendActionEnabled = previewState.isSendActionEnabled,
        shouldShowRecordAction = previewState.shouldShowRecordAction,
        recordingGestureState = ConversationSendActionButtonGestureState(
            cancelDragDistancePx = cancelDragDistancePx,
            lockDragDistancePx = lockDragDistancePx,
        ),
        messageFieldFocusRequester = null,
        presentation = presentation,
        onContactAttachClick = {},
        onMediaPickerClick = {},
        onLockedAudioRecordingStartRequest = {},
        onMessageTextChange = {},
        onAudioRecordingStartRequest = {},
        onAudioRecordingDrag = {},
        onAudioRecordingLock = { false },
        onAudioRecordingFinish = {},
        onSendClick = {},
    )
}

@Preview(
    name = "Compose Bar States - Light",
    showBackground = true,
    widthDp = 920,
)
@Preview(
    name = "Compose Bar States - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 920,
)
@Composable
private fun ConversationComposeBarStatesPreview() {
    ConversationComposeBarPreviewGrid(
        previewStates = conversationComposeBarPreviewStates,
    )
}
