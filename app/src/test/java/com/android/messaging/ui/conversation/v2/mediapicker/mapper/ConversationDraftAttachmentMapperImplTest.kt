package com.android.messaging.ui.conversation.v2.mediapicker.mapper

import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.media.model.ConversationMediaItem
import com.android.messaging.data.media.model.ConversationMediaType
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationCapturedMedia
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationDraftAttachmentMapperImplTest {

    private val mapper = ConversationDraftAttachmentMapperImpl()

    @Test
    fun map_mediaItem_returnsDraftAttachment() {
        val mediaItem = ConversationMediaItem(
            mediaId = "media-1",
            contentUri = "content://media/1",
            contentType = "image/jpeg",
            mediaType = ConversationMediaType.Image,
            width = 640,
            height = 480,
            durationMillis = null,
        )

        val attachment = mapper.map(
            mediaItem = mediaItem,
        )

        assertEquals(
            ConversationDraftAttachment(
                contentType = "image/jpeg",
                contentUri = "content://media/1",
                width = 640,
                height = 480,
            ),
            attachment,
        )
    }

    @Test
    fun map_capturedMedia_returnsDraftAttachment() {
        val capturedMedia = ConversationCapturedMedia(
            contentUri = "content://scratch/1",
            contentType = "video/mp4",
            width = 1920,
            height = 1080,
        )

        val attachment = mapper.map(
            capturedMedia = capturedMedia,
        )

        assertEquals(
            ConversationDraftAttachment(
                contentType = "video/mp4",
                contentUri = "content://scratch/1",
                width = 1920,
                height = 1080,
            ),
            attachment,
        )
    }
}
