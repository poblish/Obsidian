package org.hiatusuk.obsidian.web.proxy.config

import org.hiatusuk.obsidian.config.FeatureConfiguration
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.proxy.delegates.WebProxies
import javax.inject.Inject

@FeatureConfiguration("proxy")
@ScenarioScope
class WebProxyConfig @Inject
constructor(private val proxies: WebProxies) {

    fun configure(opts: Map<String,Any>) {
        proxies.startup(opts) { false }
    }
}
