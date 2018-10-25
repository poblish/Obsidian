package org.hiatusuk.obsidian.web.server.jetty.delegates

import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.server.handler.ContextHandlerCollection

class WebContexts {

    // Wouldn't normally lazy-init, but if set, a Quit event could call us and load loads of Jetty classes, even though no apps to be deployed
    private var handlerColl: ContextHandlerCollection? = null

    @Throws(Exception::class)
    fun addContext(prefix: String, handler: Handler) {
        val ctxtHandler = ContextHandler(prefix)
        ctxtHandler.handler = handler
        addContext(ctxtHandler)
    }

    @Throws(Exception::class)
    fun addContext(handler: Handler) {
        getHandler().addHandler(handler)
        handler.start()
    }

    fun getHandler(): ContextHandlerCollection {
        if (handlerColl == null) {
            handlerColl = ContextHandlerCollection()
        }
        return handlerColl!!
    }
}
