package org.hiatusuk.obsidian.run

import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import org.hiatusuk.obsidian.di.ScenarioScope
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.ie.InternetExplorerOptions
import org.openqa.selenium.opera.OperaOptions
import org.slf4j.Logger
import java.io.FileInputStream
import java.io.FileWriter
import java.util.*
import javax.inject.Inject

@ScenarioScope
class LocalBrowserProperties @Inject
constructor(private val props: RunProperties, val log: Logger) {

    // Set "webdriver.chrome.driver" and optional "webdriver.chrome.logfile" properties to pass through to Chrome
    fun usingChrome(opts: ChromeOptions) : ChromeOptions {
        val configuredPath = props.konfig[CHROME_PATH_KEY]
        System.setProperty(CHROME_DRIVER_PATH, configuredPath)

        var logOutput = "Using $CHROME_DRIVER_PATH = $configuredPath"

        // Log path set?
        val logsPath = props.konfig.getOrNull(CHROME_LOGS_PATH_KEY)
        if (logsPath != null) {
            System.setProperty(CHROME_LOGS_PATH, logsPath)
            logOutput += ", $CHROME_LOGS_PATH = $logsPath"
        }

        log.debug(logOutput)

        return opts
    }

    fun usingOpera(opts: OperaOptions) : OperaOptions {
        val configuredPath = props.konfig[OPERA_PATH_KEY]
        System.setProperty(OPERA_DRIVER_PATH, configuredPath)
        log.debug("Using {} = {}", OPERA_DRIVER_PATH, configuredPath)
        return opts
    }

    fun usingIE(opts: InternetExplorerOptions) : InternetExplorerOptions {
        val configuredPath = props.konfig[IE_PATH_KEY]
        System.setProperty(IE_DRIVER_PATH, configuredPath)
        log.debug("Using {} = {}", IE_DRIVER_PATH, configuredPath)
        return opts
    }

    fun currentChromeDriverPath() : String {
        return props.konfig.getOrElse(CHROME_PATH_KEY, "")
    }

    fun currentOperaDriverPath() : String {
        return props.konfig.getOrElse(OPERA_PATH_KEY, "")
    }

    fun currentIeDriverPath() : String {
        return props.konfig.getOrElse(IE_PATH_KEY, "")
    }

    // Write value to config file *and* update System property
    fun updateConfiguration(propertyName: String, propertyValue: String?) {
        val obsidianProps = object : Properties() {
            @Synchronized override fun keys(): Enumeration<Any> {
                // Ensure at least some kind of consistency when rewriting properties!
                return Collections.enumeration(TreeSet<Any>(super.keys))
            }
        }

        if (props.obsidianPropertiesFile.exists()) {
            FileInputStream(props.obsidianPropertiesFile).use { obsidianProps.load(it) }
        }

        obsidianProps.setProperty(propertyName, propertyValue)
        FileWriter(props.obsidianPropertiesFile).use { obsidianProps.store(it, "Obsidian properties") }

        System.setProperty(propertyName, propertyValue)
    }

    companion object {
        const val CHROME_DRIVER_PATH = "webdriver.chrome.driver"
        private val CHROME_PATH_KEY = Key(CHROME_DRIVER_PATH, stringType)

        const val CHROME_LOGS_PATH = "webdriver.chrome.logfile"
        private val CHROME_LOGS_PATH_KEY = Key(CHROME_LOGS_PATH, stringType)

        const val OPERA_DRIVER_PATH = "webdriver.opera.driver"
        private val OPERA_PATH_KEY = Key(OPERA_DRIVER_PATH, stringType)

        const val IE_DRIVER_PATH = "webdriver.ie.driver"
        private val IE_PATH_KEY = Key(IE_DRIVER_PATH, stringType)
    }
}
