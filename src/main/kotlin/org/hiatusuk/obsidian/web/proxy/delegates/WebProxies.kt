package org.hiatusuk.obsidian.web.proxy.delegates

import net.lightbody.bmp.BrowserMobProxy
import net.lightbody.bmp.BrowserMobProxyServer
import net.lightbody.bmp.client.ClientUtil
import net.lightbody.bmp.core.har.Har
import net.lightbody.bmp.proxy.CaptureType.*
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.events.AfterGoToUrl
import org.hiatusuk.obsidian.run.events.ApplicationQuit
import org.hiatusuk.obsidian.run.events.BeforeGoToUrl
import org.openqa.selenium.Proxy
import org.slf4j.Logger
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject

@ScenarioScope
class WebProxies @Inject
internal constructor(private val log: Logger) {

    private var server: BrowserMobProxyServer? = null
    private var proxy: Proxy? = null
    private var harRecordingOn: Boolean = false

    val isProxyRunning: Boolean
        get() = server != null

    private val workableHar: Har
        get() {
            if (server == null) {
                throw RuntimeException("Proxy server not running!")
            }

            return currentHar() ?: throw RuntimeException("No HAR to dump (null)")
        }

    val browserMobProxy: BrowserMobProxy
        get() {
            if (server == null) {
                throw RuntimeException("Server not configured")
            }

            return server!!
        }

    fun startup(options: Map<String, Any>, serverInterceptor: (BrowserMobProxyServer) -> Boolean) {

        // FIXME Arguments currently ignored. There should be one Server per config, not one 'global' one.

        val proxyPort = 8202 // 4444

        if (server != null) {
            log.info("Proxy already running on :{}", proxyPort)
            return
        }

        server = BrowserMobProxyServer()
        log.info("Starting proxy on :{}", proxyPort)

        server!!.setHarCaptureTypes(EnumSet.of(REQUEST_CONTENT, RESPONSE_CONTENT, REQUEST_BINARY_CONTENT, RESPONSE_BINARY_CONTENT, RESPONSE_HEADERS))

        serverInterceptor(server!!)

        server!!.start(proxyPort)

        log.info("Using {}", ClientUtil.getConnectableAddress())
        // get the Selenium proxy object
        proxy = ClientUtil.createSeleniumProxy(server!!)
    }

    @ApplicationQuit
    fun stop() {
        // Disallow concurrent stop() and getProxy(). Doesn't totally eliminate occasional 'java.net.SocketException: Socket is closed' after close()
        synchronized(STOP_STATUS_LOCK) {
            if (server != null) {
                try {
                    log.info("Stopping proxy...")
                    server!!.stop()
                } catch (e: IllegalStateException) {
                    log.warn("Stop failed: " + e.message)
                }

                server = null
            }

            // Clear Proxy, so that *future* WebDrivers don't get created with one.
            proxy = null
        }
    }

    fun startRecording() {
        if (server == null) {
            return
        }

        log.debug("HAR Recording ON...")

        harRecordingOn = true
    }

    fun getProxy(): Proxy? {
        // Disallow concurrent stop() and getProxy(). Doesn't totally eliminate occasional 'java.net.SocketException: Socket is closed' after close()
        synchronized(STOP_STATUS_LOCK) {
            return proxy
        }
    }

    @BeforeGoToUrl
    fun newHar(inPageRef: String): Har? {
        if (server == null || !harRecordingOn) {
            return null
        }

        log.debug("HAR Starting: {} ...", inPageRef)

        return server!!.newHar(inPageRef)
    }

    fun currentHar(): Har? {
        return if (server != null) server!!.har else null
    }

    fun dumpHar() {
        val h = workableHar

        log.debug("HAR contents:")

        for (page in h.log.pages) {
            log.debug("> Page '{}', onload: {} ms", page.title, page.pageTimings.onLoad)
        }

        for (e in h.log.entries) {
            val et = e.timings
            log.debug("> Entry:  url={}, q={}", e.request.url, e.request.queryString)
            log.debug(">> Times: {}, {}, {}, {}, {}, {}, {}", et.blocked, et.send, et.receive, et.connect, et.dns, et.ssl, et.wait)
            log.debug(">> Response headers: {}", if (e.response.headers.isEmpty()) "<none>" else e.response.headers)
        }
    }

    @Throws(IOException::class)
    fun copyHar(inDest: File) {
        val h = workableHar
        log.debug("Copying HAR to '{}'", inDest)
        h.writeTo(inDest)
    }

    @AfterGoToUrl
    fun finishHar(): Har? {
        if (server == null || !harRecordingOn) {
            return null
        }

        log.debug("HAR Finishing...")
        harRecordingOn = false

        // return server.endHar();  // v.2.1

        server!!.endPage()
        return server!!.har
    }

    companion object {
        private val STOP_STATUS_LOCK = ByteArray(0)
    }
}
