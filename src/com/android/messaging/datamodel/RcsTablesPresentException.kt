package com.android.messaging.datamodel

internal class RcsTablesPresentException(
    tables: List<String>,
) : IllegalStateException("Refusing to drop the RCS tables $tables")
