package org.hiatusuk.obsidian.web.selenium.cmd

import org.hiatusuk.obsidian.cmd.Delay
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.Lifecycle
import org.hiatusuk.obsidian.run.delegates.ScenarioDefaultsContext
import org.hiatusuk.obsidian.run.events.AfterWebClick
import org.hiatusuk.obsidian.run.exceptions.ExitScenarioException
import org.hiatusuk.obsidian.run.state.ScenarioState
import org.hiatusuk.obsidian.web.selenium.config.RequiresBrowser
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.hiatusuk.obsidian.web.selenium.find.FindRequest
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions
import org.slf4j.Logger
import javax.inject.Inject

//TODO Should really add a @Validate handler
@Command("click")
@RequiresBrowser
@ScenarioScope
class ClickCmd @Inject
internal constructor(private val finders: ElementFinders,
                     private val web: WebState,
                     private val scenarioState: ScenarioState,
                     private val scenarioDefaults: ScenarioDefaultsContext,
                     private val delayCmd: Delay,
                     private val lifecycle: Lifecycle,
                     private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        try {
            val exitBehaviour = getExitBehaviour(inCmd)

            val hoveringOverElement = inCmd.optString("hoveringOver")

            val timeout = inCmd.optInteger("timeout")
                    .orElse(if (exitBehaviour == "fail") /* Default */ 5000 else 1000)
            val endTime = System.currentTimeMillis() + timeout
            var stillWaiting = false

            while (true) {
                try {
                    val clickableElement = getElementToClick(inCmd, stillWaiting)

                    if (hoveringOverElement.isPresent) {
                        Actions(web.driver).moveToElement( findWith( hoveringOverElement.get() ).first() )
                                .click(clickableElement)
                                .perform()
                    }
                    else {
                        clickableElement.click()
                    }

                    delayCmd.run( /* Want default only */null, scenarioDefaults["click", "thenWait"])
                    runAfterClickHandlers()  // Not sure if this should be before/after delay, but this is 'safer'
                    break
                } catch (t: Throwable) {
                    if (System.currentTimeMillis() < endTime) {
                        // Ignore, give another chance!
                        stillWaiting = true
                        Thread.sleep(334)
                    } else
                    /* Timed out!! Do what AssertCmd does */ {

                        if (exitBehaviour == "ignore") {
                            log.info("Ignoring missing <click>")
                            return
                        } else if (exitBehaviour == "exit") {
                            log.info("Ignoring missing <click> and skipping scenario")
                            throw ExitScenarioException()
                        }

                        throw t
                    }
                }

            }
        } catch (e: WebDriverException) {
            throw RuntimeException(e)
        }
    }

    // private fun validate(identifier: Any) {
        // *NO*, we cannot check at this point, because scenarioState.getDefaultTarget() - the implicit 'this' - is only set during
        // a for: loop and can't be determined right now. Well, we *could* whether we appear between a for: and a next: but the
        // validation API assumes all commands are self-contained, so this can't be done at the moment.
    // }

    private fun getElementToClick(inCmd: CommandSpec, stillWaiting: Boolean): WebElement {
        when {
            inCmd.has("find") -> {
                val req = findWith(inCmd.getString("find"))
                log.info("< Selenium click " + req + (if (stillWaiting) STILL_WAITING_MSG else "") + " >")
                return req.first()
            }
            inCmd.hasString() -> {  // 'click: #foo'
                val req = findWith(inCmd.string)
                log.info("< Selenium click " + req + (if (stillWaiting) STILL_WAITING_MSG else "") + " >")
                return req.first()
            }
            scenarioState.defaultTarget.isPresent -> {  // 'click:'
                log.info("< Selenium click " + scenarioState.defaultTarget.get() + (if (stillWaiting) STILL_WAITING_MSG else "") + " >")
                return scenarioState.defaultTarget.get().first()
            }
            else -> throw RuntimeException("No identifier/path specified for command")  // Already covered by validate()
        }
    }

    // Allow overrides
    private fun findWith(inSemSelector: String): FindRequest {
        return finders.with(inSemSelector)
    }

    private fun getExitBehaviour(inCmd: CommandSpec): String {
        return if (inCmd.hasString()) {
            "fail"
        } else inCmd.optString("else").orElse("fail")

// Values are 'fail', 'ignore (run next cmd)', 'exit (scenario)'
    }

    private fun runAfterClickHandlers() {
        lifecycle.call(AfterWebClick::class.java)
    }

    companion object {

        private const val STILL_WAITING_MSG = " (still waiting...)"
    }
}