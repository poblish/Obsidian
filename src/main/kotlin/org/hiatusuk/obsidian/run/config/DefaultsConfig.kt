package org.hiatusuk.obsidian.run.config

import org.hiatusuk.obsidian.config.FeatureConfiguration
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.delegates.ScenarioDefaultsContext
import org.slf4j.Logger
import javax.inject.Inject

@FeatureConfiguration("defaults")
@ScenarioScope
class DefaultsConfig @Inject
internal constructor(private val defaults: ScenarioDefaultsContext, private val log: Logger) {

    fun configure(inPayload: Map<String, Any>) {
        val defaultsConfigMap = inPayload as Map<String, Map<String, Any>>
        for ((key, value) in defaultsConfigMap) {
            for ((key1, value1) in value) {
                defaults.store(key, key1, value1)
            }
        }
        log.debug("Setting defaults: {}", defaults)
    }
}