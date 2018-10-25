package org.hiatusuk.obsidian.run.config

import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.config.FeatureConfiguration
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.delegates.CommandNamespaces
import org.hiatusuk.obsidian.run.state.ScenarioState
import javax.inject.Inject

@FeatureConfiguration("baseUrl")
@ScenarioScope
class BaseUrlConfig @Inject
constructor(private val scenarioState: ScenarioState,
            private val varCtxt: VariablesContext,
            private val ns: CommandNamespaces) {

    fun configure(inPayload: Map<String,Any>) {

        var fullBaseUrl = inPayload["url"] as String? // Only true in 'full' mode
        if (fullBaseUrl == null) {
            scenarioState.baseUrl = varCtxt.resolve(inPayload.entries.first().key as String?)
        }
        else {
            if (inPayload.containsKey("or")) {

                // Run these in sequence, each overwriting what's before, providing the 'when' filter passes
                for (orEntry in (inPayload["or"] as List<Map<String, Any>>)) {

                    // For easier parsing!
                    val orSpec = CommandSpec(varCtxt, ns, orEntry, false)

                    // Return if an (optional) boolean "when" clause resolves to false. See also @SetVariableCmd
                    val whenFilter = orSpec.optBoolean("when")
                    if (!whenFilter.orElse(true)) {
                        continue
                    }

                    fullBaseUrl = orSpec.string
                }
            }

            scenarioState.baseUrl = fullBaseUrl
        }
    }
}
