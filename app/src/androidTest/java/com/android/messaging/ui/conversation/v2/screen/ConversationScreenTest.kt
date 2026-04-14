package com.android.messaging.ui.conversation.v2.screen

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_COMPOSE_BAR_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_LOADING_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_MEDIA_PICKER_OVERLAY_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_MESSAGES_LIST_TEST_TAG
import com.android.messaging.ui.conversation.v2.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.v2.conversationMessageItemTestTag
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.v2.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationLaunchRequest
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMediaPickerOverlayUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenScaffoldUiState
import com.android.messaging.ui.core.AppTheme
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ConversationScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        unmockkAll()
        clearAllMocks()
    }

    @Test
    fun loadingState_showsLoadingIndicator() {
        val screenModel = createScreenModel()

        setScreenContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithTag(CONVERSATION_LOADING_INDICATOR_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun presentState_showsMessagesList() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 8,
                latestMessageId = "message-8",
                latestMessageIncoming = false,
            ),
        )

        setScreenContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .assertExists()
        composeTestRule
            .onNodeWithTag(CONVERSATION_LOADING_INDICATOR_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun stoppingLifecycle_persistsDraft() {
        val screenModel = createScreenModel()
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
                        launchRequest = createLaunchRequest(conversationId = "conversation-1"),
                        screenModel = screenModel.model,
                    )
                }
            }
        }
        composeTestRule.runOnIdle {
            lifecycleOwner.moveTo(state = Lifecycle.State.CREATED)
        }
        composeTestRule.waitForIdle()

        verify(exactly = 1) {
            screenModel.model.persistDraft()
        }
    }

    @Test
    fun outgoingInsert_scrollsToLatestMessage() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 30,
                latestMessageId = "message-30",
                latestMessageIncoming = false,
            ),
        )

        setScreenContent(screenModel = screenModel.model)
        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .performScrollToIndex(index = 20)

        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
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
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 30,
                latestMessageId = "message-30",
                latestMessageIncoming = false,
            ),
        )

        setScreenContent(screenModel = screenModel.model)
        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .performScrollToIndex(index = 20)

        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
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
        val screenModel = createScreenModel()
        var launchRequest by mutableStateOf(
            createLaunchRequest(conversationId = "conversation-1"),
        )
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
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
                    launchRequest = launchRequest,
                    screenModel = screenModel.model,
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
            launchRequest = createLaunchRequest(
                conversationId = "conversation-2",
                launchGeneration = 2,
            )
            screenModel.scaffoldUiStateFlow.value = createPresentUiState(
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

    @Test
    fun openingMediaPicker_hidesComposerAndShowsOverlay() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 3,
                latestMessageId = "message-3",
                latestMessageIncoming = false,
            ),
        )

        setScreenContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithTag(CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_COMPOSE_BAR_TEST_TAG)
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithTag(CONVERSATION_MEDIA_PICKER_OVERLAY_TEST_TAG)
            .assertCountEquals(1)
    }

    private fun setScreenContent(screenModel: ConversationScreenModel) {
        composeTestRule.setContent {
            AppTheme {
                ConversationScreen(
                    launchRequest = createLaunchRequest(conversationId = "conversation-1"),
                    screenModel = screenModel,
                )
            }
        }
    }

    private fun createPresentUiState(
        messages: List<ConversationMessageUiModel>,
    ): ConversationScreenScaffoldUiState {
        return ConversationScreenScaffoldUiState(
            metadata = ConversationMetadataUiState.Present(
                title = "Weekend plan",
                selfParticipantId = "self-1",
                isGroupConversation = false,
                participantCount = 2,
                composerAvailability = ConversationComposerAvailability.editable(),
            ),
            messages = ConversationMessagesUiState.Present(
                messages = messages.toPersistentList(),
            ),
            composer = ConversationComposerUiState(
                isMessageFieldEnabled = true,
                isAttachmentActionEnabled = true,
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

    private fun createScreenModel(): ScreenModelHandle {
        val effectsFlow = MutableSharedFlow<ConversationScreenEffect>()
        val scaffoldUiStateFlow = MutableStateFlow(ConversationScreenScaffoldUiState())
        val mediaPickerOverlayUiStateFlow = MutableStateFlow(
            ConversationMediaPickerOverlayUiState(),
        )
        val model = mockk<ConversationScreenModel>(relaxed = true)

        every { model.effects } returns effectsFlow
        every { model.scaffoldUiState } returns scaffoldUiStateFlow
        every { model.mediaPickerOverlayUiState } returns mediaPickerOverlayUiStateFlow

        return ScreenModelHandle(
            model = model,
            scaffoldUiStateFlow = scaffoldUiStateFlow,
        )
    }

    private class ScreenModelHandle(
        val model: ConversationScreenModel,
        val scaffoldUiStateFlow: MutableStateFlow<ConversationScreenScaffoldUiState>,
    )

    private class TestLifecycleOwner(
        initialState: Lifecycle.State,
    ) : LifecycleOwner {
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

    private fun createLaunchRequest(
        conversationId: String,
        launchGeneration: Int = 1,
    ): ConversationLaunchRequest {
        return ConversationLaunchRequest(
            launchGeneration = launchGeneration,
            conversationId = conversationId,
        )
    }
}
