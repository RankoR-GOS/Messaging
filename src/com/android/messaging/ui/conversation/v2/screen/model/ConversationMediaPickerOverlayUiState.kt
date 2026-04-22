package com.android.messaging.ui.conversation.v2.screen.model

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.conversation.v2.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationMediaPickerUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class ConversationMediaPickerOverlayUiState(
    val mediaPicker: ConversationMediaPickerUiState = ConversationMediaPickerUiState(),
    val attachments: ImmutableList<ComposerAttachmentUiModel> = persistentListOf(),
    val conversationTitle: String? = null,
    val isSendActionEnabled: Boolean = false,
)
