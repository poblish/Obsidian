package org.hiatusuk.obsidian.web.selenium.find

import com.codahale.metrics.MetricRegistry
import java.util.Optional

import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.hiatusuk.obsidian.web.selenium.find.filters.Filter
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.openqa.selenium.By
import org.openqa.selenium.WebElement

class FindByPartialTextRequest internal constructor(inWeb: WebState, exceptions: RuntimeExceptions, inMetrics: MetricRegistry, private val text: String) : FindRequest(inWeb, exceptions, inMetrics) {

    public override fun getAll(vararg filters: Filter): Collection<WebElement> {
        // FIXME Could this be slow? Do we need to support a.@My Value too?
        return FindRequest.filter(timedFindElements(web.driverOnValidPage, By.partialLinkText(text), metrics), *filters)
    }

    public override fun getOptionalFirst(vararg filters: Filter): Optional<WebElement> {
        return FindRequest.filter(timedFindElement(web.driverOnValidPage, By.partialLinkText(text), metrics), *filters)
    }

    override fun toString(): String {
        return "first element containing text '$text'"
    }
}
