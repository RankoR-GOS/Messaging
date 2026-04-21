package com.android.messaging.ui.conversation.v2.messages.delegate

import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.conversation.v2.common.ConversationScreenDelegate
import com.android.messaging.ui.conversation.v2.messages.mapper.ConversationMessageUiModelMapper
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.v2.messages.repository.ConversationVCardMetadataRepository
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal interface ConversationMessagesDelegate :
    ConversationScreenDelegate<ConversationMessagesUiState>

internal class ConversationMessagesDelegateImpl @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val conversationMessageUiModelMapper: ConversationMessageUiModelMapper,
    private val conversationVCardMetadataRepository: ConversationVCardMetadataRepository,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ConversationMessagesDelegate {

    private val _state = MutableStateFlow<ConversationMessagesUiState>(
        value = ConversationMessagesUiState.Loading,
    )

    override val state = _state.asStateFlow()

    private var isBound = false

    override fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<String?>,
    ) {
        if (isBound) {
            return
        }

        isBound = true

        scope.launch(defaultDispatcher) {
            conversationIdFlow.collectLatest { conversationId ->
                _state.value = ConversationMessagesUiState.Loading

                if (conversationId == null) {
                    return@collectLatest
                }

                observeConversationMessagesUiState(
                    conversationId = conversationId,
                ).collect { currentMessagesUiState ->
                    _state.value = currentMessagesUiState
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeConversationMessagesUiState(
        conversationId: String,
    ): Flow<ConversationMessagesUiState> {
        return conversationsRepository
            .getConversationMessages(conversationId = conversationId)
            .map { messages ->
                messages
                    .asSequence()
                    .map(conversationMessageUiModelMapper::map)
                    .toImmutableList()
            }
            .flatMapLatest { messages ->
                observeConversationMessagesUiState(
                    messages = messages,
                )
            }
            .flowOn(defaultDispatcher)
    }

    private fun observeConversationMessagesUiState(
        messages: List<ConversationMessageUiModel>,
    ): Flow<ConversationMessagesUiState> {
        val vCardContentUris = messages
            .asSequence()
            .flatMap { message -> message.parts.asSequence() }
            .mapNotNull { part ->
                (part as? ConversationMessagePartUiModel.Attachment.VCard)
                    ?.contentUri
                    ?.toString()
            }
            .distinct()
            .toList()

        if (vCardContentUris.isEmpty()) {
            return flowOf(
                ConversationMessagesUiState.Present(
                    messages = messages.toImmutableList(),
                ),
            )
        }

        val vCardAttachmentUiStateFlows = vCardContentUris.map { contentUri ->
            conversationVCardMetadataRepository
                .observeAttachmentMetadata(contentUri = contentUri)
                .map { metadata ->
                    contentUri to metadata
                }
        }

        return combine(flows = vCardAttachmentUiStateFlows) { contentUriAndUiStates ->
            val vCardAttachmentMetadata = contentUriAndUiStates.associate { pair ->
                pair.first to pair.second
            }

            ConversationMessagesUiState.Present(
                messages = messages
                    .map { message ->
                        message.withVCardAttachmentMetadata(
                            vCardAttachmentMetadata = vCardAttachmentMetadata,
                        )
                    }
                    .toImmutableList(),
            )
        }
    }
}

private fun ConversationMessageUiModel.withVCardAttachmentMetadata(
    vCardAttachmentMetadata: Map<String, ConversationVCardAttachmentMetadata>,
): ConversationMessageUiModel {
    return copy(
        parts = parts.map { part ->
            part.withVCardAttachmentMetadata(
                vCardAttachmentMetadata = vCardAttachmentMetadata,
            )
        },
    )
}

private fun ConversationMessagePartUiModel.withVCardAttachmentMetadata(
    vCardAttachmentMetadata: Map<String, ConversationVCardAttachmentMetadata>,
): ConversationMessagePartUiModel {
    return when (this) {
        is ConversationMessagePartUiModel.Attachment.VCard -> {
            val contentUri = contentUri?.toString()

            copy(
                metadata = contentUri?.let(vCardAttachmentMetadata::get),
            )
        }

        is ConversationMessagePartUiModel.Attachment.Audio,
        is ConversationMessagePartUiModel.Attachment.File,
        is ConversationMessagePartUiModel.Attachment.Image,
        is ConversationMessagePartUiModel.Attachment.Video,
        is ConversationMessagePartUiModel.Text,
        -> {
            this
        }
    }
}
