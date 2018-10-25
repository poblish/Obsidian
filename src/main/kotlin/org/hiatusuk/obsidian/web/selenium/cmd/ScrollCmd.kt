package org.hiatusuk.obsidian.web.selenium.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.openqa.selenium.JavascriptExecutor
import org.slf4j.Logger
import javax.inject.Inject

@ScenarioScope
@Command("scroll")
class ScrollCmd @Inject
constructor(private val web: WebState, private val finders: ElementFinders, private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val req = finders.with(inCmd.string)
        log.info("< Selenium scrolling into view $req >")
        (web.driver as JavascriptExecutor).executeScript("arguments[0].scrollIntoView(true);", req.first())
    }
}
