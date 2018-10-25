package org.hiatusuk.obsidian.api.wiremock.delegates

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.hiatusuk.obsidian.api.wiremock.config.WireMockConfig
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.events.ApplicationQuit
import org.slf4j.Logger
import javax.inject.Inject

@ScenarioScope
class WireMockServerState @Inject
internal constructor(private val config: WireMockConfig, private val log: Logger) {

    var wireMockServer: WireMockServer? = null
    private var serverRunning: Boolean = false
    private var isUsed: Boolean = false

    @Synchronized
    fun restart() {
        stop()
        start()
    }

    private fun start() {
        if (serverRunning) {
            return
        }

        if (wireMockServer == null) {
            wireMockServer = WireMockServer(options().port(config.localPort))
        }

        log.info("Starting WireMock...")
        wireMockServer!!.start()
        serverRunning = true
    }

    @ApplicationQuit
    @Synchronized
    fun stop() {
        if (!serverRunning) {
            return
        }

        wireMockServer!!.resetAll()  // All state go!

        log.info("Stopping WireMock...")
        wireMockServer!!.stop()
        this.serverRunning = false
        this.isUsed = false
    }
}
