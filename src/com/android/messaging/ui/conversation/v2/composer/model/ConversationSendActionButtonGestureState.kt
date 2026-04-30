package com.android.messaging.ui.conversation.v2.composer.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class ConversationSendActionButtonGestureState(
    val cancelDragDistancePx: Float = 0f,
    val lockDragDistancePx: Float = 0f,
)
