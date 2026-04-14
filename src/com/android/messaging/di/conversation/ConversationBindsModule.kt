package com.android.messaging.di.conversation

import com.android.messaging.data.conversation.mapper.ConversationDraftMessageDataMapper
import com.android.messaging.data.conversation.mapper.ConversationDraftMessageDataMapperImpl
import com.android.messaging.data.conversation.mapper.ConversationMessageDataDraftMapper
import com.android.messaging.data.conversation.mapper.ConversationMessageDataDraftMapperImpl
import com.android.messaging.data.conversation.repository.ConversationDraftStore
import com.android.messaging.data.conversation.repository.ConversationDraftStoreImpl
import com.android.messaging.data.conversation.repository.ConversationDraftsRepository
import com.android.messaging.data.conversation.repository.ConversationDraftsRepositoryImpl
import com.android.messaging.data.conversation.repository.ConversationMetadataNotifier
import com.android.messaging.data.conversation.repository.ConversationMetadataNotifierImpl
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.data.conversation.repository.ConversationsRepositoryImpl
import com.android.messaging.data.media.repository.ConversationMediaRepository
import com.android.messaging.data.media.repository.ConversationMediaRepositoryImpl
import com.android.messaging.domain.conversation.usecase.SendConversationDraft
import com.android.messaging.domain.conversation.usecase.SendConversationDraftImpl
import com.android.messaging.ui.conversation.v2.composer.mapper.ConversationComposerUiStateMapper
import com.android.messaging.ui.conversation.v2.composer.mapper.ConversationComposerUiStateMapperImpl
import com.android.messaging.ui.conversation.v2.mediapicker.ConversationAttachmentBridge
import com.android.messaging.ui.conversation.v2.mediapicker.ConversationAttachmentBridgeImpl
import com.android.messaging.ui.conversation.v2.messages.mapper.ConversationMessageUiModelMapper
import com.android.messaging.ui.conversation.v2.messages.mapper.ConversationMessageUiModelMapperImpl
import com.android.messaging.ui.conversation.v2.metadata.mapper.ConversationMetadataUiStateMapper
import com.android.messaging.ui.conversation.v2.metadata.mapper.ConversationMetadataUiStateMapperImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ConversationBindsModule {

    @Binds
    @Reusable
    abstract fun bindConversationDraftMessageDataMapper(
        impl: ConversationDraftMessageDataMapperImpl,
    ): ConversationDraftMessageDataMapper

    @Binds
    @Reusable
    abstract fun bindConversationMessageDataDraftMapper(
        impl: ConversationMessageDataDraftMapperImpl,
    ): ConversationMessageDataDraftMapper

    @Binds
    @Reusable
    abstract fun bindConversationDraftStore(
        impl: ConversationDraftStoreImpl,
    ): ConversationDraftStore

    @Binds
    @Reusable
    abstract fun bindConversationMetadataNotifier(
        impl: ConversationMetadataNotifierImpl,
    ): ConversationMetadataNotifier

    @Binds
    @Reusable
    abstract fun bindConversationDraftsRepository(
        impl: ConversationDraftsRepositoryImpl,
    ): ConversationDraftsRepository

    @Binds
    @Reusable
    abstract fun bindConversationsRepository(
        impl: ConversationsRepositoryImpl,
    ): ConversationsRepository

    @Binds
    abstract fun bindConversationAttachmentBridge(
        impl: ConversationAttachmentBridgeImpl,
    ): ConversationAttachmentBridge

    @Binds
    abstract fun bindConversationComposerUiStateMapper(
        impl: ConversationComposerUiStateMapperImpl,
    ): ConversationComposerUiStateMapper

    @Binds
    abstract fun bindConversationMessageUiModelMapper(
        impl: ConversationMessageUiModelMapperImpl,
    ): ConversationMessageUiModelMapper

    @Binds
    @Reusable
    abstract fun bindConversationMediaRepository(
        impl: ConversationMediaRepositoryImpl,
    ): ConversationMediaRepository

    @Binds
    abstract fun bindConversationMetadataUiStateMapper(
        impl: ConversationMetadataUiStateMapperImpl,
    ): ConversationMetadataUiStateMapper

    @Binds
    @Reusable
    abstract fun bindSendConversationDraft(
        impl: SendConversationDraftImpl,
    ): SendConversationDraft
}
