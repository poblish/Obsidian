package org.hiatusuk.obsidian.threads.delegates

import org.apache.commons.lang3.tuple.Pair
import org.hiatusuk.obsidian.cmd.CallSubroutineCmd
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.cmd.api.CommandSpecFactory
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.threads.ThreadSubmitter
import org.hiatusuk.obsidian.utils.Duration
import org.slf4j.Logger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import javax.inject.Inject

@ScenarioScope
class ThreadHandler @Inject
internal constructor(private val cmdSpecs: CommandSpecFactory,
                     private val subroutineCmd: CallSubroutineCmd,
                     private val exceptions: RuntimeExceptions) {

    fun determineCorePoolSize(inCmd: CommandSpec): Int {

        if (!inCmd.has("threads")) {
            return 1
        }

        var coreThreads: Int
        var showMaxError = false

        try {
            val m = inCmd.getMap("threads")
            if (m.containsKey("max")) {
                showMaxError = true
            }

            coreThreads = if (m.containsKey("core")) Integer.parseInt(m["core"].toString()) else 1
        } catch (e: RuntimeException) {
            coreThreads = inCmd.optInteger("threads").orElse(1)
        }

        if (showMaxError) {
            throw exceptions.illegalArgument("Maximum thread count not supported for scheduled pools")
        }

        if (coreThreads < 1) {
            throw exceptions.illegalArgument("Core thread count must be >= 1")
        }

        return coreThreads
    }

    fun determinePoolSize(inCmd: CommandSpec): Pair<Int, Int> {

        if (!inCmd.has("threads")) {
            return Pair.of(1, 1)
        }

        var coreThreads: Int
        var maxThreads: Int

        try {
            val m = inCmd.getMap("threads")
            coreThreads = if (m.containsKey("core")) Integer.parseInt(m["core"].toString()) else 1
            maxThreads = if (m.containsKey("max")) Integer.parseInt(m["max"].toString()) else 1
        } catch (e: RuntimeException) {
            maxThreads = inCmd.optInteger("threads").orElse(1)
            coreThreads = maxThreads
        }

        if (coreThreads < 1 || maxThreads < 1) {
            throw exceptions.illegalArgument("Thread counts must be >= 1")
        }

        if (coreThreads > maxThreads) {
            throw exceptions.illegalArgument("Core thread count cannot be greater than maximum")
        }

        return Pair.of(coreThreads, maxThreads)
    }

    fun runThreads(inCmd: CommandSpec, inLog: Logger, exec: ExecutorService, submitter: ThreadSubmitter) {

        val forHowLong = inCmd.optDuration("for")
        val subroutineName = inCmd.getString("run")
        // FIXME Ignore for now... final String finalSubroutineName = inCmd.optString("finallyRun").orNull();

        val scheduled = inCmd.has("every") || inCmd.has("delayEvery")

        if (forHowLong.isPresent) { // Scheduled only
            if (inCmd.has("times")) {
                throw exceptions.runtime("Cannot specify number of runs when a time limit is given")
            }

            inLog.info("Keep calling task for {}...", forHowLong.get())

            val task = {
                try {
                    subroutineCmd.run(cmdSpecs.create("call", "$subroutineName()"))
                } catch (e: Throwable) {
                    e.printStackTrace() // Throwables.propagate(e);
                }
            }

            val initialDelay = inCmd.optDuration("delay").orElse(Duration.ZERO)  // Yuk, duplication. Also, should be schedule only!

            submitter.submitThread(task)

            val fixedRateDuration = inCmd.optDuration("every").orElse(Duration.ZERO)
            val fixedDelayDuration = inCmd.optDuration("delayEvery").orElse(Duration.ZERO)

            val et = System.nanoTime() + forHowLong.get().toNanos() + initialDelay.toNanos() - fixedRateDuration.toNanos() - fixedDelayDuration.toNanos()  // Must include any initial delay!

            do {
                Thread.sleep(100)  // Too short? Need to be responsive...
            } while (et - System.nanoTime() >= 0)

            // inLog.info("Ready to shut down");
        } else {  // Scheduled or not

            val numRuns = inCmd.optInteger("times").orElse(1)

            val latch = CountDownLatch(numRuns)

            if (scheduled) {
                inLog.info("Scheduling {} calls...", numRuns)
            } else {
                inLog.info("Starting {} calls...", numRuns)
            }

            val task = {
                try {
                    subroutineCmd.run(cmdSpecs.create("call", "$subroutineName()"))
                } catch (e: Throwable) {
                    throw RuntimeException(e)
                } finally {
                    latch.countDown()
                }
            }

            if (scheduled) {
                submitter.submitThread(task)
            } else {  // No schedule
                for (run in 0 until numRuns) {
                    submitter.submitThread(task)
                }
            }

            inLog.debug("Waiting for completion...")
            latch.await()
        }

        inLog.info("Shutting down...")
        exec.shutdownNow()
    }
}
