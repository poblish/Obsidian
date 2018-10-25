package org.hiatusuk.obsidian.web.selenium.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.slf4j.Logger
import javax.inject.Inject

@ScenarioScope
@Command("submit")
class FormSubmitCmd @Inject
constructor(private val finders: ElementFinders, private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val req = finders.with(inCmd.string)
        log.info("< Selenium submitting form $req >")
        req.first().submit()
    }
}
