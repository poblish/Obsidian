package org.hiatusuk.obsidian.run.delegates

import com.codahale.metrics.MetricRegistry
import org.hiatusuk.obsidian.utils.TimeDifference
import org.slf4j.Logger
import javax.inject.Inject

open class DelayHandler @Inject
constructor(private val metrics: MetricRegistry,
            private val log: Logger) {

    open fun doDelay(msecs: Long) {
        metrics.timer("delays").time().use {
            log.debug("Waiting for {}...", TimeDifference.getFormattedTimeDiff(msecs))
            Thread.sleep(msecs)
        }
    }
}
