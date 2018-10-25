package org.hiatusuk.obsidian.protocol.http.utils

import org.apache.http.client.methods.*
import org.apache.http.client.protocol.HttpClientContext
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.state.ScenarioState
import java.io.IOException
import java.net.URI
import javax.inject.Inject

@ScenarioScope
class HttpRequestBuilder @Inject
constructor(private val scenarioState: ScenarioState) {

    @Throws(IOException::class)
    @JvmOverloads
    fun buildGetRequest(targetSpec: String, ctxt: HttpClientContext = HttpClientContext(), inLogCustomHeaders: Boolean = true): HttpUriRequest {
        return buildHttpRequest(targetSpec, ExtendedHttpGet(), ctxt, inLogCustomHeaders)
    }

    @Throws(IOException::class)
    fun buildHeadRequest(targetSpec: String, ctxt: HttpClientContext): HttpHead {
        return buildHttpRequest(targetSpec, HttpHead(""), ctxt, true) as HttpHead
    }

    @Throws(IOException::class)
    fun buildDeleteRequest(targetSpec: String, ctxt: HttpClientContext): HttpDelete {
        return buildHttpRequest(targetSpec, HttpDelete(""), ctxt, true) as HttpDelete
    }

    @Throws(IOException::class)
    fun buildPostRequest(targetSpec: String, ctxt: HttpClientContext): HttpPost {
        return buildHttpRequest(targetSpec, HttpPost(""), ctxt, true) as HttpPost
    }

    @Throws(IOException::class)
    fun buildPutRequest(targetSpec: String, ctxt: HttpClientContext): HttpPut {
        return buildHttpRequest(targetSpec, HttpPut(""), ctxt, true) as HttpPut
    }

    @Throws(IOException::class)
    fun buildOptionsRequest(targetSpec: String, ctxt: HttpClientContext): HttpOptions {
        return buildHttpRequest(targetSpec, HttpOptions(""), ctxt, true) as HttpOptions
    }

    @Throws(IOException::class)
    private fun buildHttpRequest(targetSpec: String, ioRequest: HttpRequestBase, httpCtxt: HttpClientContext, inLogCustomHeaders: Boolean): HttpRequestBase {
        // Need to obtain both URL and (optional) credentials

        if (HttpUtils.applyHeadersAndPropertiesToRequest(targetSpec, ioRequest, httpCtxt, inLogCustomHeaders))
        /* Found a URL this way? */ {
            return ioRequest
        }

        for (eachParam in targetSpec.split(',').map { it.trim() }) {
            if (eachParam.startsWith("http")) {  // Default single-arg
                ioRequest.uri = URI.create(eachParam)
                return ioRequest
            } else if (eachParam.startsWith("/")) {
                if (scenarioState.hasBaseUrl()) {
                    ioRequest.uri = URI.create(scenarioState.baseUrl!! + eachParam)
                    return ioRequest
                }
                throw RuntimeException("Relative URL '$eachParam' cannot be used without a base URL having been set")
            }
        }

        throw RuntimeException("ERROR: HTTP target neither starts with 'http' nor contains expected parameters")
    }
}
