package org.hiatusuk.obsidian.protocol.http.utils

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.httpclient.HttpClientMetricNameStrategies
import com.codahale.metrics.httpclient.InstrumentedHttpRequestExecutor

import java.io.IOException
import javax.inject.Inject
import org.apache.http.HttpClientConnection
import org.apache.http.HttpException
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.protocol.HttpContext
import org.hiatusuk.obsidian.protocol.http.delegates.CurrentHttpRequestTimings

class TimedHttpRequestExecutor @Inject
internal constructor(private val currentHttpTimings: CurrentHttpRequestTimings,
                     registry: MetricRegistry) : InstrumentedHttpRequestExecutor(registry, HttpClientMetricNameStrategies.HOST_AND_METHOD) {

    @Throws(HttpException::class, IOException::class)
    override fun execute(request: HttpRequest, conn: HttpClientConnection, context: HttpContext): HttpResponse? {
        currentHttpTimings.clearAll()

        val startNs = System.nanoTime()
        try {
            return super.execute(request, conn, context)
        } finally {
            currentHttpTimings.setLastExecutionTime(System.nanoTime() - startNs)
        }
    }

    @Throws(IOException::class, HttpException::class)
    override fun doSendRequest(request: HttpRequest, conn: HttpClientConnection, context: HttpContext): HttpResponse? {
        val startNs = System.nanoTime()
        try {
            return super.doSendRequest(request, conn, context)
        } finally {
            currentHttpTimings.setLastSendTime(System.nanoTime() - startNs)
        }
    }

    @Throws(HttpException::class, IOException::class)
    override fun doReceiveResponse(request: HttpRequest, conn: HttpClientConnection, context: HttpContext): HttpResponse? {
        val startNs = System.nanoTime()
        try {
            return super.doReceiveResponse(request, conn, context)
        } finally {
            currentHttpTimings.setLastReceiveTime(System.nanoTime() - startNs)
        }
    }
}
