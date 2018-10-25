package org.hiatusuk.obsidian.xml.lookup

import org.w3c.dom.Document
import org.xml.sax.SAXException
import java.io.IOException
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.Source

interface XmlLookupType {

    @Throws(IOException::class, SAXException::class, ParserConfigurationException::class)
    fun document(): Document

    @Throws(SAXException::class, IOException::class, ParserConfigurationException::class)
    fun validDocument(): Document

    @Throws(SAXException::class, IOException::class, ParserConfigurationException::class)
    fun validDocument(inSchemaSource: Source): Document
}