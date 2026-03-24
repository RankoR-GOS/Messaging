package com.android.messaging.data.conversation.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import app.cash.turbine.test
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ConversationListItemData
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.datamodel.data.MessageData
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationsRepositoryImplTest {

    private lateinit var contentResolver: ContentResolver

    @Before
    fun setUp() {
        unmockkAll()
        clearAllMocks()
        contentResolver = mockk()
    }

    @Test
    fun getConversationMessages_registersAndUnregistersObserverForCollection() = runTest {
        val registeredObservers = mutableListOf<ContentObserver>()
        val capturedProjections = mutableListOf<Array<String>?>()
        val repository = createRepository(
            testDispatcher = UnconfinedTestDispatcher(scheduler = testScheduler),
        )
        val expectedUri = MessagingContentProvider.buildConversationMessagesUri(CONVERSATION_ID)

        stubObserverRegistration(
            registeredObservers = registeredObservers,
            expectedUri = expectedUri,
        )
        stubQuery(
            expectedUri = expectedUri,
            capturedProjections = capturedProjections,
            result = createConversationMessagesCursor(rows = emptyList()),
        )

        repository.getConversationMessages(conversationId = CONVERSATION_ID).test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 1) {
            contentResolver.registerContentObserver(expectedUri, true, registeredObservers.single())
        }
        verify(exactly = 1) {
            contentResolver.unregisterContentObserver(registeredObservers.single())
        }
        assertEquals(ConversationMessageData.getProjection().toList(), capturedProjections.single()?.toList())
    }

    @Test
    fun getConversationMetadata_registersAndUnregistersObserverForCollection() = runTest {
        val registeredObservers = mutableListOf<ContentObserver>()
        val capturedProjections = mutableListOf<Array<String>?>()
        val repository = createRepository(
            testDispatcher = UnconfinedTestDispatcher(scheduler = testScheduler),
        )
        val expectedUri = MessagingContentProvider.buildConversationMetadataUri(CONVERSATION_ID)

        stubObserverRegistration(
            registeredObservers = registeredObservers,
            expectedUri = expectedUri,
        )
        stubQuery(
            expectedUri = expectedUri,
            capturedProjections = capturedProjections,
            result = createConversationMetadataCursor(
                row = conversationMetadataRow(
                    conversationName = "Weekend plan",
                    selfParticipantId = "self-1",
                    participantCount = 3,
                ),
            ),
        )

        repository.getConversationMetadata(conversationId = CONVERSATION_ID).test {
            assertEquals("Weekend plan", awaitItem()?.conversationName)
            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 1) {
            contentResolver.registerContentObserver(expectedUri, true, registeredObservers.single())
        }
        verify(exactly = 1) {
            contentResolver.unregisterContentObserver(registeredObservers.single())
        }
        assertEquals(ConversationListItemData.PROJECTION.toList(), capturedProjections.single()?.toList())
    }

    @Test
    fun getConversationMetadata_emitsMappedMetadata() = runTest {
        val registeredObservers = mutableListOf<ContentObserver>()
        val capturedProjections = mutableListOf<Array<String>?>()
        val repository = createRepository(
            testDispatcher = UnconfinedTestDispatcher(scheduler = testScheduler),
        )
        val expectedUri = MessagingContentProvider.buildConversationMetadataUri(CONVERSATION_ID)

        stubObserverRegistration(
            registeredObservers = registeredObservers,
            expectedUri = expectedUri,
        )
        stubQuery(
            expectedUri = expectedUri,
            capturedProjections = capturedProjections,
            result = createConversationMetadataCursor(
                row = conversationMetadataRow(
                    conversationName = "Carol, Dave, Erin",
                    selfParticipantId = "self-2",
                    participantCount = 3,
                ),
            ),
        )

        repository.getConversationMetadata(conversationId = CONVERSATION_ID).test {
            val metadata = awaitItem()

            assertEquals("Carol, Dave, Erin", metadata?.conversationName)
            assertEquals("self-2", metadata?.selfParticipantId)
            assertEquals(true, metadata?.isGroupConversation)
            assertEquals(3, metadata?.participantCount)

            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(ConversationListItemData.PROJECTION.toList(), capturedProjections.single()?.toList())
    }

    @Test
    fun getConversationMetadata_returnsNullWhenCursorIsEmpty() = runTest {
        val registeredObservers = mutableListOf<ContentObserver>()
        val capturedProjections = mutableListOf<Array<String>?>()
        val repository = createRepository(
            testDispatcher = UnconfinedTestDispatcher(scheduler = testScheduler),
        )
        val expectedUri = MessagingContentProvider.buildConversationMetadataUri(CONVERSATION_ID)

        stubObserverRegistration(
            registeredObservers = registeredObservers,
            expectedUri = expectedUri,
        )
        stubQuery(
            expectedUri = expectedUri,
            capturedProjections = capturedProjections,
            result = createConversationMetadataCursor(row = null),
        )

        repository.getConversationMetadata(conversationId = CONVERSATION_ID).test {
            assertEquals(null, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(ConversationListItemData.PROJECTION.toList(), capturedProjections.single()?.toList())
    }

    @Test
    fun getConversationMessages_emitsMessagesInUiOrderWithLegacyClusteringRules() = runTest {
        val registeredObservers = mutableListOf<ContentObserver>()
        val capturedProjections = mutableListOf<Array<String>?>()
        val repository = createRepository(
            testDispatcher = UnconfinedTestDispatcher(scheduler = testScheduler),
        )
        val expectedUri = MessagingContentProvider.buildConversationMessagesUri(CONVERSATION_ID)
        val messagesInUiOrder = listOf(
            messageRow(
                messageId = "pair-1",
                participantId = "participant-a",
                selfParticipantId = "self-1",
                receivedTimestamp = 0L,
                status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                text = "Pair top",
            ),
            messageRow(
                messageId = "pair-2",
                participantId = "participant-a",
                selfParticipantId = "self-1",
                receivedTimestamp = 30_000L,
                status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                text = "Pair bottom",
            ),
            messageRow(
                messageId = "gap-break",
                participantId = "participant-a",
                selfParticipantId = "self-1",
                receivedTimestamp = 91_000L,
                status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                text = "Gap break",
            ),
            messageRow(
                messageId = "sender-break",
                participantId = "participant-b",
                selfParticipantId = "self-1",
                receivedTimestamp = 100_000L,
                status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                text = "Different sender",
            ),
            messageRow(
                messageId = "outgoing-1",
                participantId = "self-sender",
                selfParticipantId = "self-1",
                receivedTimestamp = 130_000L,
                status = MessageData.BUGLE_STATUS_OUTGOING_COMPLETE,
                text = "Outgoing top",
            ),
            messageRow(
                messageId = "outgoing-2",
                participantId = "self-sender",
                selfParticipantId = "self-1",
                receivedTimestamp = 150_000L,
                status = MessageData.BUGLE_STATUS_OUTGOING_COMPLETE,
                text = "Outgoing bottom",
            ),
            messageRow(
                messageId = "self-break",
                participantId = "self-sender",
                selfParticipantId = "self-2",
                receivedTimestamp = 170_000L,
                status = MessageData.BUGLE_STATUS_OUTGOING_COMPLETE,
                text = "Different self participant",
            ),
        )

        stubObserverRegistration(
            registeredObservers = registeredObservers,
            expectedUri = expectedUri,
        )
        stubQuery(
            expectedUri = expectedUri,
            capturedProjections = capturedProjections,
            result = createConversationMessagesCursor(rows = messagesInUiOrder.asReversed()),
        )

        repository.getConversationMessages(conversationId = CONVERSATION_ID).test {
            val messages = awaitItem()

            assertEquals(
                messagesInUiOrder.map { it.messageId },
                messages.map { it.messageId },
            )
            assertEquals(
                messagesInUiOrder.map { it.text },
                messages.map { it.text },
            )

            assertClusterState(
                message = messages[0],
                canClusterWithPrevious = false,
                canClusterWithNext = true,
            )
            assertClusterState(
                message = messages[1],
                canClusterWithPrevious = true,
                canClusterWithNext = false,
            )
            assertClusterState(
                message = messages[2],
                canClusterWithPrevious = false,
                canClusterWithNext = false,
            )
            assertClusterState(
                message = messages[3],
                canClusterWithPrevious = false,
                canClusterWithNext = false,
            )
            assertClusterState(
                message = messages[4],
                canClusterWithPrevious = false,
                canClusterWithNext = true,
            )
            assertClusterState(
                message = messages[5],
                canClusterWithPrevious = true,
                canClusterWithNext = false,
            )
            assertClusterState(
                message = messages[6],
                canClusterWithPrevious = false,
                canClusterWithNext = false,
            )

            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 1) { contentResolver.query(expectedUri, any(), null, null, null) }
        assertEquals(ConversationMessageData.getProjection().toList(), capturedProjections.single()?.toList())
    }

    @Test
    fun getConversationMessages_requeriesWhenObserverChanges() = runTest {
        val registeredObservers = mutableListOf<ContentObserver>()
        val capturedProjections = mutableListOf<Array<String>?>()
        val repository = createRepository(
            testDispatcher = UnconfinedTestDispatcher(scheduler = testScheduler),
        )
        val expectedUri = MessagingContentProvider.buildConversationMessagesUri(CONVERSATION_ID)
        val firstMessage = messageRow(
            messageId = "first",
            participantId = "participant-a",
            selfParticipantId = "self-1",
            receivedTimestamp = 1_000L,
            status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
            text = "First",
        )
        val secondMessage = messageRow(
            messageId = "second",
            participantId = "participant-a",
            selfParticipantId = "self-1",
            receivedTimestamp = 2_000L,
            status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
            text = "Second",
        )

        stubObserverRegistration(
            registeredObservers = registeredObservers,
            expectedUri = expectedUri,
        )
        every {
            contentResolver.query(
                expectedUri,
                any(),
                null,
                null,
                null,
            )
        } answers {
            capturedProjections.add(secondArg<Array<String>?>())
            when (capturedProjections.size) {
                1 -> createConversationMessagesCursor(rows = listOf(firstMessage))
                2 -> createConversationMessagesCursor(rows = listOf(secondMessage, firstMessage))
                else -> error("Unexpected query call ${capturedProjections.size}")
            }
        }

        repository.getConversationMessages(conversationId = CONVERSATION_ID).test {
            assertEquals(listOf("first"), awaitItem().map { it.messageId })

            registeredObservers.single().onChange(false)

            assertEquals(
                listOf("first", "second"),
                awaitItem().map { it.messageId },
            )

            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 2) {
            contentResolver.query(
                expectedUri,
                any(),
                null,
                null,
                null,
            )
        }
        assertEquals(
            listOf(
                ConversationMessageData.getProjection().toList(),
                ConversationMessageData.getProjection().toList(),
            ),
            capturedProjections.map { it?.toList() },
        )
    }

    @Test
    fun getConversationMessages_singleMessageHasNoClustering() = runTest {
        val registeredObservers = mutableListOf<ContentObserver>()
        val capturedProjections = mutableListOf<Array<String>?>()
        val repository = createRepository(
            testDispatcher = UnconfinedTestDispatcher(scheduler = testScheduler),
        )
        val expectedUri = MessagingContentProvider.buildConversationMessagesUri(CONVERSATION_ID)
        val singleMessage = messageRow(
            messageId = "only",
            participantId = "participant-a",
            selfParticipantId = "self-1",
            receivedTimestamp = 1_000L,
            status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
            text = "Only message",
        )

        stubObserverRegistration(
            registeredObservers = registeredObservers,
            expectedUri = expectedUri,
        )
        stubQuery(
            expectedUri = expectedUri,
            capturedProjections = capturedProjections,
            result = createConversationMessagesCursor(rows = listOf(singleMessage)),
        )

        repository.getConversationMessages(conversationId = CONVERSATION_ID).test {
            val messages = awaitItem()

            assertEquals(1, messages.size)
            assertEquals("only", messages[0].messageId)
            assertClusterState(
                message = messages[0],
                canClusterWithPrevious = false,
                canClusterWithNext = false,
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getConversationMessages_clustersAtExactlyOneMinuteButNotOneMillisOver() = runTest {
        val registeredObservers = mutableListOf<ContentObserver>()
        val capturedProjections = mutableListOf<Array<String>?>()
        val repository = createRepository(
            testDispatcher = UnconfinedTestDispatcher(scheduler = testScheduler),
        )
        val expectedUri = MessagingContentProvider.buildConversationMessagesUri(CONVERSATION_ID)
        val messagesInUiOrder = listOf(
            messageRow(
                messageId = "msg-a",
                participantId = "participant-a",
                selfParticipantId = "self-1",
                receivedTimestamp = 0L,
                status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                text = "First",
            ),
            messageRow(
                messageId = "msg-b",
                participantId = "participant-a",
                selfParticipantId = "self-1",
                receivedTimestamp = 60_000L,
                status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                text = "Exactly one minute later",
            ),
            messageRow(
                messageId = "msg-c",
                participantId = "participant-a",
                selfParticipantId = "self-1",
                receivedTimestamp = 120_001L,
                status = MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                text = "One millisecond over one minute from msg-b",
            ),
        )

        stubObserverRegistration(
            registeredObservers = registeredObservers,
            expectedUri = expectedUri,
        )
        stubQuery(
            expectedUri = expectedUri,
            capturedProjections = capturedProjections,
            result = createConversationMessagesCursor(rows = messagesInUiOrder.asReversed()),
        )

        repository.getConversationMessages(conversationId = CONVERSATION_ID).test {
            val messages = awaitItem()

            assertEquals(3, messages.size)

            // msg-a → msg-b: exactly 60,000ms apart — clusters (condition is strictly >)
            assertClusterState(
                message = messages[0],
                canClusterWithPrevious = false,
                canClusterWithNext = true,
            )
            assertClusterState(
                message = messages[1],
                canClusterWithPrevious = true,
                canClusterWithNext = false,
            )
            // msg-b → msg-c: 60,001ms apart — does NOT cluster
            assertClusterState(
                message = messages[2],
                canClusterWithPrevious = false,
                canClusterWithNext = false,
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getConversationMessages_returnsEmptyListWhenQueryReturnsNull() = runTest {
        val registeredObservers = mutableListOf<ContentObserver>()
        val capturedProjections = mutableListOf<Array<String>?>()
        val repository = createRepository(
            testDispatcher = UnconfinedTestDispatcher(scheduler = testScheduler),
        )
        val expectedUri = MessagingContentProvider.buildConversationMessagesUri(CONVERSATION_ID)

        stubObserverRegistration(
            registeredObservers = registeredObservers,
            expectedUri = expectedUri,
        )
        stubQuery(
            expectedUri = expectedUri,
            capturedProjections = capturedProjections,
            result = null,
        )

        repository.getConversationMessages(conversationId = CONVERSATION_ID).test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(ConversationMessageData.getProjection().toList(), capturedProjections.single()?.toList())
    }

    private fun createRepository(testDispatcher: TestDispatcher): ConversationsRepositoryImpl {
        return ConversationsRepositoryImpl(
            contentResolver = contentResolver,
            defaultDispatcher = testDispatcher,
            ioDispatcher = testDispatcher,
        )
    }

    private fun stubObserverRegistration(
        registeredObservers: MutableList<ContentObserver>,
        expectedUri: Uri,
    ) {
        every {
            contentResolver.registerContentObserver(
                expectedUri,
                true,
                any(),
            )
        } answers {
            registeredObservers.add(thirdArg<ContentObserver>())
        }
        every { contentResolver.unregisterContentObserver(any()) } just runs
    }

    private fun stubQuery(
        expectedUri: Uri,
        capturedProjections: MutableList<Array<String>?>,
        result: Cursor?,
    ) {
        every {
            contentResolver.query(
                expectedUri,
                any(),
                null,
                null,
                null,
            )
        } answers {
            capturedProjections.add(secondArg<Array<String>?>())
            result
        }
    }

    private fun createConversationMessagesCursor(rows: List<TestMessageRow>): Cursor {
        val projection = ConversationMessageData.getProjection()
        val rowsByColumn = rows.map { it.toColumnValues() }
        var position = -1
        val cursor = mockk<Cursor>()

        fun currentRow(): Map<String, Any?> {
            check(position in rowsByColumn.indices) {
                "Cursor position $position is out of bounds for ${rowsByColumn.size} rows"
            }
            return rowsByColumn[position]
        }

        fun moveToPosition(positionToMove: Int): Boolean {
            return when {
                rowsByColumn.isEmpty() -> {
                    position = -1
                    false
                }

                positionToMove < 0 -> {
                    position = -1
                    false
                }

                positionToMove >= rowsByColumn.size -> {
                    position = rowsByColumn.size
                    false
                }

                else -> {
                    position = positionToMove
                    true
                }
            }
        }

        every { cursor.count } returns rowsByColumn.size
        every { cursor.close() } just runs
        every { cursor.getPosition() } answers { position }
        every { cursor.isBeforeFirst } answers {
            rowsByColumn.isNotEmpty() && position < 0
        }
        every { cursor.isAfterLast } answers {
            rowsByColumn.isNotEmpty() && position >= rowsByColumn.size
        }
        every { cursor.isFirst } answers {
            rowsByColumn.isNotEmpty() && position == 0
        }
        every { cursor.isLast } answers {
            rowsByColumn.isNotEmpty() && position == rowsByColumn.lastIndex
        }
        every { cursor.moveToPosition(any()) } answers {
            moveToPosition(positionToMove = firstArg())
        }
        every { cursor.move(any()) } answers {
            moveToPosition(positionToMove = position + firstArg<Int>())
        }
        every { cursor.moveToFirst() } answers {
            moveToPosition(positionToMove = 0)
        }
        every { cursor.moveToLast() } answers {
            moveToPosition(positionToMove = rowsByColumn.lastIndex)
        }
        every { cursor.moveToNext() } answers {
            val nextPosition = if (position < 0) 0 else position + 1
            moveToPosition(positionToMove = nextPosition)
        }
        every { cursor.moveToPrevious() } answers {
            val previousPosition = if (position > rowsByColumn.lastIndex) {
                rowsByColumn.lastIndex
            } else {
                position - 1
            }
            moveToPosition(positionToMove = previousPosition)
        }
        every { cursor.getString(any()) } answers {
            currentRow()[projection[firstArg<Int>()]]?.toString()
        }
        every { cursor.getInt(any()) } answers {
            currentRow()[projection[firstArg<Int>()]].toIntValue()
        }
        every { cursor.getLong(any()) } answers {
            currentRow()[projection[firstArg<Int>()]].toLongValue()
        }

        return cursor
    }

    private fun createConversationMetadataCursor(row: TestConversationMetadataRow?): Cursor {
        val cursor = MatrixCursor(ConversationListItemData.PROJECTION)

        if (row != null) {
            cursor.addRow(
                ConversationListItemData.PROJECTION.map { columnName ->
                    row.toColumnValues()[columnName]
                }.toTypedArray(),
            )
        }

        return cursor
    }

    private fun assertClusterState(
        message: ConversationMessageData,
        canClusterWithPrevious: Boolean,
        canClusterWithNext: Boolean,
    ) {
        assertEquals(canClusterWithPrevious, message.canClusterWithPreviousMessage)
        assertEquals(canClusterWithNext, message.canClusterWithNextMessage)
    }

    private fun messageRow(
        messageId: String,
        participantId: String,
        selfParticipantId: String,
        receivedTimestamp: Long,
        status: Int,
        text: String,
    ): TestMessageRow {
        return TestMessageRow(
            messageId = messageId,
            participantId = participantId,
            selfParticipantId = selfParticipantId,
            receivedTimestamp = receivedTimestamp,
            status = status,
            text = text,
        )
    }

    private fun conversationMetadataRow(
        conversationName: String,
        selfParticipantId: String,
        participantCount: Int,
    ): TestConversationMetadataRow {
        return TestConversationMetadataRow(
            conversationName = conversationName,
            selfParticipantId = selfParticipantId,
            participantCount = participantCount,
        )
    }

    private data class TestMessageRow(
        val messageId: String,
        val participantId: String,
        val selfParticipantId: String,
        val receivedTimestamp: Long,
        val status: Int,
        val text: String,
    ) {
        fun toColumnValues(): Map<String, Any?> {
            return mapOf(
                MessageColumns._ID to messageId,
                MessageColumns.CONVERSATION_ID to CONVERSATION_ID,
                MessageColumns.SENDER_PARTICIPANT_ID to participantId,
                ConversationColumns.ICON to "",
                "parts_ids" to "part-$messageId",
                "parts_content_types" to TEXT_CONTENT_TYPE,
                "parts_content_uris" to "",
                "parts_widths" to "0",
                "parts_heights" to "0",
                "parts_texts" to text,
                "parts_count" to 1,
                MessageColumns.SENT_TIMESTAMP to receivedTimestamp,
                MessageColumns.RECEIVED_TIMESTAMP to receivedTimestamp,
                MessageColumns.SEEN to 1,
                MessageColumns.READ to 1,
                MessageColumns.PROTOCOL to MessageData.PROTOCOL_SMS,
                MessageColumns.STATUS to status,
                MessageColumns.SMS_MESSAGE_URI to "",
                MessageColumns.SMS_PRIORITY to 0,
                MessageColumns.SMS_MESSAGE_SIZE to 0,
                MessageColumns.MMS_SUBJECT to "",
                MessageColumns.MMS_EXPIRY to 0L,
                MessageColumns.RAW_TELEPHONY_STATUS to 0,
                MessageColumns.SELF_PARTICIPANT_ID to selfParticipantId,
                ParticipantColumns.FULL_NAME to "Sender $participantId",
                ParticipantColumns.FIRST_NAME to "Sender",
                ParticipantColumns.DISPLAY_DESTINATION to "+1555$messageId",
                ParticipantColumns.NORMALIZED_DESTINATION to "+1555$messageId",
                ParticipantColumns.PROFILE_PHOTO_URI to "",
                ParticipantColumns.CONTACT_ID to 0L,
                ParticipantColumns.LOOKUP_KEY to "",
            )
        }
    }

    private data class TestConversationMetadataRow(
        val conversationName: String,
        val selfParticipantId: String,
        val participantCount: Int,
    ) {
        fun toColumnValues(): Map<String, Any?> {
            return mapOf(
                ConversationColumns._ID to CONVERSATION_ID,
                ConversationColumns.NAME to conversationName,
                ConversationColumns.ICON to "",
                ConversationColumns.SNIPPET_TEXT to "",
                ConversationColumns.SORT_TIMESTAMP to 0L,
                MessageColumns.READ to 1,
                ConversationColumns.PREVIEW_URI to "",
                ConversationColumns.PREVIEW_CONTENT_TYPE to "",
                ConversationColumns.PARTICIPANT_CONTACT_ID to -1L,
                ConversationColumns.PARTICIPANT_LOOKUP_KEY to "",
                ConversationColumns.OTHER_PARTICIPANT_NORMALIZED_DESTINATION to "",
                ConversationColumns.PARTICIPANT_COUNT to participantCount,
                ConversationColumns.CURRENT_SELF_ID to selfParticipantId,
                ConversationColumns.NOTIFICATION_ENABLED to 1,
                ConversationColumns.NOTIFICATION_SOUND_URI to "",
                ConversationColumns.NOTIFICATION_VIBRATION to 0,
                ConversationColumns.INCLUDE_EMAIL_ADDRESS to 0,
                MessageColumns.STATUS to MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                ConversationColumns.SHOW_DRAFT to 0,
                ConversationColumns.DRAFT_PREVIEW_URI to "",
                ConversationColumns.DRAFT_PREVIEW_CONTENT_TYPE to "",
                ConversationColumns.DRAFT_SNIPPET_TEXT to "",
                ConversationColumns.ARCHIVE_STATUS to 0,
                MessageColumns._ID to "message-1",
                ConversationColumns.SUBJECT_TEXT to "",
                ConversationColumns.DRAFT_SUBJECT_TEXT to "",
                MessageColumns.RAW_TELEPHONY_STATUS to 0,
                "snippet_sender_first_name" to "",
                "snippet_sender_display_destination" to "",
                ConversationColumns.IS_ENTERPRISE to 0,
            )
        }
    }

    private fun Any?.toIntValue(): Int {
        return when (this) {
            null -> 0
            is Int -> this
            is Long -> this.toInt()
            is Boolean -> if (this) 1 else 0
            is String -> this.toInt()
            else -> error("Unsupported int value: $this")
        }
    }

    private fun Any?.toLongValue(): Long {
        return when (this) {
            null -> 0L
            is Long -> this
            is Int -> this.toLong()
            is Boolean -> if (this) 1L else 0L
            is String -> this.toLong()
            else -> error("Unsupported long value: $this")
        }
    }

    private companion object {
        private const val CONVERSATION_ID = "conversation-1"
        private const val TEXT_CONTENT_TYPE = "text/plain"
    }
}
