package org.hiatusuk.obsidian.threads.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.threads.ThreadSubmitter
import org.hiatusuk.obsidian.threads.delegates.ThreadHandler
import org.hiatusuk.obsidian.utils.Duration
import org.slf4j.LoggerFactory
import java.util.concurrent.Future
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ScenarioScope
@Command("threads:schedule")
class ThreadScheduleCmd @Inject
constructor(private val handler: ThreadHandler) : CommandIF {

    override fun run(inCmd: CommandSpec) {

        val poolSize = handler.determineCorePoolSize(inCmd)
        LOG.info("coreThreads = {}", poolSize)

        val initialDelayDuration = inCmd.optDuration("delay").orElse(Duration.ZERO)
        val fixedRateDuration = inCmd.optDuration("every").orElse(null)
        val fixedDelayDuration = inCmd.optDuration("delayEvery").orElse(null)

        if (fixedRateDuration == null && fixedDelayDuration == null) {
            throw RuntimeException("Must specify either `every` for `delayEvery` for scheduled tasks")
        }

        val exec = ScheduledThreadPoolExecutor(poolSize)

        handler.runThreads(inCmd, LOG, exec, (object : ThreadSubmitter {
            override fun submitThread(task: () -> Unit): Future<*> {
                return if (fixedRateDuration != null) {
                    exec.scheduleAtFixedRate(task, initialDelayDuration.toMillis(), fixedRateDuration.toMillis(), TimeUnit.MILLISECONDS)
                } else /* if (fixedDelayDuration != null) */ {
                    exec.scheduleWithFixedDelay(task, initialDelayDuration.toMillis(), fixedDelayDuration.toMillis(), TimeUnit.MILLISECONDS)
                }
            }
        }))
    }

    companion object {
        private val LOG = LoggerFactory.getLogger("Threads")
    }
}