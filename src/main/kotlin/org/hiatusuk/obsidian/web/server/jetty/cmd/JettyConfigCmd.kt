package org.hiatusuk.obsidian.web.server.jetty.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.server.jetty.JettyServerConfig
import org.hiatusuk.obsidian.web.server.jetty.delegates.JettyServers
import java.util.*
import javax.inject.Inject

@ScenarioScope
@Command("jetty:config")
class JettyConfigCmd @Inject constructor(private val servers: JettyServers) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val config = JettyServerConfig(inCmd.optInteger("port").orElse(JettyServerConfig.DEFAULT_SERVER_PORT))

        config.defaultDescriptor = inCmd.optString("webDefaults")
        config.addBeans(inCmd.optList("beans").orElse(ArrayList()))

        servers.setNextConfig(config)
    }
}
