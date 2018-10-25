package org.hiatusuk.obsidian.run

import com.google.common.annotations.VisibleForTesting
import org.hiatusuk.obsidian.cmd.Delay
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.run.delegates.ScenarioDefaultsContext
import org.hiatusuk.obsidian.run.events.AcceptCommand
import org.hiatusuk.obsidian.run.external.ExternalHandlers
import org.hiatusuk.obsidian.run.state.LineNumbersState
import org.hiatusuk.obsidian.run.state.RunState
import org.hiatusuk.obsidian.run.state.ScenarioState
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.hiatusuk.obsidian.web.selenium.find.FindRequest
import org.openqa.selenium.WebDriver
import org.slf4j.Logger
import javax.inject.Inject

class ForLoopHandler @Inject
constructor(@field:VisibleForTesting  // FIXME Not final!
            var web: WebState,
            private val finders: ElementFinders,
            private val varCtxt: VariablesContext,
            private val lifecycle: Lifecycle,
            private val lines: LineNumbersState,
            private val scenarioDefaults: ScenarioDefaultsContext,
            private val runState: RunState,
            private val scenarioState: ScenarioState,
            private val externalHandlers: ExternalHandlers,
            private val delayCmd: Delay,
            private val log: Logger) {

    private val forHashes = arrayListOf<Map<String, Any>>()

    fun start(list: List<Map<String, Any>>,
              finallyBlocksForScenario: Collection<Map<String, Any>>,
              inCmdRunner: (Map<String,Any>) -> Unit) {

        var savedCtxt: DriverContext? = null  // Should push/pop Stack
        var buildingForLoop = false
        val forTargets = arrayListOf<FindRequest>()
        val forUrls = arrayListOf<String>()
        var loopMode = ForLoopMode.UNKNOWN
        var cmdIdx = 0  // So we can track 'here:'
        var pc = 0
        var loopLen = -1
        var hereIdxFound = -1

        for (eachScenarioEntry in list) {
            if (eachScenarioEntry.containsKey("here")) {
                hereIdxFound = cmdIdx
                break
            }
            cmdIdx++
        }

        forHashes.clear()

        scenarioState.resetDefaultTarget()

        try {
            // Perform actions...
            while (pc < list.size && !runState.isCancelled) {

                // Skip to 'here:'
                if (hereIdxFound >= 0 && pc <= hereIdxFound) {
                    pc++
                    continue
                }

                val eachCmd = list[pc]

                if (!externalHandlers.isRunningHandler) {  // Don't try to validate handler command against *real* script
                    lifecycle.call(AcceptCommand::class.java, eachCmd)
                }

                if (ScenarioParsing.shouldSkipScenario(eachCmd)) {
                    break
                }

                cmdIdx++

                ////////////////////////////////////////////

                if (eachCmd.containsKey("for")) {
                    if (buildingForLoop) {
                        throw RuntimeException("Found nested 'for:'")
                    }

                    if (eachCmd.values.isEmpty()) {
                        throw RuntimeException("Missing 'for:' targets")
                    }

                    // Build for: targets
                    forUrls.clear()
                    forTargets.clear()

                    if (eachCmd.values.size == 1) {
                        val targetObj = eachCmd.values.first()

                        if (targetObj is List<*>) {
                            require(loopMode == ForLoopMode.UNKNOWN || loopMode == ForLoopMode.AMONG_HASHES)  // Cannot mix types

                            val hashesList = targetObj as List<Map<String, Any>>
                            for (each in hashesList) {
                                loopMode = ForLoopMode.AMONG_HASHES
                                forHashes.add(each)
                            }
                        } else
                        /* if (targetObj instanceof Map) */ {

                            val forTargetsMap = targetObj as Map<String, Any>

                            for (eachTargetName in forTargetsMap.keys) {
                                // A bit hacky
                                if (eachTargetName.startsWith("/")) {
                                    require(loopMode != ForLoopMode.AMONG_ELEMENTS)  // Cannot mix types
                                    loopMode = ForLoopMode.AMONG_URLS
                                    forUrls.add(eachTargetName)
                                } else {
                                    require(loopMode != ForLoopMode.AMONG_URLS)  // Cannot mix types
                                    loopMode = ForLoopMode.AMONG_ELEMENTS
                                    forTargets.add(finders.with(eachTargetName))
                                }
                            }
                        }
                    }

                    when (loopMode) {
                        ForLoopMode.AMONG_ELEMENTS -> {
                            scenarioState.setDefaultTarget(forTargets.removeAt(0))
                            savedCtxt = DriverContext(web.driver)
                        }
                        ForLoopMode.AMONG_URLS -> handleForUrl(forUrls.removeAt(0))
                        ForLoopMode.AMONG_HASHES -> processNextHash()
                        else -> throw RuntimeException("Unexpected state")
                    }

                    buildingForLoop = true
                    loopLen = 0
                    pc++
                    continue
                } else if (eachCmd.containsKey("next")) {

                    buildingForLoop = false

                    try {
                        when (loopMode) {
                            ForLoopMode.AMONG_ELEMENTS -> {
                                if (savedCtxt == null) {
                                    throw RuntimeException("Found 'next:' before 'for:'")
                                }

                                if (web.driver.currentUrl != savedCtxt.currentUrl) {
                                    applyContext(savedCtxt)  // Only if URL different
                                }

                                scenarioState.setDefaultTarget(forTargets.removeAt(0))  // Set 'this' for next run through
                            }
                            ForLoopMode.AMONG_URLS -> handleForUrl(forUrls.removeAt(0))
                            ForLoopMode.AMONG_HASHES -> processNextHash()
                            else -> throw RuntimeException("Unexpected state")
                        }

                        pc -= loopLen  // rewind...
                        continue
                    } catch (e: IndexOutOfBoundsException) {
                        pc++      // Advance PC
                        continue  // Finished
                    }

                }

                if (buildingForLoop) {
                    loopLen++
                }

                pc++

                inCmdRunner.invoke(eachCmd)
            }
        } finally {
            if (!finallyBlocksForScenario.isEmpty()) {

                lines.clear()  // Now, we've already skipped to end, so need to *reset* line numbers for finally() and pick up again...

                log.info("Perform finally() block(s)...")

                for (eachFinallyCmd in finallyBlocksForScenario) {

                    if (!externalHandlers.isRunningHandler) {
                        // Call @AcceptCommand so line numbers get picked-up for each finally() block
                        lifecycle.call(AcceptCommand::class.java, eachFinallyCmd)
                    }

                    try {
                        inCmdRunner.invoke(eachFinallyCmd)
                    } catch (e: Throwable) /* Mustn't propagate! */ {
                        log.error("Error in finally() block", e)
                    }

                }
            }
        }
    }

    private fun processNextHash() {
        val hash = forHashes.removeAt(0)
        for ((key, value) in hash) {
            varCtxt.store(key, varCtxt.resolve(value)!!)
        }
    }

    private fun handleForUrl(url: String) {
        web.goTo(url) { log.info("< Selenium load URL: $url >") }

        delayCmd.delayFor(scenarioDefaults.getElse("url", "thenWait", 0).toLong())
    }

    private fun applyContext(savedCtxt: DriverContext) {
        /* No, should do proper refresh. Was... if (!getDriver().getCurrentUrl().equals( savedCtxt.getCurrentUrl() )) */
        run {
            web.goTo(savedCtxt.currentUrl) { log.info("< Selenium restore URL: " + savedCtxt.currentUrl + " >") }

            delayCmd.delayFor(scenarioDefaults.getElse("url", "thenWait", 0).toLong())
        }
    }

    private class DriverContext(driver: WebDriver) {
        val currentUrl: String = driver.currentUrl
    }

    private enum class ForLoopMode {
        UNKNOWN, AMONG_ELEMENTS, AMONG_URLS, AMONG_HASHES
    }
}
