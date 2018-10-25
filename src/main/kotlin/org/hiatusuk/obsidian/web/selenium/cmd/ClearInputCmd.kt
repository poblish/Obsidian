package org.hiatusuk.obsidian.web.selenium.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.slf4j.Logger
import javax.inject.Inject

/**
 * Clears the first matching element, insofar as that makes sense.
 */
@ScenarioScope
@Command("clear")
class ClearInputCmd @Inject
constructor(private val finders: ElementFinders, private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val req = finders.with(inCmd.string)

        log.info("< Selenium clearing {} >", req)

        req.first().clear()
    }
}
