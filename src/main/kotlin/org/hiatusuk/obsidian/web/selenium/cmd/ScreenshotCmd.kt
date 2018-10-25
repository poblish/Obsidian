package org.hiatusuk.obsidian.web.selenium.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.selenium.delegates.Screenshots
import org.hiatusuk.obsidian.web.selenium.exceptions.NoSuchWebElementException
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.hiatusuk.obsidian.web.selenium.find.InvalidCssException
import java.util.*
import javax.inject.Inject

@ScenarioScope
@Command("screenshot")
class ScreenshotCmd @Inject
constructor(private val screenshots: Screenshots, private val elems: ElementFinders) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val simpleStringValue = inCmd.optString()

        if (simpleStringValue.isPresent) {
            try {
                // See if it's a usable web selector first...
                val elem = elems.with(simpleStringValue.get()).first()
                screenshots.outputWithSensibleName(Optional.of(elem), simpleStringValue.get())
            } catch (e: InvalidCssException) {
                screenshots.outputWithSensibleName(Optional.empty(), simpleStringValue.get())
            } catch (e: NoSuchWebElementException) {
                screenshots.outputWithSensibleName(Optional.empty(), simpleStringValue.get())
            }

        } else if (inCmd.has("of")) {
            // Do *not* catch NoSuchWebElementException
            val elem = elems.with(inCmd.getString("of")).first()
            screenshots.outputWithSensibleName(Optional.of(elem), inCmd.optString("as").orElse(""))
        } else {  // "screenshot:"
            screenshots.outputWithSensibleName(Optional.empty(), inCmd.optString("as").orElse(""))
        }
    }
}