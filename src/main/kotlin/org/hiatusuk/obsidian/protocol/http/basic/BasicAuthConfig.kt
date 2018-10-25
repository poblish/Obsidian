package org.hiatusuk.obsidian.protocol.http.basic

import net.lightbody.bmp.proxy.auth.AuthType
import org.hiatusuk.obsidian.config.FeatureConfiguration
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.proxy.config.WebProxyConfig
import org.hiatusuk.obsidian.web.proxy.delegates.WebProxies
import org.slf4j.Logger
import javax.inject.Inject

@FeatureConfiguration("basicAuth")
@ScenarioScope
class BasicAuthConfig @Inject
constructor(private val proxies: WebProxies,
            private val proxyConfig: WebProxyConfig,
            private val log: Logger) {

    fun configure(inPayload: Map<String,Any>) {
        if (!proxies.isProxyRunning) {
            log.info("No proxy server running. Starting one now...")
            proxyConfig.configure( HashMap() )
        }

        for ((key, value) in inPayload) {
            val credsString = value as String
            val idx = credsString.indexOf(':')
            val user = credsString.substring(0, idx)
            val pass = credsString.substring(idx + 1)

            proxies.browserMobProxy.autoAuthorization(key, user, pass, AuthType.BASIC)
        }
    }
}

