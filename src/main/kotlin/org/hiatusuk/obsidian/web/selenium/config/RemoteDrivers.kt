package org.hiatusuk.obsidian.web.selenium.config

import com.codahale.metrics.MetricRegistry
import com.google.common.annotations.VisibleForTesting
import org.hiatusuk.obsidian.config.ConfigUtils
import org.hiatusuk.obsidian.config.FeatureConfiguration
import org.hiatusuk.obsidian.context.ExposedMethod
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.LocalBrowserProperties
import org.hiatusuk.obsidian.run.RunProperties
import org.hiatusuk.obsidian.run.events.AfterBrowserCompletedScenario
import org.hiatusuk.obsidian.run.events.BeforeScenarioElement
import org.hiatusuk.obsidian.run.events.BeforeScriptExecution
import org.hiatusuk.obsidian.run.state.ScenarioState
import org.hiatusuk.obsidian.utils.StringUtils
import org.hiatusuk.obsidian.web.proxy.delegates.WebProxies
import org.hiatusuk.obsidian.web.selenium.driver.noop.NullWebDriver
import org.hiatusuk.obsidian.web.selenium.driver.profiles.cmd.ChromeOptionsConfig
import org.hiatusuk.obsidian.web.selenium.driver.profiles.delegates.ProfilesState
import org.hiatusuk.obsidian.web.selenium.exceptions.UnreachableWebBrowserException
import org.hiatusuk.obsidian.web.selenium.utils.WebDriverUtils
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.edge.EdgeDriver
import org.openqa.selenium.edge.EdgeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.ie.InternetExplorerDriver
import org.openqa.selenium.ie.InternetExplorerOptions
import org.openqa.selenium.opera.OperaDriver
import org.openqa.selenium.opera.OperaOptions
import org.openqa.selenium.phantomjs.PhantomJSDriver
import org.openqa.selenium.remote.*
import org.openqa.selenium.safari.SafariDriver
import org.openqa.selenium.safari.SafariOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject

@ScenarioScope
@FeatureConfiguration("browsers")
class RemoteDrivers @Inject
constructor(private val proxies: WebProxies,
            private val runProps: RunProperties,
            private val localBrowserProps: LocalBrowserProperties,
            private val profilesState: ProfilesState,
            private val scenarioState: ScenarioState,
            private val metrics: MetricRegistry,
            private val log: Logger) {

    private val driversSpecified = linkedSetOf<WebDriver>()
    private val sharedDriverInsts = LinkedHashMap<Capabilities, WebDriver>()

    // @FeatureConfiguration
    fun configure(inPayload: Map<String,Any>) {
        clear()  // Just in case...

        for ((key, value) in ConfigUtils.mapEntries(inPayload)) {
            when (value) {
                is List<*> -> {
                    // Multiple instances per browser type, each with a set of custom capabilities
                    val instEntries = value as List<Map<String, Any>>
                    for (eachInst in instEntries) {
                        obtainBrowser(key, eachInst)
                    }
                }
                is Map<*, *> -> // One instance, with custom capabilities
                    obtainBrowser(key, value as Map<String, Any>)
                else -> // One instance, no custom capabilities
                    obtainBrowser(key, EMPTY_MAP)
            }
        }
    }

    fun drivers(): Iterable<WebDriver> {
        return driversSpecified.toList()  // copy
    }

    @AfterBrowserCompletedScenario
    fun tryQuit(inDriver: WebDriver) {
        try {
            inDriver.manage().deleteAllCookies()  // FIXME Is this required? Here for compatibility purposes
        } catch (e: WebDriverException) {
            log.warn("Could not delete cookies (\"{}\")", StringUtils.firstLine(e.message))
        } catch (e: UnsupportedOperationException) {
            // Just ignore. Probably the NullWebDriver
        }
    }

    fun tryQuit() {
        try {
            for (eachDriver in sharedDriverInsts.values) {

                if (eachDriver !is NullWebDriver) {
                    log.info("*** Quitting shared {} session", eachDriver)
                }

                performDriverQuit(eachDriver)
            }
        } finally {
            sharedDriverInsts.clear()
        }
    }

    private fun performDriverQuit(inDriver: WebDriver) {
        try {
            inDriver.quit()
        } catch (e: UnreachableBrowserException) {
            /* Less noisy */ throw UnreachableWebBrowserException("Error communicating with the remote browser. It may have died.")
        } catch (e: UnsupportedCommandException) {  // Seen when using Sauce Labs
            throw RuntimeException(StringUtils.firstLine(e.message))
        }
    }

    fun removeReferences(inDriver: WebDriver) {
        // Create Map wrapper to prevent CMEs that have been seen
        for ((key, value) in LinkedHashMap(sharedDriverInsts)) {
            if (inDriver === value) {
                // Ignore 'reuseBrowserSessionsAcrossScenarioFiles' ...?
                log.info("*** Quitting shared {} session", inDriver)
                sharedDriverInsts.remove(key)
            }
        }
    }

    private fun obtainBrowser(inBrowserNameStr: String, customCapabilities: Map<String, Any>) {
        obtainBrowser(parseCapabilitiesString(inBrowserNameStr), inBrowserNameStr, customCapabilities)
    }

    // Need to "clone" rather than modify the input caps, in case it's a static ChromeOptions() for example
    private fun obtainBrowser(requestedCaps: Capabilities) {
        obtainBrowser(MutableCapabilities(requestedCaps), requestedCaps.browserName, emptyMap())
    }

    private fun obtainBrowser(requestedCaps: MutableCapabilities, inBrowserNameStr: String, customCapabilities: Map<String, Any>) {
        val caps = applyProxySettings(requestedCaps, inBrowserNameStr)

        if (!customCapabilities.isEmpty()) {
            log.debug("Setting custom capabilities {} for '{}'", customCapabilities, inBrowserNameStr)

            for ((key, value) in customCapabilities) {
                caps.setCapability(key, value)
            }
        }

        when (caps.browserName.toLowerCase()) {
            "internet explorer" -> obtainIeWebDriver(caps)
            "microsoftedge" -> obtainWebDriver(caps) { EdgeDriver(EdgeOptions().merge(caps)) }
            "chrome" -> obtainChromeWebDriver(caps)
            "safari" -> obtainSafariWebDriver(caps)
            "opera", "operablink" -> obtainOperaWebDriver(caps)
            "firefox" -> obtainFirefoxWebDriver(caps)
            "htmlunit" -> obtainHtmlUnitDriver(caps)
            "phantomjs" -> obtainPhantomJsDriver(caps)
            else -> throw RuntimeException("Unknown browser with: $caps")
        }
    }

    @VisibleForTesting
    private fun obtainFirefoxWebDriver(caps: Capabilities) {
        obtainWebDriver(caps) {
            val opts = FirefoxOptions(caps)

            if (profilesState.firefoxProfile != null) {
                opts.profile = profilesState.firefoxProfile
            }

            FirefoxDriver(opts)
        }
    }

    @VisibleForTesting
    private fun obtainChromeWebDriver(caps: Capabilities) {
        obtainWebDriver(caps) { ChromeDriver( localBrowserProps.usingChrome(profilesState.chromeOptions!!).merge(caps)) }
    }

    private fun obtainIeWebDriver(caps: Capabilities) {
        obtainWebDriver(caps) { InternetExplorerDriver( localBrowserProps.usingIE(InternetExplorerOptions()).merge(caps)) }
    }

    private fun obtainOperaWebDriver(caps: Capabilities) {
        obtainWebDriver(caps) { OperaDriver( localBrowserProps.usingOpera( OperaOptions() ).merge(caps)) }
    }

    private fun obtainSafariWebDriver(caps: Capabilities) {
        obtainWebDriver(caps) { SafariDriver(SafariOptions().merge(caps)) }
    }

    @VisibleForTesting
    private fun obtainHtmlUnitDriver(caps: Capabilities) {
        obtainWebDriver(caps) { HtmlUnitDriver(caps) }
    }

    @VisibleForTesting
    private fun obtainPhantomJsDriver(caps: Capabilities) {
        obtainWebDriver(caps) { PhantomJSDriver(caps) }
    }

    private fun obtainWebDriver(inRequirements: Capabilities, localInstanceSupplier: () -> WebDriver) {
        if (!sharedDriverInsts.containsKey(inRequirements)) {
            sharedDriverInsts[inRequirements] = obtainNewWebDriver(inRequirements, localInstanceSupplier)
        }
        driversSpecified.add(sharedDriverInsts[inRequirements]!!)
    }

    private fun obtainNewWebDriver(inRequirements: Capabilities, localInstanceSupplier: () -> WebDriver): WebDriver {
        if (inRequirements.getCapability("localOnly") != null ||
                !runProps.isUseSeleniumGrid ||
                !runProps.seleniumGridUrl.isPresent) {

            log.info("Creating new local {} instance", inRequirements.browserName)

            metrics.timer("RemoteDrivers.createLocal").time().use { return localInstanceSupplier() }
        }

        try {
            log.info("Requesting Grid instance via {}...", inRequirements)

            metrics.timer("RemoteDrivers.createRemote").time().use { return RemoteWebDriver(runProps.seleniumGridUrl.get(), inRequirements) }
        } catch (e: RuntimeException) {
            log.info("Creating new local {} instance due to remote error (\"{}\")", inRequirements.browserName, StringUtils.firstLine(e.message))

            metrics.timer("RemoteDrivers.createLocal").time().use { return localInstanceSupplier() }
        }

    }

    // Don't need to consider *all* browsers, as purpose is just to set the 'default' browser as we can
    @BeforeScriptExecution
    fun ensureBestBrowser() {
        if (driversSpecified.isEmpty()) {
            identifyBestBrowser()
        }
    }

    @BeforeScenarioElement
    fun clear() {
        driversSpecified.clear()

        THREAD_DRIVERS_STATE.set(driversSpecified)
    }

    // Don't need to consider *all* browsers, as purpose is just to set the 'default' browser as we can
    private fun identifyBestBrowser() {
        // Some users are relying on the built-in 'url: /' default and will be interacting with site - perhaps in a way HtmlUnit
        // can't handle - without a good way of us knowing (short of counting all clicks, types, and use of CSS in asserts...).
        // Otherwise the scan for 'url' commands would have been fine.
        // This workaround plays safer while hopefully allowing all 'back-end-only' (and non-site) scripts to use HtmlUnit/NOOP without
        // needing to be explicitly configured. It catches demo-type scripts that don't have a base URL but that do use url:, as well
        // as functional tests that 99.9% will have baseUrl, and which may or may not actually use url:
        if (scenarioState.requiresBrowsers()) {
            val defaults = runProps.defaultDesiredCapabilities
            if (defaults.isPresent) {
                obtainBrowser(defaults.get())
                return
            }

            obtainBrowser(ChromeOptionsConfig.vanillaOptions())
        } else {
            // We used to create HtmlUnitDriver, but quicker to create a genuine NOOP Driver
            val caps = DesiredCapabilities( mapOf(CapabilityType.BROWSER_NAME to "no-op") )

            obtainWebDriver(caps) { NullWebDriver(caps) }
        }
    }

    private fun applyProxySettings(inCaps: MutableCapabilities, inBrowserName: String): MutableCapabilities {
        if (proxies.getProxy() != null) {
            log.info("Using proxy {} for {}...", proxies.getProxy(), inBrowserName)
            inCaps.setCapability(CapabilityType.PROXY, proxies.getProxy())
        }
        return inCaps
    }

    companion object {

        private val EMPTY_MAP = emptyMap<String, Any>()

        fun parseCapabilitiesString(inStr: String): MutableCapabilities {

            val bvPattern = Pattern.compile("^((?:ie|chrome|edge|ff|firefox|htmlunit|ipad(?: air| retina)?|iPhone|opera|phantomjs|safari)(?:\\+metrics)?) ?(latest|(?:\\d+(?:\\.\\d*)?)(?:s| Plus)?|(?:\\.\\d+)(?:s| Plus)?)?", Pattern.CASE_INSENSITIVE)
            val bvMatcher = bvPattern.matcher(inStr)
            if (!bvMatcher.find()) {
                throw RuntimeException("Could not match '$inStr'")
            }

            val platPattern = Pattern.compile("on ((?:[A-Z]* ?)+) ?((?:\\d+(?:\\.\\d*)?)|(?:\\.\\d+))?", Pattern.CASE_INSENSITIVE)
            val platMatcher = platPattern.matcher(inStr)
            val gotPlat = platMatcher.find()
            val browserVers = bvMatcher.group(2)

            val platformName = if (gotPlat) platMatcher.group(1).trim().toLowerCase() else ""
            val platformVersion = if (gotPlat && platMatcher.groupCount() >= 2) platMatcher.group(2) ?: "" else ""

            val browserName = bvMatcher.group(1).toLowerCase()

            // System.out.println("*** Browser: " + browserName + ", version: " + browserVers + "; Platform: " + platformName + ", version: " + platformVersion);

            var dc = MutableCapabilities()

            when (browserName) {
                "ie" -> dc = DesiredCapabilities.internetExplorer()
                "edge" -> dc = DesiredCapabilities.edge()
                "chrome" -> dc = ChromeOptionsConfig.vanillaOptions()  // don't assume headless here
                "firefox" -> dc = DesiredCapabilities.firefox()
                "safari" -> dc = DesiredCapabilities.safari()
                "opera" -> dc = DesiredCapabilities.operaBlink()
                "htmlunit" -> dc = DesiredCapabilities.htmlUnit()
                "phantomjs" -> dc = DesiredCapabilities(BrowserType.PHANTOMJS, "", Platform.ANY)
                "ipad", "ipad air", "ipad retina" -> throw RuntimeException("iPad unsupported")
                "iphone" -> throw RuntimeException("iPhone unsupported")
            }// Doesn't work. Can't prevent PhantomJSDriverService logging.... dc.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, new String[]{"--webdriver-logfile=/tmp/phantomjsdriver.log"});

            dc.setCapability(CapabilityType.VERSION, browserVers)  // Pretty iffy, should FIXME

            when (platformName) {
                "win", "windows" -> when (platformVersion.toLowerCase()) {
                    "10", "10.0", "10.1" -> setPlatform(dc, Platform.WIN10)
                    "8.1" -> setPlatform(dc, Platform.WIN8_1)
                    "8", "8.0" -> setPlatform(dc, Platform.WIN8)
                    else -> setPlatform(dc, Platform.WINDOWS)
                }
                "mac" -> when (platformVersion.toLowerCase()) {
                    "10.6", "snow leopard" -> setPlatform(dc, Platform.SNOW_LEOPARD)
                    "10.7" -> {
                        LoggerFactory.getLogger(RemoteDrivers::class.java).warn("Unsupported Mac version (\"$platformVersion\"): reverting to default")
                        setPlatform(dc, Platform.MAC)
                    }
                    "10.8" -> setPlatform(dc, Platform.MOUNTAIN_LION)
                    "10.9", "mavericks" -> setPlatform(dc, Platform.MAVERICKS)
                    "10.10", "yosemite" -> setPlatform(dc, Platform.YOSEMITE)
                    "10.11", "el capitan" -> setPlatform(dc, Platform.EL_CAPITAN)
                    "10.12", "sierra" -> setPlatform(dc, Platform.SIERRA)
                    "10.13", "high sierra" -> setPlatform(dc, Platform.HIGH_SIERRA)
                    else -> setPlatform(dc, Platform.MAC)
                }

                "snow leopard" -> setPlatform(dc, Platform.SNOW_LEOPARD)
                "mountain lion" -> setPlatform(dc, Platform.MOUNTAIN_LION)
                "mavericks" -> setPlatform(dc, Platform.MAVERICKS)
                "yosemite" -> setPlatform(dc, Platform.YOSEMITE)
                "el capitan" -> setPlatform(dc, Platform.EL_CAPITAN)
                "sierra" -> setPlatform(dc, Platform.SIERRA)
                "high sierra" -> setPlatform(dc, Platform.HIGH_SIERRA)
                "ipad", "iphone" -> setPlatform(dc, Platform.MAC)  // Ugh!
            }

            return dc
        }

        private fun setPlatform(caps: MutableCapabilities, value: Platform) {
            caps.setCapability(CapabilityType.PLATFORM, value)
        }

        // So the static "driverNames" EL method can get current scenario drivers
        private val THREAD_DRIVERS_STATE = object : ThreadLocal<LinkedHashSet<WebDriver>>() {}

        @Suppress("unused")  // Use by EL
        val driverNameInstances: String
            @JvmStatic
            @ExposedMethod(namespace = "web", name = "driverNames")
            get() = THREAD_DRIVERS_STATE.get().map { WebDriverUtils.getDriverName(it) }.sortedBy { it }.joinToString(separator = ",")
    }
}
