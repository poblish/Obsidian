package org.hiatusuk.obsidian.web.selenium.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import javax.inject.Inject

@ScenarioScope
@Command("back")
class NavigateBackCmd @Inject
constructor(private val web: WebState) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        web.driver.navigate().back()
    }
}
