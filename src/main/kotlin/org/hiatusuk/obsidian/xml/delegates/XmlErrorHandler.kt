package org.hiatusuk.obsidian.xml.delegates

import org.hiatusuk.obsidian.di.ScenarioScope
import org.xml.sax.ErrorHandler
import org.xml.sax.SAXParseException
import javax.inject.Inject

// Somehow this has become stateful...
@ScenarioScope
class XmlErrorHandler @Inject
constructor() : ErrorHandler {

    private var message = "true"

    // There's no "OK" state
    fun clear() {
        message = "true"
    }

    override fun warning(e: SAXParseException) {
        message = "**** WARN: " + e.message
    }

    override fun error(e: SAXParseException) {
        message = "**** ERROR: " + e.message
    }

    override fun fatalError(e: SAXParseException) {
        message = "**** FATAL: " + e.message
    }

    fun message(): String {
        return message
    }
}
