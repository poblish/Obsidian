package org.hiatusuk.obsidian.protocol.http.lookup

import com.google.common.annotations.VisibleForTesting
import dagger.Lazy
import org.apache.http.*
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.client.utils.URIBuilder
import org.apache.http.protocol.HttpCoreContext
import org.apache.http.util.EntityUtils
import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.AssertSpecs
import org.hiatusuk.obsidian.asserts.lookups.LookupUtils
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.protocol.http.delegates.CurrentHttpRequestTimings
import org.hiatusuk.obsidian.protocol.http.utils.HttpRequestBuilder
import org.hiatusuk.obsidian.run.RunProperties
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.utils.StringUtils
import org.hiatusuk.obsidian.utils.TimeDifference.getFormattedTimeNanosDiff
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URISyntaxException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import java.util.regex.Pattern
import javax.inject.Inject

@AssertLookup(value = "http:(delete|get|head|options|post|put)", caseInsensitive = true)
@ScenarioScope
class HttpLookups @Inject
constructor(private val httpReq: HttpRequestBuilder,
            private val syncHttpClient: Lazy<HttpClient>?,
            private val exceptions: RuntimeExceptions,
            private val runProps: RunProperties,
            private val varCtxt: VariablesContext,
            private val currentTimings: CurrentHttpRequestTimings) {

    // Invoked by reflection!
    @Throws(IOException::class)
    fun lookup(targetIdentifier: String, assertSpecs: AssertSpecs): Collection<AssertTarget> {
        val httpCtxt = HttpClientContext()  // For tracking 'actual' (redirected) URL etc. See http://stackoverflow.com/questions/1456987/httpclient-4-how-to-capture-last-redirect-url

        return performLookup(targetIdentifier.substring(5, targetIdentifier.indexOf('(')).toUpperCase(), httpCtxt, targetIdentifier, Supplier { syncHttpClient!!.get() }, assertSpecs.getBoolean("ignore404"), assertSpecs.isShortForm)
    }

    @Throws(IOException::class)
    private fun performLookup(httpVerb: String,
                              httpCtxt: HttpClientContext,
                              targetIdentifier: String,
                              clientProvider: Supplier<HttpClient>,
                              ignore404: Boolean,
                              isShortForm: Boolean): Collection<AssertTarget> {

        val m = if (isShortForm) {  // Want payload bit, do *not* want to accept long-form syntax as valid short-form
            Pattern.compile("http:" + httpVerb.toLowerCase() + HTTP_PAYLOAD_ONLY_PATTERN + "$", Pattern.CASE_INSENSITIVE).matcher(targetIdentifier)
        } else {
            Pattern.compile("http:" + httpVerb.toLowerCase() + HTTP_PAYLOAD_PLUS_PROPERTY_NAMEVALUE_PATTERN, Pattern.CASE_INSENSITIVE).matcher(targetIdentifier)
        }

        if (!m.find()) {
            throw exceptions.runtime("Malformed Assert: $targetIdentifier")
        }

        ///////////////////////////////////////////////////////////////////////////

        val httpReq = createUriRequest(httpVerb, m.group(1), httpCtxt)

        val respSrc = StringBuilder()  // Ugh, can only access Entity within ResponseHandler, hence hack to make entity content accessible

        if (runProps.isLogAssertions) {
            LOG.info("< Assert: executing HTTP <{}> >", httpReq)
        }

        val propertyName = if (isShortForm) "" else m.group(2)

        var lastErrorString = ""
        var resp: HttpResponse? = null

        try {
            resp = clientProvider.get().execute(httpReq, { response ->
                val httpCode = response.statusLine.statusCode

                if (httpCode != HttpStatus.SC_OK &&
                        httpCode != HttpStatus.SC_MOVED_PERMANENTLY &&
                        httpCode != HttpStatus.SC_MOVED_TEMPORARILY &&
                        httpCode != HttpStatus.SC_NOT_FOUND && ignore404) {
                    throw exceptions.runtime("Unexpected HTTP response: " + httpCode + " @ " + httpReq.uri)
                }

                if (/* Ugh */ response.entity != null) {
                    respSrc.append(EntityUtils.toString(response.entity))
                }

                response

            }, httpCtxt)
        } catch (e: IOException) {
            lastErrorString = e.message!!

            if (propertyName != "error") {  // If we *wanted* the error, swallow the exception for matching, else rethrow
                throw e
            }
        }

        val actualUrlVisited: String

        try {
            val ub = URIBuilder((httpCtxt.getAttribute(HttpCoreContext.HTTP_TARGET_HOST) as HttpHost).toURI())
            ub.path = (httpCtxt.getAttribute(HttpCoreContext.HTTP_REQUEST) as HttpRequest).requestLine.uri
            actualUrlVisited = ub.build().toString()
        } catch (e: URISyntaxException) {
            throw exceptions.runtime(e)
        }

        varCtxt.store("lastReq",
                LastHttpLookupStateBean(
                        actualUrlVisited,
                        respSrc.toString(),
                        if (resp != null) resp.allHeaders else emptyArray(),
                        currentTimings.lastExecTimeNs,
                        currentTimings.lastSendTimeNs,
                        currentTimings.lastReceiveTimeNs,
                        exceptions))

        if (isShortForm) {
            return LookupUtils.singleTarget(resp!!.statusLine.statusCode == HttpStatus.SC_OK)
        }

        ///////////////////////////////////////////////////////////////////////////

        when (propertyName) {
            "source", "content" -> return LookupUtils.singleTarget(respSrc.toString())
            "header" -> {
                val hdr = resp!!.getFirstHeader(m.group(3))
                return LookupUtils.singleTarget(if (hdr != null) hdr.value else "")
            }
            "url" -> return LookupUtils.singleTarget(actualUrlVisited)
            "statusLine" -> return LookupUtils.singleTarget(resp!!.statusLine.toString())
            "status" -> return LookupUtils.singleTarget(resp!!.statusLine.statusCode)
            "error" -> return LookupUtils.singleTarget(lastErrorString)
        }

        throw exceptions.runtime("Unknown Assert property: $propertyName")
    }

    @VisibleForTesting
    @Throws(IOException::class)
    fun createUriRequest(httpVerb: String, targetSpec: String, httpCtxt: HttpClientContext): HttpUriRequest {
        when (httpVerb.toUpperCase()) {
            "GET" -> return httpReq.buildGetRequest(targetSpec, httpCtxt)
            "HEAD" -> return httpReq.buildHeadRequest(targetSpec, httpCtxt)
            "DELETE" -> return httpReq.buildDeleteRequest(targetSpec, httpCtxt)
            "POST" -> return httpReq.buildPostRequest(targetSpec, httpCtxt)
            "PUT" -> return httpReq.buildPutRequest(targetSpec, httpCtxt)
            "OPTIONS" -> return httpReq.buildOptionsRequest(targetSpec, httpCtxt)
        }
        throw exceptions.runtime("Unknown verb: $httpVerb")
    }

    internal class LastHttpLookupStateBean(private val url: String,
                                           private val content: String,
                                           private val headers: Array<Header>,
                                           private val lastExecTimeNs: Long,
                                           private val lastSendTimeNs: Long,
                                           private val lastReceiveTimeNs: Long,
                                           private val exceptions: RuntimeExceptions) {

        fun url() : String {
            return url
        }

        fun content() : String {
            return StringUtils.collapseWhitespace(content)
        }

        fun source() : String {
            return content()
        }

        fun header(headerName: String) : String {
            return getLastHeaderValue(headerName).orElse("")
        }

        fun headers() : String {
            return headers.joinToString(separator = ",")
        }

        private fun getLastHeaderValue(headerName: String): Optional<String> {
            //System.out.println("Find '" + headerName + "' within " + lastHeaders);
            for (each in headers) {  // FIXME Use a Map?!?
                if (each.name == headerName) {
                    return Optional.of(each.value)
                }
            }
            return Optional.empty()
        }

        fun time(timingName: String) : String {
            return when (timingName) {
                "execute" -> java.lang.Long.toString(TimeUnit.NANOSECONDS.toMillis(lastExecTimeNs))
                "send" -> java.lang.Long.toString(TimeUnit.NANOSECONDS.toMillis(lastSendTimeNs))
                "receive" -> java.lang.Long.toString(TimeUnit.NANOSECONDS.toMillis(lastReceiveTimeNs))
                else -> throw exceptions.runtime("Unknown timing: $timingName")
            }
        }

        // Called when bean is accessed directly via VarContext, e.g. via 'echo'
        override fun toString() : String {
            return "Dumping last HTTP request...\n" +
                    "URL : $url\n" +
                    "Headers : " + headers() + "\n" +
                    "Timings (exec) : " + getFormattedTimeNanosDiff(lastExecTimeNs) + "\n" +
                    "Timings (send) : " + getFormattedTimeNanosDiff(lastSendTimeNs) + "\n" +
                    "Timings (recv) : " + getFormattedTimeNanosDiff(lastReceiveTimeNs) + "\n" +
                    "Content : $content\n"
        }
    }

    companion object {

        private const val HTTP_PAYLOAD_ONLY_PATTERN = "\\((.*)\\)"
        private const val HTTP_PAYLOAD_PLUS_PROPERTY_NAMEVALUE_PATTERN = "$HTTP_PAYLOAD_ONLY_PATTERN\\.([A-Z]*)\\((.*)\\)"

        private val LOG = LoggerFactory.getLogger(HttpLookups::class.java)
    }
}
