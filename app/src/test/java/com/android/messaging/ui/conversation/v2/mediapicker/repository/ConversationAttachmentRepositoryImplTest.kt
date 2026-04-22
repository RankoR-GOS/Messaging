package com.android.messaging.ui.conversation.v2.mediapicker.repository

import android.content.ContentResolver
import android.database.MatrixCursor
import android.net.Uri
import android.provider.ContactsContract.Contacts
import app.cash.turbine.test
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.datamodel.MediaScratchFileProvider
import com.android.messaging.util.ContentType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationAttachmentRepositoryImplTest {

    @Test
    fun createDraftAttachmentFromContact_returnsVCardAttachmentForResolvedLookupKey() = runTest {
        val repository = ConversationAttachmentRepositoryImpl(
            contentResolver = createContentResolver(
                contactCursor = createContactsCursor(
                    arrayOf<Any?>("lookup-key-1"),
                ),
            ),
            ioDispatcher = Dispatchers.Unconfined,
        )

        repository.createDraftAttachmentFromContact(contactUri = CONTACT_URI).test {
            assertEquals(
                ConversationDraftAttachment(
                    contentType = ContentType.TEXT_VCARD,
                    contentUri = Uri.withAppendedPath(
                        Contacts.CONTENT_VCARD_URI,
                        "lookup-key-1",
                    ).toString(),
                ),
                awaitItem(),
            )
            awaitComplete()
        }
    }

    @Test
    fun createDraftAttachmentFromContact_returnsNullWhenLookupKeyIsMissing() = runTest {
        val repository = ConversationAttachmentRepositoryImpl(
            contentResolver = createContentResolver(
                contactCursor = createContactsCursor(
                    arrayOf<Any?>(""),
                ),
            ),
            ioDispatcher = Dispatchers.Unconfined,
        )

        repository.createDraftAttachmentFromContact(contactUri = CONTACT_URI).test {
            assertEquals(null, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun createDraftAttachmentFromContact_swallowsNonCancellationFailures() = runTest {
        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(any(), any(), any(), any(), any())
        } throws IllegalStateException("boom")
        val repository = ConversationAttachmentRepositoryImpl(
            contentResolver = contentResolver,
            ioDispatcher = Dispatchers.Unconfined,
        )

        repository.createDraftAttachmentFromContact(contactUri = CONTACT_URI).test {
            assertEquals(null, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun deleteTemporaryAttachment_deletesScratchUrisAndNoOpsElsewhere() = runTest {
        val contentResolver = createContentResolver(contactCursor = createContactsCursor())
        val repository = ConversationAttachmentRepositoryImpl(
            contentResolver = contentResolver,
            ioDispatcher = Dispatchers.Unconfined,
        )
        val scratchUri = Uri.parse("content://${MediaScratchFileProvider.AUTHORITY}/12345")
        val nonScratchUri = Uri.parse("content://example.com/12345")

        repository.deleteTemporaryAttachment(contentUri = scratchUri.toString()).test {
            assertEquals(Unit, awaitItem())
            awaitComplete()
        }

        verify(exactly = 1) {
            contentResolver.delete(scratchUri, null, null)
        }

        repository.deleteTemporaryAttachment(contentUri = nonScratchUri.toString()).test {
            assertEquals(Unit, awaitItem())
            awaitComplete()
        }

        verify(exactly = 0) {
            contentResolver.delete(nonScratchUri, null, null)
        }
    }

    @Test
    fun deleteTemporaryAttachment_swallowsNonCancellationFailures() = runTest {
        val contentResolver = createContentResolver(contactCursor = createContactsCursor())
        val scratchUri = Uri.parse("content://${MediaScratchFileProvider.AUTHORITY}/12345")
        every {
            contentResolver.delete(scratchUri, null, null)
        } throws IllegalStateException("boom")
        val repository = ConversationAttachmentRepositoryImpl(
            contentResolver = contentResolver,
            ioDispatcher = Dispatchers.Unconfined,
        )

        repository.deleteTemporaryAttachment(contentUri = scratchUri.toString()).test {
            assertEquals(Unit, awaitItem())
            awaitComplete()
        }

        verify(exactly = 1) {
            contentResolver.delete(scratchUri, null, null)
        }
    }

    private fun createContentResolver(
        contactCursor: MatrixCursor,
    ): ContentResolver {
        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.query(any(), any(), any(), any(), any())
        } returns contactCursor
        every {
            contentResolver.delete(any(), any(), any())
        } returns 1
        return contentResolver
    }

    private fun createContactsCursor(
        vararg rows: Array<Any?>,
    ): MatrixCursor {
        val cursor = MatrixCursor(arrayOf(Contacts.LOOKUP_KEY))
        rows.forEach { row ->
            cursor.addRow(row)
        }
        return cursor
    }

    private companion object {
        private const val CONTACT_URI = "content://contacts/lookup/1"
    }
}
