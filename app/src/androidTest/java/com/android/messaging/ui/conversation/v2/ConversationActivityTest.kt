package com.android.messaging.ui.conversation.v2

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryLaunchRequest
import com.android.messaging.ui.conversationlist.ConversationListActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationActivityTest {

    @Test
    fun launch_parsesIntentExtrasAndConsumesDraftExtra() {
        val draftData = MessageData.createDraftSmsMessage(
            "conversation-1",
            "self-1",
            "Hello",
        )
        val scenario = ActivityScenario.launch<ConversationActivity>(
            createConversationIntent().apply {
                putExtra(UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID, "conversation-1")
                putExtra(UIIntents.UI_INTENT_EXTRA_DRAFT_DATA, draftData)
                putExtra(UIIntents.UI_INTENT_EXTRA_ATTACHMENT_URI, "content://media/image/1")
                putExtra(UIIntents.UI_INTENT_EXTRA_ATTACHMENT_TYPE, "image/jpeg")
            },
        )

        scenario.use { scenario ->
            scenario.onActivity { activity ->
                val launchRequest = activity.getLaunchRequest()

                assertNotNull(launchRequest)
                assertEquals(0, launchRequest?.launchGeneration)
                assertEquals("conversation-1", launchRequest?.conversationId)
                assertEquals("Hello", launchRequest?.draftData?.messageText)
                assertEquals("self-1", launchRequest?.draftData?.selfId)
                assertEquals("content://media/image/1", launchRequest?.startupAttachmentUri)
                assertEquals("image/jpeg", launchRequest?.startupAttachmentType)
                assertFalse(activity.intent.hasExtra(UIIntents.UI_INTENT_EXTRA_DRAFT_DATA))
            }
        }
    }

    @Test
    fun launch_treatsBlankAttachmentExtrasAsAbsent() {
        val scenario = ActivityScenario.launch<ConversationActivity>(
            createConversationIntent().apply {
                putExtra(UIIntents.UI_INTENT_EXTRA_ATTACHMENT_URI, "")
                putExtra(UIIntents.UI_INTENT_EXTRA_ATTACHMENT_TYPE, "")
            },
        )

        scenario.use { scenario ->
            scenario.onActivity { activity ->
                val launchRequest = activity.getLaunchRequest()

                assertEquals(0, launchRequest?.launchGeneration)
                assertNull(launchRequest?.startupAttachmentUri)
                assertNull(launchRequest?.startupAttachmentType)
            }
        }
    }

    @Test
    fun onNewIntent_incrementsLaunchGenerationAndReplacesLaunchRequest() {
        val scenario = ActivityScenario.launch<ConversationActivity>(
            createConversationIntent().apply {
                putExtra(UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID, "conversation-1")
            },
        )

        scenario.use { scenario ->
            scenario.onActivity { activity ->
                activity.invokeOnNewIntent(
                    intent = createConversationIntent().apply {
                        putExtra(UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID, "conversation-2")
                    },
                )

                assertEquals(
                    ConversationEntryLaunchRequest(
                        launchGeneration = 1,
                        conversationId = "conversation-2",
                    ),
                    activity.getLaunchRequest(),
                )
            }
        }
    }

    @Test
    fun launch_withConversationListRedirect_finishesAndStartsConversationListActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(
            ConversationListActivity::class.java.name,
            null,
            false,
        )
        val scenario = ActivityScenario.launch<ConversationActivity>(
            createConversationIntent().apply {
                putExtra(UIIntents.UI_INTENT_EXTRA_GOTO_CONVERSATION_LIST, true)
            },
        )

        try {
            val startedActivity = instrumentation.waitForMonitorWithTimeout(
                monitor,
                5_000,
            )

            assertNotNull(startedActivity)
            assertTrue(scenario.state == Lifecycle.State.DESTROYED)

            startedActivity?.finish()
        } finally {
            instrumentation.removeMonitor(monitor)
            scenario.close()
        }
    }

    private fun createConversationIntent(): Intent {
        return Intent(
            ApplicationProvider.getApplicationContext(),
            ConversationActivity::class.java,
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun ConversationActivity.getLaunchRequest(): ConversationEntryLaunchRequest? {
        val method = ConversationActivity::class.java.getDeclaredMethod("getLaunchRequest")
        method.isAccessible = true
        return method.invoke(this) as ConversationEntryLaunchRequest?
    }

    private fun ConversationActivity.invokeOnNewIntent(intent: Intent) {
        val method = ConversationActivity::class.java.getDeclaredMethod(
            "onNewIntent",
            Intent::class.java,
        )
        method.isAccessible = true
        method.invoke(this, intent)
    }
}
