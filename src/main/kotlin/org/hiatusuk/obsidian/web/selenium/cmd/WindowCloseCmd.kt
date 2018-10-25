package org.hiatusuk.obsidian.web.selenium.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.slf4j.Logger
import javax.inject.Inject

@ScenarioScope
@Command("window close")
class WindowCloseCmd @Inject
constructor(private val web: WebState, private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        log.info("< Selenium closing current window >")
        web.driver.close()
    }
}
