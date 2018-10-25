package org.hiatusuk.obsidian.web.selenium.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.selenium.delegates.NewlyCreatedWindowState
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.slf4j.Logger
import javax.inject.Inject

@ScenarioScope
@Command("popup return")
class PopupReturnCmd @Inject
constructor(private val web: WebState,
            private val createdWindowState: NewlyCreatedWindowState,
            private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        if (createdWindowState.originalWindowHandle != null) {
            log.info("< Selenium return to window '{}' >", createdWindowState.originalWindowHandle)
            web.driver.switchTo().window(createdWindowState.originalWindowHandle)
        } else {
            throw RuntimeException("No window available to return to.")
        }
    }
}