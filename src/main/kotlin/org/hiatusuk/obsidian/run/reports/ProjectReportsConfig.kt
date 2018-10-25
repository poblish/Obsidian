package org.hiatusuk.obsidian.run.reports

import com.google.common.base.Strings
import org.hiatusuk.obsidian.config.FeatureConfiguration
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.events.BeforeScenario
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject

@FeatureConfiguration("Project Reports")
@ScenarioScope
class ProjectReportsConfig @Inject
constructor(private val reportingXmlState: ReportingXmlState) {

    private var testCaseDataLocation = Optional.empty<String>()
    private var generatedReportLocation = Optional.empty<String>()

    fun configure(inPayload: Map<String,Any>) {
        this.testCaseDataLocation = Optional.ofNullable( Strings.emptyToNull( inPayload["testCaseDataLocation"] as String))
        this.generatedReportLocation = Optional.ofNullable( Strings.emptyToNull( inPayload["generatedReportLocation"] as String))
    }

    @BeforeScenario  // Iffy, prob shouldn't have events within configs...
    @Throws(IOException::class)
    fun reloadXml() {
        if (testCaseDataLocation.isPresent) {
            reportingXmlState.load(File(testCaseDataLocation.get()))
        }
    }

    fun produceReport() {
        generatedReportLocation.ifPresent { reportingXmlState.outputReport(File(it)) }
    }
}
