package org.hiatusuk.obsidian.web.selenium.delegates

import com.codahale.metrics.MetricRegistry
import com.google.common.annotations.VisibleForTesting
import org.apache.commons.io.FileUtils
import org.hiatusuk.obsidian.context.ExposedMethod
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.Lifecycle
import org.hiatusuk.obsidian.run.events.AfterGoToUrl
import org.hiatusuk.obsidian.run.events.AfterScenario
import org.hiatusuk.obsidian.run.events.ApplicationQuit
import org.hiatusuk.obsidian.run.events.BeforeGoToUrl
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.run.state.ScenarioState
import org.hiatusuk.obsidian.utils.StringUtils
import org.hiatusuk.obsidian.utils.StringUtils.checkSet
import org.hiatusuk.obsidian.utils.StringUtils.replace
import org.hiatusuk.obsidian.web.selenium.config.RemoteDrivers
import org.hiatusuk.obsidian.web.selenium.driver.noop.NullWebDriver
import org.hiatusuk.obsidian.web.selenium.utils.WebDriverUtils
import org.openqa.selenium.*
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.remote.UnreachableBrowserException
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.*
import javax.inject.Inject

@ScenarioScope
open class WebState @Inject
constructor(private val drivers: RemoteDrivers?,
            private val scenarioState: ScenarioState?,
            private val lifecycle: Lifecycle?,
            private val exceptions: RuntimeExceptions?,
            private val metrics: MetricRegistry) {

    private var optDriver = Optional.empty<WebDriver>()
    private var baseUrl: String? = null
    private var initialised: Boolean = false

    val driver: WebDriver
        get() {
            if (!optDriver.isPresent) {
                throw WebDriverException("No current WebDriver in use")
            }
            return optDriver.get()
        }

    val hasRealBrowser: Boolean
        get() {
            return if (!optDriver.isPresent) {
                false
            }
            else optDriver.get() !is NullWebDriver
        }

    // We're being called by something that wants to *do* or *find* something on a web page, but we may never
    // have explicitly been told to visit one. So we go to front page and hope that's what the user intended...
    // ... provided a Base Url has been set. If not, there's nothing we can do. Let caller error if necessary.
    // Safer to just go to baseUrl rather than /, which would break local file paths, etc.
    val driverOnValidPage: WebDriver
        get() {
            if (!optDriver.isPresent) {
                throw WebDriverException("No current WebDriver in use")
            }
            if (scenarioState!!.lastUrlVisited.isEmpty() && !scenarioState.baseUrl!!.isEmpty()) {
                goTo(scenarioState.baseUrl) { LOG.info("< Selenium: do implicit load of / >") }
            }

            return optDriver.get()
        }

    fun init(driver: WebDriver) {
        initWithUrl(driver, scenarioState!!.baseUrl)
    }

    @VisibleForTesting
    fun initWithUrl(driver: WebDriver, inBaseUrl: String?) {
        baseUrl = inBaseUrl ?: ""
        optDriver = Optional.of(driver)
        initialised = true

        // Add browser name to log entries
        var browserName = WebDriverUtils.getDriverName(driver)

        EL_BROWSER_NAME.set(browserName)  // EL doesn't seem to be able to call non-static methods, so forced to expose this way

        if (/* Grid one */ driver.javaClass.name == RemoteWebDriver::class.java.name) {
            browserName = "R:" + browserName!!
        }
        MDC.put("driver", browserName!! + ",")
    }

    fun goTo(inUrl: String) {
        goTo(inUrl, NOOP_LOG_HANDLER)
    }

    open fun goTo(inUrl: String?, inUrlLogger: () -> Unit) {
        if (scenarioState!!.isScenariosFailed && (inUrl == null)) {  // Don't risk throwing new exception if we're already bailing-out and stack is unwinding
            return
        }

        require(!scenarioState.baseUrl!!.isEmpty() || !inUrl!!.startsWith("/")) {"Can't visit relative URL without setting a default 'siteUrl'"}

        // We *cannot* URL-encode the entire thing, because users expect to pass URL fragments verbatim, even including pre-encoded bits. Yes.
        // However, some Gherkin users may well pass through 'John Smith' or '"big dogs"' as search URL fragments and expect not to get 'URISyntaxException: Illegal character in query...'
        // Below, we do the bare minimum required to get existing integration tests to work.

        val fixedUrl = replace(inUrl!!.replace(' ', '+'), "\"", "%22")
        scenarioState.lastUrlVisited = checkSet(fixedUrl)

        metrics.timer("WebState.goTo(url) (full)").time().use {

            runBeforeUrlHandlers(inUrl)

            try {
                performGoTo(fixedUrl, inUrlLogger)
            } catch (e: UnreachableBrowserException) {
                /* Less noisy */ throw exceptions!!.unreachableWebBrowser("Error communicating with the remote browser. It may have died.")
            } catch (e: UnsupportedCommandException) {  // Seen when using Sauce Labs
                throw exceptions!!.runtime(StringUtils.firstLine(e.message)!!)
            }

            runAfterUrlHandlers(inUrl)
        }
    }

    // Copied from Fluentlenium
    private fun performGoTo(url: String?, inUrlLogger: () -> Unit) {
        var ourUrl: String? = url ?: throw IllegalArgumentException("Url is mandatory")
        if (!baseUrl!!.isEmpty()) {
            val uri = URI.create(ourUrl!!)
            if (!uri.isAbsolute) {
                ourUrl = baseUrl!! + ourUrl
            }
        }

        inUrlLogger()

        try {
            metrics.timer("WebState.goTo(url) (get-only)").time().use { driver.get(ourUrl) }
        }
        catch (e : UnsupportedOperationException) {  // NullWebDriver getting called. Ensure we get a line number
            throw exceptions!!.unsupportedOperation()
        }
    }

    fun executeScript(script: String, vararg args: Any): Any {
        return (driver as JavascriptExecutor).executeScript(script, *args)
    }

    fun switchTo() {
        driver.switchTo().defaultContent()
    }

    fun switchTo(element: WebElement) {
        if (initialised) {
            // More reliable than the Fluent version
            driver.switchTo().frame(element)
        }
    }

    fun takeScreenShot(fileName: String) : Boolean {
        return takeScreenShot(Optional.empty(), fileName)
    }

    fun takeScreenShot(selectedElem: Optional<WebElement>, fileName: String) : Boolean {
        if (initialised) {
            metrics.timer("WebState.takeScreenShot").time().use { performTakeScreenShot(selectedElem, fileName) }
        } else {
            LOG.debug("Cannot take screenshot as no browser is in use")
        }
        return initialised
    }

    // Copied from Fluentlenium
    private fun performTakeScreenShot(selectedElem: Optional<WebElement>, fileName: String) {
        if (driver !is TakesScreenshot) {
            throw WebDriverException("Current browser doesn't allow taking screenshot.")
        }

        val target = if (selectedElem.isPresent) selectedElem.get() else driver as TakesScreenshot
        try {
            val scrFile = target.getScreenshotAs(OutputType.FILE)
            FileUtils.copyFile(scrFile, File(fileName))
        } catch (e: IOException) {
            e.printStackTrace()
            throw RuntimeException("error when taking the snapshot", e)
        }

    }

    //    public Keyboard getKeyboard()  {
    //        return ((HasInputDevices) getDriver()).getKeyboard();
    //    }

    private fun runBeforeUrlHandlers(inUrl: String) {
        lifecycle!!.call(BeforeGoToUrl::class.java, inUrl)
    }

    private fun runAfterUrlHandlers(inUrl: String) {
        lifecycle!!.call(AfterGoToUrl::class.java, inUrl)
    }

    // @AfterScenario is called *before* @AfterScenariosFailed, where Screenshots is bound, so we can't clear
    // 'currentDriver' as that'll cause screenshots to fail with 'No current WebDriver' etc.
    @AfterScenario
    fun clearRefs() {
        EL_BROWSER_NAME.set(null)
        MDC.remove("driver")
    }

    @ApplicationQuit
    fun quit() {
        // If the driver instance is going away, ensure we remove it from our internal list
        val currDriver = optDriver.orElse(null)  // NB. 'currentDriver' is 'API' level, not internal level
        if (currDriver != null) {
            drivers!!.removeReferences(currDriver)  // Ugh!
            try {
                currDriver.quit()
            } catch (e: UnreachableBrowserException) {
                LOG.error("Ignore UnreachableBrowserException when quitting browser")  // Ignore *noisy* error
            } catch (e: WebDriverException) {
                LOG.error("Ignore error when quitting browser: $e")
            }

        }
    }

    companion object {

        private val EL_BROWSER_NAME = object : ThreadLocal<String>() {}

        private val LOG = LoggerFactory.getLogger("Web")

        private val NOOP_LOG_HANDLER = { }

        @Suppress("unused")
        @JvmStatic
        val browserName: String
            @ExposedMethod(namespace = "web")
            get() = EL_BROWSER_NAME.get() ?: "unknown"
    }
}