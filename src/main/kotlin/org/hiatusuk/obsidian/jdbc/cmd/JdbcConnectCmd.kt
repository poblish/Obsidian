package org.hiatusuk.obsidian.jdbc.cmd

import com.mysql.cj.jdbc.MysqlDataSource
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.jdbc.delegates.JdbcEnvironments
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.postgresql.ds.PGSimpleDataSource
import org.slf4j.Logger
import java.sql.SQLException
import javax.inject.Inject

@ScenarioScope
@Command("jdbc:connect")
class JdbcConnectCmd @Inject
internal constructor(private val envts: JdbcEnvironments, private val exceptions: RuntimeExceptions, private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val name = inCmd.optString("name").orElse("untitled-" + System.nanoTime())
        val host = inCmd.optString("host").orElse("localhost")
        val port = inCmd.getString("port")
        val database = inCmd.getString("database")
        val user = inCmd.getString("user")
        val pass = inCmd.getString("pass")

        val type = inCmd.getString("type").toLowerCase()
        when (type) {
            "postgres" -> {
                val source = PGSimpleDataSource()
                source.applicationName = name
                source.serverName = host
                source.portNumber = Integer.parseInt(port)
                source.databaseName = database
                source.user = user
                source.password = pass

                log.info("Created DataSource: {}", source)

                envts.put( /* Only one supported at a time */"main", source)
            }
            "mysql" -> {
                val mysqlDS = MysqlDataSource()
                mysqlDS.description = name
                mysqlDS.serverName = host
                mysqlDS.portNumber = Integer.parseInt(port)
                mysqlDS.databaseName = database
                mysqlDS.user = user
                mysqlDS.setPassword(pass)

                try {
                    mysqlDS.useSSL = false  // Make configurable
                } catch (e: SQLException) {
                    throw exceptions.runtime(e)
                }

                log.info("Created DataSource: {}", mysqlDS)

                envts.put( /* Only one supported at a time */"main", mysqlDS)
            }
            else -> throw exceptions.runtime("Unsupported DB type")
        }

    }
}
