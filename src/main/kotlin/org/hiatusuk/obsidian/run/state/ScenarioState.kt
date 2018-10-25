package org.hiatusuk.obsidian.run.state

import com.codahale.metrics.MetricRegistry
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.cmd.api.CommandSpecFactory
import org.hiatusuk.obsidian.config.ConfigUtils
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.RunProperties
import org.hiatusuk.obsidian.run.ScenarioParsing
import org.hiatusuk.obsidian.run.delegates.CommandMappings
import org.hiatusuk.obsidian.run.events.AfterScenariosFailed
import org.hiatusuk.obsidian.run.events.ApplicationQuit
import org.hiatusuk.obsidian.run.events.BeforeScenario
import org.hiatusuk.obsidian.run.events.BeforeScenarioElement
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.web.selenium.config.RequiresBrowser
import org.hiatusuk.obsidian.web.selenium.find.FindRequest
import org.hiatusuk.obsidian.web.utils.UrlUtils
import org.yaml.snakeyaml.Yaml
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.util.*
import javax.inject.Inject

@ScenarioScope
class ScenarioState @Inject
constructor(private val props: RunProperties,
            private val cmdSpecs: CommandSpecFactory,
            private val mappings: CommandMappings,
            private val exceptions: RuntimeExceptions,
            private val metrics: MetricRegistry) {

    var currentScenarioName: String? = null
        private set

    var currentScenarioPath: String? = null
        private set

    private var currentScenarioYamlData: String? = null

    private var startTimeMillis = -1L
    var isScenariosFailed: Boolean = false
        private set

    private var siteBaseUrl: String? = ""
    var lastUrlVisited = ""
    var defaultTarget = Optional.empty<FindRequest>()
        private set
    var lastCommand = CommandSpec.empty()  // No need for null-checks
    private var requiresBrowsers: Boolean = false

    internal var elementsActuallyRun: Boolean = false
    private var quitHandlerRun: Boolean = false

    private val yamlInst = Yaml()  // Remember, not thread-safe
    private var yamlDocs = emptyList<Any>()

    // Synchronization done *purely* to protect the Yaml inst, no more promises than that
    var scenarioData: String?
        get() = this.currentScenarioYamlData
        set(data) {
            this.currentScenarioYamlData = data
            synchronized(yamlInst) {
                metrics.timer("ScenarioState.loadYaml").time().use { yamlDocs = yamlInst.loadAll(data).toList() }
            }
        }

    // Basic validation: http://stackoverflow.com/a/5719282/954442
    // Fine to lose information
    var baseUrl: String?
        get() = siteBaseUrl
        set(url) {
            val nonnullUrl = UrlUtils.expandAndCorrectUrl(url!!, null)

            if (!nonnullUrl.isEmpty()) {
                try {
                    URL(nonnullUrl).toURI()
                } catch (e: MalformedURLException) {
                    throw exceptions.runtime("Invalid URL: $nonnullUrl")
                } catch (e: URISyntaxException) {
                    throw exceptions.runtime("Invalid URL: $nonnullUrl")
                }
            }

            this.siteBaseUrl = nonnullUrl
        }

    @BeforeScenarioElement
    fun resetForScenarioFile() {
        elementsActuallyRun = false
        siteBaseUrl = ""
        currentScenarioYamlData = null
        currentScenarioPath = currentScenarioYamlData
        currentScenarioName = currentScenarioPath
        lastCommand = CommandSpec.empty()  // No need for null-checks
        requiresBrowsers = false
        startTimeMillis = System.currentTimeMillis()
    }

    @BeforeScenario
    fun resetForBrowser() {
        lastUrlVisited = ""  // Reset 'implicit /' check
    }

    fun setScenarioName(name: String) {
        this.currentScenarioName = name
    }

    fun currentScenarioYaml(): List<Any> {
        return yamlDocs
    }

    fun resetDefaultTarget() {
        defaultTarget = Optional.empty()
    }

    fun setDefaultTarget(inTarget: FindRequest) {
        defaultTarget = Optional.of(inTarget)
    }

    fun hasBaseUrl(): Boolean {
        return siteBaseUrl != null && !siteBaseUrl!!.isEmpty()
    }

    fun setScenarioPath(absolutePath: String) {
        this.currentScenarioPath = checkNotNull(absolutePath)
    }

    @AfterScenariosFailed
    fun failed(error: Throwable) {
        this.isScenariosFailed = true
    }

    @ApplicationQuit
    fun ensureScenariosRan() {
        if (!this.quitHandlerRun && !this.elementsActuallyRun && !props.ignoreScenariosWithMissingSteps) {
            this.quitHandlerRun = true
            throw exceptions.illegalArgument("No scenario steps actually performed, this is not a valid use case.")
        }
    }

    fun detectBrowserUsage() {
        this.requiresBrowsers = usesUrls()
    }

    private fun usesUrls(): Boolean {
        metrics.timer("ScenarioState.requiresBrowserCheck").time().use {
            for (eachDoc in yamlDocs) {
                for ((_, value) in ScenarioParsing.elements(eachDoc, 0, ScenarioParsing.LISTS)) {
                    if (checkCmdEntries(value as List<Map<String,Any>?>)) {
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun checkCmdEntries(cmdEntries: Iterable<Map<String, Any>?>): Boolean {
        for (eachCmd in cmdEntries.filter { it != null }) {
            if (eachCmd!!.containsKey("initially")) {
                if (checkCmdEntries(ConfigUtils.mapElements(eachCmd["initially"]))) {
                    return true
                }
                continue
            }

            val cmd = this.mappings.commandForName(this.cmdSpecs.createForValidation(eachCmd))
            if (cmd != null && cmd.javaClass.getAnnotation(RequiresBrowser::class.java) != null) {
                return true
            }
        }

        return false
    }

    fun requiresBrowsers(): Boolean {
        return requiresBrowsers
    }
}
