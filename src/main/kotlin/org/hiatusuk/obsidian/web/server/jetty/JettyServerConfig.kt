package org.hiatusuk.obsidian.web.server.jetty

import com.google.common.base.MoreObjects
import java.util.*


class JettyServerConfig(val port: Int) {
    var defaultDescriptor = Optional.empty<String>()
        set(desc) {
            field = requireNotNull(desc)
        }
    private val beanDefs = arrayListOf<Map<String, Any>>()

    fun addBeans(inBeans: List<Map<String, Any>>) {
        beanDefs.addAll(inBeans)
    }

    fun beanDefs(): Iterable<Map<String, Any>> {
        return beanDefs
    }

    override fun toString(): String {
        return MoreObjects.toStringHelper(this).omitNullValues()
                .add("port", port)
                .add("defaultDescriptor", this.defaultDescriptor.orElse(null))
                .add("beanDefs", if (beanDefs.isEmpty()) null else beanDefs).toString()
    }

    companion object {
        const val DEFAULT_SERVER_PORT = 8080
    }
}
