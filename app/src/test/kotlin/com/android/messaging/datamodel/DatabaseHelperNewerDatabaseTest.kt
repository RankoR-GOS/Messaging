package com.android.messaging.datamodel

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import com.android.messaging.BugleApplication
import com.android.messaging.FactoryTestAccess
import com.android.messaging.R
import com.android.messaging.data.databaselifecycle.DatabaseLifecycle
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.testutil.backupRulesExcludingDatabase
import com.android.messaging.testutil.installTestFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * A downgrade from a version that keeps RCS messages must neither drop them nor leave the database
 * unopenable: incoming SMS are written to it before they reach the system provider, so a database
 * that fails to open loses every SMS that arrives until the newer version is back.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = DatabaseHelperNewerDatabaseTestApplication::class)
class DatabaseHelperNewerDatabaseTest {

    private val context: Context = RuntimeEnvironment.getApplication().applicationContext
    private val currentVersion = context.getString(R.string.database_version).toInt()
    private val databaseFile = context.getDatabasePath(DatabaseHelper.DATABASE_NAME)
    private val newerDatabaseFile = context.getDatabasePath(DatabaseHelper.NEWER_DATABASE_NAME)

    @Before
    fun setUp() {
        installTestFactory(context = context)
        mockkObject(DatabaseLifecycle.Companion)
        every { DatabaseLifecycle.get(any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME)
        context.deleteDatabase(DatabaseHelper.NEWER_DATABASE_NAME)
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun aNewerDatabaseWithRcsTables_isSetAsideAndAFreshOneOpensInItsPlace() {
        writeDatabase(version = currentVersion + 1, withRcsTable = true)

        openWithThisVersion { db ->
            assertEquals(currentVersion, db.version)
            assertEquals(0, db.countRows(DatabaseHelper.CONVERSATIONS_TABLE))
            assertFalse(db.hasTable(RCS_TABLE))
        }

        SQLiteDatabase.openDatabase(newerDatabaseFile.path, null, SQLiteDatabase.OPEN_READONLY)
            .use { newer ->
                assertEquals(currentVersion + 1, newer.version)
                assertEquals(1, newer.countRows(DatabaseHelper.CONVERSATIONS_TABLE))
                assertEquals(1, newer.countRows(RCS_TABLE))
            }
    }

    @Test
    fun aNewerDatabaseWithoutRcsTables_isStillRebuiltInPlace() {
        writeDatabase(version = currentVersion + 1, withRcsTable = false)

        openWithThisVersion { db ->
            assertEquals(currentVersion, db.version)
            assertEquals(0, db.countRows(DatabaseHelper.CONVERSATIONS_TABLE))
        }

        assertFalse(newerDatabaseFile.exists())
    }

    /**
     * Version 6 replaces this path with an upgrade that leaves the database at its version (step
     * 2.8). Until then the guard has to fail it in place: setting it aside would file an older
     * database as the newer one and replace one that a downgrade set aside before.
     */
    @Test
    fun aFailedUpgradeWithRcsTables_failsToOpenAndSetsNothingAside() {
        // Version 2 adds a column the current tables already have, so the upgrade fails.
        writeDatabase(version = 1, withRcsTable = true)

        assertThrows(RcsTablesPresentException::class.java) { openWithThisVersion {} }

        assertFalse(newerDatabaseFile.exists())
        SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READONLY)
            .use { db ->
                assertEquals(1, db.version)
                assertEquals(1, db.countRows(DatabaseHelper.CONVERSATIONS_TABLE))
                assertEquals(1, db.countRows(RCS_TABLE))
            }
    }

    @Test
    fun theSetAsideDatabase_isExcludedFromBackupsLikeTheDatabaseItself() {
        listOf(DatabaseHelper.DATABASE_NAME, DatabaseHelper.NEWER_DATABASE_NAME).forEach { name ->
            assertEquals(
                "$name is backed up",
                setOf("cloud-backup", "device-transfer"),
                backupRulesExcludingDatabase(context = context, databaseName = name),
            )
        }
    }

    private fun writeDatabase(version: Int, withRcsTable: Boolean) {
        databaseFile.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { db ->
            DatabaseHelper.rebuildTables(db)
            db.insert(
                DatabaseHelper.CONVERSATIONS_TABLE,
                null,
                contentValuesOf(ConversationColumns.NAME to "Weekend plan"),
            )
            if (withRcsTable) {
                db.execSQL("CREATE TABLE $RCS_TABLE (_id INTEGER PRIMARY KEY, message_id INTEGER)")
                db.execSQL("INSERT INTO $RCS_TABLE (_id, message_id) VALUES (1, 1)")
            }
            db.version = version
        }
    }

    private fun openWithThisVersion(block: (SQLiteDatabase) -> Unit) {
        val helper = DatabaseHelper.getNewInstanceForTest(context)
        try {
            block(helper.openWritableDatabase())
        } finally {
            helper.close()
        }
    }

    private fun SQLiteDatabase.hasTable(name: String): Boolean {
        return rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(name),
        ).use { cursor -> cursor.moveToFirst() }
    }

    private fun SQLiteDatabase.countRows(table: String): Int {
        return rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
    }

    private companion object {
        const val RCS_TABLE = "rcs_messages"
    }
}

internal class DatabaseHelperNewerDatabaseTestApplication : BugleApplication() {
    override fun onCreate() {
        setTestsRunning()
    }
}
