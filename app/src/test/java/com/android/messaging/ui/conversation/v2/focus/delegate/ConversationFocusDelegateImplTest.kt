package com.android.messaging.ui.conversation.v2.focus.delegate

import com.android.messaging.datamodel.BugleNotifications
import com.android.messaging.datamodel.DataModel
import com.android.messaging.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationFocusDelegateImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher = UnconfinedTestDispatcher())

    private lateinit var dataModel: DataModel

    @Before
    fun setUp() {
        dataModel = mockk(relaxed = true)
        mockkStatic(DataModel::class)
        every { DataModel.get() } returns dataModel
        mockkStatic(BugleNotifications::class)
        every {
            BugleNotifications.markMessagesAsRead(any(), any())
        } just runs
    }

    @After
    fun tearDown() {
        unmockkStatic(DataModel::class)
        unmockkStatic(BugleNotifications::class)
    }

    @Test
    fun setScreenFocused_withConversationId_setsFocusAndMarksRead() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val conversationIdFlow = MutableStateFlow<String?>(value = "conversation-1")
        val delegate = ConversationFocusDelegateImpl(
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )
        delegate.bind(scope = backgroundScope, conversationIdFlow = conversationIdFlow)

        delegate.setScreenFocused(focused = true)
        advanceUntilIdle()

        verify(exactly = 1) {
            dataModel.setFocusedConversation("conversation-1")
        }
        verify(exactly = 1) {
            BugleNotifications.markMessagesAsRead(
                "conversation-1",
                true,
            )
        }
    }

    @Test
    fun setScreenFocused_withCancelNotificationFalse_propagatesFlag() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val conversationIdFlow = MutableStateFlow<String?>(value = "conversation-1")
        val delegate = ConversationFocusDelegateImpl(
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )
        delegate.bind(scope = backgroundScope, conversationIdFlow = conversationIdFlow)

        delegate.setScreenFocused(focused = true, cancelNotification = false)
        advanceUntilIdle()

        verify(exactly = 1) {
            BugleNotifications.markMessagesAsRead(
                "conversation-1",
                false,
            )
        }
    }

    @Test
    fun setScreenFocused_withNullConversationId_doesNotMarkRead() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val conversationIdFlow = MutableStateFlow<String?>(value = null)
        val delegate = ConversationFocusDelegateImpl(
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )
        delegate.bind(scope = backgroundScope, conversationIdFlow = conversationIdFlow)

        delegate.setScreenFocused(focused = true)
        advanceUntilIdle()

        verify(exactly = 0) {
            BugleNotifications.markMessagesAsRead(any(), any())
        }
        verify(exactly = 0) {
            dataModel.setFocusedConversation(match { id -> id != null })
        }
    }

    @Test
    fun setScreenFocused_withBlankConversationId_doesNotMarkRead() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val conversationIdFlow = MutableStateFlow<String?>(value = "   ")
        val delegate = ConversationFocusDelegateImpl(
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )
        delegate.bind(scope = backgroundScope, conversationIdFlow = conversationIdFlow)

        delegate.setScreenFocused(focused = true)
        advanceUntilIdle()

        verify(exactly = 0) {
            BugleNotifications.markMessagesAsRead(any(), any())
        }
    }

    @Test
    fun setScreenFocused_unfocused_clearsFocusedConversation() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val conversationIdFlow = MutableStateFlow<String?>(value = "conversation-1")
        val delegate = ConversationFocusDelegateImpl(
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )
        delegate.bind(scope = backgroundScope, conversationIdFlow = conversationIdFlow)

        delegate.setScreenFocused(focused = true)
        advanceUntilIdle()
        delegate.setScreenFocused(focused = false)
        advanceUntilIdle()

        verifyOrder {
            dataModel.setFocusedConversation("conversation-1")
            dataModel.setFocusedConversation(null)
        }
    }

    @Test
    fun conversationIdSwap_whileFocused_marksReadForNewConversation() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val conversationIdFlow = MutableStateFlow<String?>(value = "conversation-1")
        val delegate = ConversationFocusDelegateImpl(
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )
        delegate.bind(scope = backgroundScope, conversationIdFlow = conversationIdFlow)

        delegate.setScreenFocused(focused = true)
        advanceUntilIdle()
        conversationIdFlow.value = "conversation-2"
        advanceUntilIdle()

        verify(exactly = 1) {
            BugleNotifications.markMessagesAsRead("conversation-1", true)
        }
        verify(exactly = 1) {
            BugleNotifications.markMessagesAsRead("conversation-2", true)
        }
        verify(exactly = 1) {
            dataModel.setFocusedConversation("conversation-2")
        }
    }

    @Test
    fun setScreenFocused_repeatedFocusedRequests_collapsedByDistinctUntilChanged() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val conversationIdFlow = MutableStateFlow<String?>(value = "conversation-1")
        val delegate = ConversationFocusDelegateImpl(
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )
        delegate.bind(scope = backgroundScope, conversationIdFlow = conversationIdFlow)

        delegate.setScreenFocused(focused = true)
        delegate.setScreenFocused(focused = true)
        delegate.setScreenFocused(focused = true)
        advanceUntilIdle()

        verify(exactly = 1) {
            BugleNotifications.markMessagesAsRead("conversation-1", true)
        }
    }

    @Test
    fun bind_calledTwice_onlyBindsFirstScope() = runTest(
        context = mainDispatcherRule.testDispatcher,
    ) {
        val conversationIdFlow = MutableStateFlow<String?>(value = "conversation-1")
        val secondConversationIdFlow = MutableStateFlow<String?>(value = "conversation-rebound")
        val delegate = ConversationFocusDelegateImpl(
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )
        delegate.bind(scope = backgroundScope, conversationIdFlow = conversationIdFlow)
        delegate.bind(scope = backgroundScope, conversationIdFlow = secondConversationIdFlow)

        delegate.setScreenFocused(focused = true)
        advanceUntilIdle()

        verify(exactly = 1) {
            BugleNotifications.markMessagesAsRead("conversation-1", true)
        }
        verify(exactly = 0) {
            BugleNotifications.markMessagesAsRead("conversation-rebound", any())
        }
    }
}
