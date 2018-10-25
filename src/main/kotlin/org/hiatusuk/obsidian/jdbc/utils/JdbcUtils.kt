package org.hiatusuk.obsidian.jdbc.utils

import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLException
import java.util.*

object JdbcUtils {

    @Throws(SQLException::class)
    fun rowToString(currRow: ResultSet, metaData: ResultSetMetaData): String {
        val each = LinkedHashMap<String, Any?>()

        for (i in 1 until metaData.columnCount + 1) {
            val originalName = metaData.getColumnName(i)
            var nameToUse = originalName
            var appendCount = 1

            while (each.containsKey(nameToUse)) {  // Disallow collisions
                nameToUse = originalName + "_" + appendCount++
            }

            each[nameToUse] = currRow.getObject(i)
        }

        return each.toString()
    }

    fun expandShortFormSelectClause(input: String): String {
        val trimmedInput = input.trim()
        val selWords = trimmedInput.toLowerCase()
                .split(Regex("\\s+"))
                .map { it.trim() }
                .filter { it.isNotBlank() }

        // Logic is that if word 'select' appears anywhere, don't prefix. This allows 'EXPLAIN ANALYZE SELECT ...' through
        return if (!selWords.contains("select")) "SELECT $trimmedInput" else trimmedInput
    }
}
