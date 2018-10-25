package org.hiatusuk.obsidian.protocol.http.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.proxy.delegates.WebProxies
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ScenarioScope
@Command("bandwidth")
class BandwidthCmd @Inject
internal constructor(private val proxies: WebProxies) : CommandIF {

    override fun run(inCmd: CommandSpec) {

        proxies.stop()

        proxies.startup(emptyMap()) {

            val downloadKbps = inCmd.optLong("downloadKbps").orElse(0L)
            val uploadKbps = inCmd.optLong("uploadKbps").orElse(0L)
            val latency = inCmd.optLong("latency").orElse(0L)

            it.readBandwidthLimit = getBps(downloadKbps)
            it.writeBandwidthLimit = getBps(uploadKbps)
            it.setLatency(latency, TimeUnit.MILLISECONDS)

            false
        }
    }

    private fun getBps(inKbps: Long): Long {
        return inKbps * 1000 / 8
    }
}