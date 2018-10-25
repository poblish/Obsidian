package org.hiatusuk.obsidian.xml.lookup

import org.apache.http.client.methods.HttpUriRequest
import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.AssertLookups
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.protocol.http.utils.HttpRequestBuilder
import org.hiatusuk.obsidian.run.RunProperties
import org.hiatusuk.obsidian.run.events.AfterScenario
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.utils.StringUtils.collapseWhitespace
import org.hiatusuk.obsidian.xml.delegates.DocumentParser
import org.slf4j.Logger
import org.w3c.dom.Document
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.util.regex.Pattern
import javax.inject.Inject
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.Source

@AssertLookup("xml\\(")
@ScenarioScope
class XmlLookups @Inject
constructor(private val runProps: RunProperties,
            private val requester: XmlLookupsRequester,
            private val httpReq: HttpRequestBuilder,
            private val docParse: DocumentParser,
            private val varCtxt: VariablesContext,
            private val exceptions: RuntimeExceptions,
            private val log: Logger) {

    // Only public to help SetVariableCmd
    @Throws(IOException::class)
    fun lookup(targetIdentifier: String): Collection<AssertTarget> {
        val m = XML_HANDLER_PATTERN.matcher( collapseWhitespace(targetIdentifier) )
        if (!m.find()) {
            throw exceptions.runtime("Malformed Assert: $targetIdentifier")
        }

        val value = m.group(1).trim()
        val propertyName = m.group(2).trim()
        val propertyQuery = m.group(3).trim()

        when {
            value.startsWith("file:") -> {  // FIXME Yuk!
                val f = File(m.group(1).trim().substring(5))

                if (runProps.isLogAssertions) {
                    log.info("< Asserting against local file '[]', query: '{}' >", f, propertyQuery)
                }

                return lookup(FileBasedLookup(f), propertyName, propertyQuery)
            }
            value.startsWith("<") -> return lookup(StringBasedLookup(value), propertyName, propertyQuery)  // Assume inline XML text
            else -> {
                val getReq = httpReq.buildGetRequest(value)

                if (runProps.isLogAssertions) {
                    log.info("< Asserting against '" + getReq.uri + "', query: '" + propertyQuery + "' >")
                }

                return lookup(HttpBasedLookup(getReq), propertyName, propertyQuery)
            }
        }
    }

    @Throws(IOException::class)
    private fun lookup(inLookupType: XmlLookupType, propertyName: String, propertyQuery: String): Collection<AssertTarget> {
        try {
            return listOf(AssertTarget(requester.getLookupValue(inLookupType, propertyName, propertyQuery)))
        } catch (e: SAXException) {
            throw exceptions.runtime(e)
        }
    }

    // FIXME Need to store *source* too, of Strings and Files
    private fun rememberDocument(inDoc: Document): Document {
        varCtxt.store("lastXml", LastXmlLookupStateBean(inDoc, requester))
        return inDoc
    }

    @AfterScenario
    fun resetAfterScenario() {
        varCtxt.remove("lastXml")
    }

    @Suppress("ConvertSecondaryConstructorToPrimary", "unused")  // Accessed via EL
    internal class LastXmlLookupStateBean {

        // Unsupported at present: var lastContent: String? = null
        private var lastXmlDoc: () -> Document?
        private val requester: XmlLookupsRequester

        constructor(doc: Document, requester: XmlLookupsRequester) {
            this.lastXmlDoc = {doc}
            this.requester = requester
        }

        fun content() : Document {
            return lastXmlDoc.invoke()!!
        }

        fun validate() : String {
            return requester.getLookupValue( ExistingDocumentLookup(content()), "validate", "")
        }

        fun string(path: String) : String {
            return requester.getLookupValue( ExistingDocumentLookup(content()), "string", path)
        }

        fun node(path: String) : String {
            return requester.getLookupValue( ExistingDocumentLookup(content()), "node", path)
        }

        fun nodes(path: String) : String {
            return requester.getLookupValue( ExistingDocumentLookup(content()), "nodes", path)
        }

        fun nodesSorted(path: String) : String {
            return requester.getLookupValue( ExistingDocumentLookup(content()), "nodesSorted", path)
        }

        fun count(path: String) : String {
            return requester.getLookupValue( ExistingDocumentLookup(content()), "count", path)
        }
    }

    class ExistingDocumentLookup(private val doc: Document) : XmlLookupType {

        override fun document(): Document {
            return doc
        }

        override fun validDocument(): Document {
            return doc
        }

        override fun validDocument(inSchemaSource: Source): Document {
            throw UnsupportedOperationException()  // FIXME. Is this possible?
        }
    }

    private inner class StringBasedLookup internal constructor(private val xmlContent: String?) : XmlLookupType {

        init {
            require(xmlContent != null && !xmlContent.isEmpty()) {"Cannot parse empty XML content"}
        }

        @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
        override fun document(): Document {
            return rememberDocument(docParse.parse(docParse.docBuilder, xmlContent))
        }

        @Throws(SAXException::class, IOException::class, ParserConfigurationException::class)
        override fun validDocument(): Document {
            return rememberDocument(docParse.validatingDocBuilder.parse(InputSource(StringReader(xmlContent))))
        }

        @Throws(SAXException::class, IOException::class, ParserConfigurationException::class)
        override fun validDocument(inSchemaSource: Source): Document {
            return rememberDocument(docParse.parse(docParse.getExternalValidatingDocBuilder(inSchemaSource), xmlContent))
        }
    }

    private inner class FileBasedLookup internal constructor(private val file: File) : XmlLookupType {

        @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
        override fun document(): Document {
            return rememberDocument(docParse.parse(docParse.docBuilder, file))
        }

        @Throws(SAXException::class, IOException::class, ParserConfigurationException::class)
        override fun validDocument(): Document {
            return rememberDocument(docParse.validatingDocBuilder.parse(file))
        }

        @Throws(SAXException::class, IOException::class, ParserConfigurationException::class)
        override fun validDocument(inSchemaSource: Source): Document {
            return rememberDocument(docParse.parse(docParse.getExternalValidatingDocBuilder(inSchemaSource), file))
        }
    }

    private inner class HttpBasedLookup internal constructor(private val getReq: HttpUriRequest) : XmlLookupType {

        @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
        override fun document(): Document {
            return rememberDocument(docParse.parse(getReq))
        }

        @Throws(SAXException::class, IOException::class, ParserConfigurationException::class)
        override fun validDocument(): Document {
            return rememberDocument(docParse.parse(docParse.validatingDocBuilder, getReq))
        }

        @Throws(SAXException::class, IOException::class, ParserConfigurationException::class)
        override fun validDocument(inSchemaSource: Source): Document {
            return rememberDocument(docParse.parse(docParse.getExternalValidatingDocBuilder(inSchemaSource), getReq))
        }
    }

    companion object {
        private val XML_HANDLER_PATTERN = Pattern.compile("xml" + AssertLookups.METHOD_HANDLER_PATTERN_STR, Pattern.CASE_INSENSITIVE)
    }
}
