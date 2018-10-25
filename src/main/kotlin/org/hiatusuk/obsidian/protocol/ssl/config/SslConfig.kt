package org.hiatusuk.obsidian.protocol.ssl.config

import org.hiatusuk.obsidian.config.FeatureConfiguration
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.protocol.ssl.delegates.SslKeystores
import javax.inject.Inject

@FeatureConfiguration("SSL")
@ScenarioScope
class SslConfig @Inject
internal constructor(private val ssl: SslKeystores) {

    fun configure(sslMap: Map<String, Any>) {
        ssl.setKeyStorePath(sslMap["keyStore"] as String)
        ssl.setTrustStorePath(sslMap["trustStore"] as String)
    }
}
