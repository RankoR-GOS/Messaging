package com.android.messaging.ui.conversation.v2.mediapicker

import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.media.model.ConversationMediaItem
import com.android.messaging.data.media.model.ConversationMediaType
import com.android.messaging.data.media.repository.ConversationMediaRepository
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.v2.composer.model.ConversationDraftState
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationCapturedMedia
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationMediaPickerDelegateImplTest {

    @Test
    fun onGalleryMediaConfirmed_mapsMediaItemsToDraftAttachments() = runTest {
        val draftDelegate = createConversationDraftDelegateMock()
        val attachmentBridge = createConversationAttachmentBridgeMock()
        val repository = createConversationMediaRepositoryMock()
        val delegate = createDelegate(
            draftDelegate = draftDelegate,
            attachmentBridge = attachmentBridge,
            repository = repository,
            defaultDispatcher = Dispatchers.Unconfined,
        )
        val mediaItems = listOf(
            createMediaItem(
                mediaId = "1",
                contentUri = "content://media/1",
                contentType = "image/jpeg",
                mediaType = ConversationMediaType.Image,
                width = 640,
                height = 480,
                durationMillis = null,
            ),
            createMediaItem(
                mediaId = "2",
                contentUri = "content://media/2",
                contentType = "video/mp4",
                mediaType = ConversationMediaType.Video,
                width = 1280,
                height = 720,
                durationMillis = 5000L,
            ),
        )
        val attachments = listOf(
            ConversationDraftAttachment(
                contentType = "image/jpeg",
                contentUri = "content://media/1",
                width = 640,
                height = 480,
            ),
            ConversationDraftAttachment(
                contentType = "video/mp4",
                contentUri = "content://media/2",
                width = 1280,
                height = 720,
            ),
        )

        every {
            attachmentBridge.createDraftAttachments(mediaItems = mediaItems)
        } returns attachments

        delegate.onGalleryMediaConfirmed(mediaItems = mediaItems)

        verify(exactly = 1) {
            attachmentBridge.createDraftAttachments(mediaItems = mediaItems)
        }
        verify(exactly = 1) {
            draftDelegate.addAttachments(attachments = attachments)
        }
    }

    @Test
    fun onCapturedMediaReady_addsSingleDraftAttachment() = runTest {
        val draftDelegate = createConversationDraftDelegateMock()
        val attachmentBridge = createConversationAttachmentBridgeMock()
        val repository = createConversationMediaRepositoryMock()
        val delegate = createDelegate(
            draftDelegate = draftDelegate,
            attachmentBridge = attachmentBridge,
            repository = repository,
            defaultDispatcher = Dispatchers.Unconfined,
        )
        val capturedMedia = ConversationCapturedMedia(
            contentUri = "content://scratch/1",
            contentType = "image/jpeg",
            width = 800,
            height = 600,
        )
        val attachment = ConversationDraftAttachment(
            contentType = "image/jpeg",
            contentUri = "content://scratch/1",
            width = 800,
            height = 600,
        )

        every {
            attachmentBridge.createDraftAttachment(capturedMedia = capturedMedia)
        } returns attachment

        delegate.onCapturedMediaReady(capturedMedia = capturedMedia)

        verify(exactly = 1) {
            attachmentBridge.createDraftAttachment(capturedMedia = capturedMedia)
        }
        verify(exactly = 1) {
            draftDelegate.addAttachments(attachments = listOf(attachment))
        }
    }

    @Test
    fun onGalleryVisibilityChanged_loadsGalleryOnceAndExposesItems() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val draftDelegate = createConversationDraftDelegateMock()
        val attachmentBridge = createConversationAttachmentBridgeMock()
        val mediaItems = listOf(
            createMediaItem(
                mediaId = "1",
                contentUri = "content://media/1",
                contentType = "image/jpeg",
                mediaType = ConversationMediaType.Image,
                width = 640,
                height = 480,
                durationMillis = null,
            ),
        )
        val repository = createConversationMediaRepositoryMock(
            recentMediaFlow = flowOf(mediaItems),
        )
        val conversationIdFlow = MutableStateFlow<String?>(null)
        val delegate = createDelegate(
            draftDelegate = draftDelegate,
            attachmentBridge = attachmentBridge,
            repository = repository,
            defaultDispatcher = dispatcher,
        )
        val boundScope = CoroutineScope(dispatcher + SupervisorJob())

        try {
            delegate.bind(
                scope = boundScope,
                conversationIdFlow = conversationIdFlow,
            )

            delegate.onGalleryVisibilityChanged(isVisible = true)
            advanceUntilIdle()

            verify(exactly = 1) {
                repository.getRecentMedia(limit = any())
            }
            assertFalse(delegate.state.value.isLoadingGallery)
            assertEquals(1, delegate.state.value.galleryItems.size)

            delegate.onGalleryVisibilityChanged(isVisible = true)
            advanceUntilIdle()

            verify(exactly = 1) {
                repository.getRecentMedia(limit = any())
            }
        } finally {
            boundScope.cancel()
        }
    }

    @Test
    fun onGalleryVisibilityChanged_failureClearsLoadingState() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val draftDelegate = createConversationDraftDelegateMock()
        val attachmentBridge = createConversationAttachmentBridgeMock()
        val repository = createConversationMediaRepositoryMock(
            recentMediaFlow = flow {
                throw IllegalStateException("boom")
            },
        )
        val delegate = createDelegate(
            draftDelegate = draftDelegate,
            attachmentBridge = attachmentBridge,
            repository = repository,
            defaultDispatcher = dispatcher,
        )
        val boundScope = CoroutineScope(dispatcher + SupervisorJob())

        try {
            delegate.bind(
                scope = boundScope,
                conversationIdFlow = MutableStateFlow(null),
            )

            delegate.onGalleryVisibilityChanged(isVisible = true)
            advanceUntilIdle()

            verify(exactly = 1) {
                repository.getRecentMedia(limit = any())
            }
            assertFalse(delegate.state.value.isLoadingGallery)
            assertTrue(delegate.state.value.galleryItems.isEmpty())
        } finally {
            boundScope.cancel()
        }
    }

    @Test
    fun onRemoveResolvedAttachment_removesDraftAndDeletesTemporaryAttachment() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val draftDelegate = createConversationDraftDelegateMock()
        val attachmentBridge = createConversationAttachmentBridgeMock(
            deleteTemporaryAttachmentFlow = flowOf(Unit),
        )
        val repository = createConversationMediaRepositoryMock()
        val delegate = createDelegate(
            draftDelegate = draftDelegate,
            attachmentBridge = attachmentBridge,
            repository = repository,
            defaultDispatcher = dispatcher,
        )
        val boundScope = CoroutineScope(dispatcher + SupervisorJob())

        try {
            delegate.bind(
                scope = boundScope,
                conversationIdFlow = MutableStateFlow(null),
            )

            delegate.onRemoveResolvedAttachment(contentUri = REMOTE_CONTENT_URI)
            advanceUntilIdle()

            verify(exactly = 1) {
                draftDelegate.removeAttachment(contentUri = REMOTE_CONTENT_URI)
            }
            verify(exactly = 1) {
                attachmentBridge.deleteTemporaryAttachment(contentUri = REMOTE_CONTENT_URI)
            }
        } finally {
            boundScope.cancel()
        }
    }

    @Test
    fun onRemovePendingAttachment_removesPendingDraftAttachment() = runTest {
        val draftDelegate = createConversationDraftDelegateMock()
        val delegate = createDelegate(
            draftDelegate = draftDelegate,
            attachmentBridge = createConversationAttachmentBridgeMock(),
            repository = createConversationMediaRepositoryMock(),
            defaultDispatcher = Dispatchers.Unconfined,
        )

        delegate.onRemovePendingAttachment(pendingAttachmentId = PENDING_ATTACHMENT_ID)

        verify(exactly = 1) {
            draftDelegate.removePendingAttachment(pendingAttachmentId = PENDING_ATTACHMENT_ID)
        }
    }

    @Test
    fun onGalleryMediaConfirmed_ignoresEmptyLists() = runTest {
        val draftDelegate = createConversationDraftDelegateMock()
        val attachmentBridge = createConversationAttachmentBridgeMock()
        val delegate = createDelegate(
            draftDelegate = draftDelegate,
            attachmentBridge = attachmentBridge,
            repository = createConversationMediaRepositoryMock(),
            defaultDispatcher = Dispatchers.Unconfined,
        )

        delegate.onGalleryMediaConfirmed(mediaItems = emptyList())

        verify(exactly = 0) {
            attachmentBridge.createDraftAttachments(any())
        }
        verify(exactly = 0) {
            draftDelegate.addAttachments(any())
        }
    }

    private fun createDelegate(
        draftDelegate: ConversationDraftDelegate,
        attachmentBridge: ConversationAttachmentBridge,
        repository: ConversationMediaRepository,
        defaultDispatcher: CoroutineDispatcher,
    ): ConversationMediaPickerDelegateImpl {
        return ConversationMediaPickerDelegateImpl(
            conversationDraftDelegate = draftDelegate,
            conversationAttachmentBridge = attachmentBridge,
            conversationMediaRepository = repository,
            defaultDispatcher = defaultDispatcher,
        )
    }

    private fun createConversationDraftDelegateMock(): ConversationDraftDelegate {
        val stateFlow = MutableStateFlow(ConversationDraftState())
        val draftDelegate = mockk<ConversationDraftDelegate>(relaxed = true)
        every { draftDelegate.state } returns stateFlow
        return draftDelegate
    }

    private fun createConversationAttachmentBridgeMock(
        deleteTemporaryAttachmentFlow: Flow<Unit> = flowOf(Unit),
    ): ConversationAttachmentBridge {
        val attachmentBridge = mockk<ConversationAttachmentBridge>()
        every {
            attachmentBridge.deleteTemporaryAttachment(any())
        } returns deleteTemporaryAttachmentFlow
        return attachmentBridge
    }

    private fun createConversationMediaRepositoryMock(
        recentMediaFlow: Flow<List<ConversationMediaItem>> = flowOf(emptyList()),
    ): ConversationMediaRepository {
        val repository = mockk<ConversationMediaRepository>()
        every {
            repository.getRecentMedia(any())
        } returns recentMediaFlow
        return repository
    }

    private fun createMediaItem(
        mediaId: String,
        contentUri: String,
        contentType: String,
        mediaType: ConversationMediaType,
        width: Int?,
        height: Int?,
        durationMillis: Long?,
    ): ConversationMediaItem {
        return ConversationMediaItem(
            mediaId = mediaId,
            contentUri = contentUri,
            contentType = contentType,
            mediaType = mediaType,
            width = width,
            height = height,
            durationMillis = durationMillis,
        )
    }

    private companion object {
        private const val PENDING_ATTACHMENT_ID = "pending-1"
        private const val REMOTE_CONTENT_URI = "content://media/1"
    }
}
