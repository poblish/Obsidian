package org.hiatusuk.obsidian.javascript.jasmine.delegates

import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.events.BeforeScenarioElement
import javax.inject.Inject

@ScenarioScope
class JasmineRunStatus
@Inject constructor() {
    var passes: Int = 0
    var fails: Int = 0
    private var runCompleted: Boolean = false

    @BeforeScenarioElement
    fun resetForScenarioFile() {
        fails = 0
        passes = fails
    }

    fun hasRunCompleted(): Boolean {
        return runCompleted
    }

    fun runCompleted() {
        runCompleted = true
    }
}
