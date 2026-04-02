package com.android.messaging.di.conversation

import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftDelegateImpl
import com.android.messaging.ui.conversation.v2.mediapicker.ConversationMediaPickerDelegate
import com.android.messaging.ui.conversation.v2.mediapicker.ConversationMediaPickerDelegateImpl
import com.android.messaging.ui.conversation.v2.messages.delegate.ConversationMessagesDelegate
import com.android.messaging.ui.conversation.v2.messages.delegate.ConversationMessagesDelegateImpl
import com.android.messaging.ui.conversation.v2.metadata.delegate.ConversationMetadataDelegate
import com.android.messaging.ui.conversation.v2.metadata.delegate.ConversationMetadataDelegateImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal abstract class ConversationViewModelBindsModule {

    @Binds
    @ViewModelScoped
    abstract fun bindConversationDraftDelegate(
        impl: ConversationDraftDelegateImpl,
    ): ConversationDraftDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindConversationMediaPickerDelegate(
        impl: ConversationMediaPickerDelegateImpl,
    ): ConversationMediaPickerDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindConversationMessagesDelegate(
        impl: ConversationMessagesDelegateImpl,
    ): ConversationMessagesDelegate

    @Binds
    @ViewModelScoped
    abstract fun bindConversationMetadataDelegate(
        impl: ConversationMetadataDelegateImpl,
    ): ConversationMetadataDelegate
}
