package org.hiatusuk.obsidian.protocol.ssl.delegates

import com.codahale.metrics.MetricRegistry
import org.apache.http.config.Registry
import org.apache.http.config.RegistryBuilder
import org.apache.http.conn.socket.ConnectionSocketFactory
import org.apache.http.conn.socket.PlainConnectionSocketFactory
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.ssl.SSLContextBuilder
import org.hiatusuk.obsidian.di.ScenarioScope
import org.slf4j.Logger
import java.io.File
import javax.inject.Inject
import javax.net.ssl.SSLContext

@ScenarioScope
class SslKeystores @Inject
internal constructor(private val metrics: MetricRegistry, private val log: Logger) {

    private var keyStorePath: String? = null
    private var trustStorePath: String? = null

    // See: http://literatejava.com/networks/ignore-ssl-certificate-errors-apache-httpclient-4-4/
    val sslContext: SSLContext?
        get() {
            if (keyStorePath == null || trustStorePath == null) {
                log.warn("No key/truststore path configured, so SSL support disabled")
                return null
            }

            log.info("Initialising SSL...")

            metrics.timer("SslKeystores.getSslContext").time().use { ignored ->
                return SSLContextBuilder.create()
                        .loadKeyMaterial(File(keyStorePath!!), "changeit".toCharArray(), "changeit".toCharArray())
                        .loadTrustMaterial(File(trustStorePath!!), "changeit".toCharArray()) { arg0, arg1 -> true }
                        .setProtocol("TLS")
                        .build()
            }
        }

    fun setKeyStorePath(path: String) {
        this.keyStorePath = requireNotNull(path)
    }

    fun setTrustStorePath(path: String) {
        this.trustStorePath = requireNotNull(path)
    }

    // See: http://literatejava.com/networks/ignore-ssl-certificate-errors-apache-httpclient-4-4/
    fun createHttpRegistry(inSslCtxt: SSLContext?): Registry<ConnectionSocketFactory> {
        return RegistryBuilder.create<ConnectionSocketFactory>()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", if (inSslCtxt != null) SSLConnectionSocketFactory(inSslCtxt, NoopHostnameVerifier.INSTANCE) else /* Default */ SSLConnectionSocketFactory.getSocketFactory())
                .build()
    }
}
