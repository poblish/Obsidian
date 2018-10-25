package org.hiatusuk.obsidian.jdbc.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.jdbc.delegates.JdbcEnvironments
import org.slf4j.Logger
import javax.inject.Inject

// Clearly this is potentially dangerous and may have a limited life as a result.
@ScenarioScope
@Command("jdbc:update")
class JdbcUpdateCmd @Inject
internal constructor(private val envts: JdbcEnvironments, private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val updateString = inCmd.string

        envts["main"].connection.use {
            it.createStatement().use { s ->
                log.info("Running UPDATE command: `{}`", updateString)
                s.executeUpdate(updateString)
            }
        }
    }
}
