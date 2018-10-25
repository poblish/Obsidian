package org.hiatusuk.obsidian.jdbc.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.jdbc.delegates.JdbcEnvironments
import org.hiatusuk.obsidian.jdbc.utils.JdbcUtils
import org.slf4j.Logger
import java.sql.ResultSetMetaData
import javax.inject.Inject

/**
 * Really only useful for running functions, procedures, etc. See JdbcSelectLookups for actual value-checking
 *
 */
@ScenarioScope
@Command("jdbc:select")
class JdbcSelectCmd @Inject constructor(private val envts: JdbcEnvironments, private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val selString = JdbcUtils.expandShortFormSelectClause(inCmd.string)

        envts["main"].connection.use {
            it.createStatement().use { s ->
                log.info("Running SELECT command: `{}`", selString)

                var metaData: ResultSetMetaData? = null
                var idx = 0

                s.executeQuery(selString).use { rs ->
                    while (rs.next()) {

                        if (metaData == null) {
                            metaData = rs.metaData
                        }

                        log.info(">> Row [{}]: {}", idx++, JdbcUtils.rowToString(rs, metaData!!))
                    }
                }
            }
        }
    }
}
