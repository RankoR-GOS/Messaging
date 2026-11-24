package com.android.messaging.di.message

import com.android.messaging.data.message.mapper.MessageDataMapper
import com.android.messaging.data.message.mapper.MessageDataMapperImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MessageBindsModule {

    @Binds
    @Reusable
    abstract fun bindMessageDataMapper(impl: MessageDataMapperImpl): MessageDataMapper
}
