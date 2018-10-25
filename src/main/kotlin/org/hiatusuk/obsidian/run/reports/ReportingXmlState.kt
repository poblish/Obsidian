package org.hiatusuk.obsidian.run.reports

import com.codahale.metrics.MetricRegistry
import com.google.common.io.Files
import org.hiatusuk.obsidian.cases.Case
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.slf4j.Logger
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.xml.sax.SAXException
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

@ScenarioScope
class ReportingXmlState @Inject
constructor(private val metrics: MetricRegistry,
            private val exceptions: RuntimeExceptions,
            private val log: Logger) {

    private var doc: Document? = null
    private var xpath: XPath? = null
    private var testcasesFile: File? = null
    private var modified: Boolean = false

    @Throws(IOException::class)
    fun load(testcasesFile: File) {

        check(!modified) {"Attempt to reset XML State before document changes have been persisted."}

        this.testcasesFile = checkNotNull(testcasesFile)

        metrics.timer("DOM.parse").time().use {
            try {
                val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

                doc = docBuilder.parse(testcasesFile)
                log.debug("XML: Loaded Document")

                xpath = XPathFactory.newInstance().newXPath()
            } catch (e1: ParserConfigurationException) {
                throw exceptions.runtime(e1)
            } catch (e2: SAXException) {
                throw exceptions.runtime(e2)
            }
        }
    }

    fun storeResult(inCase: Case, newValue: String) {
        try {
            if (inCase.subtypes.isEmpty()) {
                addOrUpdateResult(inCase, newValue, "/cases/case[@id='" + inCase.id + "']")
            } else {
                for (eachSubtype in inCase.subtypes) { // FIXME Must support empty subtypes, i.e. 'Overall'
                    addOrUpdateResult(inCase, newValue, "/cases/case[@id='" + inCase.id + "']/subtype[@id='" + eachSubtype + "']")
                }
            }
        } catch (e: XPathExpressionException) {
            throw exceptions.runtime(e)
        }

    }

    @Throws(XPathExpressionException::class)
    private fun addOrUpdateResult(inCase: Case, newValue: String, inXpathQuery: String) {
        val resultNode = xpath!!.evaluate("$inXpathQuery/result", doc!!, XPathConstants.NODE) as Node?
        if (resultNode != null) { // Existing node
            val existingText = resultNode.textContent

            if (existingText == null || existingText != newValue) {
                resultNode.textContent = newValue
                log.trace("XML: Modified node: $resultNode")
                modified = true
            }
        } else {
            val parentNode = xpath!!.evaluate(inXpathQuery, doc, XPathConstants.NODE) as Node? ?:
                throw exceptions.runtime("No entry for $inXpathQuery found in test cases XML. Please add it.")

            val newResultNode = doc!!.createElement("result")
            newResultNode!!.textContent = newValue
            parentNode.appendChild(newResultNode)
            log.trace("XML: Added node: $newResultNode")
            modified = true
        }
    }

    fun outputReport(generatedReportsDir: File) {

        val transFactory = TransformerFactory.newInstance()

        if (modified) {
            metrics.timer("DOM.rewrite").time().use {
                try {
                    val xmlTransformer = transFactory.newTransformer()
                    xmlTransformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")

                    log.info("XML: Persisting state to: " + testcasesFile!!)

                    xmlTransformer.transform(DOMSource(doc), StreamResult(FileOutputStream(testcasesFile!!)))

                    modified = false
                } catch (e: TransformerException) {
                    throw exceptions.runtime(e)
                }
            }
        }

        metrics.timer("XSLT.output").time().use {
            try {
                val xmlTransformer = transFactory.newTransformer(StreamSource(FileReader("src/test/resources/templates/report_output_generator.xslt")))

                val repOutputFile = File(generatedReportsDir, "project_reports/index.html")
                Files.createParentDirs(repOutputFile)

                xmlTransformer.transform(DOMSource(doc), StreamResult(FileOutputStream(repOutputFile)))
            } catch (e: TransformerException) {
                throw exceptions.runtime(e)
            }
        }
    }
}
