package com.android.messaging.ui.conversation.v2.composer.delegate

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.ui.conversation.v2.composer.mapper.ConversationComposerAttachmentUiModelMapper
import com.android.messaging.ui.conversation.v2.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.v2.composer.model.ConversationDraftState
import com.android.messaging.ui.conversation.v2.messages.mapper.ConversationVCardAttachmentUiModelMapper
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentUiModel
import com.android.messaging.ui.conversation.v2.messages.repository.ConversationVCardMetadataRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationComposerAttachmentsDelegateImplTest {

    @Test
    fun bind_seedsInitialStateFromCurrentDraftState() {
        val attachmentMapper = mockk<ConversationComposerAttachmentUiModelMapper>()
        val vCardUiModelMapper = mockk<ConversationVCardAttachmentUiModelMapper>()
        val metadataRepository = mockk<ConversationVCardMetadataRepository>()
        val expectedState = persistentListOf<ComposerAttachmentUiModel>(
            ComposerAttachmentUiModel.Resolved.File(
                key = "content://attachments/file/1",
                contentType = "application/pdf",
                contentUri = "content://attachments/file/1",
            ),
        )
        every {
            attachmentMapper.map(any(), any())
        } returns expectedState
        val delegate = ConversationComposerAttachmentsDelegateImpl(
            conversationComposerAttachmentUiModelMapper = attachmentMapper,
            conversationVCardAttachmentUiModelMapper = vCardUiModelMapper,
            conversationVCardMetadataRepository = metadataRepository,
            defaultDispatcher = Dispatchers.Unconfined,
        )

        delegate.bind(
            scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob()),
            draftStateFlow = MutableStateFlow(
                ConversationDraftState(
                    draft = ConversationDraft(
                        attachments = persistentListOf(
                            ConversationDraftAttachment(
                                contentType = "application/pdf",
                                contentUri = "content://attachments/file/1",
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(expectedState, delegate.state.value)
    }

    @Test
    fun bind_updatesVCardAttachmentWhenMetadataArrives() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val attachmentMapper = mockk<ConversationComposerAttachmentUiModelMapper>()
        val vCardUiModelMapper = mockk<ConversationVCardAttachmentUiModelMapper>()
        val metadataRepository = mockk<ConversationVCardMetadataRepository>()
        val loadingUiModel = ConversationVCardAttachmentUiModel(
            type = ConversationVCardAttachmentType.CONTACT,
            titleTextResId = 1,
            subtitleTextResId = 2,
        )
        val loadedMetadata = ConversationVCardAttachmentMetadata.Loaded(
            type = ConversationVCardAttachmentType.CONTACT,
            displayName = "Sam Rivera",
            details = "555-000-8901",
            locationAddress = null,
        )
        val loadedUiModel = ConversationVCardAttachmentUiModel(
            type = ConversationVCardAttachmentType.CONTACT,
            titleText = "Sam Rivera",
            subtitleText = "555-000-8901",
        )
        val initialAttachment = ComposerAttachmentUiModel.Resolved.VCard(
            key = "content://attachments/vcard/1",
            contentType = "text/x-vCard",
            contentUri = "content://attachments/vcard/1",
            vCardUiModel = loadingUiModel,
        )
        every {
            attachmentMapper.map(any(), any())
        } returns persistentListOf(initialAttachment)
        every {
            metadataRepository.observeAttachmentMetadata(
                contentUri = "content://attachments/vcard/1"
            )
        } returns flowOf(loadedMetadata)
        every {
            vCardUiModelMapper.map(metadata = loadedMetadata)
        } returns loadedUiModel
        val delegate = ConversationComposerAttachmentsDelegateImpl(
            conversationComposerAttachmentUiModelMapper = attachmentMapper,
            conversationVCardAttachmentUiModelMapper = vCardUiModelMapper,
            conversationVCardMetadataRepository = metadataRepository,
            defaultDispatcher = dispatcher,
        )
        val boundScope = CoroutineScope(dispatcher + SupervisorJob())

        try {
            delegate.bind(
                scope = boundScope,
                draftStateFlow = MutableStateFlow(
                    ConversationDraftState(
                        draft = ConversationDraft(
                            attachments = persistentListOf(
                                ConversationDraftAttachment(
                                    contentType = "text/x-vCard",
                                    contentUri = "content://attachments/vcard/1",
                                ),
                            ),
                        ),
                    ),
                ),
            )
            advanceUntilIdle()

            assertEquals(
                persistentListOf(
                    initialAttachment.copy(vCardUiModel = loadedUiModel),
                ),
                delegate.state.value,
            )
        } finally {
            boundScope.cancel()
        }
    }

    @Test
    fun bind_doesNotRestartObservationWhenOnlyDraftTextChanges() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val attachmentMapper = mockk<ConversationComposerAttachmentUiModelMapper>()
        val vCardUiModelMapper = mockk<ConversationVCardAttachmentUiModelMapper>()
        val metadataRepository = mockk<ConversationVCardMetadataRepository>()
        val draftStateFlow = MutableStateFlow(
            ConversationDraftState(
                draft = ConversationDraft(
                    messageText = "hello",
                    attachments = persistentListOf(
                        ConversationDraftAttachment(
                            contentType = "application/pdf",
                            contentUri = "content://attachments/file/1",
                        ),
                    ),
                ),
            ),
        )
        every {
            attachmentMapper.map(any(), any())
        } returns persistentListOf(
            ComposerAttachmentUiModel.Resolved.File(
                key = "content://attachments/file/1",
                contentType = "application/pdf",
                contentUri = "content://attachments/file/1",
            ),
        )
        val delegate = ConversationComposerAttachmentsDelegateImpl(
            conversationComposerAttachmentUiModelMapper = attachmentMapper,
            conversationVCardAttachmentUiModelMapper = vCardUiModelMapper,
            conversationVCardMetadataRepository = metadataRepository,
            defaultDispatcher = dispatcher,
        )
        val boundScope = CoroutineScope(dispatcher + SupervisorJob())

        try {
            delegate.bind(
                scope = boundScope,
                draftStateFlow = draftStateFlow,
            )
            advanceUntilIdle()

            draftStateFlow.value = draftStateFlow.value.copy(
                draft = draftStateFlow.value.draft.copy(
                    messageText = "updated text",
                ),
            )
            advanceUntilIdle()

            verify(exactly = 2) {
                attachmentMapper.map(any(), any())
            }
        } finally {
            boundScope.cancel()
        }
    }

    @Test
    fun bind_observesDuplicateVCardUrisOnlyOnce() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val attachmentMapper = mockk<ConversationComposerAttachmentUiModelMapper>()
        val vCardUiModelMapper = mockk<ConversationVCardAttachmentUiModelMapper>(relaxed = true)
        val metadataRepository = mockk<ConversationVCardMetadataRepository>()
        every {
            attachmentMapper.map(any(), any())
        } returns persistentListOf(
            createVCardAttachment(),
            createVCardAttachment(),
        )
        every {
            metadataRepository.observeAttachmentMetadata(
                contentUri = "content://attachments/vcard/1"
            )
        } returns flowOf(ConversationVCardAttachmentMetadata.Loading)
        every {
            vCardUiModelMapper.map(metadata = ConversationVCardAttachmentMetadata.Loading)
        } returns ConversationVCardAttachmentUiModel(
            type = ConversationVCardAttachmentType.CONTACT,
            titleTextResId = 1,
            subtitleTextResId = 2,
        )
        val delegate = ConversationComposerAttachmentsDelegateImpl(
            conversationComposerAttachmentUiModelMapper = attachmentMapper,
            conversationVCardAttachmentUiModelMapper = vCardUiModelMapper,
            conversationVCardMetadataRepository = metadataRepository,
            defaultDispatcher = dispatcher,
        )
        val boundScope = CoroutineScope(dispatcher + SupervisorJob())

        try {
            delegate.bind(
                scope = boundScope,
                draftStateFlow = MutableStateFlow(
                    ConversationDraftState(
                        draft = ConversationDraft(
                            attachments = persistentListOf(
                                ConversationDraftAttachment(
                                    contentType = "text/x-vCard",
                                    contentUri = "content://attachments/vcard/1",
                                ),
                                ConversationDraftAttachment(
                                    contentType = "text/x-vCard",
                                    contentUri = "content://attachments/vcard/1",
                                ),
                            ),
                        ),
                    ),
                ),
            )
            advanceUntilIdle()

            verify(exactly = 1) {
                metadataRepository.observeAttachmentMetadata(
                    contentUri = "content://attachments/vcard/1",
                )
            }
        } finally {
            boundScope.cancel()
        }
    }

    private fun createVCardAttachment(): ComposerAttachmentUiModel.Resolved.VCard {
        return ComposerAttachmentUiModel.Resolved.VCard(
            key = "content://attachments/vcard/1",
            contentType = "text/x-vCard",
            contentUri = "content://attachments/vcard/1",
            vCardUiModel = ConversationVCardAttachmentUiModel(
                type = ConversationVCardAttachmentType.CONTACT,
                titleTextResId = 1,
                subtitleTextResId = 2,
            ),
        )
    }
}
