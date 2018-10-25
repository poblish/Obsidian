package org.hiatusuk.obsidian.cmd

import org.hiatusuk.obsidian.asserts.cmd.AssertCmd
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.cmd.api.Validate
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.delegates.ScenarioDefaultsContext
import org.hiatusuk.obsidian.utils.TerminalColours
import org.hiatusuk.obsidian.utils.TimeDifference
import org.hiatusuk.obsidian.web.selenium.find.InvalidCssException
import org.slf4j.Logger
import java.io.IOException
import javax.inject.Inject

@ScenarioScope
@Command("waitFor")
class WaitForCmd @Inject
constructor(// Allow overrides
        private val assertCmd: AssertCmd,
        private val scenarioDefaults: ScenarioDefaultsContext,
        private val log: Logger) : CommandIF {

    @Validate
    fun validate(inCmd: CommandSpec) {
        AssertCmd.validatePayload(inCmd)
    }

    @Throws(IOException::class)
    override fun run(inCmd: CommandSpec) {
        val timeout = getTimeout(inCmd)
        val endTime = System.currentTimeMillis() + timeout
        var count = 0

        while (true) {
            try {
                if (count++ % 3 == 0) {  // Show at t = 0, 3, 6, ... to be less noisy, i.e. about once per second
                    log.info("< Waiting up to {} for '{}{}{}' to be true >", TimeDifference.getFormattedTimeDiff(timeout.toLong()), TerminalColours.assertClauseColour(), getWaitForClause(inCmd), TerminalColours.reset())
                }
                assertCmd.doAssert(inCmd)
                break
            } catch (e: InvalidCssException) {
                throw e  // Not a timing issue, bail out now!
            } catch (t: Throwable) {
                if (System.currentTimeMillis() < endTime) {
                    // Ignore, give another chance!
                    Thread.sleep(334)
                } else
                /* Timed out!! Do what AssertCmd does */ {
                    throw t
                }
            }
        }
    }

    private fun getWaitForClause(inCmd: CommandSpec): Any {
        return if (inCmd.hasString()) {
            inCmd.string
        } else inCmd.asMap()
    }

    private fun getTimeout(inCmd: CommandSpec): Int {
        val defaultTimeout = scenarioDefaults.getElse("waitFor", "timeout", 10000)

        return if (inCmd.hasString()) {
            defaultTimeout  // Use default
        } else {
            val timeout = inCmd.optInteger("timeout").orElse(defaultTimeout)
            inCmd.disable("timeout")
            timeout
        }
    }
}
