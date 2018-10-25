package org.hiatusuk.obsidian.run.config

import org.hiatusuk.obsidian.config.FeatureConfiguration
import org.hiatusuk.obsidian.di.ScenarioScope
import org.slf4j.Logger
import javax.inject.Inject

@FeatureConfiguration("systemProperties")
@ScenarioScope
class SystemPropertiesConfig @Inject
constructor(private val log: Logger) {

    fun configure(inPayload: Map<String, Any>) {
        for ((key, value) in inPayload) {
            // Keep it simple: if key present, set the System property come what may
            val newValue = value.toString()
            System.setProperty(key, newValue)
            log.debug("System Property '{}' => '{}'", key, newValue)
        }
    }
}
