package com.android.messaging.data.conversation.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.database.MatrixCursor
import android.net.Uri
import app.cash.turbine.test
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ParticipantData
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationParticipantsRepositoryImplTest {

    private lateinit var contentResolver: ContentResolver

    @Before
    fun setUp() {
        contentResolver = mockk()
    }

    @Test
    fun getParticipants_registersAndUnregistersObserverForCollection() = runTest {
        val registeredObserver = slot<ContentObserver>()
        val expectedUri = MessagingContentProvider.buildConversationParticipantsUri(
            CONVERSATION_ID,
        )
        val repository = createRepository()

        stubObserverRegistration(
            expectedUri = expectedUri,
            registeredObserver = registeredObserver,
        )
        every {
            contentResolver.query(
                expectedUri,
                ParticipantData.ParticipantsQuery.PROJECTION,
                null,
                null,
                null,
            )
        } returns createParticipantsCursor()

        repository.getParticipants(conversationId = CONVERSATION_ID).test {
            assertEquals(emptyList<ConversationRecipient>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 1) {
            contentResolver.registerContentObserver(
                expectedUri,
                true,
                registeredObserver.captured,
            )
        }
        verify(exactly = 1) {
            contentResolver.unregisterContentObserver(registeredObserver.captured)
        }
    }

    @Test
    fun getParticipants_mapsRowsAndFiltersSelfBlankAndDuplicateDestinations() = runTest {
        val registeredObserver = slot<ContentObserver>()
        val expectedUri = MessagingContentProvider.buildConversationParticipantsUri(
            CONVERSATION_ID,
        )
        val repository = createRepository()

        stubObserverRegistration(
            expectedUri = expectedUri,
            registeredObserver = registeredObserver,
        )
        every {
            contentResolver.query(
                expectedUri,
                ParticipantData.ParticipantsQuery.PROJECTION,
                null,
                null,
                null,
            )
        } returns createParticipantsCursor(
            participantRow(
                id = "self-1",
                subId = ParticipantData.DEFAULT_SELF_SUB_ID,
                sendDestination = "+1 555 0000",
                displayDestination = "+1 555 0000",
                fullName = "Self",
            ),
            participantRow(
                id = "1",
                sendDestination = "+1 555 0100",
                displayDestination = "+1 555 0100",
                fullName = "Ada",
                photoUri = "content://photos/1",
            ),
            participantRow(
                id = "2",
                sendDestination = "   ",
                displayDestination = "   ",
                fullName = "Ignored",
            ),
            participantRow(
                id = "3",
                sendDestination = "+1 555 0100",
                displayDestination = "+1 555 0100",
                fullName = "Ada Duplicate",
            ),
            participantRow(
                id = "4",
                sendDestination = "+1 555 0101",
                displayDestination = "Bob",
                fullName = "Bob",
            ),
        )

        repository.getParticipants(conversationId = CONVERSATION_ID).test {
            assertEquals(
                listOf(
                    ConversationRecipient(
                        id = "1",
                        displayName = "Ada",
                        destination = "+1 555 0100",
                        photoUri = "content://photos/1",
                        secondaryText = "+1 555 0100",
                    ),
                    ConversationRecipient(
                        id = "4",
                        displayName = "Bob",
                        destination = "+1 555 0101",
                        secondaryText = null,
                    ),
                ),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getParticipants_requeriesWhenConversationParticipantsChange() = runTest {
        val registeredObserver = slot<ContentObserver>()
        val expectedUri = MessagingContentProvider.buildConversationParticipantsUri(
            CONVERSATION_ID,
        )
        val repository = createRepository()
        var currentCursor = createParticipantsCursor(
            participantRow(
                id = "1",
                sendDestination = "+1 555 0100",
                displayDestination = "+1 555 0100",
                fullName = "Ada",
            ),
        )

        stubObserverRegistration(
            expectedUri = expectedUri,
            registeredObserver = registeredObserver,
        )
        every {
            contentResolver.query(
                expectedUri,
                ParticipantData.ParticipantsQuery.PROJECTION,
                null,
                null,
                null,
            )
        } answers {
            currentCursor
        }

        repository.getParticipants(conversationId = CONVERSATION_ID).test {
            assertEquals(
                listOf(
                    ConversationRecipient(
                        id = "1",
                        displayName = "Ada",
                        destination = "+1 555 0100",
                        secondaryText = "+1 555 0100",
                    ),
                ),
                awaitItem(),
            )

            currentCursor = createParticipantsCursor(
                participantRow(
                    id = "2",
                    sendDestination = "+1 555 0101",
                    displayDestination = "+1 555 0101",
                    fullName = "Bob",
                ),
            )
            registeredObserver.captured.onChange(false)

            assertEquals(
                listOf(
                    ConversationRecipient(
                        id = "2",
                        displayName = "Bob",
                        destination = "+1 555 0101",
                        secondaryText = "+1 555 0101",
                    ),
                ),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createRepository(): ConversationParticipantsRepositoryImpl {
        return ConversationParticipantsRepositoryImpl(
            contentResolver = contentResolver,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    private fun stubObserverRegistration(
        expectedUri: Uri,
        registeredObserver: io.mockk.CapturingSlot<ContentObserver>,
    ) {
        every {
            contentResolver.registerContentObserver(
                expectedUri,
                true,
                capture(registeredObserver),
            )
        } just runs
        every { contentResolver.unregisterContentObserver(any()) } just runs
    }

    private fun createParticipantsCursor(vararg rows: Array<Any?>): MatrixCursor {
        return MatrixCursor(
            ParticipantData.ParticipantsQuery.PROJECTION,
        ).apply {
            rows.forEach { row ->
                addRow(row)
            }
        }
    }

    private fun participantRow(
        id: String,
        subId: Int = ParticipantData.OTHER_THAN_SELF_SUB_ID,
        sendDestination: String,
        displayDestination: String,
        fullName: String,
        photoUri: String? = null,
    ): Array<Any?> {
        return arrayOf(
            id,
            subId,
            ParticipantData.INVALID_SLOT_ID,
            sendDestination.trim(),
            sendDestination,
            displayDestination,
            fullName,
            null,
            photoUri,
            1L,
            "lookup-$id",
            0,
            0,
            null,
            sendDestination,
        )
    }

    private companion object {
        private const val CONVERSATION_ID = "conversation-1"
    }
}
