package org.hiatusuk.obsidian.run.state

import org.hiatusuk.obsidian.di.ScenarioScope
import javax.inject.Inject
import javax.inject.Provider

@ScenarioScope
class RecordedErrors @Inject
internal constructor(private val scenarioState: Provider<ScenarioState>) {

    internal fun newError(error: Throwable): RecordedError {
        val state = scenarioState.get()
        return RecordedError(state.currentScenarioName, state.currentScenarioPath, error)
    }
}
