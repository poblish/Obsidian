package org.hiatusuk.obsidian.cucumber

import java.io.File
import java.util.ArrayList


class CucumberInputs {

    private val gherkinScenarios = ArrayList<File>()
    private val gherkinImplScenarios = ArrayList<File>()

    val scenarios: List<File>
        get() = gherkinScenarios

    val implementations: List<File>
        get() = gherkinImplScenarios

    fun addFeatures(allGherkinFiles: Collection<File>) {
        gherkinScenarios.addAll(allGherkinFiles)
    }

    fun addImplementations(inImplFiles: Collection<File>) {
        gherkinImplScenarios.addAll(inImplFiles)
    }
}
