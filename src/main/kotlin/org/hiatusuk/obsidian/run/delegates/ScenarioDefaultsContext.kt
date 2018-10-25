package org.hiatusuk.obsidian.run.delegates

import com.google.common.collect.HashBasedTable
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.events.AfterScenario
import org.slf4j.Logger
import java.util.*
import javax.inject.Inject

@ScenarioScope
class ScenarioDefaultsContext @Inject constructor(private val log: Logger) {

    private val scenarioDefaults = HashBasedTable.create<String, String, Any>()

    @AfterScenario
    fun resetAfterScenario() {
        if (!scenarioDefaults.isEmpty) {
            log.debug("Clearing defaults: {}", scenarioDefaults)
        }
        scenarioDefaults.clear()
    }

    fun store(inType: String, inName: String, inVal: Any) {
        scenarioDefaults.put(inType, inName, inVal)
    }

    operator fun get(inType: String, inName: String): Int {
        return getOptional(inType, inName).orElse(0)
    }

    fun getElse(inType: String, inName: String, inDefaultValue: Int): Int {
        return getOptional(inType, inName).orElse(inDefaultValue)
    }

    fun getOptional(inType: String, inName: String): Optional<Int> {
        if (scenarioDefaults.contains(inType, inName)) {
            val defValue = scenarioDefaults.get(inType, inName)
            return Optional.of((defValue as? Number)?.toInt() ?: Integer.parseInt(defValue.toString()))
        }

        return Optional.empty()
    }

    override fun toString(): String {
        return scenarioDefaults.toString()
    }
}
