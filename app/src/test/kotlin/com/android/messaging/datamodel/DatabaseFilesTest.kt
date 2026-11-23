package com.android.messaging.datamodel

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DatabaseFilesTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun moveDatabase_movesTheJournalFilesWithTheDatabase() {
        val source = folder.file(name = SOURCE, content = "database")
        folder.file(name = "$SOURCE-wal", content = "wal")
        folder.file(name = "$SOURCE-shm", content = "shm")
        val target = File(folder.root, TARGET)

        moveDatabase(source = source, target = target)

        assertEquals(setOf(TARGET, "$TARGET-wal", "$TARGET-shm"), folder.root.list()?.toSet())
        assertEquals("database", target.readText())
        assertEquals("wal", File(folder.root, "$TARGET-wal").readText())
    }

    @Test
    fun moveDatabase_replacesTheTargetAndDropsItsStaleJournalFiles() {
        val source = folder.file(name = SOURCE, content = "newer database")
        folder.file(name = TARGET, content = "older database")
        folder.file(name = "$TARGET-journal", content = "older journal")

        moveDatabase(source = source, target = File(folder.root, TARGET))

        assertEquals(setOf(TARGET), folder.root.list()?.toSet())
        assertEquals("newer database", File(folder.root, TARGET).readText())
    }

    @Test
    fun moveDatabase_whenTheDatabaseCantBeMoved_leavesItsJournalFilesInPlace() {
        val wal = folder.file(name = "$SOURCE-wal", content = "wal")

        assertThrows(IllegalStateException::class.java) {
            moveDatabase(source = File(folder.root, SOURCE), target = File(folder.root, TARGET))
        }

        assertEquals("wal", wal.readText())
        assertFalse(File(folder.root, "$TARGET-wal").exists())
    }

    private fun TemporaryFolder.file(name: String, content: String): File {
        return newFile(name).apply { writeText(content) }
    }

    private companion object {
        const val SOURCE = "bugle_db"
        const val TARGET = "bugle_db_newer"
    }
}
