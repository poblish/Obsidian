package org.hiatusuk.obsidian.threads.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.threads.ThreadSubmitter
import org.hiatusuk.obsidian.threads.delegates.ThreadHandler
import org.slf4j.LoggerFactory
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ScenarioScope
@Command("threads:submit")
class ThreadSubmitCmd @Inject
constructor(private val handler: ThreadHandler) : CommandIF {

    override fun run(inCmd: CommandSpec) {

        val poolSize = handler.determinePoolSize(inCmd)
        LOG.info("coreThreads = {}, maxThreads = {}", poolSize.left, poolSize.right)

        val exec = ThreadPoolExecutor(poolSize.left, poolSize.right, 0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue())

        handler.runThreads(inCmd, LOG, exec, (object : ThreadSubmitter {
            override fun submitThread(task: () -> Unit): Future<*> {
                return exec.submit(task)
            }
        }))
    }

    companion object {
        private val LOG = LoggerFactory.getLogger("Threads")
    }
}