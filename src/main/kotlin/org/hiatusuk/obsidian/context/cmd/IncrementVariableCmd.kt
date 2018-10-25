package org.hiatusuk.obsidian.context.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.di.ScenarioScope
import javax.inject.Inject

@ScenarioScope
@Command("inc")
class IncrementVariableCmd @Inject
internal constructor(private val varCtxt: VariablesContext) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val varName = inCmd.string
        varCtxt.store(varName, varCtxt.resolve("\${$varName + 1}" as String?)!!)
    }
}
