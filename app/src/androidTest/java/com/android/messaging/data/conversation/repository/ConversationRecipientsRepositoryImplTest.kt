package com.android.messaging.data.conversation.repository

import android.content.ContentResolver
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Directory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ConversationRecipientsRepositoryImplTest {

    private lateinit var contentResolver: ContentResolver

    @Before
    fun setUp() {
        contentResolver = mockk()
    }

    @Test
    fun searchRecipients_isColdUntilCollected() {
        every {
            contentResolver.query(any(), any(), any<Bundle>(), any())
        } returns createPhoneCursor()
        val repository = createRepository()

        @Suppress("UnusedFlow")
        repository.searchRecipients(
            query = "",
            offset = 0,
        )

        verify(exactly = 0) {
            contentResolver.query(any(), any(), any<Bundle>(), any())
        }
    }

    @Test
    fun searchRecipients_withBlankQuery_usesDefaultPhoneDirectoryQueryAndMapsRows() {
        runTest {
            val capturedUris = mutableListOf<Uri>()
            val capturedQueryArgs = mutableListOf<Bundle>()
            every {
                contentResolver.query(
                    capture(capturedUris),
                    any(),
                    capture(capturedQueryArgs),
                    any(),
                )
            } returns createPhoneCursor(
                phoneRow(
                    id = 1L,
                    destination = "+1 555 0100",
                    displayName = "Ada Lovelace",
                    photoUri = "content://photos/1",
                    sortKey = "ada",
                ),
                phoneRow(
                    id = 2L,
                    destination = "+1 555 0101",
                    displayName = "   ",
                    photoUri = "",
                    sortKey = "hopper",
                ),
                phoneRow(
                    id = 3L,
                    destination = "   ",
                    displayName = "Ignored",
                    photoUri = null,
                    sortKey = "ignored",
                ),
            )
            val repository = createRepository()

            val page = repository.searchRecipients(
                query = "",
                offset = 0,
            ).first()

            assertEquals(createDefaultPhoneQueryUri(), capturedUris.single())
            assertEquals(
                listOf(Phone.SORT_KEY_PRIMARY),
                capturedQueryArgs.single()
                    .getStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS)
                    ?.toList(),
            )
            assertEquals(
                ContentResolver.QUERY_SORT_DIRECTION_ASCENDING,
                capturedQueryArgs.single()
                    .getInt(ContentResolver.QUERY_ARG_SORT_DIRECTION),
            )
            assertEquals(
                listOf(
                    ConversationRecipient(
                        id = "1",
                        displayName = "Ada Lovelace",
                        destination = "+1 555 0100",
                        photoUri = "content://photos/1",
                        secondaryText = "+1 555 0100",
                    ),
                    ConversationRecipient(
                        id = "2",
                        displayName = "+1 555 0101",
                        destination = "+1 555 0101",
                        photoUri = null,
                        secondaryText = null,
                    ),
                ),
                page.recipients,
            )
            assertNull(page.nextOffset)
        }
    }

    @Test
    fun searchRecipients_withTextQuery_mergesSortsAndDeduplicatesPhoneAndEmailResults() {
        runTest {
            val capturedUris = mutableListOf<Uri>()
            every {
                contentResolver.query(capture(capturedUris), any(), any<Bundle>(), any())
            } answers {
                when (capturedUris.last()) {
                    createPhoneQueryUri(query = "ali") -> {
                        createPhoneCursor(
                            phoneRow(
                                id = 1L,
                                destination = "+1 555 0100",
                                displayName = "Alice",
                                sortKey = "alice",
                            ),
                            phoneRow(
                                id = 2L,
                                destination = "+1 555 0100",
                                displayName = "Alice Duplicate",
                                sortKey = "alice",
                            ),
                            phoneRow(
                                id = 3L,
                                destination = "+1 555 0999",
                                displayName = "",
                                sortKey = "zeta",
                            ),
                        )
                    }

                    createEmailQueryUri(query = "ali") -> {
                        createEmailCursor(
                            emailRow(
                                id = 4L,
                                destination = "alice@example.com",
                                displayName = "Alice",
                                sortKey = "alice",
                            ),
                            emailRow(
                                id = 5L,
                                destination = "bob@example.com",
                                displayName = "Bob",
                                sortKey = "bob",
                            ),
                        )
                    }

                    else -> {
                        error("Unexpected query URI: ${capturedUris.last()}")
                    }
                }
            }
            val repository = createRepository()

            val page = repository.searchRecipients(
                query = "ali",
                offset = 0,
            ).first()

            assertEquals(
                listOf(
                    createPhoneQueryUri(query = "ali"),
                    createEmailQueryUri(query = "ali"),
                ),
                capturedUris,
            )
            assertEquals(
                listOf(
                    ConversationRecipient(
                        id = "1",
                        displayName = "Alice",
                        destination = "+1 555 0100",
                        secondaryText = "+1 555 0100",
                    ),
                    ConversationRecipient(
                        id = "4",
                        displayName = "Alice",
                        destination = "alice@example.com",
                        secondaryText = "alice@example.com",
                    ),
                    ConversationRecipient(
                        id = "5",
                        displayName = "Bob",
                        destination = "bob@example.com",
                        secondaryText = "bob@example.com",
                    ),
                    ConversationRecipient(
                        id = "3",
                        displayName = "+1 555 0999",
                        destination = "+1 555 0999",
                    ),
                ),
                page.recipients,
            )
            assertNull(page.nextOffset)
        }
    }

    @Test
    fun searchRecipients_withDigitQuery_usesDefaultPhoneFallbackWhenTextQueriesAreEmpty() {
        runTest {
            val capturedUris = mutableListOf<Uri>()
            every {
                contentResolver.query(capture(capturedUris), any(), any<Bundle>(), any())
            } answers {
                when (capturedUris.last()) {
                    createPhoneQueryUri(query = "555") -> {
                        createPhoneCursor()
                    }

                    createEmailQueryUri(query = "555") -> {
                        createEmailCursor()
                    }

                    createDefaultPhoneQueryUri() -> {
                        createPhoneCursor(
                            phoneRow(
                                id = 7L,
                                destination = "+1 (555) 0100",
                                displayName = "Ada",
                                sortKey = "ada",
                            ),
                            phoneRow(
                                id = 8L,
                                destination = "+1 (777) 0100",
                                displayName = "Grace",
                                sortKey = "grace",
                            ),
                        )
                    }

                    else -> {
                        error("Unexpected query URI: ${capturedUris.last()}")
                    }
                }
            }
            val repository = createRepository()

            val page = repository.searchRecipients(
                query = "555",
                offset = 0,
            ).first()

            assertEquals(
                listOf(
                    createPhoneQueryUri(query = "555"),
                    createEmailQueryUri(query = "555"),
                    createDefaultPhoneQueryUri(),
                ),
                capturedUris,
            )
            assertEquals(
                listOf(
                    ConversationRecipient(
                        id = "7",
                        displayName = "Ada",
                        destination = "+1 (555) 0100",
                        secondaryText = "+1 (555) 0100",
                    ),
                ),
                page.recipients,
            )
        }
    }

    @Test
    fun searchRecipients_paginatesResultsAndReturnsEmptyPagePastTheEnd() {
        runTest {
            every {
                contentResolver.query(any(), any(), any<Bundle>(), any())
            } answers {
                createPhoneCursor(
                    *List(size = 201) { index ->
                        phoneRow(
                            id = index.toLong(),
                            destination = "+1 555 ${
                                index.toString().padStart(length = 4, padChar = '0')
                            }",
                            displayName = "Contact $index",
                            sortKey = "contact-${
                                index.toString().padStart(length = 4, padChar = '0')
                            }",
                        )
                    }.toTypedArray(),
                )
            }
            val repository = createRepository()

            val firstPage = repository.searchRecipients(
                query = "",
                offset = 0,
            ).first()
            val secondPage = repository.searchRecipients(
                query = "",
                offset = 200,
            ).first()
            val emptyPage = repository.searchRecipients(
                query = "",
                offset = 500,
            ).first()

            assertEquals(200, firstPage.recipients.size)
            assertEquals(200, firstPage.nextOffset)
            assertEquals(1, secondPage.recipients.size)
            assertNull(secondPage.nextOffset)
            assertEquals(emptyList<ConversationRecipient>(), emptyPage.recipients)
            assertNull(emptyPage.nextOffset)
        }
    }

    private fun createRepository(): ConversationRecipientsRepositoryImpl {
        return ConversationRecipientsRepositoryImpl(
            contentResolver = contentResolver,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    private fun createDefaultPhoneQueryUri(): Uri {
        return Phone.CONTENT_URI
            .buildUpon()
            .appendQueryParameter(
                ContactsContract.DIRECTORY_PARAM_KEY,
                Directory.DEFAULT.toString(),
            )
            .build()
    }

    private fun createPhoneQueryUri(query: String): Uri {
        return Phone.CONTENT_FILTER_URI
            .buildUpon()
            .appendPath(query)
            .appendQueryParameter(
                ContactsContract.DIRECTORY_PARAM_KEY,
                Directory.DEFAULT.toString(),
            )
            .build()
    }

    private fun createEmailQueryUri(query: String): Uri {
        return Email.CONTENT_FILTER_URI
            .buildUpon()
            .appendPath(query)
            .appendQueryParameter(
                ContactsContract.DIRECTORY_PARAM_KEY,
                Directory.DEFAULT.toString(),
            )
            .build()
    }

    private fun createPhoneCursor(vararg rows: Array<Any?>): MatrixCursor {
        return MatrixCursor(
            arrayOf(
                Phone._ID,
                Phone.NUMBER,
                Phone.DISPLAY_NAME_PRIMARY,
                Phone.PHOTO_THUMBNAIL_URI,
                Phone.SORT_KEY_PRIMARY,
            ),
        ).apply {
            rows.forEach(::addRow)
        }
    }

    private fun createEmailCursor(vararg rows: Array<Any?>): MatrixCursor {
        return MatrixCursor(
            arrayOf(
                Email._ID,
                Email.ADDRESS,
                Phone.DISPLAY_NAME_PRIMARY,
                Phone.PHOTO_THUMBNAIL_URI,
                Email.SORT_KEY_PRIMARY,
            ),
        ).apply {
            rows.forEach(::addRow)
        }
    }

    private fun phoneRow(
        id: Long,
        destination: String,
        displayName: String,
        photoUri: String? = null,
        sortKey: String,
    ): Array<Any?> {
        return arrayOf(
            id,
            destination,
            displayName,
            photoUri,
            sortKey,
        )
    }

    private fun emailRow(
        id: Long,
        destination: String,
        displayName: String,
        photoUri: String? = null,
        sortKey: String,
    ): Array<Any?> {
        return arrayOf(
            id,
            destination,
            displayName,
            photoUri,
            sortKey,
        )
    }
}
