package org.hiatusuk.obsidian.xml.delegates

import org.hiatusuk.obsidian.config.FeatureConfiguration
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.events.AfterScenario
import java.util.*
import javax.inject.Inject
import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext

@ScenarioScope
@FeatureConfiguration("xmlns")
class XmlNamespaces
@Inject constructor(private val varCtxt : VariablesContext) {

    private val prefixUrlMappings = HashMap<String, String>()
    private var namespacesUsed: Boolean = false

    fun configure(inPayload: Map<String, String>) {
        this.prefixUrlMappings.putAll(inPayload)
        this.namespacesUsed = true

        varCtxt.store("obsidian_xmlns", NsWrapperForEL(newNamespaceContext()))
    }

    @AfterScenario
    fun resetAfterScenario() {
        this.prefixUrlMappings.clear()
        this.namespacesUsed = false
    }

    fun usingNamespaces(): Boolean {
        return this.namespacesUsed
    }

    fun newNamespaceContext(): NamespaceContext {
        return newNamespaceContext(prefixUrlMappings)
    }

    companion object {

        @Suppress("unused")  // For exposing to EL
        private class NsWrapperForEL(private val nsContext: NamespaceContext) {

            fun namespaceURI(prefix: String): String? {
                return nsContext.getNamespaceURI(prefix)
            }

            fun prefix(namespaceURI: String): String? {
                return nsContext.getPrefix(namespaceURI)
            }

            fun prefixes(namespaceURI: String): String {
                return nsContext.getPrefixes(namespaceURI).asSequence().joinToString()
            }
        }

        // See: http://www.edankert.com/defaultnamespaces.html
        private fun newNamespaceContext(inMappings: Map<String, String>): NamespaceContext {
            return object : NamespaceContext {
                override fun getNamespaceURI(prefix: String): String? {
                    return if (inMappings.containsKey(prefix)) {
                        inMappings[prefix]
                    } else XMLConstants.NULL_NS_URI
                }

                override fun getPrefix(namespaceURI: String): String? {
                    for ((key, value) in inMappings) {
                        if (value == namespaceURI) {
                            return key
                        }
                    }
                    return null
                }

                override fun getPrefixes(namespaceURI: String): Iterator<String> {
                    val prefixes = ArrayList<String>()

                    for ((key, value) in inMappings) {
                        if (value == namespaceURI) {
                            prefixes.add(key)
                        }
                    }

                    return prefixes.iterator()
                }
            }
        }
    }
}
