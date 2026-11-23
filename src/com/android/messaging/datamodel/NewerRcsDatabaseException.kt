package com.android.messaging.datamodel

internal class NewerRcsDatabaseException(
    tables: List<String>,
) : IllegalStateException("A newer version's database has the RCS tables $tables")
