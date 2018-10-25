package org.hiatusuk.obsidian.web.selenium.find

import com.codahale.metrics.MetricRegistry

import java.util.Arrays
import java.util.Optional

import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.hiatusuk.obsidian.web.selenium.find.filters.Filter
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.utils.StringUtils
import org.hiatusuk.obsidian.web.selenium.find.filters.FilterPredicate
import org.openqa.selenium.*
import org.openqa.selenium.remote.UnreachableBrowserException

abstract class FindRequest protected constructor(protected val web: WebState,
                                                 protected val exceptions: RuntimeExceptions,
                                                 protected val metrics: MetricRegistry) {

    internal fun timedFindElements(inDriver: WebDriver, by: By, inMetrics: MetricRegistry): List<WebElement> {
        inMetrics.timer("FindRequest.findElements(" + by.javaClass.simpleName + ")").time().use { return inDriver.findElements(by) }
    }

    internal fun timedFindElement(inDriver: WebDriver, by: By, inMetrics: MetricRegistry): WebElement {
        inMetrics.timer("FindRequest.findElement(" + by.javaClass.simpleName + ")").time().use { return inDriver.findElement(by) }
    }

    fun exists(vararg filters: Filter): Boolean {
        return try {
            optFirst(*filters).isPresent
        } catch (e: Throwable) /* Just in case this leaks out */ {
            false
        }

    }

    fun count(vararg filters: Filter): Int {
        return all(*filters).size
    }

    fun all(vararg filters: Filter): Collection<WebElement> {
        try {
            return getAll(*filters)
        } catch (e: UnsupportedCommandException) {
            if (e.message!!.contains("Job is not in progress")) {  // Seen when using Sauce Labs
                throw exceptions.runtime("Cannot perform lookup as remote job is no longer in progress")
            }
            throw e
        } catch (e: WebDriverException) {
            if (e.message!!.contains("job has already finished")) {  // Seen when using Sauce Labs
                throw exceptions.runtime(StringUtils.firstLine(e.message)!!)
            }
            throw e
        }
    }

    fun optFirst(vararg filters: Filter): Optional<WebElement> {
        try {
            return getOptionalFirst(*filters)
        } catch (e: org.openqa.selenium.NoSuchElementException) {
            return Optional.empty()
        } catch (e: UnsupportedCommandException) {
            if (e.message!!.contains("Job is not in progress")) {  // Seen when using Sauce Labs
                throw exceptions.runtime("Cannot perform lookup as remote job is no longer in progress")
            }
            throw e
        } catch (e: UnreachableBrowserException) {
            /* Less noisy */ throw exceptions.unreachableWebBrowser("Error communicating with the remote browser. It may have died.")
        } catch (e: WebDriverException) {
            if (e.message!!.contains("job has already finished")) {  // Seen when using Sauce Labs
                throw exceptions.runtime(StringUtils.firstLine(e.message)!!)
            }
            throw e
        }
    }

    fun first(vararg filters: Filter): WebElement {
        val fe = optFirst(*filters)
        if (fe.isPresent) {
            return fe.get()
        }

        if (filters.isNotEmpty()) {
            throw exceptions.noSuchWebElement("Could not find " + this + " with filters: " + Arrays.toString(filters))
        }

        throw exceptions.noSuchWebElement("Could not find " + this)
    }

    protected abstract fun getAll(vararg filters: Filter): Collection<WebElement>
    protected abstract fun getOptionalFirst(vararg filters: Filter): Optional<WebElement>

    companion object {

        fun filter(original: Collection<WebElement>, vararg filters: Filter): Collection<WebElement> {

            if (original.isEmpty() || filters.isEmpty()) {
                return original
            }

            // All filters are applied as post-filters. Up to the user to specify more efficient pre-filters
            // themselves, e.g. using CSS3 syntax rather than "with"

            var postFiltered = original
            for (selector in filters) {
                postFiltered = postFiltered.filter(FilterPredicate(selector))
            }

            return postFiltered
        }

        fun filter(original: WebElement, vararg filters: Filter): Optional<WebElement> {
            return if (filter(arrayListOf(original), *filters).isEmpty()) {
                Optional.empty()
            } else Optional.of(original)
        }
    }
}
