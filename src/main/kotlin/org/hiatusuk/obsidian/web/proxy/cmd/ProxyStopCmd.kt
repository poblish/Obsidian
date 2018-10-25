package org.hiatusuk.obsidian.web.proxy.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.proxy.delegates.WebProxies
import javax.inject.Inject

@ScenarioScope
@Command("proxy stop")
class ProxyStopCmd @Inject
constructor(private val proxies: WebProxies) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        proxies.stop()
    }
}
