package org.hiatusuk.obsidian.asserts.lookups

import com.google.common.annotations.VisibleForTesting
import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.RunProperties
import org.hiatusuk.obsidian.run.delegates.ExtractionHandler
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.utils.ParseUtils
import org.hiatusuk.obsidian.web.selenium.cmd.DomDump
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.hiatusuk.obsidian.web.selenium.find.FindRequest
import org.hiatusuk.obsidian.web.selenium.find.InvalidCssException
import org.hiatusuk.obsidian.web.selenium.find.filters.Filter
import org.hiatusuk.obsidian.web.selenium.utils.WebDriverUtils.elementToString
import org.slf4j.Logger
import java.io.IOException
import javax.inject.Inject

@ScenarioScope
class AssertLookups @Inject
constructor(@VisibleForTesting val lookupsRegistry: AssertLookupsRegistry,
            private val finders: ElementFinders,
            private val web: WebState,
            private val ctxt: VariablesContext,
            private val runProps: RunProperties,
            private val exceptions: RuntimeExceptions,
            private val log: Logger) {

    // Allow overrides
    private fun findWith(inSemSelector: String): FindRequest {
        return finders.with(inSemSelector)
    }

    @Throws(IOException::class)
    fun getAssertionTargets(assertSpecs: AssertSpecs): Collection<AssertTarget> {
        // Identify correct target (validate has already happened)
        val targetIdentifier = assertSpecs.getString("that") ?: ""

        assertSpecs.remove("that")

        /////////////////////////////////////////////////////  Handle assert: {that: extract(...), matches: ...}

        val extractResult = ExtractionHandler().matchString(targetIdentifier)

        if (extractResult.matched()) {
            val targetPrefix = if (targetIdentifier.startsWith("!")) "!" else ""  // Pass on the ! syntax to target if set on identifier

            // Now the ugly business of reconstructing the list and its contents...
            return obtainAssertionTargets(assertSpecs, targetPrefix + extractResult.contents!!)
                    .map { AssertTarget(extractResult.extractString(it.text)) }
        }

        return obtainAssertionTargets(assertSpecs, targetIdentifier)
    }

    @Throws(IOException::class)
    private fun obtainAssertionTargets(assertSpecs: AssertSpecs, targetIdentifier: String): Collection<AssertTarget> {

        if (ParseUtils.parseNumber(targetIdentifier) != null) {  // Is numeric?
            return LookupUtils.singleTarget(targetIdentifier)
        }

        /////////////////////////////////////////////  New ! syntax for string-on-string comparisons

        if (targetIdentifier.startsWith("!")) {
            return LookupUtils.singleTarget(targetIdentifier.substring(1))
        }

        /////////////////////////////////////////////////////

        val registryResult = lookupsRegistry.lookupValueForTargetIdentifier(targetIdentifier, assertSpecs)
        if (registryResult != null) {
            return registryResult
        }

        /////////////////////////////////////////////////////

        val filters = arrayListOf<Filter>()

        val customCssFilters = LinkedHashMap<String, String>()

        for ((key, value1) in assertSpecs.entrySet()) {
            val value = value1.toString()

            when (key) {
                "withClass" -> filters.add(Filter("class", value))
                "with", "withAttr" -> if (value.startsWith("style.")) {
                    // Custom CSS matching... See http://stackoverflow.com/a/8426901/954442 for why we can't use Filter
                    val eqPos = value.indexOf('=')
                    customCssFilters[value.substring(6, eqPos).trim()] = ctxt.resolve(value.substring(eqPos + 1).trim() as String?)!!
                } else {
                    val eqPos = value.indexOf('=')
                    filters.add(Filter(value.substring(0, eqPos).trim(), ctxt.resolve(value.substring(eqPos + 1).trim() as String?)!!))
                }
            }
        }

        if (runProps.isDebug && (!filters.isEmpty() || !customCssFilters.isEmpty())) {
            log.debug("Using inline filters: {}, post-CSS filters: {}", filters, customCssFilters)
        }

        ///////////////////////////////////////////////////////////////////////////  Test plausible web element identifier...

        val targetReq: FindRequest

        try {
            targetReq = findWith(targetIdentifier)
        } catch (e: InvalidCssException) {
            // We thought it might be a CSS path, it wasn't. So just return the value as-is.
            return LookupUtils.singleTarget(targetIdentifier)
        }

        ///////////////////////////////////////////////////////////////////////////

        val targets = targetReq.all(*filters.toTypedArray())

        if (targets.isEmpty()) {
            if (filters.size > 0) {
                throw exceptions.noSuchWebElement("Could not find $targetReq with filters: $filters")
            }

            throw exceptions.noSuchWebElement("Could not find $targetReq")
        }

        ///////////////////////////////////////////////////////////////////////////  Custom CSS matching...

        val firstMatch = targets.first()

        for ((key, value) in customCssFilters) {
            val rawCssVal = firstMatch.getCssValue(key) ?: ""

            val actualFilterVals = if (rawCssVal.startsWith("rgb(") || rawCssVal.startsWith("rgba(")) {  // FIXME Ugh, vile
                arrayListOf(rawCssVal)
            } else {
                // For each one we're looking for, what values does the actual element have?
                rawCssVal.split(' ').map { it.trim() }
            }

            // No match? Create a helpful error message...
            if (!actualFilterVals.contains(value)) {
                val targetStrs = targets.map { input -> DomDump.toString(web.driver, input) }

                throw exceptions.runtime("CSS property '$key' doesn't have value '$value' for $targetStrs, actually: $actualFilterVals")
            }
        }

        if (targetIdentifier == "*") {
            return firstMatch.text.split('\n').map { AssertTarget(it) }  // *All* elems, stitched together
        }

        return targets.map { AssertTarget( elementToString(it) ) }
    }

    companion object {
        const val SIMPLE_METHOD_HANDLER_PATTERN_STR = "\\((.*)\\)"
        private const val OBJ_PROPERTIES_HANDLER_PATTERN_STR = "\\.([A-Z]*)\\((.*)\\)"
        const val METHOD_HANDLER_PATTERN_STR = SIMPLE_METHOD_HANDLER_PATTERN_STR + OBJ_PROPERTIES_HANDLER_PATTERN_STR
    }
}
