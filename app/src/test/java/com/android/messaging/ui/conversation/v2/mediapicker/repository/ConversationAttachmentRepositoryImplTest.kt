package com.android.messaging.ui.conversation.v2.mediapicker.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.database.MatrixCursor
import android.net.Uri
import android.os.Environment
import android.provider.ContactsContract.Contacts
import android.provider.MediaStore
import app.cash.turbine.test
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.datamodel.MediaScratchFileProvider
import com.android.messaging.ui.conversation.v2.mediapicker.model.AttachmentToSave
import com.android.messaging.ui.conversation.v2.mediapicker.model.PhotoPickerDraftAttachment
import com.android.messaging.ui.conversation.v2.mediapicker.model.PhotoPickerDraftAttachmentResult
import com.android.messaging.util.ContentType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class ConversationAttachmentRepositoryImplTest {

    @Test
    fun createDraftAttachmentsFromPhotoPicker_resolvesImageAttachment() = runTest {
        val imageUri = Uri.parse("content://picker/image")
        val scratchUri = Uri.parse("content://${MediaScratchFileProvider.AUTHORITY}/scratch-image")
        val scratchBytes = ByteArrayOutputStream()
        val contentResolver = createContentResolverForPhotoPicker(
            uri = imageUri,
            contentType = "image/png",
            bytes = onePixelPngBytes,
            scratchUri = scratchUri,
            scratchSink = scratchBytes,
        )
        val repository = ConversationAttachmentRepositoryImpl(
            contentResolver = contentResolver,
            ioDispatcher = Dispatchers.Unconfined,
        )

        mockkStatic(MediaScratchFileProvider::class)
        try {
            every {
                MediaScratchFileProvider.buildMediaScratchSpaceUri("png")
            } returns scratchUri

            repository.createDraftAttachmentsFromPhotoPicker(
                contentUris = listOf(imageUri.toString()),
            ).test {
                assertEquals(
                    PhotoPickerDraftAttachmentResult.Resolved(
                        photoPickerDraftAttachment = PhotoPickerDraftAttachment(
                            sourceContentUri = imageUri.toString(),
                            draftAttachment = ConversationDraftAttachment(
                                contentType = "image/png",
                                contentUri = scratchUri.toString(),
                                width = 1,
                                height = 1,
                            ),
                        ),
                    ),
                    awaitItem(),
                )
                awaitComplete()
            }

            assertArrayEquals(onePixelPngBytes, scratchBytes.toByteArray())
            verify(exactly = 1) {
                contentResolver.openOutputStream(scratchUri)
            }
        } finally {
            unmockkStatic(MediaScratchFileProvider::class)
        }
    }

    @Test
    fun createDraftAttachmentsFromPhotoPicker_dropsUnsupportedAttachment() = runTest {
        val contentResolver = createContentResolverForPhotoPicker(
            uri = Uri.parse("content://picker/file"),
            contentType = "application/pdf",
            bytes = ByteArray(0),
        )
        val repository = ConversationAttachmentRepositoryImpl(
            contentResolver = contentResolver,
            ioDispatcher = Dispatchers.Unconfined,
        )

        repository.createDraftAttachmentsFromPhotoPicker(
            contentUris = listOf("content://picker/file"),
        ).test {
            assertEquals(
                PhotoPickerDraftAttachmentResult.Failed(
                    sourceContentUri = "content://picker/file",
                ),
                awaitItem(),
            )
            awaitComplete()
        }
    }

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

    @Test
    fun saveAttachmentsToMediaStore_savesImageToPicturesAndFinalizesPendingRow() = runTest {
        val pendingUri = Uri.parse("content://media/external/images/media/pending")
        val sink = ByteArrayOutputStream()
        val contentResolver = createContentResolverForSave(
            pendingUri = pendingUri,
            sourceBytes = byteArrayOf(1, 2, 3),
            sink = sink,
        )
        val repository = ConversationAttachmentRepositoryImpl(
            contentResolver = contentResolver,
            ioDispatcher = Dispatchers.Unconfined,
        )

        repository.saveAttachmentsToMediaStore(
            attachments = listOf(
                AttachmentToSave(
                    contentType = "image/jpeg",
                    contentUri = "content://source/image.jpg",
                ),
            ),
        ).test {
            assertEquals(
                SaveAttachmentsResult(
                    imageCount = 1,
                    videoCount = 0,
                    otherCount = 0,
                    failCount = 0,
                ),
                awaitItem(),
            )
            awaitComplete()
        }

        val insertValues = slot<ContentValues>()
        verify(exactly = 1) {
            contentResolver.insert(
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                capture(insertValues),
            )
        }
        assertEquals(
            "${Environment.DIRECTORY_PICTURES}/Messaging",
            insertValues.captured.getAsString(MediaStore.MediaColumns.RELATIVE_PATH),
        )
        assertEquals(
            "image/jpeg",
            insertValues.captured.getAsString(MediaStore.MediaColumns.MIME_TYPE),
        )
        assertEquals(
            1,
            insertValues.captured.getAsInteger(MediaStore.MediaColumns.IS_PENDING),
        )

        assertArrayEquals(byteArrayOf(1, 2, 3), sink.toByteArray())

        val finalizeValues = slot<ContentValues>()
        verify(exactly = 1) {
            contentResolver.update(pendingUri, capture(finalizeValues), null, null)
        }
        assertEquals(
            0,
            finalizeValues.captured.getAsInteger(MediaStore.MediaColumns.IS_PENDING),
        )
        verify(exactly = 0) {
            contentResolver.delete(pendingUri, null, null)
        }
    }

    @Test
    fun saveAttachmentsToMediaStore_savesVideoToPicturesAndCountsAsVideo() = runTest {
        val pendingUri = Uri.parse("content://media/external/video/media/pending")
        val contentResolver = createContentResolverForSave(pendingUri = pendingUri)
        val repository = ConversationAttachmentRepositoryImpl(
            contentResolver = contentResolver,
            ioDispatcher = Dispatchers.Unconfined,
        )

        repository.saveAttachmentsToMediaStore(
            attachments = listOf(
                AttachmentToSave(
                    contentType = "video/mp4",
                    contentUri = "content://source/video.mp4",
                ),
            ),
        ).test {
            assertEquals(
                SaveAttachmentsResult(
                    imageCount = 0,
                    videoCount = 1,
                    otherCount = 0,
                    failCount = 0,
                ),
                awaitItem(),
            )
            awaitComplete()
        }

        val insertValues = slot<ContentValues>()
        verify(exactly = 1) {
            contentResolver.insert(
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                capture(insertValues),
            )
        }
        assertEquals(
            "${Environment.DIRECTORY_PICTURES}/Messaging",
            insertValues.captured.getAsString(MediaStore.MediaColumns.RELATIVE_PATH),
        )
    }

    @Test
    fun saveAttachmentsToMediaStore_savesAudioToMusicAndCountsAsOther() = runTest {
        val pendingUri = Uri.parse("content://media/external/audio/media/pending")
        val contentResolver = createContentResolverForSave(pendingUri = pendingUri)
        val repository = ConversationAttachmentRepositoryImpl(
            contentResolver = contentResolver,
            ioDispatcher = Dispatchers.Unconfined,
        )

        repository.saveAttachmentsToMediaStore(
            attachments = listOf(
                AttachmentToSave(
                    contentType = "audio/mpeg",
                    contentUri = "content://source/audio.mp3",
                ),
            ),
        ).test {
            assertEquals(
                SaveAttachmentsResult(
                    imageCount = 0,
                    videoCount = 0,
                    otherCount = 1,
                    failCount = 0,
                ),
                awaitItem(),
            )
            awaitComplete()
        }

        val insertValues = slot<ContentValues>()
        verify(exactly = 1) {
            contentResolver.insert(
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                capture(insertValues),
            )
        }
        assertEquals(
            "${Environment.DIRECTORY_MUSIC}/Messaging",
            insertValues.captured.getAsString(MediaStore.MediaColumns.RELATIVE_PATH),
        )
    }

    @Test
    fun saveAttachmentsToMediaStore_countsFailureWhenInsertReturnsNull() = runTest {
        val contentResolver = mockk<ContentResolver>()
        every { contentResolver.insert(any(), any()) } returns null
        val repository = ConversationAttachmentRepositoryImpl(
            contentResolver = contentResolver,
            ioDispatcher = Dispatchers.Unconfined,
        )

        repository.saveAttachmentsToMediaStore(
            attachments = listOf(
                AttachmentToSave(
                    contentType = "image/jpeg",
                    contentUri = "content://source/image.jpg",
                ),
            ),
        ).test {
            assertEquals(
                SaveAttachmentsResult(
                    imageCount = 0,
                    videoCount = 0,
                    otherCount = 0,
                    failCount = 1,
                ),
                awaitItem(),
            )
            awaitComplete()
        }

        verify(exactly = 0) { contentResolver.openInputStream(any()) }
        verify(exactly = 0) { contentResolver.update(any(), any(), any(), any()) }
        verify(exactly = 0) { contentResolver.delete(any(), any(), any()) }
    }

    @Test
    fun saveAttachmentsToMediaStore_deletesPendingRowAndCountsFailureWhenCopyThrows() = runTest {
        val pendingUri = Uri.parse("content://media/external/images/media/pending")
        val contentResolver = mockk<ContentResolver>()
        every { contentResolver.insert(any(), any()) } returns pendingUri
        every { contentResolver.openInputStream(any()) } throws IOException("boom")
        every { contentResolver.delete(pendingUri, null, null) } returns 1
        val repository = ConversationAttachmentRepositoryImpl(
            contentResolver = contentResolver,
            ioDispatcher = Dispatchers.Unconfined,
        )

        repository.saveAttachmentsToMediaStore(
            attachments = listOf(
                AttachmentToSave(
                    contentType = "image/jpeg",
                    contentUri = "content://source/image.jpg",
                ),
            ),
        ).test {
            assertEquals(
                SaveAttachmentsResult(
                    imageCount = 0,
                    videoCount = 0,
                    otherCount = 0,
                    failCount = 1,
                ),
                awaitItem(),
            )
            awaitComplete()
        }

        verify(exactly = 1) {
            contentResolver.delete(pendingUri, null, null)
        }
        verify(exactly = 0) {
            contentResolver.update(pendingUri, any(), any(), any())
        }
    }

    @Test
    fun saveAttachmentsToMediaStore_aggregatesCountsAcrossImageAndVideo() = runTest {
        val pendingUri = Uri.parse("content://media/external/pending")
        val contentResolver = createContentResolverForSave(pendingUri = pendingUri)
        val repository = ConversationAttachmentRepositoryImpl(
            contentResolver = contentResolver,
            ioDispatcher = Dispatchers.Unconfined,
        )

        repository.saveAttachmentsToMediaStore(
            attachments = listOf(
                AttachmentToSave(
                    contentType = "image/jpeg",
                    contentUri = "content://source/image.jpg",
                ),
                AttachmentToSave(
                    contentType = "video/mp4",
                    contentUri = "content://source/video.mp4",
                ),
            ),
        ).test {
            assertEquals(
                SaveAttachmentsResult(
                    imageCount = 1,
                    videoCount = 1,
                    otherCount = 0,
                    failCount = 0,
                ),
                awaitItem(),
            )
            awaitComplete()
        }
    }

    private fun createContentResolverForSave(
        pendingUri: Uri,
        sourceBytes: ByteArray = ByteArray(0),
        sink: ByteArrayOutputStream = ByteArrayOutputStream(),
    ): ContentResolver {
        val contentResolver = mockk<ContentResolver>()
        every { contentResolver.insert(any(), any()) } returns pendingUri
        every { contentResolver.openInputStream(any()) } answers
            { ByteArrayInputStream(sourceBytes) }
        every { contentResolver.openOutputStream(pendingUri) } returns sink
        every { contentResolver.update(any(), any(), any(), any()) } returns 1
        every { contentResolver.delete(any(), any(), any()) } returns 1

        return contentResolver
    }

    private fun createContentResolverForPhotoPicker(
        uri: Uri,
        contentType: String,
        bytes: ByteArray,
        scratchUri: Uri = Uri.parse("content://${MediaScratchFileProvider.AUTHORITY}/scratch"),
        scratchSink: ByteArrayOutputStream = ByteArrayOutputStream(),
    ): ContentResolver {
        val contentResolver = mockk<ContentResolver>()
        every { contentResolver.getType(uri) } returns contentType
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(bytes)
        every { contentResolver.openInputStream(scratchUri) } returns ByteArrayInputStream(bytes)
        every { contentResolver.openOutputStream(scratchUri) } returns scratchSink
        every { contentResolver.delete(scratchUri, null, null) } returns 1
        return contentResolver
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

        private val onePixelPngBytes = byteArrayOf(
            0x89.toByte(),
            0x50,
            0x4E,
            0x47,
            0x0D,
            0x0A,
            0x1A,
            0x0A,
            0x00,
            0x00,
            0x00,
            0x0D,
            0x49,
            0x48,
            0x44,
            0x52,
            0x00,
            0x00,
            0x00,
            0x01,
            0x00,
            0x00,
            0x00,
            0x01,
            0x08,
            0x06,
            0x00,
            0x00,
            0x00,
            0x1F,
            0x15,
            0xC4.toByte(),
            0x89.toByte(),
            0x00,
            0x00,
            0x00,
            0x0A,
            0x49,
            0x44,
            0x41,
            0x54,
            0x78,
            0x9C.toByte(),
            0x63,
            0x00,
            0x01,
            0x00,
            0x00,
            0x05,
            0x00,
            0x01,
            0x0D,
            0x0A,
            0x2D,
            0xB4.toByte(),
            0x00,
            0x00,
            0x00,
            0x00,
            0x49,
            0x45,
            0x4E,
            0x44,
            0xAE.toByte(),
            0x42,
            0x60,
            0x82.toByte(),
        )
    }
}
