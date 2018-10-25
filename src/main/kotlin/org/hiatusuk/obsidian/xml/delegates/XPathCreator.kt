package org.hiatusuk.obsidian.xml.delegates

import javax.inject.Inject
import javax.xml.namespace.NamespaceContext
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathFactory

class XPathCreator @Inject
constructor(private val xmlns: XmlNamespaces) {

    fun newXPath(): XPath {
        return newXPath(xmlns.newNamespaceContext())
    }

    companion object {

        private val XPATH_F = XPathFactory.newInstance()

        // See: http://www.edankert.com/defaultnamespaces.html
        fun newXPath(nsCtxt: NamespaceContext): XPath {
            val xpath = XPATH_F.newXPath()
            xpath.namespaceContext = nsCtxt
            return xpath
        }
    }
}
