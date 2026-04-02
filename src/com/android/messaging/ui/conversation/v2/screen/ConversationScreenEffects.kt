package com.android.messaging.ui.conversation.v2.screen

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import com.android.messaging.util.UiUtils

@Composable
internal fun ConversationScreenEffects(
    screenModel: ConversationScreenModel,
) {
    val context = LocalContext.current

    LaunchedEffect(screenModel, context) {
        screenModel.effects.collect { effect ->
            when (effect) {
                is ConversationScreenEffect.OpenAttachmentPreview -> {
                    openAttachmentPreview(
                        context = context,
                        contentUri = effect.contentUri,
                        contentType = effect.contentType,
                    )
                }

                is ConversationScreenEffect.ShowMessage -> {
                    UiUtils.showToastAtBottom(effect.messageResId)
                }
            }
        }
    }
}

private fun openAttachmentPreview(
    context: Context,
    contentUri: String,
    contentType: String,
) {
    val previewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(contentUri.toUri(), contentType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    runCatching {
        context.startActivity(previewIntent)
    }.onFailure {
        UiUtils.showToastAtBottom(R.string.activity_not_found_message)
    }
}
