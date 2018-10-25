package org.hiatusuk.obsidian.run.delegates

import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.events.AfterScenariosFailed
import org.hiatusuk.obsidian.run.events.AfterScenariosPassed
import org.hiatusuk.obsidian.run.state.ScenarioState
import org.slf4j.Logger
import javax.inject.Inject

@ScenarioScope
class ScenarioStatusPublisher @Inject
internal constructor(private val scenario: ScenarioState,
                     private val log: Logger) {

//    @AcceptCommand
//    fun publish(currentCmd: Map<String, Any>) {
//        socketIo.publish("status", Status(lineNumbers.currentLine(), lineNumbers.countLines(), scenario.currentScenarioPath, ""))
//    }

    @AfterScenariosPassed
    fun passed() {
        if (scenario.currentScenarioPath == null) {
            log.warn("No ScenarioState to publish.")
            return
        }

        // socketIo.publish("status", Status( /* Ugh...? */-1, lineNumbers.countLines(), scenario.currentScenarioPath, "true"))
    }

    @AfterScenariosFailed
    fun failed(error: Throwable) {
        // socketIo.publish("status", Status( /* Ugh...? */-1, lineNumbers.countLines(), scenario.currentScenarioPath, "false"))
    }

    // private class Status internal constructor(val lineNumber: Int, val totalLines: Int, val filePath: String?, val status: String)
}
