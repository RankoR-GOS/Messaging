package com.android.messaging.ui.conversation.v2.screen

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.ui.conversation.v2.CONVERSATION_LOADING_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_MESSAGES_LIST_TEST_TAG
import com.android.messaging.ui.conversation.v2.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.v2.conversationMessageItemTestTag
import com.android.messaging.ui.conversation.v2.messages.model.ConversationMessageUiModel
import com.android.messaging.ui.conversation.v2.messages.model.ConversationMessagesUiState
import com.android.messaging.ui.conversation.v2.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import com.android.messaging.ui.conversation.v2.screen.model.ConversationUiState
import com.android.messaging.ui.core.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConversationScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loadingState_showsLoadingIndicator() {
        val screenModel = FakeConversationScreenModel()

        setScreenContent(screenModel = screenModel)

        composeTestRule
            .onNodeWithTag(CONVERSATION_LOADING_INDICATOR_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun presentState_showsMessagesList() {
        val screenModel = FakeConversationScreenModel()
        screenModel.uiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 8,
                latestMessageId = "message-8",
                latestMessageIncoming = false,
            ),
        )

        setScreenContent(screenModel = screenModel)

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .assertExists()
        composeTestRule
            .onNodeWithTag(CONVERSATION_LOADING_INDICATOR_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun stoppingLifecycle_persistsDraft() {
        val screenModel = FakeConversationScreenModel()
        lateinit var lifecycleOwner: TestLifecycleOwner

        composeTestRule.runOnIdle {
            lifecycleOwner = TestLifecycleOwner(
                initialState = Lifecycle.State.RESUMED,
            )
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                AppTheme {
                    ConversationScreen(
                        conversationId = "conversation-1",
                        screenModel = screenModel,
                    )
                }
            }
        }
        composeTestRule.runOnIdle {
            lifecycleOwner.moveTo(state = Lifecycle.State.CREATED)
        }
        composeTestRule.waitForIdle()

        assertEquals(1, screenModel.persistDraftCalls)
    }

    @Test
    fun outgoingInsert_scrollsToLatestMessage() {
        val screenModel = FakeConversationScreenModel()
        screenModel.uiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 30,
                latestMessageId = "message-30",
                latestMessageIncoming = false,
            ),
        )

        setScreenContent(screenModel = screenModel)
        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .performScrollToIndex(index = 20)

        screenModel.uiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 31,
                latestMessageId = "message-31",
                latestMessageIncoming = false,
            ),
        )
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(conversationMessageItemTestTag(messageId = "message-31"))
            .assertExists()
    }

    @Test
    fun incomingInsert_doesNotScrollToLatestWhenUserIsAwayFromEnd() {
        val screenModel = FakeConversationScreenModel()
        screenModel.uiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 30,
                latestMessageId = "message-30",
                latestMessageIncoming = false,
            ),
        )

        setScreenContent(screenModel = screenModel)
        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .performScrollToIndex(index = 20)

        screenModel.uiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 31,
                latestMessageId = "message-31",
                latestMessageIncoming = true,
            ),
        )
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(conversationMessageItemTestTag(messageId = "message-31"))
            .assertDoesNotExist()
    }

    @Test
    fun conversationChange_resetsListStateToLatestMessage() {
        val screenModel = FakeConversationScreenModel()
        var conversationId by mutableStateOf("conversation-1")
        screenModel.uiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 30,
                latestMessageId = "conversation-1-message-30",
                latestMessageIncoming = false,
                messageIdPrefix = "conversation-1-message",
            ),
        )

        composeTestRule.setContent {
            AppTheme {
                ConversationScreen(
                    conversationId = conversationId,
                    screenModel = screenModel,
                )
            }
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .performScrollToIndex(index = 20)
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag(conversationMessageItemTestTag(messageId = "conversation-1-message-30"))
            .assertDoesNotExist()

        composeTestRule.runOnIdle {
            conversationId = "conversation-2"
            screenModel.uiStateFlow.value = createPresentUiState(
                messages = createMessages(
                    count = 5,
                    latestMessageId = "conversation-2-message-5",
                    latestMessageIncoming = false,
                    messageIdPrefix = "conversation-2-message",
                ),
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(conversationMessageItemTestTag(messageId = "conversation-2-message-5"))
            .assertExists()
    }

    private fun setScreenContent(screenModel: FakeConversationScreenModel) {
        composeTestRule.setContent {
            AppTheme {
                ConversationScreen(
                    conversationId = "conversation-1",
                    screenModel = screenModel,
                )
            }
        }
    }

    private fun createPresentUiState(
        messages: List<ConversationMessageUiModel>,
    ): ConversationUiState {
        return ConversationUiState(
            metadata = ConversationMetadataUiState.Present(
                title = "Weekend plan",
                selfParticipantId = "self-1",
                isGroupConversation = false,
                participantCount = 2,
                composerAvailability = ConversationComposerAvailability.editable(),
            ),
            messages = ConversationMessagesUiState.Present(
                messages = messages,
            ),
            composer = ConversationComposerUiState(
                isMessageFieldEnabled = true,
                isSendEnabled = true,
            ),
        )
    }

    private fun createMessages(
        count: Int,
        latestMessageId: String,
        latestMessageIncoming: Boolean,
        messageIdPrefix: String = "message",
    ): List<ConversationMessageUiModel> {
        val messages = mutableListOf<ConversationMessageUiModel>()
        for (index in 1..count) {
            val messageId = "$messageIdPrefix-$index"
            val isLatestMessage = messageId == latestMessageId
            messages += ConversationMessageUiModel(
                messageId = messageId,
                conversationId = "conversation-1",
                text = "Message $index",
                parts = emptyList(),
                sentTimestamp = index.toLong(),
                receivedTimestamp = index.toLong(),
                displayTimestamp = index.toLong(),
                status = ConversationMessageUiModel.Status.Outgoing.Complete,
                isIncoming = isLatestMessage && latestMessageIncoming,
                senderDisplayName = null,
                senderAvatarUri = null,
                senderContactLookupKey = null,
                canClusterWithPrevious = false,
                canClusterWithNext = false,
                mmsSubject = null,
                protocol = ConversationMessageUiModel.Protocol.SMS,
            )
        }

        return messages
    }

    private class FakeConversationScreenModel : ConversationScreenModel {
        val effectsFlow = MutableSharedFlow<ConversationScreenEffect>()
        val uiStateFlow = MutableStateFlow(ConversationUiState())

        override val effects: Flow<ConversationScreenEffect> = effectsFlow
        override val uiState: StateFlow<ConversationUiState> = uiStateFlow

        var attachmentClicks = 0
        var persistDraftCalls = 0
        var sendClicks = 0

        override fun onConversationChanged(conversationId: String?) {
        }

        override fun onMessageTextChanged(text: String) {
        }

        override fun onAttachmentClick() {
            attachmentClicks += 1
        }

        override fun onSendClick() {
            sendClicks += 1
        }

        override fun persistDraft() {
            persistDraftCalls += 1
        }
    }

    private class TestLifecycleOwner(initialState: Lifecycle.State) : LifecycleOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)

        init {
            lifecycleRegistry.currentState = initialState
        }

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        fun moveTo(
            state: Lifecycle.State,
        ) {
            lifecycleRegistry.currentState = state
        }
    }
}
