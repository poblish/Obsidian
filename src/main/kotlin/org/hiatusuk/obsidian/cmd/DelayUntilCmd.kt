package org.hiatusuk.obsidian.cmd

import com.codahale.metrics.MetricRegistry
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.utils.ParseUtils
import org.slf4j.Logger
import java.util.regex.Pattern
import javax.inject.Inject

@ScenarioScope
@Command("delay until")
class DelayUntilCmd @Inject
constructor(private val metrics: MetricRegistry, private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val untilSpec = inCmd.string
        val m = DU_FORMAT.matcher(untilSpec)
        if (!m.find()) {
            throw RuntimeException("Could not parse: $untilSpec")
        }

        val st = java.lang.Long.parseLong(m.group(1))
        val delayDuration = ParseUtils.parseDuration(m.group(2))
        val et = st + delayDuration.toMillis()

        if (et < st) {
            throw RuntimeException("Error: end time < start time")
        }

        if (et < System.currentTimeMillis()) {
            throw RuntimeException("Error: cannot wait until t=$et as it has already passed")
        }

        metrics.timer("delays").time().use {
            log.debug("Waiting from t={} to t={}...", System.currentTimeMillis(), et)
            Thread.sleep(et - System.currentTimeMillis())
        }
    }

    companion object {

        private val DU_FORMAT = Pattern.compile("(\\d+) \\+ (.*)")
    }
}