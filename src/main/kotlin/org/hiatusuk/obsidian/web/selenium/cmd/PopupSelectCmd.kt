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
@Command("popup select")
class PopupSelectCmd @Inject
constructor(private val web: WebState,
            private val createdWindowState: NewlyCreatedWindowState,
            private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        if (createdWindowState.newWindowHandle.isPresent) {
            log.info("< Selenium switchTo window '{}' >", createdWindowState.newWindowHandle.get())
            web.driver.switchTo().window(createdWindowState.newWindowHandle.get())
        } else {
            throw RuntimeException("No newly created window was tracked.")
        }
    }
}