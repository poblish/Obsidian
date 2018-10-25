package org.hiatusuk.obsidian.protocol.http.utils

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Charsets
import com.google.common.io.BaseEncoding
import org.apache.http.HttpEntity
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.HttpHeaders.AUTHORIZATION
import org.apache.http.HttpHeaders.CACHE_CONTROL
import org.apache.http.NameValuePair
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.message.BasicNameValuePair
import org.hiatusuk.obsidian.utils.StringUtils
import org.hiatusuk.obsidian.utils.StringUtils.checkSet
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.charset.Charset
import java.util.regex.Pattern

object HttpUtils {

    private val LOG = LoggerFactory.getLogger("HTTP")
    private val PARAMS_SECTION = Pattern.compile("params:\\s+\\{([^{]*)\\}")

    @VisibleForTesting
    @Throws(IOException::class)
    fun applyHeadersAndPropertiesToRequest(origSpec: String, req: HttpRequestBase, httpCtxt: HttpClientContext, inLogCustomHeaders: Boolean): Boolean {
        var gotDestUri = false

        val postReqParams = arrayListOf<NameValuePair>()

        var httpSpec = origSpec

        //////////////////////////////////////  Special handling for 'params', all because it contains its own commas...

        val paramsMatcher = PARAMS_SECTION.matcher(httpSpec)
        if (paramsMatcher.find()) {

            // Unpack the FORM params...
            for (eachPair in paramsMatcher.group(1).split(',').map { it.trim() }) {
                val keyValList = eachPair.split(':').map { it.trim() }
                postReqParams.add(BasicNameValuePair(keyValList[0], keyValList[1]))
            }

            httpSpec = paramsMatcher.replaceAll("")  // Remove from payload

            require(req is HttpEntityEnclosingRequest) {"Entities/content not supported for this type"}
            LOG.debug("Sending FormEntity parameters: {}", postReqParams)
            (req as HttpEntityEnclosingRequest).entity = UrlEncodedFormEntity(postReqParams)
        }

        //////////////////////////////////////////////////////////////////

        val customHeadersAdded = LinkedHashMap<String,String>()  // So we only log new headers *after* checking validity, to prevent silly logging for bogus requests

        for (eachParam in httpSpec.split(',').map { it.trim() }) {
            if (eachParam.startsWith("http") || eachParam.startsWith("/")) {  // Default single-arg
                continue
            }

            val cp = eachParam.indexOf(':')
            if (cp < 0) {
                continue
            }

            val key = StringUtils.stripQuotes(eachParam.substring(0, cp))
            val value = StringUtils.stripQuotes(eachParam.substring(cp + 1).trim()).trim()
            // System.out.println("> key " + key + " / " + value);

            val cs = Charsets.UTF_8

            when (key) {
                "to" -> {
                    req.uri = URI.create(value)
                    gotDestUri = true
                }
                "file" -> {
                    require(req is HttpEntityEnclosingRequest) {"Entities/content not supported for this type"}
                    (req as HttpEntityEnclosingRequest).entity = StringEntity( File(value).readText(cs), cs)
                }
                "content" -> {
                    require(req is HttpEntityEnclosingRequest) {"Entities/content not supported for this type"}
                    (req as HttpEntityEnclosingRequest).entity = entityForContent(value, cs)
                }
                "content-type" -> req.setHeader("content-type", value)
                "no-cache" -> cacheControl(req, false)
                "ua" -> req.setHeader("User-Agent", value)
                "credentials" -> HttpUtils.basicAuth(req, value)
                "digest credentials" -> HttpUtils.digestAuth(httpCtxt, value)
                else -> {
                    req.setHeader(key, value)
                    customHeadersAdded[key] = value  // For logging purposes
                }
            }
        }

        if (gotDestUri) {
            if (inLogCustomHeaders) {
                for ((key, value) in customHeadersAdded) {
                    LOG.debug("Setting header '{}' => '{}'", key, value)
                }
            }
        }

        return gotDestUri // Is complete?
    }

    private fun entityForContent(value: String, cs: Charset): HttpEntity {
        if (!value.startsWith("[") || !value.endsWith("]")) {
            return StringEntity(value, cs)
        }

        val builder = MultipartEntityBuilder.create()
        builder.setMode(HttpMultipartMode.STRICT)

        val p = Pattern.compile("\\{([^}]*)\\}")
        val m = p.matcher(value.substring(1, value.length - 1).trim())
        while (m.find()) {
            handleMultiPart(builder, m.group(1))
        }

        return builder.build()
    }

    private fun handleMultiPart(builder: MultipartEntityBuilder, partStr: String) {

        var name: String? = null
        var type = ContentType.DEFAULT_TEXT
        var text: String? = null

        for (clause in partStr.split('|').map { it.trim() }) {
            val idx = clause.indexOf(':')
            val value = clause.substring(idx + 1).trim()

            when (clause.substring(0, idx).toLowerCase().trim()) {
                "name" -> name = value
                "content-type" -> type = ContentType.parse(value)
                "text" -> text = value
            }
        }

        builder.addTextBody(checkNotNull(name), checkNotNull(text), type)
    }

    // NoJavaDoc
    // Version for HttpCore / Sync HTTP
    private fun cacheControl(req: HttpUriRequest, inCache: Boolean): HttpUriRequest {
        if (!inCache) {
            req.setHeader(CACHE_CONTROL, "no-cache")
        }
        return req
    }

    // NoJavaDoc
    // Version for HttpCore / Sync HTTP
    private fun basicAuth(req: HttpUriRequest, inCreds: String): HttpUriRequest {
        req.setHeader(AUTHORIZATION, getBasicAuthCredString(inCreds))
        return req
    }

    // NoJavaDoc
    // Version for HttpCore / Sync HTTP
    private fun digestAuth(inCtxt: HttpClientContext, inCreds: String) {

        val credArray = inCreds.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        val provider = BasicCredentialsProvider()
        provider.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(credArray[0], credArray[1]))

        inCtxt.credentialsProvider = provider
    }

    private fun getBasicAuthCredString(inCreds: String): String {
        return "Basic " + BaseEncoding.base64().encode(checkSet(inCreds).toByteArray(Charsets.US_ASCII))
    }
}
