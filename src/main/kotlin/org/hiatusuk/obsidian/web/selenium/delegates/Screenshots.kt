package org.hiatusuk.obsidian.web.selenium.delegates

import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.events.AfterScenariosFailed
import org.hiatusuk.obsidian.run.events.OnSuppressedError
import org.hiatusuk.obsidian.run.state.LineNumbersState
import org.hiatusuk.obsidian.run.state.ScenarioState
import org.hiatusuk.obsidian.utils.StringUtils
import org.openqa.selenium.UnsupportedCommandException
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.WebElement
import org.openqa.selenium.remote.UnreachableBrowserException
import org.slf4j.Logger
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Named

@ScenarioScope
class Screenshots @Inject
constructor(@param:Named("outputDir") private val outputDir: File,
            private val web: WebState,
            private val scenarioState: ScenarioState,
            private val lineNumbers: LineNumbersState,
            private val log: Logger) {

    @OnSuppressedError
    fun onEachError(error: Throwable) {
        // Skip if not possible or v.unlikely to be a user error that requires a screenshot
        if (error is IOException || !web.hasRealBrowser) {
            return
        }

        outputWithSensibleName(Optional.empty(), "Error")
    }

    @AfterScenariosFailed
    fun onFinalError(error: Throwable) {
        // Skip if not possible or v.unlikely to be a user error that requires a screenshot
        if (error is IOException || !web.hasRealBrowser) {
            return
        }

        outputWithSensibleName(Optional.empty(), "FAIL")
    }

    fun outputWithSensibleName(selectedElem: Optional<WebElement>, inName: String) {
        val timeVal = Date().time.toString().substring(6)  // Strip off pointless prefix
        val ssPath = File(outputDir, (scenarioState.currentScenarioName ?: "???") + "_" +
                (if (lineNumbers.currentLine() >= 0) "Line#" + lineNumbers.currentLine() + "_" else "") +
                (if (inName.isEmpty()) "" else inName + "_") + timeVal + ".png").toString()
        try {
            if (web.takeScreenShot(selectedElem, ssPath)) {
                log.info("Screenshot saved to: $ssPath")
            }
        } catch (e: UnreachableBrowserException) {
            log.warn("Couldn't write screenshot as the browser wasn't reachable.")
        } catch (ee: UnsupportedCommandException) {
            log.warn("Couldn't write screenshot: either the browser doesn't support this, or it has already finished.")
        } catch (ee: WebDriverException) {
            log.warn("Couldn't write screenshot (\"{}\")", StringUtils.firstLine(ee.message))
        }
    }
}
