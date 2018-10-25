package org.hiatusuk.obsidian.web.selenium.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.slf4j.Logger
import javax.inject.Inject

/**
 * Ensure a checkbox is checked
 */
@ScenarioScope
@Command("checked")
class CheckedCmd @Inject
constructor(private val finders: ElementFinders, private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        for (elem in finders.with(inCmd.string).all()) {
            if (!elem.isSelected) {
                log.info("< Selenium checking {} >", elem)
                elem.click()
            }
        }
    }
}
