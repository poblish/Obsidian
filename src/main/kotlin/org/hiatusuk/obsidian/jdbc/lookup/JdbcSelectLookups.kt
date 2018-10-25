package org.hiatusuk.obsidian.jdbc.lookup

import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.AssertLookups
import org.hiatusuk.obsidian.asserts.lookups.LookupUtils
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.jdbc.delegates.JdbcEnvironments
import org.hiatusuk.obsidian.jdbc.utils.JdbcUtils
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.slf4j.Logger
import java.sql.ResultSetMetaData
import java.util.regex.Pattern
import javax.inject.Inject

@AssertLookup("jdbc:select\\(")
@ScenarioScope
class JdbcSelectLookups @Inject
constructor(private val envts: JdbcEnvironments, private val exceptions: RuntimeExceptions, private val log: Logger) {

    fun lookup(targetIdentifier: String): Collection<AssertTarget> {
        val m = HANDLER_PATTERN.matcher(targetIdentifier)
        if (!m.find()) {
            throw exceptions.runtime("Malformed Assert: $targetIdentifier")
        }

        val selString = JdbcUtils.expandShortFormSelectClause(m.group(1))

        envts["main"].connection.use {
            it.createStatement().use { s ->
                log.info("Running SELECT command: `{}`", selString)

                val textElems = arrayListOf<String>()

                var metaData: ResultSetMetaData? = null

                s.executeQuery(selString).use { rs ->
                    while (rs.next()) {

                        if (metaData == null) {
                            metaData = rs.metaData
                        }

                        textElems.add(JdbcUtils.rowToString(rs, metaData!!))
                    }
                }

                return LookupUtils.singleTarget(textElems)
            }
        }
    }

    companion object {
        private val HANDLER_PATTERN = Pattern.compile("jdbc:select" + AssertLookups.SIMPLE_METHOD_HANDLER_PATTERN_STR, Pattern.CASE_INSENSITIVE)
    }
}
