package org.hiatusuk.obsidian.remote.googleapi.config

import org.hiatusuk.obsidian.asserts.lookups.AssertLookupsRegistry
import org.hiatusuk.obsidian.asserts.lookups.AssertSpecs
import org.hiatusuk.obsidian.config.FeatureConfiguration
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.remote.googleapi.delegates.GoogleApiAccounts
import javax.inject.Inject

@FeatureConfiguration("google api")
@ScenarioScope
class GoogleApiConfig @Inject
constructor(private val lookupsRegistry: AssertLookupsRegistry,
            private val googleApi: GoogleApiAccounts) {

    fun configure(inPayload: Map<String, Any>) {
        val rawKey = inPayload["apiKey"] as String

        // Yuk, do we really need to go through this hoop to pull a value? (though we're a Config and get called before Set:)
        val registryResult = lookupsRegistry.lookupValueForTargetIdentifier(rawKey, AssertSpecs(HashMap()))

        if (registryResult != null) {
            googleApi.apiKey = registryResult.first().text
        } else {
            googleApi.apiKey = rawKey
        }
    }
}
