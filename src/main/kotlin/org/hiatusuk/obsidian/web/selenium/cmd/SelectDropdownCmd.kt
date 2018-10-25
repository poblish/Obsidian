package org.hiatusuk.obsidian.web.selenium.cmd

import org.hiatusuk.obsidian.cmd.Delay
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.delegates.ScenarioDefaultsContext
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.openqa.selenium.support.ui.Select
import javax.inject.Inject

@ScenarioScope
@Command("select")
class SelectDropdownCmd @Inject
constructor(private val delayCmd: Delay, private val finders: ElementFinders, private val defaults: ScenarioDefaultsContext) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val selObj = Select(finders.with(inCmd.string).first())

        when {
            inCmd.has("by label") -> selObj.selectByVisibleText(inCmd.getString("by label"))
            inCmd.has("remove label") -> selObj.deselectByVisibleText(inCmd.getString("remove label"))
            inCmd.has("remove all") -> selObj.deselectAll()
            else -> throw RuntimeException("Unknown select: command... $inCmd")
        }

        delayCmd.delayFor(inCmd.optInteger("thenWait").orElse(defaults["select", "thenWait"]).toLong())
    }
}
