package com.android.messaging.ui.conversation.v2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.android.messaging.ui.conversation.v2.entry.NewChatScreen
import com.android.messaging.ui.conversation.v2.recipientpicker.RecipientPickerScreen
import com.android.messaging.ui.conversation.v2.screen.ConversationScreen
import com.android.messaging.ui.conversation.v2.screen.model.ConversationLaunchRequest

@Composable
internal fun ConversationNavGraph(
    launchRequest: ConversationLaunchRequest?,
    modifier: Modifier = Modifier,
    onFinish: () -> Unit,
) {
    val backStack = rememberNavBackStack(initialNavKey(launchRequest = launchRequest))

    val entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator<NavKey>(),
    )

    val entryProvider = remember(launchRequest, onFinish) {
        entryProvider {
            entry<ConversationNavKey> { navKey ->
                ConversationScreen(
                    launchRequest = launchRequestForConversation(
                        launchRequest = launchRequest,
                        conversationId = navKey.conversationId,
                    ),
                    onNavigateBack = {
                        popBackStackOrFinish(
                            backStack = backStack,
                            onFinish = onFinish,
                        )
                    },
                )
            }

            entry<NewChatNavKey> {
                NewChatScreen()
            }

            entry<RecipientPickerNavKey> { navKey ->
                RecipientPickerScreen(mode = navKey.mode)
            }
        }
    }

    LaunchedEffect(launchRequest) {
        updateBackStackForLaunch(
            backStack = backStack,
            launchRequest = launchRequest,
        )
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = {
            popBackStackOrFinish(
                backStack = backStack,
                onFinish = onFinish,
            )
        },
        entryDecorators = entryDecorators,
        entryProvider = entryProvider,
    )
}

private fun initialNavKey(launchRequest: ConversationLaunchRequest?): NavKey {
    return launchRequest
        ?.conversationId
        ?.let(::ConversationNavKey)
        ?: NewChatNavKey
}

private fun launchRequestForConversation(
    launchRequest: ConversationLaunchRequest?,
    conversationId: String,
): ConversationLaunchRequest? {
    return launchRequest?.copy(conversationId = conversationId)
}

private fun updateBackStackForLaunch(
    backStack: MutableList<NavKey>,
    launchRequest: ConversationLaunchRequest?,
) {
    val destination = initialNavKey(launchRequest = launchRequest)

    if (backStack.size == 1 && backStack.firstOrNull() == destination) {
        return
    }

    backStack.clear()
    backStack.add(destination)
}

private fun popBackStackOrFinish(
    backStack: MutableList<NavKey>,
    onFinish: () -> Unit,
) {
    if (backStack.size > 1) {
        backStack.removeAt(backStack.lastIndex)
        return
    }

    onFinish()
}
