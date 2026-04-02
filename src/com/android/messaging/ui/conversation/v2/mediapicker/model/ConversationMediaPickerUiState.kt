package com.android.messaging.ui.conversation.v2.mediapicker.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.media.model.ConversationMediaItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class ConversationMediaPickerUiState(
    val galleryItems: ImmutableList<ConversationMediaItem> = persistentListOf(),
    val isLoadingGallery: Boolean = false,
)
