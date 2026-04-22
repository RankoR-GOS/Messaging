package com.android.messaging.ui.conversation.v2.mediapicker

import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.media.model.ConversationMediaItem
import com.android.messaging.data.media.model.ConversationMediaType
import com.android.messaging.data.media.repository.ConversationMediaRepository
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.v2.composer.model.ConversationDraftState
import com.android.messaging.ui.conversation.v2.mediapicker.mapper.ConversationDraftAttachmentMapper
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationCapturedMedia
import com.android.messaging.ui.conversation.v2.mediapicker.repository.ConversationAttachmentRepository
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
        val attachmentMapper = createConversationDraftAttachmentMapperMock()
        val attachmentRepository = createConversationAttachmentRepositoryMock()
        val repository = createConversationMediaRepositoryMock()
        val delegate = createDelegate(
            draftDelegate = draftDelegate,
            attachmentMapper = attachmentMapper,
            attachmentRepository = attachmentRepository,
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
            attachmentMapper.map(mediaItem = mediaItems[0])
        } returns attachments[0]
        every {
            attachmentMapper.map(mediaItem = mediaItems[1])
        } returns attachments[1]

        delegate.onGalleryMediaConfirmed(mediaItems = mediaItems)

        verify(exactly = 1) {
            attachmentMapper.map(mediaItem = mediaItems[0])
        }
        verify(exactly = 1) {
            attachmentMapper.map(mediaItem = mediaItems[1])
        }
        verify(exactly = 1) {
            draftDelegate.addAttachments(attachments = attachments)
        }
    }

    @Test
    fun onCapturedMediaReady_addsSingleDraftAttachment() = runTest {
        val draftDelegate = createConversationDraftDelegateMock()
        val attachmentMapper = createConversationDraftAttachmentMapperMock()
        val delegate = createDelegate(
            draftDelegate = draftDelegate,
            attachmentMapper = attachmentMapper,
            attachmentRepository = createConversationAttachmentRepositoryMock(),
            repository = createConversationMediaRepositoryMock(),
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
            attachmentMapper.map(capturedMedia = capturedMedia)
        } returns attachment

        delegate.onCapturedMediaReady(capturedMedia = capturedMedia)

        verify(exactly = 1) {
            attachmentMapper.map(capturedMedia = capturedMedia)
        }
        verify(exactly = 1) {
            draftDelegate.addAttachments(attachments = listOf(attachment))
        }
    }

    @Test
    fun onContactCardPicked_addsResolvedContactAttachment() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val draftDelegate = createConversationDraftDelegateMock()
        val attachment = ConversationDraftAttachment(
            contentType = "text/x-vCard",
            contentUri = "content://contacts/as_vcard/1",
        )
        val attachmentRepository = createConversationAttachmentRepositoryMock(
            createDraftAttachmentFromContactFlow = flowOf(attachment),
        )
        val delegate = createDelegate(
            draftDelegate = draftDelegate,
            attachmentMapper = createConversationDraftAttachmentMapperMock(),
            attachmentRepository = attachmentRepository,
            repository = createConversationMediaRepositoryMock(),
            defaultDispatcher = dispatcher,
        )
        val boundScope = CoroutineScope(dispatcher + SupervisorJob())

        try {
            delegate.bind(
                scope = boundScope,
                conversationIdFlow = MutableStateFlow(value = null),
            )

            delegate.onContactCardPicked(contactUri = CONTACT_URI)
            advanceUntilIdle()

            verify(exactly = 1) {
                attachmentRepository.createDraftAttachmentFromContact(contactUri = CONTACT_URI)
            }
            verify(exactly = 1) {
                draftDelegate.addAttachments(attachments = listOf(attachment))
            }
        } finally {
            boundScope.cancel()
        }
    }

    @Test
    fun onContactCardPicked_ignoresBlankUris() = runTest {
        val attachmentRepository = createConversationAttachmentRepositoryMock()
        val delegate = createDelegate(
            draftDelegate = createConversationDraftDelegateMock(),
            attachmentMapper = createConversationDraftAttachmentMapperMock(),
            attachmentRepository = attachmentRepository,
            repository = createConversationMediaRepositoryMock(),
            defaultDispatcher = Dispatchers.Unconfined,
        )

        delegate.onContactCardPicked(contactUri = "   ")

        verify(exactly = 0) {
            attachmentRepository.createDraftAttachmentFromContact(any())
        }
    }

    @Test
    fun onGalleryVisibilityChanged_loadsGalleryOnceAndExposesItems() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val draftDelegate = createConversationDraftDelegateMock()
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
        val conversationIdFlow = MutableStateFlow<String?>(value = null)
        val delegate = createDelegate(
            draftDelegate = draftDelegate,
            attachmentMapper = createConversationDraftAttachmentMapperMock(),
            attachmentRepository = createConversationAttachmentRepositoryMock(),
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
        val repository = createConversationMediaRepositoryMock(
            recentMediaFlow = flow {
                throw IllegalStateException("boom")
            },
        )
        val delegate = createDelegate(
            draftDelegate = draftDelegate,
            attachmentMapper = createConversationDraftAttachmentMapperMock(),
            attachmentRepository = createConversationAttachmentRepositoryMock(),
            repository = repository,
            defaultDispatcher = dispatcher,
        )
        val boundScope = CoroutineScope(dispatcher + SupervisorJob())

        try {
            delegate.bind(
                scope = boundScope,
                conversationIdFlow = MutableStateFlow(value = null),
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
        val attachmentRepository = createConversationAttachmentRepositoryMock(
            deleteTemporaryAttachmentFlow = flowOf(Unit),
        )
        val delegate = createDelegate(
            draftDelegate = draftDelegate,
            attachmentMapper = createConversationDraftAttachmentMapperMock(),
            attachmentRepository = attachmentRepository,
            repository = createConversationMediaRepositoryMock(),
            defaultDispatcher = dispatcher,
        )
        val boundScope = CoroutineScope(dispatcher + SupervisorJob())

        try {
            delegate.bind(
                scope = boundScope,
                conversationIdFlow = MutableStateFlow(value = null),
            )

            delegate.onRemoveResolvedAttachment(contentUri = REMOTE_CONTENT_URI)
            advanceUntilIdle()

            verify(exactly = 1) {
                draftDelegate.removeAttachment(contentUri = REMOTE_CONTENT_URI)
            }
            verify(exactly = 1) {
                attachmentRepository.deleteTemporaryAttachment(contentUri = REMOTE_CONTENT_URI)
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
            attachmentMapper = createConversationDraftAttachmentMapperMock(),
            attachmentRepository = createConversationAttachmentRepositoryMock(),
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
        val attachmentMapper = createConversationDraftAttachmentMapperMock()
        val draftDelegate = createConversationDraftDelegateMock()
        val delegate = createDelegate(
            draftDelegate = draftDelegate,
            attachmentMapper = attachmentMapper,
            attachmentRepository = createConversationAttachmentRepositoryMock(),
            repository = createConversationMediaRepositoryMock(),
            defaultDispatcher = Dispatchers.Unconfined,
        )

        delegate.onGalleryMediaConfirmed(mediaItems = emptyList())

        verify(exactly = 0) {
            attachmentMapper.map(mediaItem = any())
        }
        verify(exactly = 0) {
            draftDelegate.addAttachments(any())
        }
    }

    private fun createDelegate(
        draftDelegate: ConversationDraftDelegate,
        attachmentMapper: ConversationDraftAttachmentMapper,
        attachmentRepository: ConversationAttachmentRepository,
        repository: ConversationMediaRepository,
        defaultDispatcher: CoroutineDispatcher,
    ): ConversationMediaPickerDelegateImpl {
        return ConversationMediaPickerDelegateImpl(
            conversationDraftDelegate = draftDelegate,
            conversationAttachmentRepository = attachmentRepository,
            conversationDraftAttachmentMapper = attachmentMapper,
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

    private fun createConversationDraftAttachmentMapperMock(): ConversationDraftAttachmentMapper {
        return mockk(relaxed = true)
    }

    private fun createConversationAttachmentRepositoryMock(
        createDraftAttachmentFromContactFlow: Flow<ConversationDraftAttachment?> = flowOf(null),
        deleteTemporaryAttachmentFlow: Flow<Unit> = flowOf(Unit),
    ): ConversationAttachmentRepository {
        val attachmentRepository = mockk<ConversationAttachmentRepository>()
        every {
            attachmentRepository.createDraftAttachmentFromContact(any())
        } returns createDraftAttachmentFromContactFlow
        every {
            attachmentRepository.deleteTemporaryAttachment(any())
        } returns deleteTemporaryAttachmentFlow
        return attachmentRepository
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
        private const val CONTACT_URI = "content://contacts/lookup/1"
        private const val PENDING_ATTACHMENT_ID = "pending-1"
        private const val REMOTE_CONTENT_URI = "content://remote/1"
    }
}
