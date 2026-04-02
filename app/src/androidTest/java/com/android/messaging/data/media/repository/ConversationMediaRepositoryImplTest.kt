package com.android.messaging.data.media.repository

import android.content.ContentResolver
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.messaging.data.media.model.ConversationMediaItem
import com.android.messaging.data.media.model.ConversationMediaType
import com.android.messaging.util.ContentType
import com.android.messaging.util.UriUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationMediaRepositoryImplTest {

    @Test
    fun getRecentMedia_queriesMediaStoreWithExpectedProjectionAndArgs() = runTest {
        val contentResolver = mockk<ContentResolver>()
        val capturedUri = slot<Uri>()
        val capturedProjection = slot<Array<String>>()
        val capturedQueryArgs = slot<Bundle>()

        every {
            contentResolver.query(
                capture(capturedUri),
                capture(capturedProjection),
                capture(capturedQueryArgs),
                null,
            )
        } returns createCursor()

        val repository = createRepository(contentResolver = contentResolver)

        repository.getRecentMedia(limit = 37).single()

        verify(exactly = 1) {
            contentResolver.query(
                any<Uri>(),
                any<Array<String>>(),
                any<Bundle>(),
                null,
            )
        }

        assertRecentMediaQuery(
            uri = capturedUri.captured,
            projection = capturedProjection.captured,
            queryArgs = capturedQueryArgs.captured,
            limit = 37,
        )
    }

    @Test
    fun getRecentMedia_mapsCursorRows() = runTest {
        val contentResolver = mockk<ContentResolver>()
        stubQuery(
            contentResolver = contentResolver,
            result = createCursor(),
        )
        val repository = createRepository(contentResolver = contentResolver)

        val items = repository.getRecentMedia(limit = 37).single()

        assertEquals(
            listOf(
                ConversationMediaItem(
                    mediaId = "10",
                    contentUri = UriUtil.getContentUriForMediaStoreId(10L).toString(),
                    contentType = ContentType.IMAGE_UNSPECIFIED,
                    mediaType = ConversationMediaType.Image,
                    width = null,
                    height = 720,
                    durationMillis = null,
                ),
                ConversationMediaItem(
                    mediaId = "11",
                    contentUri = UriUtil.getContentUriForMediaStoreId(11L).toString(),
                    contentType = ContentType.VIDEO_UNSPECIFIED,
                    mediaType = ConversationMediaType.Video,
                    width = 1920,
                    height = 1080,
                    durationMillis = 1234L,
                ),
            ),
            items,
        )
    }

    @Test
    fun getRecentMedia_returnsEmptyListForNullCursor() = runTest {
        val contentResolver = mockk<ContentResolver>()
        stubQuery(
            contentResolver = contentResolver,
            result = null,
        )
        val repository = createRepository(contentResolver = contentResolver)

        val items = repository.getRecentMedia(limit = 7).single()

        assertEquals(emptyList<ConversationMediaItem>(), items)
    }

    private fun createRepository(
        contentResolver: ContentResolver,
    ): ConversationMediaRepositoryImpl {
        return ConversationMediaRepositoryImpl(
            contentResolver = contentResolver,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    private fun stubQuery(
        contentResolver: ContentResolver,
        result: Cursor?,
    ) {
        every {
            contentResolver.query(
                any<Uri>(),
                any<Array<String>>(),
                any<Bundle>(),
                null,
            )
        } returns result
    }

    private fun assertRecentMediaQuery(
        uri: Uri,
        projection: Array<String>,
        queryArgs: Bundle,
        limit: Int,
    ) {
        assertEquals(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
            uri,
        )
        assertEquals(
            listOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.WIDTH,
                MediaStore.Files.FileColumns.HEIGHT,
                MediaStore.Video.VideoColumns.DURATION,
            ),
            projection.toList(),
        )
        assertEquals(
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN " +
                "(${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}," +
                "${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})",
            queryArgs.getString(ContentResolver.QUERY_ARG_SQL_SELECTION),
        )
        assertEquals(
            listOf(MediaStore.Files.FileColumns.DATE_ADDED),
            queryArgs.getStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS)?.toList(),
        )
        assertEquals(
            ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
            queryArgs.getInt(ContentResolver.QUERY_ARG_SORT_DIRECTION),
        )
        assertEquals(
            limit,
            queryArgs.getInt(ContentResolver.QUERY_ARG_LIMIT),
        )
    }

    private fun createCursor(): Cursor {
        return MatrixCursor(
            arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.WIDTH,
                MediaStore.Files.FileColumns.HEIGHT,
                MediaStore.Video.VideoColumns.DURATION,
            ),
        ).apply {
            addRow(
                arrayOf<Any?>(
                    10L,
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
                    "",
                    1L,
                    0,
                    720,
                    0L,
                ),
            )
            addRow(
                arrayOf<Any?>(
                    11L,
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO,
                    "",
                    2L,
                    1920,
                    1080,
                    1234L,
                ),
            )
        }
    }
}
