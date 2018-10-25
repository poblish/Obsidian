package org.hiatusuk.obsidian.run.profiles

import org.hiatusuk.obsidian.config.FeatureConfiguration
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.RunProperties
import javax.inject.Inject

@FeatureConfiguration("profiles")
@ScenarioScope
class ScenarioProfilesConfig
@Inject constructor(private val runProps: RunProperties) {

    var profileAccepted : Boolean = true

    fun configure(scenarioProfiles: Map<String, Any>) {
        profileAccepted = Profiles.shouldRunScenario(runProps.specifiedProfiles, scenarioProfiles.keys)
    }
}
