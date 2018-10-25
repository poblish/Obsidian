package org.hiatusuk.obsidian.web.selenium.cmd

import org.hiatusuk.obsidian.asserts.ValidationException
import org.hiatusuk.obsidian.cmd.Delay
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.cmd.api.Validate
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.delegates.ScenarioDefaultsContext
import org.hiatusuk.obsidian.run.state.ScenarioState
import org.hiatusuk.obsidian.web.selenium.config.RequiresBrowser
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.hiatusuk.obsidian.web.utils.UrlUtils
import org.slf4j.LoggerFactory
import javax.inject.Inject

@ScenarioScope
@RequiresBrowser
@Command("url")
class GoToUrl @Inject
constructor(private val web: WebState,
            private val delayCmd: Delay,
            private val scenarioDefaults: ScenarioDefaultsContext,
            private val scenarioState: ScenarioState) : CommandIF {

    @Validate
    fun validate(inCmd: CommandSpec) {
        if (getURL(inCmd).isEmpty()) {
            throw ValidationException("Blank URL specified")
        }
    }

    override fun run(inCmd: CommandSpec) {
        run(inCmd, 0)
    }

    fun run(inCmd: CommandSpec, defaultUrlLoadWait: Int) {
        val url = getURL(inCmd)

        if (scenarioState.lastCommand == inCmd) {
            LOG.warn("Ignoring duplicated URL request: {}", url)
            return
        }

        web.goTo(url) { LOG.info("< Selenium load URL: $url >") }

        delayCmd.delayFor(inCmd.optInteger("thenWait").orElse(scenarioDefaults.getElse("url", "thenWait", defaultUrlLoadWait)).toLong())
    }

    private fun getURL(inCmd: CommandSpec): String {
        if (inCmd.string.startsWith("^")) {
            if (scenarioState.hasBaseUrl()) {
                val suffix = if (inCmd.string.isEmpty()) "" else inCmd.string.substring(1)
                val stripDoubleSlashes = (scenarioState.baseUrl!!.endsWith("/") && suffix.startsWith("/"))

                return scenarioState.baseUrl!! + (if (stripDoubleSlashes) suffix.substring(1) else suffix)
            }
            else {
                throw RuntimeException("Cannot go to baseUrl, as one hasn't been set")
            }
        }
        return UrlUtils.expandAndCorrectUrl(inCmd.string, scenarioState.lastUrlVisited)
    }

    companion object {

        private val LOG = LoggerFactory.getLogger("Web")
    }
}