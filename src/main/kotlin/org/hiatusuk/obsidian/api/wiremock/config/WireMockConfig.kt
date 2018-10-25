package org.hiatusuk.obsidian.api.wiremock.config

import org.hiatusuk.obsidian.config.FeatureConfiguration
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.utils.ParseUtils
import javax.inject.Inject

@ScenarioScope
@FeatureConfiguration("api:wiremock")
class WireMockConfig @Inject
constructor() {

    var localPort = DEFAULT_LOCAL_PORT
        private set

    fun configure(inPayload: Map<String,Any>) {
        localPort = ParseUtils.valueToInt((inPayload).getOrDefault("local-port", "" + DEFAULT_LOCAL_PORT), null)
    }

    companion object {
        private const val DEFAULT_LOCAL_PORT = 8089
    }
}
