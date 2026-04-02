package com.android.messaging.ui.conversation.v2.mediapicker

import android.content.ContentResolver
import android.net.Uri
import app.cash.turbine.test
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.media.model.ConversationMediaItem
import com.android.messaging.data.media.model.ConversationMediaType
import com.android.messaging.datamodel.MediaScratchFileProvider
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationCapturedMedia
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationAttachmentBridgeImplTest {

    @Before
    fun setUp() {
        unmockkAll()
        clearAllMocks()
    }

    @Test
    fun createDraftAttachments_mapsMediaItemsToDraftAttachments() {
        val bridge = ConversationAttachmentBridgeImpl(
            contentResolver = mockContentResolver(),
            ioDispatcher = Dispatchers.Unconfined,
        )

        val attachments = bridge.createDraftAttachments(
            mediaItems = listOf(
                createMediaItem(
                    mediaId = "1",
                    contentUri = "content://media/1",
                    contentType = "image/jpeg",
                    width = 120,
                    height = 80,
                    durationMillis = null,
                ),
                createMediaItem(
                    mediaId = "2",
                    contentUri = "content://media/2",
                    contentType = "video/mp4",
                    width = null,
                    height = 480,
                    durationMillis = 1234L,
                ),
            ),
        )

        assertEquals(
            listOf(
                ConversationDraftAttachment(
                    contentType = "image/jpeg",
                    contentUri = "content://media/1",
                    width = 120,
                    height = 80,
                ),
                ConversationDraftAttachment(
                    contentType = "video/mp4",
                    contentUri = "content://media/2",
                    width = null,
                    height = 480,
                ),
            ),
            attachments,
        )
    }

    @Test
    fun createDraftAttachment_mapsCapturedMediaToDraftAttachment() {
        val bridge = ConversationAttachmentBridgeImpl(
            contentResolver = mockContentResolver(),
            ioDispatcher = Dispatchers.Unconfined,
        )

        val attachment = bridge.createDraftAttachment(
            capturedMedia = ConversationCapturedMedia(
                contentUri = "content://scratch/1",
                contentType = "image/jpeg",
                width = 640,
                height = 480,
            ),
        )

        assertEquals(
            ConversationDraftAttachment(
                contentType = "image/jpeg",
                contentUri = "content://scratch/1",
                width = 640,
                height = 480,
            ),
            attachment,
        )
    }

    @Test
    fun deleteTemporaryAttachment_deletesScratchUrisAndNoOpsElsewhere() = runTest {
        val contentResolver = mockContentResolver()
        val bridge = ConversationAttachmentBridgeImpl(
            contentResolver = contentResolver,
            ioDispatcher = Dispatchers.Unconfined,
        )
        val scratchUri = Uri.parse(
            "content://${MediaScratchFileProvider.AUTHORITY}/12345",
        )
        val nonScratchUri = Uri.parse("content://example.com/12345")

        bridge.deleteTemporaryAttachment(contentUri = scratchUri.toString()).test {
            assertEquals(Unit, awaitItem())
            awaitComplete()
        }

        verify(exactly = 1) {
            contentResolver.delete(scratchUri, null, null)
        }

        bridge.deleteTemporaryAttachment(contentUri = nonScratchUri.toString()).test {
            assertEquals(Unit, awaitItem())
            awaitComplete()
        }

        verify(exactly = 0) {
            contentResolver.delete(nonScratchUri, null, null)
        }
    }

    @Test
    fun deleteTemporaryAttachment_swallowsNonCancellationFailures() = runTest {
        val contentResolver = mockContentResolver()
        val bridge = ConversationAttachmentBridgeImpl(
            contentResolver = contentResolver,
            ioDispatcher = Dispatchers.Unconfined,
        )
        val scratchUri = Uri.parse(
            "content://${MediaScratchFileProvider.AUTHORITY}/12345",
        )

        every {
            contentResolver.delete(scratchUri, null, null)
        } throws IllegalStateException("boom")

        bridge.deleteTemporaryAttachment(contentUri = scratchUri.toString()).test {
            assertEquals(Unit, awaitItem())
            awaitComplete()
        }

        verify(exactly = 1) {
            contentResolver.delete(scratchUri, null, null)
        }
    }

    private fun mockContentResolver(): ContentResolver {
        val contentResolver = mockk<ContentResolver>()
        every {
            contentResolver.delete(any(), any(), any())
        } returns 1
        return contentResolver
    }

    private fun createMediaItem(
        mediaId: String,
        contentUri: String,
        contentType: String,
        width: Int?,
        height: Int?,
        durationMillis: Long?,
    ): ConversationMediaItem {
        return ConversationMediaItem(
            mediaId = mediaId,
            contentUri = contentUri,
            contentType = contentType,
            mediaType = ConversationMediaType.Image,
            width = width,
            height = height,
            durationMillis = durationMillis,
        )
    }
}
