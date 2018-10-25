package org.hiatusuk.obsidian.cases.delegates

import org.hiatusuk.obsidian.cases.Case
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.events.AfterScenariosFailed
import org.hiatusuk.obsidian.run.events.AfterScenariosPassed
import org.hiatusuk.obsidian.run.events.BeforeScriptExecution
import org.hiatusuk.obsidian.run.reports.ProjectReportsConfig
import org.hiatusuk.obsidian.run.reports.ReportingXmlState
import org.slf4j.LoggerFactory
import javax.inject.Inject

@ScenarioScope
class CasesState @Inject
constructor(private val reportingXmlState: ReportingXmlState,
            private val projectReports: ProjectReportsConfig) {

    private val currentCases = arrayListOf<Case>()
    private val provisionalPasses = arrayListOf<Case>()

    val description: String
        get() = currentCases.toString()

    @BeforeScriptExecution  // Must complete/clear down before moving to next scenario
    @AfterScenariosPassed  // ... and always at the end of the run
    fun passCurrentCases() {
        currentCases.addAll(provisionalPasses)
        provisionalPasses.clear()

        if (!currentCases.isEmpty()) {
            LOG.info("Passing: {}", currentCases)

            for (eachCase in /* Prevent CME */ ArrayList(currentCases)) {
                currentCases.remove(eachCase)  // Remove now, just in case handler is broken, and case hangs around forever
                reportingXmlState.storeResult(eachCase, "Y")
            }
            currentCases.clear()  // Just in case
        }

        projectReports.produceReport()
    }

    @AfterScenariosFailed
    fun failCurrentCases(error: Throwable?) {
        currentCases.addAll(provisionalPasses)
        provisionalPasses.clear()

        if (!currentCases.isEmpty()) {
            LOG.info("FAILING: {}", currentCases)

            for (eachCase in /* Prevent CME */ ArrayList(currentCases)) {
                currentCases.remove(eachCase)  // Remove now, just in case handler is broken, and case hangs around forever
                reportingXmlState.storeResult(eachCase, "N")
            }
            currentCases.clear()  // Just in case
        }

        projectReports.produceReport()
    }

    fun provisionallyPassCurrentCases() {
        if (!currentCases.isEmpty()) {
            LOG.info("Provisionally passing: {}", currentCases)
            provisionalPasses.addAll(currentCases)
            currentCases.clear()
        }
    }

    fun add(inCase: Case) {
        currentCases.add(inCase)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger("Cases")
    }
}
