package com.android.messaging.util

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import com.android.messaging.Factory
import com.android.messaging.R
import com.android.messaging.data.conversationsettings.repository.ConversationSnoozeQuery
import com.android.messaging.datamodel.BugleNotifications

object InConversationSound {

    private const val VOLUME = 0.25f

    @Volatile
    private var playing: Ringtone? = null

    @JvmStatic
    fun playIfEnabled(conversationId: String?) {
        val context = Factory.get().applicationContext
        if (
            conversationId.isNullOrEmpty() ||
            !isEnabled(context) ||
            !isAudible(context, conversationId)
        ) {
            return
        }

        val ringtone = RingtoneUtil.getNotificationRingtoneUri(conversationId, null)
            ?.let { RingtoneManager.getRingtone(context, it) }
            ?: return

        ringtone.audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        ringtone.volume = VOLUME
        ringtone.isLooping = false

        playing?.stop()
        playing = ringtone
        ringtone.play()
    }

    private fun isEnabled(context: Context): Boolean {
        return BuglePrefs.getApplicationPrefs().getBoolean(
            context.getString(R.string.in_conversation_sound_pref_key),
            context.resources.getBoolean(R.bool.in_conversation_sound_pref_default),
        )
    }

    private fun isAudible(context: Context, conversationId: String): Boolean {
        val notificationManager = NotificationChannelUtil.getNotificationManager()
        val interruptionFilter = notificationManager.currentInterruptionFilter
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // A conversation gets its own channel only once a notification has been posted for it,
        // so fall back to the channel it would have inherited from.
        val channel = NotificationChannelUtil.getConversationChannel(conversationId)
            ?: notificationManager
                .getNotificationChannel(NotificationChannelUtil.INCOMING_MESSAGES)

        return notificationManager.areNotificationsEnabled() &&
            interruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL &&
            audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL &&
            // IMPORTANCE_NONE means blocked, below IMPORTANCE_DEFAULT means silent.
            (channel == null || channel.importance >= NotificationManager.IMPORTANCE_DEFAULT) &&
            !BugleNotifications.isConversationBlocked(conversationId) &&
            !ConversationSnoozeQuery.isConversationSnoozed(conversationId)
    }
}
