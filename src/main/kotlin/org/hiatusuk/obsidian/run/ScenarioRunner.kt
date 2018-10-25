package org.hiatusuk.obsidian.run

import com.codahale.metrics.MetricRegistry
import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Throwables
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.cmd.api.CommandSpecFactory
import org.hiatusuk.obsidian.config.ConfigUtils
import org.hiatusuk.obsidian.config.ConfigurationRegistry
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.cucumber.delegates.CucumberRunStats
import org.hiatusuk.obsidian.run.delegates.ApplicationShutdownState
import org.hiatusuk.obsidian.run.delegates.CommandMappings
import org.hiatusuk.obsidian.run.delegates.DuplicateScenarioNameChecker
import org.hiatusuk.obsidian.run.events.*
import org.hiatusuk.obsidian.run.exceptions.ExitScenarioException
import org.hiatusuk.obsidian.run.external.ExternalHandlers
import org.hiatusuk.obsidian.run.profiles.ScenarioProfilesConfig
import org.hiatusuk.obsidian.run.state.LineNumbersState
import org.hiatusuk.obsidian.run.state.RunState
import org.hiatusuk.obsidian.run.state.ScenarioState
import org.hiatusuk.obsidian.run.state.SuppressedErrors
import org.hiatusuk.obsidian.utils.TerminalColours
import org.hiatusuk.obsidian.web.selenium.config.RemoteDrivers
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.slf4j.Logger
import java.io.Closeable
import java.io.IOException
import javax.inject.Inject
import javax.inject.Provider

class ScenarioRunner
@Inject
constructor(@field:VisibleForTesting val runProps: RunProperties,
            @field:VisibleForTesting var inputs: RunInputs,
            @field:VisibleForTesting val web: WebState,
            @field:VisibleForTesting internal val scenarioState: ScenarioState,
            private val appShutdownState: ApplicationShutdownState,
            private val lineNumbers: LineNumbersState,
            private val runState: RunState,
            val drivers: RemoteDrivers,
            private val cmdSpecs: CommandSpecFactory,
            @field:VisibleForTesting internal val forLoopHandler: ForLoopHandler,
            private val lifecycle: Lifecycle,
            @field:VisibleForTesting internal val variablesContext: VariablesContext,
            @field:VisibleForTesting internal val errorsState: SuppressedErrors,
            private val scenarioNameChecker: DuplicateScenarioNameChecker,
            externalHandlers: Provider<ExternalHandlers>,
            private val cmdMappings: CommandMappings,
            private val configs: ConfigurationRegistry,
            private val profiles: ScenarioProfilesConfig,
            val cucumberStats: CucumberRunStats,  // FIXME Should not be here, only needed to help complex CucumberRuntime bootstrapping
            private val metrics: MetricRegistry,
            private val LOG: Logger) : Closeable {

    private val externalHandlers: ExternalHandlers = externalHandlers.get()

    private var hasLoggedResultsForRun: Boolean = false
    private var hasAddedShutdownHook: Boolean = false
    internal var suppressRunnerCompletionLogging = false


    fun start(vararg filters: ScriptletCall) {
        try {
            runScenarioStartup()

            runScenarios(*filters)

            runAfterScenariosPassed()
        } catch (e: Throwable) {
            runAfterScenariosFailed(e)
            Throwables.throwIfUnchecked(e)
            throw RuntimeException(e)
        } finally {
            try {
                if (filters.isEmpty()) {  // Don't do any quitting when in filters/Cucumber mode. FIXME Hacky!
                    // Any shared instances...
                    drivers.tryQuit()
                }
            } finally {
                logResultsForRun()
            }
        }
    }

    fun startExternalHandlersRun(vararg filters: ScriptletCall) {
        externalHandlers.wrapRunner { start(*filters) }
    }

    @Throws(IOException::class)
    private fun runScenarios(vararg filters: ScriptletCall) {

        appShutdownState.recordScenarioStart()

        if (filters.isNotEmpty()) {
            LOG.info("> Processing Cucumber implementations: {}", inputs)
        } else {
            LOG.info("{}> Running Scenarios: {}{}", TerminalColours.green(), inputs, TerminalColours.reset())
        }

        if (inputs.isEmpty) {
            return
        }

        if (!hasAddedShutdownHook) {
            Runtime.getRuntime().addShutdownHook(Thread(Runnable {
                this@ScenarioRunner.quit()

                if (!suppressRunnerCompletionLogging) {
                    appShutdownState.reportOnExit()
                }
            }))
            hasAddedShutdownHook = true
        }

        for (eachScenarioInput in inputs) {

            runBeforeScenarioElement()

            if (runState.isCancelled) {
                break
            }

            scenarioState.setScenarioPath(eachScenarioInput.location)

            val originalYamlStr = eachScenarioInput.text
            lineNumbers.acceptScenario(originalYamlStr)

            scenarioState.scenarioData = ScenarioParsing.handleYamlIncludes(originalYamlStr, eachScenarioInput)

            if (!obtainBaseUrlAndClearData()) {
                continue  // Probably excluded by profile filter
            }

            scenarioState.detectBrowserUsage()

            ///////////////////////////////////////////////////////////////////////////

            runBeforeScriptExecution(eachScenarioInput)

            var scenarioSucceeded = false  // Assumes drivers() is not empty, which I'm sure it must be

            try {
                for (eachDriver in drivers.drivers()) {
                    LOG.info("{}>> Processing: '{}' using {}{}", TerminalColours.greenBold(), eachScenarioInput, eachDriver, TerminalColours.reset())

                    web.init(eachDriver)

                    runBeforeScenario()

                    if (filters.isEmpty()) {
                        for (eachDoc in scenarioState.currentScenarioYaml()) {
                            handleScenarioElement(eachDoc, null, /* FIXME: Ever true? */ externalHandlers.isRunningHandler)
                        }
                    } else {
                        for (eachFilter in filters) {
                            for (eachDoc in scenarioState.currentScenarioYaml()) {
                                handleScenarioElement(eachDoc, eachFilter, /* FIXME: Ever true? */ externalHandlers.isRunningHandler)
                            }
                        }
                    }

                    lifecycle.call(AfterBrowserCompletedScenario::class.java, eachDriver)
                }

                scenarioSucceeded = !errorsState.hasErrors()
            } finally {
                if (scenarioSucceeded) {
                    lifecycle.call(AfterScenarioPassed::class.java)
                }

                runAfterScenario()
            }
        }
    }

    override// Closeable
    fun close() {
        quit()
    }

    fun quit() {
        // FIXME: This should really run *after* Quit handled, as some metrics may be missed as a result. However,
        // it generally runs as soon as scenario completed, which of course makes perfect sense in GUI mode...
        // We should probably do *both* even if it means > 1 metrics output.
        if (!hasLoggedResultsForRun) {
            logResultsForRun()
        }

        lifecycle.call(ApplicationQuit::class.java)
    }

    private fun obtainBaseUrlAndClearData(): Boolean {

        // First of all, need to pick up all 'set:' entries
        metrics.timer("ScenarioRunner.handleSets").time().use {
            for (eachDoc in scenarioState.currentScenarioYaml()) {
                for ((_, value) in ScenarioParsing.elements(eachDoc, 0, ScenarioParsing.SETTERS)) {
                    val setMap = value as Map<String, Any>
                    for ((key, value1) in setMap) {
                        variablesContext.store(key, value1)
                    }
                }
            }
        }

        variablesContext.beginScenarios()  // Apply any overrides over the top of anything defined by 'set:'. These in turn can be overridden

        // Run *before* scenario config
        if (!runProps.defaultConfig.isEmpty()) {
            LOG.debug("Applying default configuration rules: {}", runProps.defaultConfig)

            for ((defConfigKey, defConfigValue) in runProps.defaultConfig) {
                configs.handleConfigForName(defConfigKey, defConfigValue)
            }
        }

        // Now the rest of the declarations:

        metrics.timer("ScenarioRunner.runConfigs").time().use {
            for (eachDoc in scenarioState.currentScenarioYaml()) {

                var tempScenarioIdx = 0  // Just so we can title untitled scenarios

                for ((topLevelKey, topLevelValue) in ScenarioParsing.elements(eachDoc, ++tempScenarioIdx).filter
                        { c -> c.key != "set" /* Already addressed */ }) {

                    if (configs.handleConfigForName(topLevelKey, topLevelValue)) {
                        continue
                    }

                    if (topLevelValue is Map<*, *>) {
                        throw RuntimeException("Unhandled declaration: '$topLevelKey', with value '$topLevelValue'")
                    } else {
                        if (topLevelValue is Collection<*>) {
                            // Suppress ClassCastException when a bogus command (a non-Map) is found. Usually missing a colon.
                            val list = topLevelValue as List<*>
                            if (!list.isEmpty()) {
                                val first = list.first()
                                if (first != null && first !is Map<*, *>) {
                                    throw RuntimeException("Invalid command '$topLevelValue' within '$topLevelKey'. Missing a colon?")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Run *after* scenario config
        if (!runProps.overrideConfig.isEmpty()) {
            LOG.debug("Overriding configuration rules: {}", runProps.overrideConfig)

            for ((ovrConfigKey, ovrConfigValue) in runProps.overrideConfig) {
                configs.handleConfigForName(ovrConfigKey, ovrConfigValue)
            }
        }

        return profiles.profileAccepted
    }

    fun handleScenarioElement(inScenarioElem: Any,
                              inFilter: ScriptletCall?,
                              inRunningHandler: Boolean) {

        var scenarioIdx = -1  // So we can track 'here:'

        var filterWasMatched = false

        try {  // ExitScenarioException

            for (eachScenario in ScenarioParsing.elements(inScenarioElem, scenarioIdx + 1, ScenarioParsing.LISTS)) {

                if (!inRunningHandler) {  // Don't try to validate handler command against *real* script
                    scenarioNameChecker.onScenario(eachScenario.key)
                }

                if (inFilter != null) {
                    val resolvedScriptletName = variablesContext.resolve(eachScenario.key as String?)!!
                    if (inFilter.matchesScenario(resolvedScriptletName)) {
                        filterWasMatched = true

                        if (!inRunningHandler) {  // Suppress noisy logs
                            LOG.info("*** Starting filtered scenario with " + inFilter.arguments())
                        }

                        inFilter.enterFilter(resolvedScriptletName)
                    } else {
                        continue
                    }
                }

                if (!inRunningHandler) {
                    // Don't replace *real* script name with handler's one
                    scenarioState.setScenarioName(eachScenario.key)

                    // Don't run a ^handler method as if it were a real scenario, as part of the real flow
                    if (eachScenario.key.startsWith("^")) {
                        continue
                    }
                }

                /////////////////////////////////////////////////////////////////////

                val finallyBlocksForScenario = arrayListOf<Map<String, Any>>()

                if (!inRunningHandler) {  // Don't need to log that we're running login/logout handler, etc.
                    LOG.info("{}>>> Scenario: '{}'...{}", TerminalColours.darkGreenBI(), eachScenario.key, TerminalColours.reset())
                }

                scenarioIdx++

                val list = ScenarioParsing.stripIgnorableScenarioElements(eachScenario)
                if (!list.isEmpty()) {
                    scenarioState.elementsActuallyRun = true
                }

                // Validation phase...
                metrics.timer("cmd.validation").time().use {

                    if (runProps.isDebug) {
                        LOG.debug("=> Validating all: {}", list)
                    }

                    for (eachCmdForValidation in list) {
                        validateCommand(eachCmdForValidation)
                    }
                }

                for (eachCmd in list) {

                    if (eachCmd.containsKey("initially")) {
                        if (!inRunningHandler) {  // Don't support `initially` in handlers
                            LOG.info("Perform initially() block...")

                            for (eachInitiallyCmd in ConfigUtils.mapElements(eachCmd["initially"])) {
                                handleCommand(eachInitiallyCmd)  // No special exception-handling logic
                            }
                        }
                    } else if (eachCmd.containsKey("finally")) {
                        finallyBlocksForScenario.addAll(ConfigUtils.mapElements(eachCmd["finally"]))
                    }
                }

                forLoopHandler.start(list, finallyBlocksForScenario) { handleCommand(it) }
            }

        } catch (e: ExitScenarioException) {
            LOG.debug("Exited scenario by request.")
        }

        if (filterWasMatched) {
            inFilter!!.exitFilter()
        } else inFilter?.handleNotFound()
    }

    private fun validateCommand(eachCmd: Map<String, Any>) {
        val cmd = cmdSpecs.createForValidation(eachCmd)

        if (runProps.isDebug) {
            LOG.debug("=> Validating: {}", cmd)
        }

        cmdMappings.validate(cmd)
    }

    private fun handleCommand(eachCmd: Map<String,Any>) {
        val cmdName = eachCmd.keys.first()
        if (cmdName == "finally" || cmdName == "initially" || cmdName == "skip") {
            return  // Ignore, these are 'keywords', not a command
        }

        /////////////////////////////////////////////////////////

        val cmd = cmdSpecs.create(eachCmd)
        try {
            handleCommandWrapped(cmd)
        } catch (/* A 'signal', not an error */ t: ExitScenarioException) {
            throw t
        } catch (t: Throwable) {
            errorsState.onError(t)
        } finally {
            scenarioState.lastCommand = cmd
        }
    }

    @Throws(IOException::class)
    private fun handleCommandWrapped(inCmd: CommandSpec) {

        if (runProps.isDebug) {
            LOG.debug("=> Processing: {}", inCmd)
        }

        //////////////////////////////////////////////////////  Command-matching

        val mappedCmd = cmdMappings.commandForName(inCmd)
        if (mappedCmd != null) {
            mappedCmd.run(inCmd)
        } else {
            throw RuntimeException(lineNumbers.status() + "Unhandled command: " + inCmd)
        }
    }

    private fun logResultsForRun() {
        try {
            lifecycle.call(OutputFinalReports::class.java)
        } finally {
            hasLoggedResultsForRun = true
        }
    }

    private fun runScenarioStartup() {
        hasLoggedResultsForRun = false
        lifecycle.call(ScenarioStartup::class.java)
    }

    private fun runBeforeScenarioElement() {
        lifecycle.call(BeforeScenarioElement::class.java)
    }

    private fun runBeforeScriptExecution(input: RunInput) {
        lifecycle.call(BeforeScriptExecution::class.java, input)
    }

    private fun runBeforeScenario() {
        lifecycle.call(BeforeScenario::class.java)
    }

    private fun runAfterScenario() {
        lifecycle.call(AfterScenario::class.java)
    }

    private fun runAfterScenariosPassed() {
        lifecycle.call(AfterScenariosPassed::class.java)
    }

    private fun runAfterScenariosFailed(error: Throwable) {
        lifecycle.call(AfterScenariosFailed::class.java, error)
    }
}