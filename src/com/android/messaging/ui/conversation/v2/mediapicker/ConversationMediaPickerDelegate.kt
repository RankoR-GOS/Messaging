package com.android.messaging.ui.conversation.v2.mediapicker

import com.android.messaging.data.media.model.ConversationMediaItem
import com.android.messaging.data.media.repository.ConversationMediaRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.conversation.v2.common.ConversationScreenDelegate
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.v2.mediapicker.mapper.ConversationDraftAttachmentMapper
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationCapturedMedia
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationMediaPickerUiState
import com.android.messaging.ui.conversation.v2.mediapicker.repository.ConversationAttachmentRepository
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import com.android.messaging.util.LogUtil
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal interface ConversationMediaPickerDelegate :
    ConversationScreenDelegate<ConversationMediaPickerUiState> {
    val effects: Flow<ConversationScreenEffect>

    fun onGalleryMediaConfirmed(mediaItems: List<ConversationMediaItem>)

    fun onGalleryVisibilityChanged(isVisible: Boolean)

    fun onCapturedMediaReady(capturedMedia: ConversationCapturedMedia)

    fun onContactCardPicked(contactUri: String?)

    fun onRemovePendingAttachment(pendingAttachmentId: String)

    fun onRemoveResolvedAttachment(contentUri: String)

    fun onScreenCleared()
}

internal class ConversationMediaPickerDelegateImpl @Inject constructor(
    private val conversationDraftDelegate: ConversationDraftDelegate,
    private val conversationAttachmentRepository: ConversationAttachmentRepository,
    private val conversationDraftAttachmentMapper: ConversationDraftAttachmentMapper,
    private val conversationMediaRepository: ConversationMediaRepository,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ConversationMediaPickerDelegate {

    private val _effects = MutableSharedFlow<ConversationScreenEffect>(
        extraBufferCapacity = 1,
    )
    private val _state = MutableStateFlow(ConversationMediaPickerUiState())
    private val pendingAttachmentJobs = mutableMapOf<String, Job>()

    override val effects = _effects.asSharedFlow()
    override val state = _state.asStateFlow()

    private var boundScope: CoroutineScope? = null

    override fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<String?>,
    ) {
        if (boundScope != null) {
            return
        }

        boundScope = scope

        scope.launch(defaultDispatcher) {
            conversationIdFlow
                .collect {
                    cancelPendingAttachmentJobs()
                }
        }
    }

    override fun onGalleryMediaConfirmed(mediaItems: List<ConversationMediaItem>) {
        if (mediaItems.isEmpty()) {
            return
        }

        conversationDraftDelegate.addAttachments(
            attachments = mediaItems.map { mediaItem ->
                conversationDraftAttachmentMapper.map(
                    mediaItem = mediaItem,
                )
            },
        )
    }

    override fun onGalleryVisibilityChanged(isVisible: Boolean) {
        if (!isVisible) {
            return
        }

        if (state.value.isLoadingGallery || state.value.galleryItems.isNotEmpty()) {
            return
        }

        boundScope?.launch(defaultDispatcher) {
            _state.update { currentMediaPickerUiState ->
                currentMediaPickerUiState.copy(isLoadingGallery = true)
            }

            conversationMediaRepository
                .getRecentMedia()
                .map { it.toImmutableList() }
                .catch { throwable ->
                    LogUtil.w(TAG, "Unable to query gallery items", throwable)

                    _state.update { currentMediaPickerUiState ->
                        currentMediaPickerUiState.copy(
                            isLoadingGallery = false,
                        )
                    }
                }
                .collect { galleryItems ->
                    _state.update { currentMediaPickerUiState ->
                        currentMediaPickerUiState.copy(
                            galleryItems = galleryItems,
                            isLoadingGallery = false,
                        )
                    }
                }
        }
    }

    override fun onCapturedMediaReady(capturedMedia: ConversationCapturedMedia) {
        conversationDraftDelegate.addAttachments(
            attachments = listOf(
                conversationDraftAttachmentMapper.map(
                    capturedMedia = capturedMedia,
                ),
            ),
        )
    }

    override fun onContactCardPicked(contactUri: String?) {
        val resolvedContactUri = contactUri?.takeIf { it.isNotBlank() } ?: return

        boundScope?.launch(defaultDispatcher) {
            conversationAttachmentRepository
                .createDraftAttachmentFromContact(contactUri = resolvedContactUri)
                .filterNotNull()
                .map(::listOf)
                .collect(conversationDraftDelegate::addAttachments)
        }
    }

    override fun onRemovePendingAttachment(pendingAttachmentId: String) {
        pendingAttachmentJobs.remove(pendingAttachmentId)?.cancel()
        conversationDraftDelegate.removePendingAttachment(
            pendingAttachmentId = pendingAttachmentId,
        )
    }

    override fun onRemoveResolvedAttachment(contentUri: String) {
        conversationDraftDelegate.removeAttachment(contentUri = contentUri)

        boundScope?.launch(defaultDispatcher) {
            conversationAttachmentRepository
                .deleteTemporaryAttachment(contentUri = contentUri)
                .collect()
        }
    }

    override fun onScreenCleared() {
        cancelPendingAttachmentJobs()
    }

    private fun cancelPendingAttachmentJobs() {
        pendingAttachmentJobs.values.forEach { it.cancel() }
        pendingAttachmentJobs.clear()
    }

    private companion object {
        private const val TAG = "ConversationMediaPickerDelegate"
    }
}
