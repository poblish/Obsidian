package org.hiatusuk.obsidian.protocol.http.har.lookup

import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.LookupUtils
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.web.proxy.delegates.WebProxies
import org.slf4j.Logger
import java.util.regex.Pattern
import javax.inject.Inject

@AssertLookup("har_")
@ScenarioScope
class HarLookups @Inject
internal constructor(private val proxies: WebProxies, private val exceptions: RuntimeExceptions, private val log: Logger) {

    fun lookup(targetIdentifier: String): Collection<AssertTarget> {

        val currentHar = proxies.currentHar()
                ?: throw exceptions.runtime("No HAR file has been captured. Did you call `har start:` ?")

        val hLog = currentHar.log

        val timingsMatcher = PAGE_PATTERN.matcher(targetIdentifier)
        if (timingsMatcher.find()) {
            val methodName = timingsMatcher.group(1).trim()
            val methodParam = timingsMatcher.group(2).trim()

            when (methodName.toLowerCase()) {
                "time" -> when (methodParam.toLowerCase()) {
                    "onload" -> {
                        val page = hLog.pages.first()  // Ugh, is really one page supported??
                        return LookupUtils.singleTarget(page.pageTimings.onLoad!!)
                    }
                    else -> throw exceptions.runtime("Unknown timing: $methodName")
                }
                else -> throw exceptions.runtime("Unknown HAR method: $methodParam")
            }
        }

        ////////////////////////////////////////////////////////////

        val m = ENTRY_PATTERN.matcher(targetIdentifier)
        if (!m.find()) {
            throw exceptions.runtime("Malformed HAR Assert: '$targetIdentifier'")
        }

        var idx = 1
        val entryRegex = m.group(idx++).trim()
        val entryRegexPattern = Pattern.compile(entryRegex)
        val methodName = m.group(idx++).trim()
        val methodParam = m.group(idx).trim()

        log.trace("{} / {} / {}", entryRegex, methodName, methodParam)

        var anyRegexMatchesFound = false

        for (eachEntry in hLog.entries) {
            log.trace("Test... '{}' vs '{}'", entryRegexPattern, eachEntry.request.url)

            // log.info( eachEntry.getTimings().getBlocked() + " / " + eachEntry.getTimings().getSend() + " / " + eachEntry.getTimings().getReceive() + " / " + eachEntry.getTimings().getConnect() + " / " + eachEntry.getTimings().getDns() + " / " + eachEntry.getTimings().getSsl() + " / " + eachEntry.getTimings().getWait());

            if (entryRegexPattern.matcher(eachEntry.request.url).find()) {
                anyRegexMatchesFound = true

                when (methodName.toLowerCase()) {
                    "query" -> {
                        val qs = eachEntry.request.queryString
                        if (qs.isEmpty()) {
                            throw exceptions.runtime("query() called, but no query strings found.")
                        }

                        for (nvp in qs) {
                            if (nvp.name == methodParam) {
                                return LookupUtils.singleTarget(nvp.value)
                            }
                        }
                    }
                    "response-header" -> {
                        val rhs = eachEntry.response.headers
                        if (rhs.isEmpty()) {
                            throw exceptions.runtime("response-header() called, but no query strings found.")
                        }

                        for (nvp in rhs) {
                            if (nvp.name == methodParam) {
                                return LookupUtils.singleTarget(nvp.value)
                            }
                        }
                    }
                    "time" -> return when (methodParam.toLowerCase()) {
                        "blocked" -> LookupUtils.singleTarget(eachEntry.timings.blocked!!)
                        "connect" -> LookupUtils.singleTarget(eachEntry.timings.connect!!)
                        "dns" -> LookupUtils.singleTarget(eachEntry.timings.dns!!)
                        "receive" -> LookupUtils.singleTarget(eachEntry.timings.receive)
                        "send" -> LookupUtils.singleTarget(eachEntry.timings.send)
                        "ssl" -> LookupUtils.singleTarget(eachEntry.timings.ssl!!)
                        "wait" -> LookupUtils.singleTarget(eachEntry.timings.wait)
                        else -> throw exceptions.runtime("Unknown timing: $methodName")
                    }
                    else -> throw exceptions.runtime("Unknown HAR method: $methodParam")
                }
            }
        }

        if (!anyRegexMatchesFound) {
            throw exceptions.runtime("No entry found with pattern '$entryRegexPattern'")
        }

        return LookupUtils.singleTarget("")
    }

    companion object {

        private val PAGE_PATTERN = Pattern.compile("har_page\\(\\).([A-Z\\-]*)\\((.*)\\)", Pattern.CASE_INSENSITIVE)
        private val ENTRY_PATTERN = Pattern.compile("har_entry\\((.*)\\)\\.([A-Z\\-]*)\\((.*)\\)", Pattern.CASE_INSENSITIVE)
    }
}
