package org.hiatusuk.obsidian.remote.googleapi.delegates

import org.hiatusuk.obsidian.di.ScenarioScope
import javax.inject.Inject

@ScenarioScope
class GoogleApiAccounts @Inject constructor() {

    var apiKey: String? = null
        set(apiKey) {
            field = requireNotNull(apiKey)
        }
}
