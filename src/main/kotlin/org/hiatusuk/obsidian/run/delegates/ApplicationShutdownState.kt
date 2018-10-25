package org.hiatusuk.obsidian.run.delegates

import org.hiatusuk.obsidian.utils.TerminalColours
import org.hiatusuk.obsidian.utils.TimeDifference
import org.slf4j.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton  // Application scope!
class ApplicationShutdownState
@Inject constructor(private val log: Logger) {

    private var startTimeNanos = 0L
    private var reported = false

    fun reset() {
        startTimeNanos = 0L
        reported = false
    }

    fun recordScenarioStart() {
        if (startTimeNanos <= 0L) {
            // First invocation only
            startTimeNanos = System.nanoTime()
        }
    }

    fun reportOnExit() {
        synchronized(this) {
            if (reported) return

            val currTimeNanos = System.nanoTime()
            log.info("{}<< Obsidian scenarios finished in {} >>{}",
                    TerminalColours.complete(),
                    TimeDifference.getFormattedTimeNanosDiff(currTimeNanos - startTimeNanos), TerminalColours.reset())

            reported = true
        }
    }
}