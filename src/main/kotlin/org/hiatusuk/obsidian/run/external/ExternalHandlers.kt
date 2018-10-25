package org.hiatusuk.obsidian.run.external

import com.codahale.metrics.MetricRegistry
import com.google.common.base.Charsets
import com.google.common.base.Suppliers
import com.google.common.cache.CacheBuilder
import com.google.common.collect.ImmutableList
import org.hiatusuk.obsidian.config.FeatureConfiguration
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.ScenarioRunner
import org.hiatusuk.obsidian.run.ScriptletCall
import org.hiatusuk.obsidian.run.SimpleMatchingRule
import org.hiatusuk.obsidian.run.events.AfterScenario
import org.hiatusuk.obsidian.run.events.AllEvents
import org.hiatusuk.obsidian.run.exceptions.MalformedScenarioException
import org.hiatusuk.obsidian.run.exceptions.MatchingScenarioNotFoundException
import org.hiatusuk.obsidian.run.state.ScenarioState
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.util.function.Supplier
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Provider

@FeatureConfiguration("handlers")
@ScenarioScope
class ExternalHandlers @Inject
constructor(private val runner: Provider<ScenarioRunner>?,  // Need to keep as a Provider to avoid Dagger stack overflow (!!)
            private val scenario: ScenarioState,
            private val metrics: MetricRegistry) {

    private val handlerScripts = linkedSetOf<String>()
    var isRunningHandler: Boolean = false
        private set

    private val includesYamlDataCache = CacheBuilder.newBuilder().build<String, List<Any>>()  // Nothing special

    fun hasHandlerScripts(): Boolean {
        return !handlerScripts.isEmpty()
    }

    @AfterScenario
    fun resetAfterScenario() {
        handlerScripts.clear()
        includesYamlDataCache.invalidateAll()
    }

    fun configure(inPayload: Map<String, Any>) {
        populate(inPayload.keys)
    }

    private fun populate(inHandlerScripts: Collection<String>) {
        handlerScripts.clear()
        LOG.debug("Registering handler scripts... {}", inHandlerScripts)
        handlerScripts.addAll(inHandlerScripts)
    }

    @AllEvents  // Listen to *all* lifecycle events so we can call any/all scripted handlers
    fun handleLifecycleEvents(actualEventAnnotation: Class<out Annotation>) {

        if (runner == null) {
            // log.warn("Cannot handleLifecycleEvents() as test runner has gone away");
            return
        }

        metrics.timer("ExternalHandlers.handleLifecycleEvents").time().use {
            // Add some degree of laziness. Obviously we can't know if there's a matching handler until we've
            // actually generated the Pattern, but at least this laziness cuts out *some* unnecessary construction
            // Perhaps if the script is empty, there are multiple handlers, etc.

            val call = /* Cache instance: */ Suppliers.memoize<ScriptletCall> { CustomScriptletCall(actualEventAnnotation) }

            // Scenario ever been run?
            if (scenario.scenarioData != null && !scenario.scenarioData!!.isEmpty()) {

                // Try the current scenario file first...

                for (eachDoc in scenario.currentScenarioYaml()) {
                    try {
                        handleScenarioElement(eachDoc, call.get())  // don't return after this, we need *all* handlers
                    } catch (e: MalformedScenarioException) {
                        LOG.warn("Ignore: " + e.message)
                    } catch (e: MatchingScenarioNotFoundException) {
                        break  // Not in the current scenario, fine. FIXME Improve error handling!
                    }

                }
            }

            // Look for external handlers too
            callHandler(call)
        }
    }

    fun callHandler(handlerName: String): Boolean {
        return callHandler(ScriptletCall(SimpleMatchingRule(handlerName)))
    }

    fun callHandler(inScriptlet: ScriptletCall): Boolean {
        val wasWithinHandler = isRunningHandler

        for (eachScript in handlerScripts) {
            LOG.trace("Calling handler script... {}", eachScript)

            for (eachDoc in parseYaml(eachScript)) {
                try {
                    isRunningHandler = true  // Should really use wrapRunner(...) but syntax is too painful
                    handleScenarioElement(eachDoc, inScriptlet)
                } finally {
                    isRunningHandler = wasWithinHandler
                }
            }
        }

        return true
    }

    // Lazy ScriptletCall construction, e.g. to avoid some unnecessariness in an @AllEvents handler
    private fun callHandler(inScriptletSupplier: Supplier<ScriptletCall>) {
        val wasWithinHandler = isRunningHandler

        for (eachScript in handlerScripts) {
            LOG.trace("Calling handler script... {}", eachScript)

            for (eachDoc in parseYaml(eachScript)) {
                try {
                    isRunningHandler = true  // Should really use wrapRunner(...) but syntax is too painful
                    handleScenarioElement(eachDoc, inScriptletSupplier.get())
                } finally {
                    isRunningHandler = wasWithinHandler
                }
            }
        }
    }

    fun handleScenarioElement(inDoc: Any, inCall: ScriptletCall) {
        runner!!.get().handleScenarioElement(inDoc, inCall, true)
    }

    private fun parseYaml(scriptFilePath: String): Iterable<Any> {
        return includesYamlDataCache.get(scriptFilePath) {
            val data = File(scriptFilePath).readText(Charsets.UTF_8)
            ImmutableList.copyOf(Yaml().loadAll(data))  // Yaml not thread-safe/reentrant, but because of cache we can probably afford to create each time.
        }
    }

    fun wrapRunner(inRunner: () -> Unit) {
        val wasWithinHandler = isRunningHandler
        try {
            isRunningHandler = true
            inRunner.invoke()
        } finally {
            isRunningHandler = wasWithinHandler
        }
    }

    private class CustomScriptletCall internal constructor(actualEventAnnotation: Class<out Annotation>) : ScriptletCall(Pattern.compile("@" + actualEventAnnotation.simpleName + "\\b")) {

        override fun matchesScenario(inScenarioName: String): Boolean {
            return this.pattern!!.matcher(inScenarioName).find()
        }

        override fun handleNotFound() {
            // Ignore
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger("Handlers")
    }
}
