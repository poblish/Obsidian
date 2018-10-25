package org.hiatusuk.obsidian.run.delegates

import com.codahale.metrics.ConsoleReporter
import com.codahale.metrics.MetricRegistry
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.RunProperties
import org.hiatusuk.obsidian.run.events.OutputFinalReports
import org.slf4j.LoggerFactory
import java.io.*
import java.util.*
import javax.inject.Inject
import javax.inject.Named

@ScenarioScope
class MetricsLogger @Inject
internal constructor(private val metrics: MetricRegistry,
                     @param:Named("outputDir") private val outputDir: File,
                     private val runProps: RunProperties) {

    @OutputFinalReports
    fun outputMetrics() {
        if (!runProps.isLogMetrics) {
            return
        }

        val timeVal = Date().time.toString().substring(6)  // Strip off pointless prefix

        val outputFile = File(outputDir, "Metrics_$timeVal.txt")

        LOG.info("Writing metrics to {}...", outputFile)

        PrintWriter(FileWriter(outputFile)).use {

            // See: http://ediweissmann.com/blog/2013/03/10/yammer-metrics-and-playframework/
            val baos = ByteArrayOutputStream()
            PrintStream(baos).use { ps ->
                ConsoleReporter.forRegistry(metrics).outputTo(ps).build().report()
                ps.flush()
            }

            it.println("MetricRegistry contents:" + System.lineSeparator() + System.lineSeparator() + String(baos.toByteArray()))
        }
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(MetricsLogger::class.java)
    }
}
