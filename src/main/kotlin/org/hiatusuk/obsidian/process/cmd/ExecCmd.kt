package org.hiatusuk.obsidian.process.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.process.ExecProcessHandler
import org.hiatusuk.obsidian.process.ProcessUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ScenarioScope
@Command("exec")
class ExecCmd @Inject constructor() : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val args = inCmd.string.split(' ').filter { it.isNotBlank() }
        val pb = ProcessUtils.processBuilder(args, ExecProcessHandler())

        LOG.debug("Running... {}", args)

        pb.start().waitFor(0, TimeUnit.SECONDS) // Wait forever
    }

    companion object {
        private val LOG = LoggerFactory.getLogger("Exec")
    }
}