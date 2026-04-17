package com.android.messaging.ui.conversation.v2.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationNavigationReducerImplTest {

    private val reducer: ConversationNavigationReducer = ConversationNavigationReducerImpl()

    @Test
    fun navigateToConversation_appendsDestinationWhenItIsNotAlreadyOnTop() {
        val backStack = mutableListOf<NavKey>(NewChatNavKey)

        reducer.navigateToConversation(
            backStack = backStack,
            conversationId = "conversation-1",
        )

        assertEquals(
            listOf(
                NewChatNavKey,
                ConversationNavKey(conversationId = "conversation-1"),
            ),
            backStack,
        )
    }

    @Test
    fun navigateToRecipientPicker_doesNotDuplicateExistingTopDestination() {
        val backStack = mutableListOf<NavKey>(
            NewChatNavKey,
            RecipientPickerNavKey(mode = RecipientPickerMode.ADD_PARTICIPANTS),
        )

        reducer.navigateToRecipientPicker(
            backStack = backStack,
            mode = RecipientPickerMode.ADD_PARTICIPANTS,
        )

        assertEquals(
            listOf(
                NewChatNavKey,
                RecipientPickerNavKey(mode = RecipientPickerMode.ADD_PARTICIPANTS),
            ),
            backStack,
        )
    }

    @Test
    fun navigateToAddParticipants_appendsDestinationWhenItIsNotAlreadyOnTop() {
        val backStack = mutableListOf<NavKey>(
            ConversationNavKey(conversationId = "conversation-1"),
        )

        reducer.navigateToAddParticipants(
            backStack = backStack,
            conversationId = "conversation-1",
        )

        assertEquals(
            listOf(
                ConversationNavKey(conversationId = "conversation-1"),
                AddParticipantsNavKey(conversationId = "conversation-1"),
            ),
            backStack,
        )
    }

    @Test
    fun navigateToAddParticipants_doesNotDuplicateExistingTopDestination() {
        val backStack = mutableListOf<NavKey>(
            ConversationNavKey(conversationId = "conversation-1"),
            AddParticipantsNavKey(conversationId = "conversation-1"),
        )

        reducer.navigateToAddParticipants(
            backStack = backStack,
            conversationId = "conversation-1",
        )

        assertEquals(
            listOf(
                ConversationNavKey(conversationId = "conversation-1"),
                AddParticipantsNavKey(conversationId = "conversation-1"),
            ),
            backStack,
        )
    }

    @Test
    fun popBackStack_returnsFalseWhenBackStackHasSingleEntry() {
        val backStack = mutableListOf<NavKey>(NewChatNavKey)

        val wasPopped = reducer.popBackStack(backStack = backStack)

        assertFalse(wasPopped)
        assertEquals(listOf(NewChatNavKey), backStack)
    }

    @Test
    fun popBackStack_removesLastEntryWhenBackStackHasMultipleEntries() {
        val backStack = mutableListOf<NavKey>(
            NewChatNavKey,
            ConversationNavKey(conversationId = "conversation-1"),
        )

        val wasPopped = reducer.popBackStack(backStack = backStack)

        assertTrue(wasPopped)
        assertEquals(
            listOf(NewChatNavKey),
            backStack,
        )
    }

    @Test
    fun replaceCurrentConversation_removesAddParticipantsAndReplacesExistingConversation() {
        val backStack = mutableListOf<NavKey>(
            ConversationNavKey(conversationId = "conversation-1"),
            AddParticipantsNavKey(conversationId = "conversation-1"),
        )

        reducer.replaceCurrentConversation(
            backStack = backStack,
            conversationId = "conversation-2",
        )

        assertEquals(
            listOf(
                ConversationNavKey(conversationId = "conversation-2"),
            ),
            backStack,
        )
    }

    @Test
    fun replaceCurrentConversation_addsConversationWhenBackStackHasNoConversationEntry() {
        val backStack = mutableListOf<NavKey>(
            NewChatNavKey,
            AddParticipantsNavKey(conversationId = "conversation-1"),
        )

        reducer.replaceCurrentConversation(
            backStack = backStack,
            conversationId = "conversation-2",
        )

        assertEquals(
            listOf(
                NewChatNavKey,
                ConversationNavKey(conversationId = "conversation-2"),
            ),
            backStack,
        )
    }

    @Test
    fun resetBackStack_keepsSingleMatchingDestinationUntouched() {
        val backStack = mutableListOf<NavKey>(NewChatNavKey)

        reducer.resetBackStack(
            backStack = backStack,
            destination = NewChatNavKey,
        )

        assertEquals(listOf(NewChatNavKey), backStack)
    }

    @Test
    fun resetBackStack_replacesExistingEntriesWithDestination() {
        val backStack = mutableListOf<NavKey>(
            NewChatNavKey,
            ConversationNavKey(conversationId = "conversation-1"),
        )

        reducer.resetBackStack(
            backStack = backStack,
            destination = ConversationNavKey(conversationId = "conversation-2"),
        )

        assertEquals(
            listOf(
                ConversationNavKey(conversationId = "conversation-2"),
            ),
            backStack,
        )
    }
}
