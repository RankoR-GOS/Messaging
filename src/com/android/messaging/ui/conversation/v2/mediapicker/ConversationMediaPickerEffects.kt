package com.android.messaging.ui.conversation.v2.mediapicker

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.android.messaging.data.media.model.ConversationMediaItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HandlePendingGallerySelectionEffect(
    pendingSelectedMediaItem: ConversationMediaItem?,
    sheetState: SheetState,
    onGalleryMediaConfirmed: (List<ConversationMediaItem>) -> Unit,
    onShowReview: (String) -> Unit,
    onSelectionHandled: () -> Unit,
) {
    LaunchedEffect(pendingSelectedMediaItem) {
        val mediaItem = pendingSelectedMediaItem ?: return@LaunchedEffect

        val shouldExpandSheet = sheetState.currentValue == SheetValue.Expanded ||
            sheetState.targetValue == SheetValue.Expanded

        if (shouldExpandSheet) {
            sheetState.partialExpand()
        }

        onGalleryMediaConfirmed(
            listOf(mediaItem),
        )
        onShowReview(mediaItem.contentUri)
        onSelectionHandled()
    }
}
