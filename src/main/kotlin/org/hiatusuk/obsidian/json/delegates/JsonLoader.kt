package org.hiatusuk.obsidian.json.delegates

import com.codahale.metrics.MetricRegistry
import com.jayway.jsonpath.*
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.json.JsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import com.jayway.jsonpath.spi.mapper.MappingProvider
import dagger.Lazy
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpUriRequest
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import java.io.IOException
import java.util.*
import javax.inject.Inject

@ScenarioScope
class JsonLoader @Inject
internal constructor(private val syncHttpClient: Lazy<HttpClient>,
                     private val varCtxt: VariablesContext,
                     private val exceptions: RuntimeExceptions,
                     private val metrics: MetricRegistry) {

    @Throws(IOException::class)
    fun loadDocument(getReq: HttpUriRequest): DocumentContext {
        setConfigDefaults()

        metrics.timer("JSON.loadFromUrl").time().use {
            getReq.setHeader("Accept", "application/json")
            return recordDocument( JsonPath.parse(syncHttpClient.get().execute(getReq).entity.content) )
        }
    }

    fun loadDocument(inContent: String): DocumentContext {
        setConfigDefaults()

        metrics.timer("JSON.loadFromString").time().use { return recordDocument( JsonPath.parse(inContent) ) }
    }

    private fun recordDocument(doc : DocumentContext) : DocumentContext {
        varCtxt.store("lastJson", LastJsonLookupStateBean(doc, this))
        return doc
    }

    internal fun findString(doc: DocumentContext, propertyName: String, path: String): String {
        when (propertyName) {
            "exists" -> {
                val response = doc.read(path, Any::class.java) ?: return "false"

                if (response is List<*> && response.isEmpty()) {
                    return "false"
                }

                return if (response is String && response.isEmpty()) {
                    "false"
                } else "true"

            }
            "node" -> {
                val nodeResp = doc.read(path, Any::class.java)
                return if (nodeResp == null) "" else (nodeResp as? List<*>)?.first()?.toString() ?: nodeResp.toString()

            }
            "nodes" -> {
                val ref = object : TypeRef<Array<String>>() {}
                val allTexts = arrayListOf<String?>()
                val sa = doc.read(path, ref) ?: return "null"
                allTexts.addAll(Arrays.asList(*sa))
                return allTexts.joinToString(separator = ",", transform = {it ?: ""})
            }
            else -> throw exceptions.runtime("Unknown Assert property: $propertyName")
        }
    }

    private fun setConfigDefaults() {
        Configuration.setDefaults(object : Configuration.Defaults {  // FIXME Factor out

            private val jsonProvider = JacksonJsonProvider()

            override fun jsonProvider(): JsonProvider {
                return jsonProvider
            }

            override fun options(): Set<Option> {
                return EnumSet.of(Option.SUPPRESS_EXCEPTIONS)
            }

            override fun mappingProvider(): MappingProvider {
                return JacksonMappingProvider()
            }
        })
    }

    @Suppress("unused")  // Accessed via EL
    internal class LastJsonLookupStateBean(private val doc: DocumentContext, private val loader: JsonLoader) {

        fun node(path: String) : String {
            return loader.findString(doc, "node", path)
        }

        fun nodes(path: String) : String {
            return loader.findString(doc, "nodes", path)
        }

        fun exists(path: String) : String {
            return loader.findString(doc, "exists", path)
        }

        // Called when bean is accessed directly via VarContext, e.g. via 'echo'
        override fun toString() : String {
            return "Dumping last JSON request...\n" +
                    "Document : $doc\n"
        }
    }
}
