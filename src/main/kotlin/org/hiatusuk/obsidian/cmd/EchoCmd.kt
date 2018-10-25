package org.hiatusuk.obsidian.cmd

import com.google.common.eventbus.EventBus
import org.hiatusuk.obsidian.asserts.lookups.AssertLookups
import org.hiatusuk.obsidian.asserts.lookups.AssertSpecs
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.delegates.ExtractionHandler
import org.hiatusuk.obsidian.web.selenium.exceptions.InvalidSelectorException
import org.hiatusuk.obsidian.web.selenium.exceptions.NoSuchWebElementException
import org.hiatusuk.obsidian.web.selenium.find.InvalidCssException
import org.openqa.selenium.WebDriverException
import org.slf4j.Logger
import javax.inject.Inject

@ScenarioScope
@Command("echo")
class EchoCmd @Inject
constructor(private val lookups: AssertLookups,
            private val events: EventBus,
            private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        var echoVal: String = inCmd.string

        val extractResult = ExtractionHandler().matchString(echoVal)

        if (extractResult.matched()) {
            echoVal = extractResult.contents!!
        }

        try {
            if (echoVal.isEmpty()) {  // Simplest/easiest handling for blank values, to prevent "missing assert target" etc.
                outputString("")
                return
            }

            // Bit iffy - try it as a path first, else echo the value. Not quite right, of course.
            // After all, the lookups could potentially be expensive!

            val results = lookups.getAssertionTargets(AssertSpecs( mapOf("that" to echoVal) ))
            val rawOutput = results.first().text

            outputString(extractResult.extractString(rawOutput))
        } catch (e: InvalidCssException) {  // E.g. for NullWebDriver
            outputString(extractResult.extractString(echoVal))
        } catch (e: WebDriverException) {
            outputString(extractResult.extractString(echoVal))
        } catch (e: NoSuchWebElementException) {
            outputString(extractResult.extractString(echoVal))
        } catch (e: InvalidSelectorException) {
            outputString(extractResult.extractString(echoVal))
        } catch (e: UnsupportedOperationException) {
            outputString(extractResult.extractString(echoVal))
        }
    }

    private fun outputString(inStr: String) {
        log.info("echo: {}", inStr)
        events.post(inStr)
    }
}