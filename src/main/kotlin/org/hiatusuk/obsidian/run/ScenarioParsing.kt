package org.hiatusuk.obsidian.run

import org.hiatusuk.obsidian.config.ConfigUtils
import org.hiatusuk.obsidian.run.exceptions.MalformedScenarioException
import org.hiatusuk.obsidian.utils.StringUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.Map.Entry

object ScenarioParsing {

    const val UNTITLED_SCENARIO_PREFIX = "Untitled-"

    private val INCLUDE_MATCHER = Pattern.compile("^(" + ".*\\b" + "include: (\\S+))", Pattern.MULTILINE)

    val SETTERS: (Entry<String,Any>) -> Boolean = { e -> e.value is Map<*, *> && "set" == e.key }
    val LISTS: (Entry<String,Any>) -> Boolean = { e -> e.value is List<*> }

    private val LOG = LoggerFactory.getLogger("Scenario")

    // Purpose is basically to strip out all unusable (non-Map) elements from the scenario, as these are unusable
    fun stripIgnorableScenarioElements(inScenarioElementFromYaml: Entry<String, Any>): List<Map<String, Any>> {
        val rawList = inScenarioElementFromYaml.value as List<Any>
        return rawList
                .filter(StatefulSkippingFilter())
                .map { elem -> elem as Map<String, Any> }
    }

    private class StatefulSkippingFilter : (Any?) -> Boolean {
        private var skipped: Boolean = false

        override fun invoke(scenarioElem: Any?): Boolean {
            if (skipped || scenarioElem == null || scenarioElem !is Map<*, *>) {
                return false
            }

            if (shouldSkipScenario(scenarioElem as Map<String, Any>)) {
                LOG.info(": <skipped>")
                skipped = true
                return false
            }

            return true
        }
    }

    fun shouldSkipScenario(inMap: Map<String, Any>): Boolean {
        if (!inMap.containsKey("skip")) {
            return false
        }

        // Our approach is: if "skip" is found, we always skip unless we get something YAML maps to Boolean FALSE
        val value = inMap["skip"]
        return value as? Boolean ?: true

    }

    fun elements(inScenarioObjFromYaml: Any,
                 inScenarioIdx: Int): Collection<Entry<String, Any>> {
        return elements(inScenarioObjFromYaml, inScenarioIdx) { true }
    }

    fun elements(inScenarioObjFromYaml: Any,
                 inScenarioIdx: Int,
                 filter: (Entry<String, Any>) -> Boolean): Collection<Entry<String, Any>> {
        if (inScenarioObjFromYaml is List<*>) {
            val namedScenario = HashMap<String, Any>()
            namedScenario[UNTITLED_SCENARIO_PREFIX + inScenarioIdx] = ConfigUtils.mapElements(inScenarioObjFromYaml)
            return filterMapEntries(namedScenario, filter)
        }

        if (inScenarioObjFromYaml !is Map<*, *>) {
            throw MalformedScenarioException(inScenarioObjFromYaml)
        }

        return filterMapEntries(inScenarioObjFromYaml as Map<String, Any>, filter)
    }

    private fun filterMapEntries(map: Map<String, Any>, filter: (Entry<String, Any>) -> Boolean): Collection<Entry<String, Any>> {
        return map.entries.filter(filter)
    }

    @Throws(IOException::class)
    fun handleYamlIncludes(inInput: RunInput): String {
        return handleYamlIncludes(inInput.text, inInput)
    }

    @Throws(IOException::class)
    fun handleYamlIncludes(inOriginalYaml: String, inInput: RunInput): String {
        var yamlStr = inOriginalYaml

        while (true) {
            // So you can have "^include: blah" as a declaration, or "- include: foo" within a scenario
            val m = INCLUDE_MATCHER.matcher(yamlStr)
            if (!m.find()) {
                break
            }

            val includedStr = inInput.sibling(m.group(2)).text
            yamlStr = StringUtils.replace(yamlStr, m.group(1), includedStr + "\r")
        }

        return yamlStr
    }
}
