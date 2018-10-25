package org.hiatusuk.obsidian.run

import com.google.common.annotations.VisibleForTesting
import org.hiatusuk.obsidian.cucumber.CucumberInputs
import org.hiatusuk.obsidian.cucumber.CucumberRuntimeFactory
import org.hiatusuk.obsidian.cucumber.ObsidianBackend
import org.hiatusuk.obsidian.di.component.DaggerApplicationComponent
import org.hiatusuk.obsidian.di.component.DaggerRunnerComponent
import org.hiatusuk.obsidian.files.FileUtils
import org.hiatusuk.obsidian.gui.GuiDelegate
import org.hiatusuk.obsidian.utils.TerminalColours
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.slf4j.bridge.SLF4JBridgeHandler
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileNotFoundException
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class ApplicationRunner(inArgs: Array<String>) {

    private val args: Array<String>
    private val applicationComponent = DaggerApplicationComponent.create()

    init {
        // Detect Help mode, and exit when done
        for (eachArg in inArgs) {
            if ("--help" == eachArg) {
                printHelp()
                System.exit(0)
            }
        }

        LOG.info("Starting Obsidian...")
        args = inArgs.clone()
    }

    fun startUp() {
        startUp(true)
    }

    fun startUpNoSystemExit() : ScenarioRunner? {
        return startUp(false)
    }

    private fun startUp(inSystemExitAtEnd: Boolean) : ScenarioRunner? {

        SLF4JBridgeHandler.removeHandlersForRootLogger()
        SLF4JBridgeHandler.install()

        val props = populateRunProperties(args)

        if (!args.contains("--nogui")) {
            return launchGUI(props)
        }

        //////////////////////////////////////////////////////////

        val cukeInputs = CucumberInputs()
        val scenarioFiles = ArrayList<File>()
        var gherkinMode = false

        for (eachArg in args) {
            if (eachArg.startsWith("--gherkinPath=")) {
                gherkinMode = true
                cukeInputs.addFeatures(FileUtils.allGherkinFiles(File(eachArg.substring(14).trim())))
            } else if (eachArg.startsWith("--implPath=")) {
                gherkinMode = true
                cukeInputs.addImplementations(FileUtils.allYamlFiles(File(eachArg.substring(11).trim())))
            }
            else if (!eachArg.startsWith("--")) {
                val f = File( eachArg.trim() )
                if (!f.exists()) {
                    LOG.error("File doesn't exist: $f")

                    if (inSystemExitAtEnd) {
                        System.exit(1)
                    } else {
                        throw FileNotFoundException("Missing $f")
                    }
                }
                scenarioFiles.add(f)
            }
        }

        LOG.debug("Using {}", props)


        if (gherkinMode) {
            val component = DaggerRunnerComponent.builder()
                    .application(applicationComponent)
                    .props(props)
                    .inputs(RunInputs(cukeInputs.implementations))
                    .build()

            val runner = component.scenarioRunner
            runGherkin(runner, cukeInputs, inSystemExitAtEnd)
            return runner
        } else if (props.parallelThreads <= 1) {
            LOG.info("Executing scenario file(s) in sequence...")

            val component = DaggerRunnerComponent.builder()
                    .application(applicationComponent)
                    .props(props)
                    .inputs(RunInputs(scenarioFiles))
                    .build()

            val runner = component.scenarioRunner
            runNoGui(null, runner, scenarioFiles, inSystemExitAtEnd)
            return runner
        } else /* Parallel */ {
            LOG.info("Executing " + scenarioFiles.size + " scenario file(s) in parallel across " + props.parallelThreads + " threads...")

            val exec = Executors.newFixedThreadPool(props.parallelThreads)

            val latch = CountDownLatch( scenarioFiles.size )

            for ((idx, file) in scenarioFiles.withIndex()) {
                exec.submit {
                    MDC.put("run", "#" + (idx + 1) + ",")
                    val component = DaggerRunnerComponent.builder().application(applicationComponent).props(props).inputs(RunInputs(file)).build()
                    runNoGui(latch, component.scenarioRunner, listOf(file), false)
                }
            }

            try {
                LOG.debug("Waiting for completion...")
                latch.await()
                System.exit(0)
            } catch (e: Throwable) {
                logFinalError(e)
            }

            return null
        }
    }

    private fun runGherkin(runner: ScenarioRunner, cukeInputs: CucumberInputs, inSystemExitAtEnd: Boolean) {
        val backend = ObsidianBackend(cukeInputs, runner, runner.variablesContext)

        try {
            CucumberRuntimeFactory(runner.variablesContext, runner.cucumberStats).create(backend, cukeInputs).run()
        } catch (e: Throwable) {
            logFinalError(e)
        } finally {
            backend.quit()

            if (inSystemExitAtEnd) {
                System.exit(0)
            }
        }
    }

    private fun runNoGui(latch: CountDownLatch?, runner: ScenarioRunner, scenarioFiles: List<File>, inSystemExitAtEnd: Boolean) {
        try {
            runner.start()
        } catch (e: Throwable) {
            logFinalError(e)
        } finally {
            runner.quit()

            latch?.countDown()

            if (inSystemExitAtEnd) {
                System.exit(0)
            }
        }
    }

    private fun populateRunProperties(args: Array<String>) : RunProperties {
        val props = RunProperties()

        for (eachArg in args) {
            if ("--noFailFast" == eachArg) {
                props.failFastMode = FailFastMode.FAIL_ON_LAST_MISMATCH
            } else if ("--metrics" == eachArg) {
                props.isLogMetrics = true
            }
            else if (eachArg.startsWith("--profiles=")) {
                val pos = eachArg.indexOf('=')
                props.specifiedProfiles.addAll( eachArg.substring(pos + 1).trim().split(',') )
            }
            else if (eachArg.startsWith("--defaultConfig=")) {
                val content = eachArg.substring(eachArg.indexOf('=') + 1).trim()
                props.defaultConfig = Yaml().load(content) as Map<String,Any>
            }
            else if (eachArg.startsWith("--overrideConfig=")) {
                val content = eachArg.substring(eachArg.indexOf('=') + 1).trim()
                props.overrideConfig = Yaml().load(content) as Map<String,Any>
            }
            else if (eachArg.startsWith("--parallelThreads=")) {
                val pos = eachArg.indexOf('=')
                if (pos > 0) {
                    props.parallelThreads = eachArg.substring(pos + 1).toInt()
                }
            }
            else if (eachArg.startsWith("--seleniumGridUrl=")) {
                val pos = eachArg.indexOf('=')
                if (pos > 0) {
                    try {
                        props.seleniumGridUrl = Optional.of(URL(eachArg.substring(pos + 1).trim()))
                        props.isUseSeleniumGrid = true
                    } catch (e: MalformedURLException) {
                        throw RuntimeException(e)
                    }

                }
            }
            else if (eachArg.startsWith("--V")) {
                val pos = eachArg.indexOf('=')
                if (pos > 0) {
                    props.runtimeOverrides[eachArg.substring(3, pos)] = eachArg.substring(pos + 1)
                }
            }
        }

        return props
    }

    private fun logFinalError(e: Throwable) {
        LOG.error(TerminalColours.error().toString() + "Final error..." + TerminalColours.reset(), e)
    }

    private fun launchGUI(props: RunProperties) : ScenarioRunner? {
        GUI_INST = GuiDelegate()

        GUI_INST.addListener{ req ->

            props.failFastMode = if (req.failFast) FailFastMode.FAIL_ON_FIRST_MISMATCH else FailFastMode.FAIL_ON_LAST_MISMATCH
            props.isLogAssertions = req.logAssertions
            props.isLogMetrics = req.logMetrics
            props.isUseSeleniumGrid = req.useSeleniumGrid
            props.seleniumGridUrl = req.seleniumGridUrl

            applicationComponent.applicationShutdownState().reset()

            val component = DaggerRunnerComponent.builder()
                    .application(applicationComponent)
                    .props(props)
                    .inputs(req.inputs)
                    .build()

            component.scenarioRunner.use {
                it.drivers.clear()

                it.suppressRunnerCompletionLogging = true
                it.start()
            }
         }

        GUI_INST.showGUI(props, LocalBrowserProperties(props, LOG))

        return null
    }

    companion object {

        @VisibleForTesting
        internal/* FIXME */ var GUI_INST = GuiDelegate()

        private val LOG = LoggerFactory.getLogger("Main")

        @VisibleForTesting
        internal fun printHelp() {
            println("Obsidian Usage - tbc")
        }
    }
}