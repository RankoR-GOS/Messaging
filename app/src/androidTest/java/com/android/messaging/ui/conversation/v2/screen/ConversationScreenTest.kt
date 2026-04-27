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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.espresso.Espresso
import com.android.messaging.R
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.ui.conversation.v2.CONVERSATION_ADD_PEOPLE_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_CALL_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_COMPOSE_BAR_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_LOADING_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_MEDIA_PICKER_OVERLAY_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_MESSAGES_LIST_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_OVERFLOW_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.v2.conversationMessageItemTestTag
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.v2.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMediaPickerOverlayUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMessageDeleteConfirmationUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMessageSelectionAction
import com.android.messaging.ui.conversation.v2.screen.model.ConversationMessageSelectionUiState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenScaffoldUiState
import com.android.messaging.ui.core.AppTheme
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.collections.immutable.persistentSetOf
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
    fun addPeopleAction_isShownInOverflowWhenEnabled_andForwardsClicks() {
        val screenModel = createScreenModel()
        var addPeopleClicks = 0
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 3,
                latestMessageId = "message-3",
                latestMessageIncoming = false,
            ),
            canAddPeople = true,
        )

        setScreenContent(
            screenModel = screenModel.model,
            onAddPeopleClick = {
                addPeopleClicks += 1
            },
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_ADD_PEOPLE_BUTTON_TEST_TAG)
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithTag(CONVERSATION_OVERFLOW_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithTag(CONVERSATION_ADD_PEOPLE_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .performClick()

        org.junit.Assert.assertEquals(1, addPeopleClicks)
    }

    @Test
    fun callAction_isShownWhenEnabled_andForwardsClicks() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 3,
                latestMessageId = "message-3",
                latestMessageIncoming = false,
            ),
            canCall = true,
            otherParticipantPhoneNumber = "+15551234567",
        )

        setScreenContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithTag(CONVERSATION_CALL_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .performClick()

        verify(exactly = 1) {
            screenModel.model.onCallClick()
        }
    }

    @Test
    fun callAction_isHiddenWhenDisabled() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 3,
                latestMessageId = "message-3",
                latestMessageIncoming = false,
            ),
            canCall = false,
        )

        setScreenContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithTag(CONVERSATION_CALL_BUTTON_TEST_TAG)
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
                        conversationId = "conversation-1",
                        launchGeneration = 1,
                        onAddPeopleClick = {},
                        onConversationDetailsClick = {},
                        onNavigateBack = {},
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
    fun pendingLaunchPayloads_seedDraftOpenAttachmentAndNotifyConsumption() {
        val screenModel = createScreenModel()
        var draftConsumedCount = 0
        var attachmentConsumedCount = 0
        val pendingDraft = ConversationDraft(
            messageText = "Hello",
        )
        val pendingAttachment = ConversationEntryStartupAttachment(
            contentType = "image/jpeg",
            contentUri = "content://media/image/1",
        )

        composeTestRule.setContent {
            AppTheme {
                ConversationScreen(
                    conversationId = "conversation-1",
                    launchGeneration = 1,
                    onAddPeopleClick = {},
                    onConversationDetailsClick = {},
                    onNavigateBack = {},
                    pendingDraft = pendingDraft,
                    pendingStartupAttachment = pendingAttachment,
                    onPendingDraftConsumed = {
                        draftConsumedCount += 1
                    },
                    onPendingStartupAttachmentConsumed = {
                        attachmentConsumedCount += 1
                    },
                    screenModel = screenModel.model,
                )
            }
        }
        composeTestRule.waitForIdle()

        verify(exactly = 1) {
            screenModel.model.onSeedDraft(
                conversationId = "conversation-1",
                draft = pendingDraft,
            )
        }
        verify(exactly = 1) {
            screenModel.model.onOpenStartupAttachment(
                conversationId = "conversation-1",
                startupAttachment = pendingAttachment,
            )
        }
        org.junit.Assert.assertEquals(1, draftConsumedCount)
        org.junit.Assert.assertEquals(1, attachmentConsumedCount)
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
        var conversationState by mutableStateOf(
            Pair(
                "conversation-1",
                1,
            ),
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
                    conversationId = conversationState.first,
                    launchGeneration = conversationState.second,
                    onAddPeopleClick = {},
                    onConversationDetailsClick = {},
                    onNavigateBack = {},
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
            conversationState = Pair(
                "conversation-2",
                2,
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
            .onNodeWithTag(
                testTag = CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG,
                useUnmergedTree = true,
            )
            .performClick()
        composeTestRule
            .onNodeWithTag(CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_COMPOSE_BAR_TEST_TAG)
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithTag(CONVERSATION_MEDIA_PICKER_OVERLAY_TEST_TAG)
            .assertCountEquals(1)
    }

    @Test
    fun longPressingMessage_forwardsLongClickToScreenModel() {
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
            .onNodeWithText("Message 3")
            .performTouchInput {
                down(center)
                advanceEventTime(1_000)
                up()
            }

        verify(exactly = 1) {
            screenModel.model.onMessageLongClick(messageId = "message-3")
        }
    }

    @Test
    fun clickingMessageInSelectionMode_forwardsClickToScreenModel() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 3,
                latestMessageId = "message-3",
                latestMessageIncoming = false,
            ),
            selection = ConversationMessageSelectionUiState(
                selectedMessageIds = persistentSetOf("message-3"),
                availableActions = persistentSetOf(
                    ConversationMessageSelectionAction.Copy,
                    ConversationMessageSelectionAction.Delete,
                ),
            ),
        )

        setScreenContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithText("Message 2")
            .performClick()

        verify(exactly = 1) {
            screenModel.model.onMessageClick(messageId = "message-2")
        }
    }

    @Test
    fun singleSelection_showsCopyAndDeleteInTopAppBar_andForwardsActionClicks() {
        val screenModel = createScreenModel()
        val copyLabel = composeTestRule.activity.getString(R.string.message_context_menu_copy_text)
        val deleteLabel = composeTestRule.activity.getString(R.string.action_delete_message)
        val moreOptionsLabel = composeTestRule.activity.getString(R.string.more_options)
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 1,
                latestMessageId = "message-1",
                latestMessageIncoming = false,
            ),
            selection = ConversationMessageSelectionUiState(
                selectedMessageIds = persistentSetOf("message-1"),
                availableActions = persistentSetOf(
                    ConversationMessageSelectionAction.Copy,
                    ConversationMessageSelectionAction.Delete,
                ),
            ),
        )

        setScreenContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithContentDescription(copyLabel)
            .assertIsDisplayed()
            .performClick()
        verify(exactly = 1) {
            screenModel.model.onMessageSelectionActionClick(
                action = ConversationMessageSelectionAction.Copy,
            )
        }

        composeTestRule
            .onNodeWithContentDescription(deleteLabel)
            .assertIsDisplayed()
            .performClick()
        verify(exactly = 1) {
            screenModel.model.onMessageSelectionActionClick(
                action = ConversationMessageSelectionAction.Delete,
            )
        }

        composeTestRule
            .onNodeWithContentDescription(copyLabel)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(deleteLabel)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(moreOptionsLabel)
            .assertDoesNotExist()
    }

    @Test
    fun singleSelectionWithSaveAttachment_showsSaveActionInOverflow_andForwardsClick() {
        val screenModel = createScreenModel()
        val moreOptionsLabel = composeTestRule.activity.getString(R.string.more_options)
        val saveAttachmentLabel = composeTestRule.activity.getString(
            R.string.action_save_attachment,
        )
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 1,
                latestMessageId = "message-1",
                latestMessageIncoming = true,
            ),
            selection = ConversationMessageSelectionUiState(
                selectedMessageIds = persistentSetOf("message-1"),
                availableActions = persistentSetOf(
                    ConversationMessageSelectionAction.Delete,
                    ConversationMessageSelectionAction.SaveAttachment,
                ),
            ),
        )

        setScreenContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithText(saveAttachmentLabel)
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithContentDescription(moreOptionsLabel)
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText(saveAttachmentLabel)
            .assertIsDisplayed()
            .performClick()

        verify(exactly = 1) {
            screenModel.model.onMessageSelectionActionClick(
                action = ConversationMessageSelectionAction.SaveAttachment,
            )
        }
    }

    @Test
    fun multiSelection_showsOnlyDeleteActionInTopAppBar() {
        val screenModel = createScreenModel()
        val copyLabel = composeTestRule.activity.getString(R.string.message_context_menu_copy_text)
        val deleteLabel = composeTestRule.activity.getString(R.string.action_delete_message)
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 2,
                latestMessageId = "message-2",
                latestMessageIncoming = false,
            ),
            selection = ConversationMessageSelectionUiState(
                selectedMessageIds = persistentSetOf("message-1", "message-2"),
                availableActions = persistentSetOf(
                    ConversationMessageSelectionAction.Delete,
                ),
            ),
        )

        setScreenContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithContentDescription(deleteLabel)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(copyLabel)
            .assertDoesNotExist()
    }

    @Test
    fun deleteConfirmationButtons_forwardToScreenModel() {
        val screenModel = createScreenModel()
        val deleteLabel = composeTestRule.activity.getString(
            R.string.delete_message_confirmation_button,
        )
        val cancelLabel = composeTestRule.activity.getString(android.R.string.cancel)
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 1,
                latestMessageId = "message-1",
                latestMessageIncoming = false,
            ),
            selection = ConversationMessageSelectionUiState(
                selectedMessageIds = persistentSetOf("message-1"),
                availableActions = persistentSetOf(
                    ConversationMessageSelectionAction.Delete,
                ),
                deleteConfirmation = ConversationMessageDeleteConfirmationUiState(
                    messageIds = persistentSetOf("message-1"),
                ),
            ),
        )

        setScreenContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithText(cancelLabel)
            .performClick()
        verify(exactly = 1) {
            screenModel.model.dismissDeleteMessageConfirmation()
        }

        composeTestRule
            .onNodeWithText(deleteLabel)
            .performClick()
        verify(exactly = 1) {
            screenModel.model.confirmDeleteSelectedMessages()
        }
    }

    @Test
    fun systemBackInSelectionMode_dismissesMessageSelection() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 2,
                latestMessageId = "message-2",
                latestMessageIncoming = false,
            ),
            selection = ConversationMessageSelectionUiState(
                selectedMessageIds = persistentSetOf("message-2"),
                availableActions = persistentSetOf(
                    ConversationMessageSelectionAction.Delete,
                ),
            ),
        )

        setScreenContent(screenModel = screenModel.model)

        Espresso.pressBack()

        verify(exactly = 1) {
            screenModel.model.dismissMessageSelection()
        }
    }

    private fun setScreenContent(
        screenModel: ConversationScreenModel,
        onAddPeopleClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AppTheme {
                ConversationScreen(
                    conversationId = "conversation-1",
                    launchGeneration = 1,
                    onAddPeopleClick = onAddPeopleClick,
                    onConversationDetailsClick = {},
                    onNavigateBack = {},
                    screenModel = screenModel,
                )
            }
        }
    }

    private fun createPresentUiState(
        messages: List<ConversationMessageUiModel>,
        canAddPeople: Boolean = false,
        canCall: Boolean = false,
        canArchive: Boolean = false,
        canUnarchive: Boolean = false,
        canAddContact: Boolean = false,
        canDeleteConversation: Boolean = false,
        isDeleteConversationConfirmationVisible: Boolean = false,
        otherParticipantPhoneNumber: String? = null,
        otherParticipantContactLookupKey: String? = null,
        isArchived: Boolean = false,
        selection: ConversationMessageSelectionUiState = ConversationMessageSelectionUiState(),
    ): ConversationScreenScaffoldUiState {
        return ConversationScreenScaffoldUiState(
            canAddPeople = canAddPeople,
            canCall = canCall,
            canArchive = canArchive,
            canUnarchive = canUnarchive,
            canAddContact = canAddContact,
            canDeleteConversation = canDeleteConversation,
            isDeleteConversationConfirmationVisible = isDeleteConversationConfirmationVisible,
            metadata = ConversationMetadataUiState.Present(
                title = "Weekend plan",
                selfParticipantId = "self-1",
                avatar = ConversationMetadataUiState.Avatar.Single(
                    photoUri = null,
                ),
                participantCount = 2,
                otherParticipantDisplayDestination = null,
                otherParticipantPhoneNumber = otherParticipantPhoneNumber,
                otherParticipantContactLookupKey = otherParticipantContactLookupKey,
                isArchived = isArchived,
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
            selection = selection,
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
                canCopyMessageToClipboard = !latestMessageIncoming,
                canDownloadMessage = false,
                canForwardMessage = true,
                canResendMessage = false,
                canSaveAttachments = false,
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

        fun moveTo(state: Lifecycle.State) {
            lifecycleRegistry.currentState = state
        }
    }
}
