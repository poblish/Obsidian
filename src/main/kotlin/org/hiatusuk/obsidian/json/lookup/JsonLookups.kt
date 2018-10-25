package org.hiatusuk.obsidian.json.lookup

import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPathException
import org.apache.http.client.protocol.HttpClientContext
import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.AssertLookups
import org.hiatusuk.obsidian.asserts.lookups.LookupUtils
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.json.delegates.JsonLoader
import org.hiatusuk.obsidian.protocol.http.utils.HttpRequestBuilder
import org.hiatusuk.obsidian.run.RunProperties
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.utils.TerminalColours
import org.slf4j.Logger
import java.util.regex.Pattern
import javax.inject.Inject

@AssertLookup("json\\(")
@ScenarioScope
class JsonLookups @Inject
internal constructor(private val httpReq: HttpRequestBuilder,
                     private val loader: JsonLoader,
                     private val exceptions: RuntimeExceptions,
                     private val runProps: RunProperties,
                     private val log: Logger) {

    fun lookup(targetIdentifier: String): Collection<AssertTarget> {
        val m = JSON_HANDLER_PATTERN.matcher(targetIdentifier)
        if (!m.find()) {
            throw exceptions.runtime("Malformed Assert: $targetIdentifier")
        }

        val payload = m.group(1).trim()
        val propertyName = m.group(2).trim()
        val propertyQuery = m.group(3).trim()

        if (runProps.isDebug) {
            log.debug("JSON: payload = '{}', property = '{}', query = '{}'", payload, propertyName, propertyQuery)
        }

        try {
            val getReq = httpReq.buildGetRequest(payload, HttpClientContext(), /* Don't log probably bogus extra headers */ false)

            if (runProps.isLogAssertions) {
                log.info("< Asserting against {}'{}'{}, query: '{}' >", TerminalColours.assertClauseColour(), getReq.uri, TerminalColours.reset(), propertyQuery)
            }

            return returnMatches(loader.loadDocument(getReq), propertyQuery, propertyName)
        } catch (e: JsonPathException) {  // E.g. "Can not deserialize instance of java.lang.String[] out of VALUE_STRING token"
            throw RuntimeException(e.cause)
        } catch (e: RuntimeException) {

            // Yuk!!! OK, we assumed the content couldn't be parsed as a URL, so treat it as a String

            if (runProps.isLogAssertions) {
                log.info("< Asserting against {}text content{}, query: '{}' >", TerminalColours.assertClauseColour(), TerminalColours.reset(), propertyQuery)
            }

            return returnMatches(loader.loadDocument( /* Resolve vars?? */m.group(1)), propertyQuery, propertyName)
        }
    }

    private fun returnMatches(doc: DocumentContext, pQuery: String, pName: String): Collection<AssertTarget> {
        return LookupUtils.singleTarget(loader.findString(doc, pName, pQuery))
    }

    companion object {
        private val JSON_HANDLER_PATTERN = Pattern.compile("json" + AssertLookups.METHOD_HANDLER_PATTERN_STR, Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
    }
}
