package org.hiatusuk.obsidian.run.exceptions

import com.google.common.annotations.VisibleForTesting
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.state.LineNumbersState
import org.hiatusuk.obsidian.web.selenium.exceptions.InvalidSelectorException
import org.hiatusuk.obsidian.web.selenium.exceptions.NoSuchWebElementException
import org.hiatusuk.obsidian.web.selenium.exceptions.UnreachableWebBrowserException
import org.hiatusuk.obsidian.web.selenium.exceptions.WebElementNotVisibleException
import org.hiatusuk.obsidian.web.selenium.find.InvalidCssException
import javax.inject.Inject

@ScenarioScope
class RuntimeExceptions @VisibleForTesting
@Inject
constructor(private val lineNumbers: LineNumbersState) {

    fun runtime(inMessage: String): RuntimeException {
        return RuntimeException(lineNumbers.status() + inMessage)
    }

    fun runtime(e: Exception): RuntimeException {
        return RuntimeException(lineNumbers.status(), e)
    }

    fun noSuchWebElement(inMessage: String): NoSuchWebElementException {
        return NoSuchWebElementException(lineNumbers.status() + inMessage)
    }

    fun webElementNotVisible(inMessage: String): WebElementNotVisibleException {
        return WebElementNotVisibleException(lineNumbers.status() + inMessage)
    }

    fun unreachableWebBrowser(inMessage: String): UnreachableWebBrowserException {
        return UnreachableWebBrowserException(lineNumbers.status() + inMessage)
    }

    fun invalidSelector(inMessage: String): InvalidSelectorException {
        return InvalidSelectorException(lineNumbers.status() + inMessage)
    }

    fun invalidCss(inMessage: String): InvalidCssException {
        return InvalidCssException(lineNumbers.status() + inMessage)
    }

    fun illegalArgument(inMessage: String): IllegalArgumentException {
        return IllegalArgumentException(lineNumbers.status() + inMessage)
    }

    fun unsupportedOperation(): UnsupportedOperationException {
        return UnsupportedOperationException(lineNumbers.status() + "This is an error, please report it.")
    }

    fun nullPointer(inMessage: String): NullPointerException {
        return NullPointerException(lineNumbers.status() + inMessage)
    }
}