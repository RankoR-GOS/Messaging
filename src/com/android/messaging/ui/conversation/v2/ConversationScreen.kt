package com.android.messaging.ui.conversation.v2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.ui.conversation.v2.component.ConversationMessages
import com.android.messaging.ui.conversation.v2.model.ConversationUiState

@Composable
internal fun ConversationScreen(
    modifier: Modifier = Modifier,
    conversationId: String? = null,
    viewModel: ConversationViewModel = viewModel(),
) {
    LaunchedEffect(conversationId) {
        viewModel.conversationId = conversationId
    }

    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
    ) { contentPadding ->
        ConversationScreenContent(
            modifier = modifier
                .padding(contentPadding),
            conversationId = conversationId,
            uiState = uiState.value,
        )
    }
}

@Composable
private fun ConversationScreenContent(
    modifier: Modifier = Modifier,
    conversationId: String?,
    uiState: ConversationUiState,
) {
    when (uiState) {
        ConversationUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is ConversationUiState.Present -> {
            val messagesListState = rememberMessagesListState(
                conversationId = conversationId,
                initialMessageIndex = uiState.messages.lastIndex.coerceAtLeast(minimumValue = 0),
            )

            ConversationMessages(
                modifier = modifier,
                messages = uiState.messages,
                listState = messagesListState,
            )
        }
    }
}

@Composable
private fun rememberMessagesListState(
    conversationId: String?,
    initialMessageIndex: Int,
): LazyListState {
    return rememberSaveable(
        conversationId,
        saver = LazyListState.Saver,
    ) {
        LazyListState(
            firstVisibleItemIndex = initialMessageIndex,
            firstVisibleItemScrollOffset = 0,
        )
    }
}
