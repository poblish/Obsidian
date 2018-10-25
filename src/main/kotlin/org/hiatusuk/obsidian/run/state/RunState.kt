package org.hiatusuk.obsidian.run.state

import org.hiatusuk.obsidian.di.ScenarioScope
import org.slf4j.Logger
import javax.inject.Inject

@ScenarioScope
class RunState @Inject constructor(private val log: Logger) {

    private var cancelled: Boolean = false
    private var paused: Boolean = false

    val isCancelled: Boolean
        get() {
            while (paused && !cancelled) {
                Thread.sleep(1000)
                log.info("RunState: <still paused>")
            }

            return cancelled
        }
}
