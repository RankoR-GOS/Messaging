package com.android.messaging.domain.conversation.usecase

import app.cash.turbine.test
import com.android.messaging.data.conversation.mapper.ConversationDraftMessageDataMapper
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.datamodel.action.InsertNewMessageAction
import com.android.messaging.datamodel.data.MessageData
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SendConversationDraftImplTest {

    @Before
    fun setUp() {
        unmockkAll()
        mockkStatic(InsertNewMessageAction::class)
        every { InsertNewMessageAction.insertNewMessage(any()) } just runs
    }

    @Test(expected = BlankConversationIdException::class)
    fun invoke_throwsWhenConversationIdIsBlank() {
        createUseCase().invoke(
            conversationId = " ",
            draft = ConversationDraft(
                messageText = "Hello",
            ),
        )
    }

    @Test(expected = EmptyConversationDraftException::class)
    fun invoke_throwsWhenDraftIsEmpty() {
        createUseCase().invoke(
            conversationId = "conversation-1",
            draft = ConversationDraft(),
        )
    }

    @Test
    fun invoke_isColdUntilCollected() {
        val messageData = createMessageData()
        val mapper = createConversationDraftMessageDataMapperMock(
            messageToReturn = messageData,
        )
        val useCase = createUseCase(
            mapper = mapper,
        )

        useCase.invoke(
            conversationId = "conversation-1",
            draft = ConversationDraft(
                messageText = "Hello",
            ),
        )

        verify(exactly = 0) {
            mapper.map(any(), any())
        }
        verify(exactly = 0) {
            InsertNewMessageAction.insertNewMessage(any())
        }
    }

    @Test
    fun invoke_mapsDraftAndSendsMessageOnCollection() {
        runTest {
            val messageData = createMessageData()
            val mapper = createConversationDraftMessageDataMapperMock(
                messageToReturn = messageData,
            )
            val draft = ConversationDraft(
                messageText = "Hello",
            )
            val useCase = createUseCase(
                mapper = mapper,
                dispatcher = StandardTestDispatcher(scheduler = testScheduler),
            )

            useCase.invoke(
                conversationId = "conversation-1",
                draft = draft,
            ).test {
                assertEquals(Unit, awaitItem())
                awaitComplete()
            }

            verify(exactly = 1) {
                mapper.map(
                    conversationId = "conversation-1",
                    draft = draft,
                )
            }
            verify(exactly = 1) {
                InsertNewMessageAction.insertNewMessage(messageData)
            }
        }
    }

    @Test
    fun invoke_wrapsMapperFailures() {
        runTest {
            val mapper = createConversationDraftMessageDataMapperMock(
                failure = IllegalStateException("mapper failure"),
            )
            val useCase = createUseCase(mapper = mapper)

            var exception: Throwable? = null
            try {
                useCase.invoke(
                    conversationId = "conversation-1",
                    draft = ConversationDraft(
                        messageText = "Hello",
                    ),
                ).collect()
            } catch (thrown: Throwable) {
                exception = thrown
            }

            assertEquals(DraftDispatchFailedException::class.java, exception?.javaClass)
            assertEquals("mapper failure", exception?.cause?.message)
        }
    }

    @Test
    fun invoke_wrapsSenderFailures() {
        runTest {
            every {
                InsertNewMessageAction.insertNewMessage(any())
            } throws IllegalStateException("sender failure")
            val useCase = createUseCase()

            var exception: Throwable? = null
            try {
                useCase.invoke(
                    conversationId = "conversation-1",
                    draft = ConversationDraft(
                        messageText = "Hello",
                    ),
                ).collect()
            } catch (thrown: Throwable) {
                exception = thrown
            }

            assertEquals(DraftDispatchFailedException::class.java, exception?.javaClass)
            assertEquals("sender failure", exception?.cause?.message)
        }
    }

    @Test
    fun invoke_rethrowsCancellation() {
        runTest {
            val cancellationException = CancellationException("cancelled")
            every {
                InsertNewMessageAction.insertNewMessage(any())
            } throws cancellationException
            val useCase = createUseCase()

            var exception: Throwable? = null
            try {
                useCase.invoke(
                    conversationId = "conversation-1",
                    draft = ConversationDraft(
                        messageText = "Hello",
                    ),
                ).collect()
            } catch (thrown: Throwable) {
                exception = thrown
            }

            assertSame(cancellationException, exception)
        }
    }

    private fun createUseCase(
        mapper: ConversationDraftMessageDataMapper = createConversationDraftMessageDataMapperMock(),
        dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
    ): SendConversationDraftImpl {
        return SendConversationDraftImpl(
            conversationDraftMessageDataMapper = mapper,
            defaultDispatcher = dispatcher,
        )
    }

    private fun createConversationDraftMessageDataMapperMock(
        messageToReturn: MessageData = createMessageData(),
        failure: Exception? = null,
    ): ConversationDraftMessageDataMapper {
        val mapper = mockk<ConversationDraftMessageDataMapper>()
        every {
            mapper.map(any(), any())
        } answers {
            failure?.let { exception ->
                throw exception
            }
            messageToReturn
        }
        return mapper
    }

    private fun createMessageData(): MessageData {
        return MessageData.createDraftSmsMessage(
            "conversation-1",
            "self-1",
            "Hello",
        )
    }
}
