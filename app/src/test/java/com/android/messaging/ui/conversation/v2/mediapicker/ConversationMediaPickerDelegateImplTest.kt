package com.android.messaging.ui.conversation.v2.mediapicker

import app.cash.turbine.test
import com.android.messaging.R
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.v2.composer.model.ConversationDraftState
import com.android.messaging.ui.conversation.v2.mediapicker.mapper.ConversationDraftAttachmentMapper
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationCapturedMedia
import com.android.messaging.ui.conversation.v2.mediapicker.model.PhotoPickerDraftAttachment
import com.android.messaging.ui.conversation.v2.mediapicker.model.PhotoPickerDraftAttachmentResult
import com.android.messaging.ui.conversation.v2.mediapicker.repository.ConversationAttachmentRepository
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
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
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationMediaPickerDelegateImplTest {

    @Test
    fun onPhotoPickerMediaSelected_addsResolvedDraftAttachments() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val draftDelegate = createConversationDraftDelegateMock()
        val attachments = listOf(
            ConversationDraftAttachment(
                contentType = "image/jpeg",
                contentUri = "content://picker/1",
                width = 640,
                height = 480,
            ),
            ConversationDraftAttachment(
                contentType = "video/mp4",
                contentUri = "content://picker/2",
                width = 1280,
                height = 720,
            ),
        )
        val attachmentRepository = createConversationAttachmentRepositoryMock(
            createDraftAttachmentsFromPhotoPickerFlow = flowOf(
                PhotoPickerDraftAttachmentResult.Resolved(
                    photoPickerDraftAttachment =
                        PhotoPickerDraftAttachment(
                            sourceContentUri = "content://picker/source/1",
                            draftAttachment = attachments[0],
                        ),
                ),
                PhotoPickerDraftAttachmentResult.Resolved(
                    photoPickerDraftAttachment =
                        PhotoPickerDraftAttachment(
                            sourceContentUri = "content://picker/source/2",
                            draftAttachment = attachments[1],
                        ),
                ),
            ),
        )
        val delegate = createDelegate(
            draftDelegate = draftDelegate,
            attachmentMapper = createConversationDraftAttachmentMapperMock(),
            attachmentRepository = attachmentRepository,
            defaultDispatcher = dispatcher,
        )
        val boundScope = CoroutineScope(dispatcher + SupervisorJob())

        try {
            delegate.bind(
                scope = boundScope,
                conversationIdFlow = MutableStateFlow(value = null),
            )

            delegate.onPhotoPickerMediaSelected(
                contentUris = listOf(
                    "content://picker/source/1",
                    "content://picker/source/2",
                ),
            )
            advanceUntilIdle()

            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                attachmentRepository.createDraftAttachmentsFromPhotoPicker(
                    contentUris = listOf(
                        "content://picker/source/1",
                        "content://picker/source/2",
                    ),
                )
            }
            verify(exactly = 1) {
                draftDelegate.addAttachments(attachments = listOf(attachments[0]))
            }
            verify(exactly = 1) {
                draftDelegate.addAttachments(attachments = listOf(attachments[1]))
            }
        } finally {
            boundScope.cancel()
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
                @Suppress("UnusedFlow")
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
            defaultDispatcher = Dispatchers.Unconfined,
        )

        delegate.onContactCardPicked(contactUri = "   ")

        verify(exactly = 0) {
            @Suppress("UnusedFlow")
            attachmentRepository.createDraftAttachmentFromContact(any())
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
            defaultDispatcher = dispatcher,
        )
        val boundScope = CoroutineScope(dispatcher + SupervisorJob())

        try {
            delegate.bind(
                scope = boundScope,
                conversationIdFlow = MutableStateFlow(value = null),
            )

            delegate.onPhotoPickerMediaSelected(contentUris = listOf(REMOTE_CONTENT_URI))
            advanceUntilIdle()
            delegate.onRemoveResolvedAttachment(contentUri = REMOTE_CONTENT_URI)
            advanceUntilIdle()

            verify(exactly = 1) {
                draftDelegate.removeAttachment(contentUri = REMOTE_CONTENT_URI)
            }
            verify(exactly = 1) {
                @Suppress("UnusedFlow")
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
            defaultDispatcher = Dispatchers.Unconfined,
        )

        delegate.onRemovePendingAttachment(pendingAttachmentId = PENDING_ATTACHMENT_ID)

        verify(exactly = 1) {
            draftDelegate.removePendingAttachment(pendingAttachmentId = PENDING_ATTACHMENT_ID)
        }
    }

    @Test
    fun onPhotoPickerMediaSelected_ignoresEmptyLists() = runTest {
        val attachmentRepository = createConversationAttachmentRepositoryMock()
        val draftDelegate = createConversationDraftDelegateMock()
        val delegate = createDelegate(
            draftDelegate = draftDelegate,
            attachmentMapper = createConversationDraftAttachmentMapperMock(),
            attachmentRepository = attachmentRepository,
            defaultDispatcher = Dispatchers.Unconfined,
        )

        delegate.onPhotoPickerMediaSelected(contentUris = emptyList())

        verify(exactly = 0) {
            @Suppress("UnusedFlow")
            attachmentRepository.createDraftAttachmentsFromPhotoPicker(any())
        }
        verify(exactly = 0) {
            draftDelegate.addAttachments(any())
        }
    }

    @Test
    fun onPhotoPickerMediaSelected_ignoresAlreadySelectedUris() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val attachmentRepository = createConversationAttachmentRepositoryMock(
            createDraftAttachmentsFromPhotoPickerFlow = flowOf(
                PhotoPickerDraftAttachmentResult.Resolved(
                    photoPickerDraftAttachment =
                        PhotoPickerDraftAttachment(
                            sourceContentUri = "content://picker/1",
                            draftAttachment = ConversationDraftAttachment(
                                contentType = "image/jpeg",
                                contentUri = "content://scratch/1",
                            ),
                        ),
                ),
            ),
        )
        val draftDelegate = createConversationDraftDelegateMock()
        val delegate = createDelegate(
            draftDelegate = draftDelegate,
            attachmentMapper = createConversationDraftAttachmentMapperMock(),
            attachmentRepository = attachmentRepository,
            defaultDispatcher = dispatcher,
        )
        val boundScope = CoroutineScope(dispatcher + SupervisorJob())

        try {
            delegate.bind(
                scope = boundScope,
                conversationIdFlow = MutableStateFlow(value = null),
            )

            delegate.onPhotoPickerMediaSelected(contentUris = listOf("content://picker/1"))
            advanceUntilIdle()
            delegate.onPhotoPickerMediaSelected(contentUris = listOf("content://picker/1"))
            advanceUntilIdle()

            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                attachmentRepository.createDraftAttachmentsFromPhotoPicker(
                    contentUris = listOf("content://picker/1"),
                )
            }
        } finally {
            boundScope.cancel()
        }
    }

    @Test
    fun onPhotoPickerMediaDeselected_removesDraftAttachmentsAndDeletesTemporaryAttachment() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val draftDelegate = createConversationDraftDelegateMock()
            val attachmentRepository = createConversationAttachmentRepositoryMock(
                createDraftAttachmentsFromPhotoPickerFlow = flowOf(
                    PhotoPickerDraftAttachmentResult.Resolved(
                        photoPickerDraftAttachment =
                            PhotoPickerDraftAttachment(
                                sourceContentUri = "content://picker/1",
                                draftAttachment = ConversationDraftAttachment(
                                    contentType = "image/jpeg",
                                    contentUri = "content://scratch/1",
                                ),
                            ),
                    ),
                ),
            )
            val delegate = createDelegate(
                draftDelegate = draftDelegate,
                attachmentMapper = createConversationDraftAttachmentMapperMock(),
                attachmentRepository = attachmentRepository,
                defaultDispatcher = dispatcher,
            )
            val boundScope = CoroutineScope(dispatcher + SupervisorJob())

            try {
                delegate.bind(
                    scope = boundScope,
                    conversationIdFlow = MutableStateFlow(value = null),
                )

                delegate.onPhotoPickerMediaSelected(contentUris = listOf("content://picker/1"))
                advanceUntilIdle()
                delegate.onPhotoPickerMediaDeselected(
                    contentUris = listOf(
                        "content://picker/1",
                        " ",
                    ),
                )
                advanceUntilIdle()

                verify(exactly = 1) {
                    draftDelegate.removeAttachment(contentUri = "content://scratch/1")
                }
                verify(exactly = 1) {
                    @Suppress("UnusedFlow")
                    attachmentRepository.deleteTemporaryAttachment(contentUri = "content://scratch/1")
                }
            } finally {
                boundScope.cancel()
            }
        }

    @Test
    fun onPhotoPickerMediaDeselected_beforeResolutionDoesNotAddResolvedAttachment() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val draftDelegate = createConversationDraftDelegateMock()
        val resolutionStarted = CompletableDeferred<Unit>()
        val releaseResolution = CompletableDeferred<Unit>()
        val attachmentRepository = createConversationAttachmentRepositoryMock(
            createDraftAttachmentsFromPhotoPickerFlow = flow {
                resolutionStarted.complete(Unit)
                releaseResolution.await()
                emit(
                    PhotoPickerDraftAttachmentResult.Resolved(
                        photoPickerDraftAttachment =
                            PhotoPickerDraftAttachment(
                                sourceContentUri = "content://picker/1",
                                draftAttachment = ConversationDraftAttachment(
                                    contentType = "image/jpeg",
                                    contentUri = "content://scratch/1",
                                ),
                            ),
                    ),
                )
            },
        )
        val delegate = createDelegate(
            draftDelegate = draftDelegate,
            attachmentMapper = createConversationDraftAttachmentMapperMock(),
            attachmentRepository = attachmentRepository,
            defaultDispatcher = dispatcher,
        )
        val boundScope = CoroutineScope(dispatcher + SupervisorJob())

        try {
            delegate.bind(
                scope = boundScope,
                conversationIdFlow = MutableStateFlow(value = null),
            )

            delegate.onPhotoPickerMediaSelected(contentUris = listOf("content://picker/1"))
            runCurrent()
            resolutionStarted.await()

            delegate.onPhotoPickerMediaDeselected(contentUris = listOf("content://picker/1"))
            releaseResolution.complete(Unit)
            advanceUntilIdle()

            verify(exactly = 0) {
                draftDelegate.addAttachments(any())
            }
            verify(exactly = 1) {
                draftDelegate.removeAttachment(contentUri = "content://picker/1")
            }
            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                attachmentRepository.deleteTemporaryAttachment(contentUri = "content://picker/1")
            }
            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                attachmentRepository.deleteTemporaryAttachment(contentUri = "content://scratch/1")
            }
        } finally {
            boundScope.cancel()
        }
    }

    @Test
    fun onPhotoPickerMediaSelected_whenResolutionFailsEmitsMessageEffect() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val draftDelegate = createConversationDraftDelegateMock()
        val attachmentRepository = createConversationAttachmentRepositoryMock(
            createDraftAttachmentsFromPhotoPickerFlow = flowOf(
                PhotoPickerDraftAttachmentResult.Failed(
                    sourceContentUri = "content://picker/1",
                ),
            ),
        )
        val delegate = createDelegate(
            draftDelegate = draftDelegate,
            attachmentMapper = createConversationDraftAttachmentMapperMock(),
            attachmentRepository = attachmentRepository,
            defaultDispatcher = dispatcher,
        )
        val boundScope = CoroutineScope(dispatcher + SupervisorJob())

        try {
            delegate.bind(
                scope = boundScope,
                conversationIdFlow = MutableStateFlow(value = null),
            )

            delegate.effects.test {
                delegate.onPhotoPickerMediaSelected(contentUris = listOf("content://picker/1"))
                advanceUntilIdle()

                assertEquals(
                    ConversationScreenEffect.ShowMessage(
                        messageResId = R.string.fail_to_load_attachment,
                    ),
                    awaitItem(),
                )
            }

            verify(exactly = 0) {
                draftDelegate.addAttachments(any())
            }
        } finally {
            boundScope.cancel()
        }
    }

    @Test
    fun photoPickerSourceContentUriByAttachmentContentUri_returnsPickerUriForResolvedAttachment() {
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val draftDelegate = createConversationDraftDelegateMock()
            val attachmentRepository = createConversationAttachmentRepositoryMock(
                createDraftAttachmentsFromPhotoPickerFlow = flowOf(
                    PhotoPickerDraftAttachmentResult.Resolved(
                        photoPickerDraftAttachment =
                            PhotoPickerDraftAttachment(
                                sourceContentUri = "content://picker/1",
                                draftAttachment = ConversationDraftAttachment(
                                    contentType = "image/jpeg",
                                    contentUri = "content://scratch/1",
                                ),
                            ),
                    ),
                ),
            )
            val delegate = createDelegate(
                draftDelegate = draftDelegate,
                attachmentMapper = createConversationDraftAttachmentMapperMock(),
                attachmentRepository = attachmentRepository,
                defaultDispatcher = dispatcher,
            )
            val boundScope = CoroutineScope(dispatcher + SupervisorJob())

            try {
                delegate.bind(
                    scope = boundScope,
                    conversationIdFlow = MutableStateFlow(value = null),
                )

                delegate.onPhotoPickerMediaSelected(contentUris = listOf("content://picker/1"))
                advanceUntilIdle()

                assertEquals(
                    "content://picker/1",
                    delegate.photoPickerSourceContentUriByAttachmentContentUri.value[
                        "content://scratch/1",
                    ],
                )
            } finally {
                boundScope.cancel()
            }
        }
    }

    private fun createDelegate(
        draftDelegate: ConversationDraftDelegate,
        attachmentMapper: ConversationDraftAttachmentMapper,
        attachmentRepository: ConversationAttachmentRepository,
        defaultDispatcher: CoroutineDispatcher,
    ): ConversationMediaPickerDelegateImpl {
        return ConversationMediaPickerDelegateImpl(
            conversationDraftDelegate = draftDelegate,
            conversationAttachmentRepository = attachmentRepository,
            conversationDraftAttachmentMapper = attachmentMapper,
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
        createDraftAttachmentsFromPhotoPickerFlow:
        Flow<PhotoPickerDraftAttachmentResult> = flowOf(),
        createDraftAttachmentFromContactFlow: Flow<ConversationDraftAttachment?> = flowOf(null),
        deleteTemporaryAttachmentFlow: Flow<Unit> = flowOf(Unit),
    ): ConversationAttachmentRepository {
        val attachmentRepository = mockk<ConversationAttachmentRepository>()
        every {
            attachmentRepository.createDraftAttachmentsFromPhotoPicker(any())
        } returns createDraftAttachmentsFromPhotoPickerFlow
        every {
            attachmentRepository.createDraftAttachmentFromContact(any())
        } returns createDraftAttachmentFromContactFlow
        every {
            attachmentRepository.deleteTemporaryAttachment(any())
        } returns deleteTemporaryAttachmentFlow
        return attachmentRepository
    }

    private companion object {
        private const val CONTACT_URI = "content://contacts/lookup/1"
        private const val PENDING_ATTACHMENT_ID = "pending-1"
        private const val REMOTE_CONTENT_URI = "content://remote/1"
    }
}
