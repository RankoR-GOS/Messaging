package com.android.messaging.domain.conversation.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.messaging.datamodel.action.GetOrCreateConversationAction
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.domain.conversation.usecase.model.ResolveConversationIdResult
import com.android.messaging.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ResolveConversationIdImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        unmockkAll()
        mockkStatic(GetOrCreateConversationAction::class)
    }

    @Test
    fun invoke_returnsEmptyDestinationsWhenAllRecipientsAreBlank() = runTest {
        val useCase = createUseCase()

        val result = useCase.invoke(
            destinations = listOf(" ", "\n"),
        )

        assertEquals(ResolveConversationIdResult.EmptyDestinations, result)
        verify(exactly = 0) {
            GetOrCreateConversationAction.getOrCreateConversation(
                any<ArrayList<ParticipantData>>(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun invoke_trimsDestinationsAndReturnsResolvedConversation() = runTest {
        val capturedParticipants = slot<ArrayList<ParticipantData>>()
        val capturedListener =
            slot<GetOrCreateConversationAction.GetOrCreateConversationActionListener>()
        val monitor = mockk<GetOrCreateConversationAction.GetOrCreateConversationActionMonitor>(
            relaxed = true,
        )
        every {
            GetOrCreateConversationAction.getOrCreateConversation(
                capture(capturedParticipants),
                any(),
                capture(capturedListener),
            )
        } answers {
            capturedListener.captured.onGetOrCreateConversationSucceeded(
                monitor,
                null,
                "conversation-123",
            )
            monitor
        }
        val useCase = createUseCase()

        val result = useCase.invoke(
            destinations = listOf(" 123 ", " alice@example.com ", ""),
        )

        assertEquals(
            ResolveConversationIdResult.Resolved(
                conversationId = "conversation-123",
            ),
            result,
        )
        assertEquals(2, capturedParticipants.captured.size)
        assertEquals("123", capturedParticipants.captured[0].sendDestination)
        assertEquals("alice@example.com", capturedParticipants.captured[1].sendDestination)
        verify(exactly = 1) {
            GetOrCreateConversationAction.getOrCreateConversation(
                any<ArrayList<ParticipantData>>(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun invoke_returnsNotResolvedWhenActionFails() = runTest {
        val capturedListener =
            slot<GetOrCreateConversationAction.GetOrCreateConversationActionListener>()
        val monitor = mockk<GetOrCreateConversationAction.GetOrCreateConversationActionMonitor>(
            relaxed = true,
        )
        every {
            GetOrCreateConversationAction.getOrCreateConversation(
                any<ArrayList<ParticipantData>>(),
                any(),
                capture(capturedListener),
            )
        } answers {
            capturedListener.captured.onGetOrCreateConversationFailed(
                monitor,
                null,
            )
            monitor
        }
        val useCase = createUseCase()

        val result = useCase.invoke(
            destinations = listOf("123"),
        )

        assertEquals(ResolveConversationIdResult.NotResolved, result)
        verify(exactly = 1) {
            GetOrCreateConversationAction.getOrCreateConversation(
                any<ArrayList<ParticipantData>>(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun invoke_unregistersActionMonitorWhenCancelled() = runTest {
        val monitor = mockk<GetOrCreateConversationAction.GetOrCreateConversationActionMonitor>()
        every {
            monitor.unregister()
        } just runs
        every {
            GetOrCreateConversationAction.getOrCreateConversation(
                any<ArrayList<ParticipantData>>(),
                any(),
                any(),
            )
        } returns monitor
        val useCase = createUseCase()

        val deferred = async {
            useCase.invoke(
                destinations = listOf("123"),
            )
        }
        advanceUntilIdle()
        deferred.cancel()
        advanceUntilIdle()

        verify(exactly = 1) {
            monitor.unregister()
        }
    }

    private fun createUseCase(): ResolveConversationIdImpl {
        return ResolveConversationIdImpl(
            mainDispatcher = mainDispatcherRule.testDispatcher,
        )
    }
}
