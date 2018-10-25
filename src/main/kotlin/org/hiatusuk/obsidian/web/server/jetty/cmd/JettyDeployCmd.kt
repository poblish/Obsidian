package org.hiatusuk.obsidian.web.server.jetty.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.server.jetty.delegates.JettyServers
import javax.inject.Inject

@ScenarioScope
@Command("jetty:deploy")
class JettyDeployCmd @Inject
constructor(private val servers: JettyServers) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val path = inCmd.getString("from")
        val contextPath = inCmd.getString("to")
        val asWar = inCmd.optString("as").map { it.equals("war", ignoreCase = true) }

        servers.deployTo(path, contextPath, asWar)
    }
}
