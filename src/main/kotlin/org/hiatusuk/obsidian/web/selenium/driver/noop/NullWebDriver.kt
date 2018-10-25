package org.hiatusuk.obsidian.web.selenium.driver.noop

import org.openqa.selenium.*
import org.openqa.selenium.logging.Logs
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.RemoteWebDriver

class NullWebDriver(inCaps: DesiredCapabilities) : RemoteWebDriver() {

    private val caps: Capabilities

    init {
        caps = requireNotNull(inCaps)
    }

    override fun getCapabilities(): Capabilities {
        return caps
    }

    override fun get(url: String) {
        throw UnsupportedOperationException()
    }

    override fun getCurrentUrl(): String? {
        return null
    }

    override fun getTitle(): String {
        throw UnsupportedOperationException()
    }

    override fun findElements(by: By): List<WebElement> {
        throw UnsupportedOperationException("This Driver does not allow actual element lookups (\"$by\"), and should never have been asked to do so.")
    }

    override fun findElement(by: By): WebElement {
        throw UnsupportedOperationException("This Driver does not allow actual element lookups (\"$by\"), and should never have been asked to do so.")
    }

    override fun getPageSource(): String {
        throw UnsupportedOperationException()
    }

    override fun close() {
        throw UnsupportedOperationException()
    }

    override fun quit() {
        // NOOP
    }

    override fun getWindowHandles(): Set<String> {
        throw UnsupportedOperationException()
    }

    override fun getWindowHandle(): String {
        throw UnsupportedOperationException()
    }

    override fun switchTo(): WebDriver.TargetLocator {
        throw UnsupportedOperationException()
    }

    override fun navigate(): WebDriver.Navigation {
        throw UnsupportedOperationException()
    }

    override fun toString(): String {
        return "NullWebDriver"
    }

    override fun manage(): WebDriver.Options {
        return OPTS
    }

    private class NullOptions : WebDriver.Options {

        override fun addCookie(cookie: Cookie) {
            throw UnsupportedOperationException()
        }

        override fun deleteCookieNamed(name: String) {
            throw UnsupportedOperationException()
        }

        override fun deleteCookie(cookie: Cookie) {
            throw UnsupportedOperationException()
        }

        override fun deleteAllCookies() {
            throw UnsupportedOperationException()
        }

        override fun getCookies(): Set<Cookie> {
            throw UnsupportedOperationException()
        }

        override fun getCookieNamed(name: String): Cookie {
            throw UnsupportedOperationException()
        }

        override fun timeouts(): WebDriver.Timeouts {
            throw UnsupportedOperationException()
        }

        override fun ime(): WebDriver.ImeHandler {
            throw UnsupportedOperationException()
        }

        override fun window(): WebDriver.Window {
            throw UnsupportedOperationException()
        }

        override fun logs(): Logs {
            throw UnsupportedOperationException()
        }
    }

    companion object {

        private val OPTS = NullOptions()
    }
}