package org.hiatusuk.obsidian.web.selenium.find

import com.codahale.metrics.MetricRegistry
import java.util.Optional

import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.hiatusuk.obsidian.web.selenium.find.filters.Filter
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.openqa.selenium.By
import org.openqa.selenium.WebElement

class FindByNameRequest internal constructor(inWeb: WebState, exceptions: RuntimeExceptions, inMetrics: MetricRegistry, private val name: String) : FindRequest(inWeb, exceptions, inMetrics) {

    public override fun getAll(vararg filters: Filter): Collection<WebElement> {
        // FIXME Could this be slow? Do we need to support a.~My Name too?
        // Far too slow, so reimplement Fluentlenium code with WebDriver API below: return inFluent.find("*", withName(text));
        return FindRequest.filter(timedFindElements(web.driverOnValidPage, By.name(name), metrics), *filters)
    }

    public override fun getOptionalFirst(vararg filters: Filter): Optional<WebElement> {
        return FindRequest.filter(timedFindElement(web.driverOnValidPage, By.name(name), metrics), *filters)
    }

    override fun toString(): String {
        return "first element named '$name'"
    }
}
