package org.hiatusuk.obsidian.xml.lookup

import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.xml.delegates.XPathCreator
import org.hiatusuk.obsidian.xml.delegates.XmlErrorHandler
import org.slf4j.Logger
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.SAXException
import java.io.IOException
import java.net.URL
import javax.inject.Inject
import javax.xml.transform.stream.StreamSource
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants.*
import javax.xml.xpath.XPathException
import javax.xml.xpath.XPathExpressionException

@ScenarioScope
class XmlLookupsRequester @Inject
constructor(private val xpaths: XPathCreator,
            private val errorHandler: XmlErrorHandler,
            private val exceptions: RuntimeExceptions,
            private val log: Logger) {

    @Throws(IOException::class, SAXException::class)
    fun getLookupValue(inLookupType: XmlLookupType, propertyName: String, propertyQuery: String): String {
        try {
            val xpath = xpaths.newXPath()

            val targetText: String

            when (propertyName) {
                "string" -> targetText = (checkNotNull(xpath.evaluate(propertyQuery, inLookupType.document(), STRING), "XPath not found: $propertyQuery")) as String
                "node" -> targetText = ((checkNotNull(xpath.evaluate(propertyQuery, inLookupType.document(), NODE), "XPath not found: $propertyQuery")) as Node).textContent

                "nodes" -> targetText = join(addNodeValues(arrayListOf(), xpath, inLookupType.document(), propertyQuery))

                "nodesSorted" -> targetText = join(addNodeValues(sortedSetOf(), xpath, inLookupType.document(), propertyQuery))

                "count" -> {
                    val cnl = getMatchingNodeList(inLookupType.document(), xpath, propertyQuery)
                    targetText = Integer.toString(cnl?.length ?: 0)
                }
                "validate" -> {
                    targetText = try {
                        errorHandler.clear()  // YuK!!

                        if (propertyQuery.isEmpty()) {
                            log.info("< Validating XML against internal schema >")
                            inLookupType.validDocument()
                        } else {
                            log.info("< Validating XML against external schema: {} >", propertyQuery)
                            inLookupType.validDocument(StreamSource(URL(propertyQuery).openStream(), propertyQuery))
                        }

                        errorHandler.message()  // YuK!!
                    } catch (e: Exception) {
                        e.message!!
                    }
                }
                else -> throw exceptions.runtime("Unknown Assert property: $propertyName")
            }

            return targetText
        } catch (e: XPathException) {
            throw exceptions.runtime(e)
        }
    }

    private fun join(c: Collection<String?>): String {
        return c.joinToString(separator = ",", transform = {it ?: ""})
    }

    @Throws(XPathExpressionException::class)
    private fun addNodeValues(ioColl: MutableCollection<String>, xpath: XPath, doc: Document, propertyQuery: String): Collection<String> {
        val nl = checkNotNull(getMatchingNodeList(doc, xpath, propertyQuery) as NodeList, "XPath not found: $propertyQuery")
        for (i in 0 until nl.length) {
            ioColl.add(nl.item(i).textContent)
        }
        return ioColl
    }

    fun <T> checkNotNull(reference: T?, errorMsg: String?): T {
        if (reference == null) {
            throw exceptions.nullPointer(errorMsg ?: "")
        }
        return reference
    }

    @Throws(XPathExpressionException::class)
    private fun getMatchingNodeList(doc: Document, xpath: XPath, propertyQuery: String): NodeList? {
        return xpath.evaluate(propertyQuery, doc, NODESET) as NodeList
    }
}
