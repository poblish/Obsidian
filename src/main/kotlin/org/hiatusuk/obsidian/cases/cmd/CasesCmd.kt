package org.hiatusuk.obsidian.cases.cmd

import org.hiatusuk.obsidian.cases.Case
import org.hiatusuk.obsidian.cases.delegates.CasesState
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.state.ScenarioState
import org.slf4j.Logger
import javax.inject.Inject

@ScenarioScope
@Command("cases")
class CasesCmd @Inject
constructor(private val casesState: CasesState,
            private val scenarioState: ScenarioState,
            private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        if (scenarioState.lastCommand.isNamed("cases")) {
            throw RuntimeException("Empty case found. No commands between the last cases: command and this one. Previous case would be incorrectly marked as passed.")
        }

        casesState.provisionallyPassCurrentCases()  // Bear in mind *all* browsers need to run before final pass!

        for (eachCase in inCmd.childElements("cases")) {
            val sts = eachCase["subtypes"]
            when (sts) {
                null -> casesState.add(Case(eachCase["id"] as String))
                is String -> casesState.add(Case(eachCase["id"] as String, sts))
                is Map<*, *> -> casesState.add(Case(eachCase["id"] as String, (sts as Map<String, Any>).keys))
            }
        }

        log.info("Starting Cases: {}", casesState.description)
    }
}
