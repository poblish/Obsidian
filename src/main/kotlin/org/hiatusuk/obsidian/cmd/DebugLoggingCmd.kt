package org.hiatusuk.obsidian.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.RunProperties
import org.hiatusuk.obsidian.run.events.AfterScenario
import javax.inject.Inject

@ScenarioScope
@Command("debug")
class DebugLoggingCmd

@Inject
constructor(private val runProps: RunProperties) : CommandIF {

    private var wasDebugTurnedOn: Boolean = false
    private var wasAssertsTurnedOn: Boolean = false

    override fun run(inCmd: CommandSpec) {
        val opts = if (inCmd.strings.size == 1 && inCmd.strings.contains("null")) emptyList() else inCmd.strings

        if (!runProps.isDebug && (opts.isEmpty() || opts.contains("on") || opts.contains("true"))) {
            wasDebugTurnedOn = true
            runProps.isDebug = true
        }

        if (!runProps.isLogAssertions && (opts.contains("asserts") || opts.contains("assertions"))) {
            wasAssertsTurnedOn = true
            runProps.isLogAssertions = true
        }
    }

    @AfterScenario
    fun resetAfterScenario() {
        // Restore previous value if there is one
        if (wasDebugTurnedOn) {
            runProps.isDebug = false
            wasDebugTurnedOn = false
        }

        if (wasAssertsTurnedOn) {
            runProps.isLogAssertions = false
            wasAssertsTurnedOn = false
        }
    }
}
