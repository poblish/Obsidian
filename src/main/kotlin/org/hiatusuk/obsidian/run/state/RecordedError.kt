package org.hiatusuk.obsidian.run.state

class RecordedError internal constructor(currentScenarioName: String?, currentScenarioPath: String?, // Disabled due to overlap with line number in error message... private int lineNo;
                                         val throwable: Throwable) {

    val currentScenarioName: String = currentScenarioName ?: "???"
    val currentScenarioPath: String = currentScenarioPath ?: "???"
}
