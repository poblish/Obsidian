package org.hiatusuk.obsidian.web.selenium.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import java.util.concurrent.Executors
import javax.inject.Inject

@ScenarioScope
@Command("alert")
class AlertCmd @Inject
constructor(private val web: WebState) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        Executors.newFixedThreadPool(1).submit { web.executeScript("alert('" + inCmd.string + "')") }
    }
}
