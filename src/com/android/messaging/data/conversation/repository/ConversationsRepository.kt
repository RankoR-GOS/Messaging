package com.android.messaging.data.conversation.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.db.ReversedCursor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface ConversationsRepository {
    fun getConversationMessages(conversationId: String): Flow<List<ConversationMessageData>>
}

internal class ConversationsRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : ConversationsRepository {

    override fun getConversationMessages(conversationId: String): Flow<List<ConversationMessageData>> {
        val uri = MessagingContentProvider.buildConversationMessagesUri(conversationId)
        return callbackFlow {
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }
            contentResolver.registerContentObserver(uri, true, observer)

            trySend(Unit)

            awaitClose {
                contentResolver.unregisterContentObserver(observer)
            }
        }
            .conflate()
            .map {
                queryConversationMessages(uri = uri)
            }
            .flowOn(ioDispatcher)
    }

    private fun queryConversationMessages(uri: Uri): List<ConversationMessageData> {
        return contentResolver
            .query(
                uri,
                ConversationMessageData.getProjection(),
                null, null, null,
            )
            ?.use { rawCursor ->
                val reversedCursor = ReversedCursor(cursor = rawCursor)

                buildList {
                    while (reversedCursor.moveToNext()) {
                        add(ConversationMessageData().apply { bind(reversedCursor) })
                    }
                }
            }
            ?: emptyList()
    }
}
