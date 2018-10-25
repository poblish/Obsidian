package org.hiatusuk.obsidian.xml.delegates

import com.codahale.metrics.MetricRegistry
import dagger.Lazy
import java.io.File
import java.io.IOException
import java.io.StringReader
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.Source
import javax.xml.validation.SchemaFactory
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpUriRequest
import org.slf4j.Logger
import org.w3c.dom.Document
import org.xml.sax.InputSource
import org.xml.sax.SAXException

class DocumentParser @Inject
constructor(private val syncHttpClient: Lazy<HttpClient>,
            private val xmlns: XmlNamespaces,
            private val errorHandler: XmlErrorHandler,
            private val metrics: MetricRegistry,
            private val log: Logger) {

    val docBuilder: DocumentBuilder
        @Throws(ParserConfigurationException::class)
        get() {
            val dbf = DocumentBuilderFactory.newInstance()
            dbf.isNamespaceAware = xmlns.usingNamespaces()

            val builder = dbf.newDocumentBuilder()
            builder.setErrorHandler(errorHandler)
            return builder
        }

    // xmlns.usingNamespaces() );
    val validatingDocBuilder: DocumentBuilder
        @Throws(ParserConfigurationException::class)
        get() {
            val dbf = DocumentBuilderFactory.newInstance()
            dbf.isValidating = true
            dbf.isNamespaceAware = true
            dbf.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema")

            val builder = dbf.newDocumentBuilder()
            builder.setErrorHandler(errorHandler)
            return builder
        }

    @Throws(IOException::class, SAXException::class)
    fun parse(db: DocumentBuilder, getReq: HttpUriRequest): Document {
        log.info("XML Requesting '{}'", getReq.uri)

        metrics.timer("DOM.loadFromUrl").time().use {
            // log.info("XML = {}", CharStreams.toString( new InputStreamReader(syncHttpClient.get().execute(getReq).getEntity().getContent(), "UTF-8" ) ));
            return db.parse(syncHttpClient.get().execute(getReq).entity.content)
        }
    }

    @Throws(IOException::class, SAXException::class)
    fun parse(db: DocumentBuilder, localFile: File): Document {
        // log.info("XML Requesting '{}'", getReq.getURI());

        metrics.timer("DOM.loadFromFile").time().use {
            // log.info("XML = {}", CharStreams.toString( new InputStreamReader(syncHttpClient.get().execute(getReq).getEntity().getContent(), "UTF-8" ) ));
            return db.parse(localFile)
        }
    }

    @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
    fun parse(getReq: HttpUriRequest): Document {
        val db = docBuilder

        log.info("XML Requesting '{}'", getReq.uri)

        metrics.timer("DOM.loadFromUrl").time().use {
            // log.info("XML = {}", CharStreams.toString( new InputStreamReader(syncHttpClient.get().execute(getReq).getEntity().getContent(), "UTF-8" ) ));
            return db.parse(syncHttpClient.get().execute(getReq).entity.content)
        }
    }

    @Throws(IOException::class, SAXException::class)
    fun parse(db: DocumentBuilder, content: String?): Document {
        log.debug("XML Parsing existing content...") //, getReq.getURI());

        metrics.timer("DOM.loadFromString").time().use {
            // log.info("XML = {}", CharStreams.toString( new InputStreamReader(syncHttpClient.get().execute(getReq).getEntity().getContent(), "UTF-8" ) ));
            return db.parse(InputSource(StringReader(content)))
        }
    }

    @Throws(SAXException::class, ParserConfigurationException::class)
    fun getExternalValidatingDocBuilder(inSchemaSource: Source): DocumentBuilder {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isValidating = false
        factory.isNamespaceAware = true

        val schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema")

        factory.schema = schemaFactory.newSchema(arrayOf(inSchemaSource))

        val builder = factory.newDocumentBuilder()
        builder.setErrorHandler(errorHandler)

        return builder
    }
}
