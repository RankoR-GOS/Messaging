package com.android.messaging.testutil

import android.content.Context
import com.android.messaging.R
import org.xmlpull.v1.XmlPullParser

private const val BACKUP_RULE_DEPTH = 2

internal fun backupRulesExcluding(context: Context, sharedPreferencesName: String): Set<String> {
    return backupRulesExcluding(
        context = context,
        domain = "sharedpref",
        path = "$sharedPreferencesName.xml",
    )
}

internal fun backupRulesExcludingDatabase(context: Context, databaseName: String): Set<String> {
    return backupRulesExcluding(context = context, domain = "database", path = databaseName)
}

private fun backupRulesExcluding(context: Context, domain: String, path: String): Set<String> {
    val excludingRules = mutableSetOf<String>()
    context.resources.getXml(R.xml.backup_rules).use { parser ->
        var rule = ""
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when {
                parser.eventType != XmlPullParser.START_TAG -> Unit
                parser.depth == BACKUP_RULE_DEPTH -> rule = parser.name
                parser.excludes(domain = domain, path = path) -> excludingRules += rule
            }
        }
    }
    return excludingRules
}

private fun XmlPullParser.excludes(domain: String, path: String): Boolean {
    return name == "exclude" &&
        getAttributeValue(null, "domain") == domain &&
        getAttributeValue(null, "path") == path
}
