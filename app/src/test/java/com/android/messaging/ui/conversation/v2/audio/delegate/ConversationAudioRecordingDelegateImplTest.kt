package com.android.messaging.ui.conversation.v2.audio.delegate

import android.net.Uri
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.repository.ConversationSubscriptionsRepository
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.conversation.v2.composer.delegate.ConversationDraftDelegate
import com.android.messaging.ui.conversation.v2.mediapicker.repository.ConversationAttachmentRepository
import com.android.messaging.ui.mediapicker.LevelTrackingMediaRecorder
import com.android.messaging.util.ContentType
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkConstructor
import io.mockk.verify
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationAudioRecordingDelegateImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var conversationAttachmentRepository: ConversationAttachmentRepository
    private lateinit var conversationSubscriptionsRepository: ConversationSubscriptionsRepository
    private lateinit var conversationDraftDelegate: ConversationDraftDelegate

    @Before
    fun setUp() {
        conversationAttachmentRepository = mockk()
        conversationSubscriptionsRepository = mockk()
        conversationDraftDelegate = mockk()

        every {
            conversationSubscriptionsRepository.resolveMaxMessageSize(any())
        } returns flowOf(500_000)
        every {
            conversationAttachmentRepository.deleteTemporaryAttachment(any())
        } returns flowOf(Unit)
        every {
            conversationDraftDelegate.addAttachments(any())
        } just runs
    }

    @After
    fun tearDown() {
        unmockkConstructor(LevelTrackingMediaRecorder::class)
    }

    @Test
    fun startRecording_startsRecorderAndPublishesRecordingState() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            mockkConstructor(LevelTrackingMediaRecorder::class)
            every {
                anyConstructed<LevelTrackingMediaRecorder>().startRecording(any(), any(), 500_000)
            } returns true

            val delegate = createBoundDelegate(scope = backgroundScope)

            delegate.startRecording(selfParticipantId = "self-1")
            runCurrent()

            assertEquals(true, delegate.state.value.isRecording)
            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                conversationSubscriptionsRepository.resolveMaxMessageSize(
                    selfParticipantId = "self-1",
                )
            }
            verify(exactly = 1) {
                anyConstructed<LevelTrackingMediaRecorder>().startRecording(any(), any(), 500_000)
            }
        }
    }

    @Test
    fun finishRecording_afterMinimumDuration_attachesRecordedAudio() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val outputUri = Uri.parse("content://scratch/audio/1")

            mockkConstructor(LevelTrackingMediaRecorder::class)
            every {
                anyConstructed<LevelTrackingMediaRecorder>().startRecording(any(), any(), 500_000)
            } returns true
            every {
                anyConstructed<LevelTrackingMediaRecorder>().stopRecording()
            } returns outputUri

            val delegate = createBoundDelegate(scope = backgroundScope)

            delegate.startRecording(selfParticipantId = "self-1")
            runCurrent()
            ShadowSystemClock.advanceBy(Duration.ofMillis(350))

            delegate.finishRecording()
            advanceTimeBy(delayTimeMillis = 500L)
            runCurrent()

            verify(exactly = 1) {
                conversationDraftDelegate.addAttachments(
                    attachments = listOf(
                        ConversationDraftAttachment(
                            contentType = ContentType.AUDIO_3GPP,
                            contentUri = outputUri.toString(),
                        ),
                    ),
                )
            }
            assertFalse(delegate.state.value.isRecording)
        }
    }

    @Test
    fun cancelRecording_deletesTemporaryAttachmentAndResetsState() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val outputUri = Uri.parse("content://scratch/audio/2")

            mockkConstructor(LevelTrackingMediaRecorder::class)
            every {
                anyConstructed<LevelTrackingMediaRecorder>().startRecording(any(), any(), 500_000)
            } returns true
            every {
                anyConstructed<LevelTrackingMediaRecorder>().stopRecording()
            } returns outputUri

            val delegate = createBoundDelegate(scope = backgroundScope)

            delegate.startRecording(selfParticipantId = "self-1")
            runCurrent()
            delegate.cancelRecording()
            runCurrent()

            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                conversationAttachmentRepository.deleteTemporaryAttachment(
                    contentUri = outputUri.toString(),
                )
            }
            verify(exactly = 0) {
                conversationDraftDelegate.addAttachments(any())
            }
            assertFalse(delegate.state.value.isRecording)
        }
    }

    private fun createBoundDelegate(scope: CoroutineScope): ConversationAudioRecordingDelegateImpl {
        return ConversationAudioRecordingDelegateImpl(
            conversationAttachmentRepository = conversationAttachmentRepository,
            conversationSubscriptionsRepository = conversationSubscriptionsRepository,
            conversationDraftDelegate = conversationDraftDelegate,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        ).also { delegate ->
            delegate.bind(
                scope = scope,
                conversationIdFlow = MutableStateFlow(value = "conversation-1"),
            )
        }
    }
}
