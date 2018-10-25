package org.hiatusuk.obsidian.run.delegates

import com.google.common.collect.HashBiMap
import org.hiatusuk.obsidian.config.FeatureConfiguration
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.events.BeforeScenarioElement
import javax.inject.Inject

@ScenarioScope
@FeatureConfiguration("ns")
class CommandNamespaces
@Inject constructor() {
    private val prefixNsMappings = HashBiMap.create<String, String>()

    @BeforeScenarioElement
    fun clearBeforeScenario() {
        this.prefixNsMappings.clear()
    }

    fun configure(inPayload: Map<String,Any>) {
        this.prefixNsMappings.putAll(inPayload as Map<String, String>)
    }

    fun toCanonicalName(cmdName: String): String {
        return replaceNames(prefixNsMappings, cmdName)
    }

    private fun replaceNames(inMap: Map<String, String>, cmdName: String): String {
        for ((key, value) in inMap) {
            if (cmdName.startsWith(key)) {
                // Replace prefix vs canonical version
                return value + cmdName.substring(key.length)
            }
        }
        return cmdName
    }
}
