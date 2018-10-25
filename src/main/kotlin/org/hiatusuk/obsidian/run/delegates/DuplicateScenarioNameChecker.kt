package org.hiatusuk.obsidian.run.delegates

import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.ScenarioParsing
import org.hiatusuk.obsidian.run.events.BeforeScriptExecution
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.run.state.ScenarioState
import java.util.regex.Pattern
import javax.inject.Inject

@ScenarioScope
class DuplicateScenarioNameChecker
@Inject constructor(private val state: ScenarioState,
                    private val exceptions: RuntimeExceptions) {

    private var entireYaml: String? = null

    @BeforeScriptExecution
    fun resetForScenarioFile() {
        entireYaml = state.scenarioData
    }

    fun onScenario(inName: String) {
        if (entireYaml == null || inName.startsWith(ScenarioParsing.UNTITLED_SCENARIO_PREFIX)) {  // Ugh, correct but hacky
            return
        }

        val fixedName = inName.trim()
        val m = Pattern.compile("^" + Pattern.quote(fixedName) + "\\s*:", Pattern.MULTILINE).matcher(entireYaml!!)
        if (!m.find()) {
            throw exceptions.runtime("ERROR: Could not find a single reference to scenario '$fixedName'")
        }
        if (m.find()) {
            throw exceptions.runtime("ERROR: Found a duplicate reference to scenario '$fixedName'. Please rename one of them.")
        }
    }
}