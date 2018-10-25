package org.hiatusuk.obsidian.web.selenium.find

import com.codahale.metrics.MetricRegistry
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.utils.StringUtils
import org.hiatusuk.obsidian.utils.XPathUtils
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import javax.inject.Inject

@ScenarioScope
class ElementFinders @Inject
constructor(private val web: WebState,
            private val exceptions: RuntimeExceptions,
            private val metrics: MetricRegistry) {

    fun with(inSemSelector: String): FindRequest {
        return withChecks(inSemSelector)
    }

    private fun withChecks(inSelector: String): FindRequest {

        val fixedSelector = StringUtils.stripQuotes(inSelector)

        if (fixedSelector.startsWith("@")) {
            // FIXME Could this be slow? Do we need to support a.@My Value too?

            return if (fixedSelector.startsWith("@*=")) {
                FindByPartialTextRequest(web, exceptions, metrics, fixedSelector.substring(3))
            } else FindByTextRequest(web, exceptions, metrics, fixedSelector.substring(1))

        }

        if (fixedSelector.startsWith("~")) {
            return FindByNameRequest(web, exceptions, metrics, fixedSelector.substring(1))
        }

        return if (XPathUtils.isXPath(fixedSelector)) {
            FindByXPathRequest(web, exceptions, metrics, XPathUtils.unwrapXPath(fixedSelector))
        } else FindByCssPathRequest(web, exceptions, metrics, fixedSelector)
    }
}
