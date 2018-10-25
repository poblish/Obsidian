package org.hiatusuk.obsidian.web.selenium.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.hiatusuk.obsidian.web.selenium.exceptions.NoSuchWebElementException
import org.hiatusuk.obsidian.web.selenium.exceptions.WebElementNotVisibleException
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.openqa.selenium.WebDriverException
import org.slf4j.Logger
import javax.inject.Inject

@ScenarioScope
@Command("switchTo")
class SwitchToFrame @Inject
constructor(private val web: WebState,
            private val finders: ElementFinders,
            private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val ignoreErrors = inCmd.optBoolean("ignoreErrors").orElse(false)

        val identifier = inCmd.optString().orElse("default")
        if (identifier == "default") {
            log.info("< Selenium switch to default frame >")
            web.switchTo() // default frame
        } else {
            val req = finders.with(identifier)
            log.info("< Selenium switch to frame $req >")

            val timeout = 5000
            val endTime = System.currentTimeMillis() + timeout

            while (true) {
                try {
                    web.switchTo(req.first())
                    break
                } catch (t: Throwable) {

                    if (ignoreErrors &&
                        ( t is WebElementNotVisibleException || t is NoSuchWebElementException || t is WebDriverException)) {
                        log.info("(Ignore failure to do so)")  // Ignore, these are expected, or rather anticipated
                        return
                    }

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
    }
}