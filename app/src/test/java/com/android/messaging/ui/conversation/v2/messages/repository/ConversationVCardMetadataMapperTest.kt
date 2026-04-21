package com.android.messaging.ui.conversation.v2.messages.repository

import com.android.messaging.datamodel.data.VCardContactItemData
import com.android.messaging.datamodel.media.VCardResource
import com.android.messaging.datamodel.media.VCardResourceEntry
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationVCardAttachmentType
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationVCardMetadataMapperTest {

    private val mapper = ConversationVCardMetadataMapperImpl()

    @Test
    fun map_contactEntry_returnsContactMetadata() {
        val vCardContactItemData = mockk<VCardContactItemData> {
            every { getDisplayName() } returns "Sam Rivera"
            every { details } returns "sam@example.com"
            every { vCardResource } returns vCardResource(
                entry = vCardResourceEntry(
                    kind = null,
                    displayAddress = null,
                ),
            )
        }

        val metadata = mapper.map(
            vCardContactItemData = vCardContactItemData,
        )

        assertEquals(
            ConversationVCardAttachmentMetadata.Loaded(
                type = ConversationVCardAttachmentType.CONTACT,
                displayName = "Sam Rivera",
                details = "sam@example.com",
                locationAddress = null,
            ),
            metadata,
        )
    }

    @Test
    fun map_locationEntry_returnsLocationMetadata() {
        val vCardContactItemData = mockk<VCardContactItemData> {
            every { getDisplayName() } returns null
            every { details } returns "New York"
            every { vCardResource } returns vCardResource(
                entry = vCardResourceEntry(
                    kind = "LoCaTiOn",
                    displayAddress = "25 11th Ave New York NY 10011 United States",
                ),
            )
        }

        val metadata = mapper.map(
            vCardContactItemData = vCardContactItemData,
        )

        assertEquals(
            ConversationVCardAttachmentMetadata.Loaded(
                type = ConversationVCardAttachmentType.LOCATION,
                displayName = null,
                details = "New York",
                locationAddress = "25 11th Ave New York NY 10011 United States",
            ),
            metadata,
        )
    }

    @Test
    fun map_blankStrings_returnsNullFields() {
        val vCardContactItemData = mockk<VCardContactItemData> {
            every { getDisplayName() } returns "   "
            every { details } returns ""
            every { vCardResource } returns vCardResource(
                entry = vCardResourceEntry(
                    kind = null,
                    displayAddress = " ",
                ),
            )
        }

        val metadata = mapper.map(
            vCardContactItemData = vCardContactItemData,
        ) as ConversationVCardAttachmentMetadata.Loaded

        assertEquals(ConversationVCardAttachmentType.CONTACT, metadata.type)
        assertNull(metadata.displayName)
        assertNull(metadata.details)
        assertNull(metadata.locationAddress)
    }

    private fun vCardResource(
        entry: VCardResourceEntry,
    ): VCardResource {
        return mockk<VCardResource> {
            every { vCards } returns listOf(entry)
        }
    }

    private fun vCardResourceEntry(
        kind: String?,
        displayAddress: String?,
    ): VCardResourceEntry {
        return mockk<VCardResourceEntry> {
            every { getKind() } returns kind
            every { getDisplayAddress() } returns displayAddress
        }
    }
}
