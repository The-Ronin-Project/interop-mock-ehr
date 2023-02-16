package com.projectronin.interop.mock.ehr.util

import org.apache.commons.lang.StringEscapeUtils

/**
 * Escapes the string so that it's suitable for use in a SQL query for most DBs.  For example: "O'Brien" becomes
 * "O''Brien".
 *
 * Some DBs use different escape characters so this won't work everywhere, but it does for MySql and should work for
 * us.
 */
fun String.escapeSQL(): String = StringEscapeUtils.escapeSql(this)
