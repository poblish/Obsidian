package org.hiatusuk.obsidian.config

import com.codahale.metrics.MetricRegistry
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.di.includes.ConfigsComponent
import org.hiatusuk.obsidian.run.delegates.ReflectionsState
import org.hiatusuk.obsidian.run.events.ScenarioStartup
import org.slf4j.Logger
import javax.inject.Inject

@ScenarioScope
class ConfigurationRegistry @Inject
constructor(private val configs: ConfigsComponent,
            private val reflections: ReflectionsState,
            private val metrics: MetricRegistry,
            private val log: Logger) {

    private val mappings = hashMapOf<String, Class<*>>()
    private var done: Boolean = false

    @ScenarioStartup
    fun startUp() {
        if (done) {
            return
        }

        val ref = reflections.all()  // Outside the timer...

        metrics.timer("ConfigurationRegistry.generate").time().use {
            for (eachClass in ref.getTypesAnnotatedWith(FeatureConfiguration::class.java)) {
                val ann = eachClass.getAnnotation(FeatureConfiguration::class.java)
                if (ann != null && mappings.put(ann.value, eachClass) != null) {
                    throw RuntimeException("Duplicate mapping")
                }
            }

            log.trace("Derived: {}", mappings)
        }

        done = true
    }

    fun handleConfigForName(cmdName: String, inPayload: Any?): Boolean {
        metrics.timer("ConfigurationRegistry.handleConfigForName").time().use {
            val clz = mappings[cmdName] ?: return false

            for (eachMethod in configs.javaClass.methods) {
                if (eachMethod.returnType == clz) {
                    try {
                        if (!eachMethod.isAccessible) {
                            eachMethod.isAccessible = true
                        }

                        val inst = eachMethod.invoke(configs)
                        clz.getDeclaredMethod("configure", Map::class.java).invoke(inst, ConfigUtils.mapEntries(inPayload))
                        return true
                    }
                    catch (e: ReflectiveOperationException) {
                        if (e.cause != null && e.cause is RuntimeException) {
                            throw e.cause as RuntimeException
                        }

                        throw RuntimeException(e)
                    }

                }
            }

            return false
        }
    }
}
