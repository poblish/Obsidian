package org.hiatusuk.obsidian.javascript.jasmine.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.javascript.jasmine.delegates.JasmineRunStatus
import org.hiatusuk.obsidian.utils.TimeDifference
import org.hiatusuk.obsidian.web.selenium.config.RequiresBrowser
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.slf4j.Logger
import java.io.File
import java.util.regex.Pattern
import javax.inject.Inject

@RequiresBrowser
@Command("jasmine:run")
@ScenarioScope
class RunJasmineSpecCmd @Inject
constructor(private val finders: ElementFinders,
            private val web: WebState,
            private val jasmineStatus: JasmineRunStatus,
            private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {

        val url = File(inCmd.string).toURI()

        web.goTo(url.toString()) { log.info("< Selenium load Jasmine spec runner: $url >") }

        val timeout = 10000 // FIXME Hardcoding
        val endTime = System.currentTimeMillis() + timeout

        val passedReq = finders.with("span.passed")
        val failedReq = finders.with("span.failed")

        while (/* Wait for at least one to arrive */ !passedReq.exists() && !failedReq.exists()) {
            log.info("< Waiting up to {} for Jasmine run to complete >", TimeDifference.getFormattedTimeDiff(timeout.toLong()))

            if (System.currentTimeMillis() < endTime) {
                // Ignore, give another chance!
                Thread.sleep(334)
            } else {
                throw RuntimeException("Command timed out")
            }
        }

        jasmineStatus.runCompleted()

        val passElem = passedReq.optFirst()

        if (passElem.isPresent) {  // *All* passed
            val m = PASSES_PATTERN.matcher(passElem.get().text)
            m.find()
            jasmineStatus.passes = Integer.parseInt(m.group(1))
            jasmineStatus.fails = 0
        } else { // *Some* failed
            val m = FAILS_PATTERN.matcher(failedReq.first().text)
            m.find()

            val total = Integer.parseInt(m.group(1))
            val fails = Integer.parseInt(m.group(2))
            jasmineStatus.passes = total - fails
            jasmineStatus.fails = fails
        }
    }

    companion object {

        private val PASSES_PATTERN = Pattern.compile("([0-9]+) specs?")
        private val FAILS_PATTERN = Pattern.compile("([0-9]+) specs?, ([0-9]+) failures?")
    }
}
