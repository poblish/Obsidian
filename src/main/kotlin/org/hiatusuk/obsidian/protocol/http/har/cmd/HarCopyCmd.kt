package org.hiatusuk.obsidian.protocol.http.har.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.proxy.delegates.WebProxies
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Named

@ScenarioScope
@Command("har copy")
class HarCopyCmd @Inject
constructor(private val proxies: WebProxies,
            @param:Named("outputDir")
            private val outputDir: File) : CommandIF {

    @Throws(IOException::class)
    override fun run(inCmd: CommandSpec) {
        val file = File(inCmd.optString("har copy").orElse(outputDir.absolutePath))
        proxies.copyHar(if (file.isDirectory) File(file, Date().time.toString() + ".har") else file)
    }
}
