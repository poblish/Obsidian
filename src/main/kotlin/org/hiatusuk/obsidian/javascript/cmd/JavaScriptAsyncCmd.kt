package org.hiatusuk.obsidian.javascript.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.openqa.selenium.JavascriptExecutor
import javax.inject.Inject

@ScenarioScope
@Command("javascript async")
class JavaScriptAsyncCmd @Inject
constructor(private val web: WebState) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        (web.driver as JavascriptExecutor).executeAsyncScript(inCmd.string)
    }
}
