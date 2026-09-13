package com.android.messaging.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import com.android.messaging.FactoryTestAccess
import com.android.messaging.R
import com.android.messaging.data.conversationsettings.repository.ConversationSnoozeQuery
import com.android.messaging.datamodel.BugleNotifications
import com.android.messaging.testutil.FakeBuglePrefs
import com.android.messaging.testutil.createIncomingMessagesTestChannel
import com.android.messaging.testutil.installTestFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class InConversationSoundTest {

    private val context: Context = RuntimeEnvironment.getApplication().applicationContext
    private val prefs = FakeBuglePrefs()
    private val ringtone = mockk<Ringtone>(relaxed = true)

    @Before
    fun setUp() {
        installTestFactory(context = context, prefs = prefs)
        createIncomingMessagesTestChannel()
        mockkStatic(RingtoneUtil::class)
        every { RingtoneUtil.getNotificationRingtoneUri(any(), any()) } returns RINGTONE_URI
        mockkStatic(RingtoneManager::class)
        every { RingtoneManager.getRingtone(any(), any()) } returns ringtone
        mockkStatic(ConversationSnoozeQuery::class)
        every { ConversationSnoozeQuery.isConversationSnoozed(any()) } returns false
        mockkStatic(BugleNotifications::class)
        every { BugleNotifications.isConversationBlocked(any()) } returns false
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun playIfEnabled_byDefault_staysSilent() {
        // The chime is opt-in: issue #298 is fixed for anyone who never touches the setting.
        InConversationSound.playIfEnabled(CONVERSATION_ID)

        verify(exactly = 0) { ringtone.play() }
    }

    @Test
    fun playIfEnabled_whenEnabled_plays() {
        givenSoundEnabled()

        InConversationSound.playIfEnabled(CONVERSATION_ID)

        verify(exactly = 1) { ringtone.play() }
    }

    @Test
    fun playIfEnabled_withoutConversationId_staysSilent() {
        givenSoundEnabled()

        InConversationSound.playIfEnabled(null)
        InConversationSound.playIfEnabled("")

        verify(exactly = 0) { ringtone.play() }
    }

    @Test
    fun playIfEnabled_whenDoNotDisturbIsOn_staysSilent() {
        givenSoundEnabled()
        shadowOf(notificationManager()).setNotificationPolicyAccessGranted(true)
        notificationManager()
            .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)

        InConversationSound.playIfEnabled(CONVERSATION_ID)

        verify(exactly = 0) { ringtone.play() }
    }

    @Test
    fun playIfEnabled_whenRingerIsSilenced_staysSilent() {
        givenSoundEnabled()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT

        InConversationSound.playIfEnabled(CONVERSATION_ID)

        verify(exactly = 0) { ringtone.play() }
    }

    @Test
    fun playIfEnabled_whenAppNotificationsAreDisabled_staysSilent() {
        givenSoundEnabled()
        shadowOf(notificationManager()).setNotificationsEnabled(false)

        InConversationSound.playIfEnabled(CONVERSATION_ID)

        verify(exactly = 0) { ringtone.play() }
    }

    @Test
    fun playIfEnabled_whenConversationIsMuted_staysSilent() {
        givenSoundEnabled()
        givenConversationChannel(NotificationManager.IMPORTANCE_LOW)

        InConversationSound.playIfEnabled(CONVERSATION_ID)

        verify(exactly = 0) { ringtone.play() }
    }

    @Test
    fun playIfEnabled_whenTheIncomingMessagesChannelIsSilenced_staysSilent() {
        // A conversation the user has only ever read live has no channel of its own yet, so the
        // one it would inherit from is what decides.
        givenSoundEnabled()
        createIncomingMessagesTestChannel(importance = NotificationManager.IMPORTANCE_LOW)

        InConversationSound.playIfEnabled(CONVERSATION_ID)

        verify(exactly = 0) { ringtone.play() }
    }

    @Test
    fun playIfEnabled_whenTheSenderIsBlocked_staysSilent() {
        givenSoundEnabled()
        every { BugleNotifications.isConversationBlocked(CONVERSATION_ID) } returns true

        InConversationSound.playIfEnabled(CONVERSATION_ID)

        verify(exactly = 0) { ringtone.play() }
    }

    @Test
    fun playIfEnabled_forABurstOfMessages_stopsThePreviousChime() {
        givenSoundEnabled()

        InConversationSound.playIfEnabled(CONVERSATION_ID)
        InConversationSound.playIfEnabled(CONVERSATION_ID)

        verify(exactly = 2) { ringtone.play() }
        verify(exactly = 1) { ringtone.stop() }
    }

    @Test
    fun playIfEnabled_whenConversationIsSnoozed_staysSilent() {
        givenSoundEnabled()
        every { ConversationSnoozeQuery.isConversationSnoozed(CONVERSATION_ID) } returns true

        InConversationSound.playIfEnabled(CONVERSATION_ID)

        verify(exactly = 0) { ringtone.play() }
    }

    @Test
    fun playIfEnabled_whenConversationSoundIsNone_staysSilent() {
        givenSoundEnabled()
        every { RingtoneUtil.getNotificationRingtoneUri(any(), any()) } returns null

        InConversationSound.playIfEnabled(CONVERSATION_ID)

        verify(exactly = 0) { ringtone.play() }
    }

    private fun givenSoundEnabled() {
        prefs.putBoolean(context.getString(R.string.in_conversation_sound_pref_key), true)
    }

    private fun givenConversationChannel(importance: Int) {
        val channel = NotificationChannel(CONVERSATION_ID, "Alice", importance)
        channel.setConversationId(NotificationChannelUtil.INCOMING_MESSAGES, CONVERSATION_ID)
        notificationManager().createNotificationChannel(channel)
    }

    private fun notificationManager(): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private companion object {
        private const val CONVERSATION_ID = "194"
        private val RINGTONE_URI: Uri = Uri.parse("content://settings/system/notification_sound")
    }
}
