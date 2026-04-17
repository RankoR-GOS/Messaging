package com.android.messaging.domain.conversation.usecase

import com.android.messaging.datamodel.data.ContactPickerData
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanAddMoreConversationParticipantsImplTest {

    @Before
    fun setUp() {
        unmockkAll()
    }

    @Test
    fun invoke_delegatesToLegacyContactPickerLimitCheck() {
        val useCase = CanAddMoreConversationParticipantsImpl()
        mockkStatic(ContactPickerData::class)
        every {
            ContactPickerData.getCanAddMoreParticipants(any())
        } answers {
            firstArg<Int>() < 5
        }

        listOf(0, 4, 5).forEach { participantCount ->
            assertEquals(
                participantCount < 5,
                useCase.invoke(participantCount = participantCount),
            )
        }

        verify(exactly = 1) {
            ContactPickerData.getCanAddMoreParticipants(0)
        }
        verify(exactly = 1) {
            ContactPickerData.getCanAddMoreParticipants(4)
        }
        verify(exactly = 1) {
            ContactPickerData.getCanAddMoreParticipants(5)
        }
    }
}
