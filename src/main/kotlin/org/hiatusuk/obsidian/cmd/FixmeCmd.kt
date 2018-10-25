package org.hiatusuk.obsidian.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import javax.inject.Inject

@ScenarioScope
@Command("FIXME")
class FixmeCmd @Inject
constructor() : CommandIF {
    override fun run(inCmd: CommandSpec) {
        throw RuntimeException("FIXME: Not implemented yet" + inCmd.optString().map { s -> " ('$s')" }.orElse(""))
    }
}