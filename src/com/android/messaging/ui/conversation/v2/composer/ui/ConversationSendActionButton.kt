package com.android.messaging.ui.conversation.v2.composer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.core.AppTheme

@Composable
internal fun ConversationSendActionButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current

    FilledIconButton(
        modifier = modifier
            .size(size = 56.dp),
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
        enabled = enabled,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.Send,
            contentDescription = stringResource(id = R.string.sendButtonContentDescription),
        )
    }
}

@Preview(name = "Enabled", showBackground = true)
@Composable
private fun ConversationSendActionButtonEnabledPreview() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ConversationSendActionButton(
                enabled = true,
                onClick = {},
            )
        }
    }
}

@Preview(name = "Disabled", showBackground = true)
@Composable
private fun ConversationSendActionButtonDisabledPreview() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ConversationSendActionButton(
                enabled = false,
                onClick = {},
            )
        }
    }
}
