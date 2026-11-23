@file:JvmName("DatabaseFiles")

package com.android.messaging.datamodel

import java.io.File

private val COMPANION_SUFFIXES = listOf("-journal", "-wal", "-shm")

/**
 * Moves a closed SQLite database and its journal files to [target], replacing whatever is there.
 * A journal file that can't be moved is deleted, so it can't be replayed into a new database
 * created at [source].
 */
internal fun moveDatabase(source: File, target: File) {
    check(source.renameTo(target)) { "Couldn't move ${source.name} to ${target.name}" }
    COMPANION_SUFFIXES.forEach { suffix ->
        val sourceCompanion = File("${source.path}$suffix")
        val targetCompanion = File("${target.path}$suffix")
        targetCompanion.delete()
        val moved = !sourceCompanion.exists() ||
            sourceCompanion.renameTo(targetCompanion) ||
            sourceCompanion.delete()
        check(moved) { "Couldn't move or delete ${sourceCompanion.name}" }
    }
}
