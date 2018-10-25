package org.hiatusuk.obsidian.javascript.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.openqa.selenium.JavascriptExecutor
import org.slf4j.Logger
import java.util.concurrent.Executors
import javax.inject.Inject

@ScenarioScope
@Command("javascript log")
class JavaScriptLogCmd @Inject
internal constructor(private val web: WebState, private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        Executors.newFixedThreadPool(1).submit {
            val result = (web.driver as JavascriptExecutor).executeScript("return " + inCmd.string + ";")

            when (result) {
                null -> log.info("JavaScript returned null")
                is Number -> log.info("JavaScript returned number: {}", result)
                else -> log.info("JavaScript returned object: '{}'", result)
            }
        }
    }
}
