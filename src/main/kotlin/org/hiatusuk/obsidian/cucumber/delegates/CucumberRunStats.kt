package org.hiatusuk.obsidian.cucumber.delegates

import cucumber.api.Result.Type
import cucumber.api.Result.Type.PASSED
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.events.ApplicationQuit
import javax.inject.Inject

@ScenarioScope
class CucumberRunStats @Inject constructor() {

    var scenarioCount = 0
    var stepCount = 0

    val scenarioStats = hashMapOf<Type,Int>()
    val stepStats = hashMapOf<Type,Int>()

    fun clear() {
        scenarioStats.clear()
        scenarioCount = 0

        stepStats.clear()
        stepCount = 0
    }

    // FIXME: *Implicit* Cucumber error-handling. Should ultimately be replaced/augmented by custom assertions in scripts
    @ApplicationQuit
    fun handleCucumberResults() {

        if (scenarioCount > scenarioStats.getOrDefault(PASSED, 0) ||
            stepCount > stepStats.getOrDefault(PASSED, 0)) {
            clear()  // Clear in case the exception triggers Quit again and we end up in a loop
            throw RuntimeException("Not all Cucumber tests passed")
        }
    }
}
