package org.hiatusuk.obsidian.asserts.cmd

import com.google.common.base.Charsets
import com.natpryce.hamkrest.*
import com.natpryce.hamkrest.assertion.assert
import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.ValidationException
import org.hiatusuk.obsidian.asserts.lookups.AssertLookups
import org.hiatusuk.obsidian.asserts.lookups.AssertLookupsRegistry
import org.hiatusuk.obsidian.asserts.lookups.AssertSpecs
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.cmd.api.Validate
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.RunProperties
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.run.state.LineNumbersState
import org.hiatusuk.obsidian.utils.ParseUtils
import org.hiatusuk.obsidian.utils.StringUtils.collapseWhitespace
import org.hiatusuk.obsidian.utils.StringUtils.toJavaSyntax
import org.hiatusuk.obsidian.utils.TerminalColours
import org.hiatusuk.obsidian.web.selenium.exceptions.NoSuchWebElementException
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.hiatusuk.obsidian.web.selenium.find.FindRequest
import org.openqa.selenium.NoSuchElementException
import org.slf4j.Logger
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlin.collections.Map.Entry

@ScenarioScope
@Command("assert")
class AssertCmd @Inject
constructor(// For overrides
        private val lookups: AssertLookups,
        private val finders: ElementFinders,
        private val ctxt: VariablesContext,
        private val lookupsRegistry: AssertLookupsRegistry,  // FIXME Only for short-form
        private val exceptions: RuntimeExceptions,
        private val runProps: RunProperties,
        private val lineNumbers: LineNumbersState,
        private val log: Logger) : CommandIF {

    @Validate
    fun validate(inCmd: CommandSpec) {
        validatePayload(inCmd)
    }

    @Throws(IOException::class)
    override fun run(inCmd: CommandSpec) {
        doAssert(inCmd)
    }

    @Throws(IOException::class)
    fun doAssert(inCmd: CommandSpec) {

        if (inCmd.hasString()) {
            // Short-form: no matchers, so use for true/false lookup values and web element (non-) existence only

            val rawTargetId = inCmd.string
            val adjustedTargetId = if (rawTargetId.startsWith("!")) rawTargetId.substring(1) else rawTargetId

            if (adjustedTargetId.equals("true", ignoreCase = true) ||
                adjustedTargetId.equals("not false", ignoreCase = true)) {
                // Special case. Clearly no kind of web identifier. Assume boolean true = OK
                return
            } else if (adjustedTargetId.equals("false", ignoreCase = true) || adjustedTargetId.equals("not true", ignoreCase = true)) {
                // Special case. Clearly no kind of web identifier. Assume boolean false = FAIL
                throw exceptions.runtime("Simple assert returned <false>")
            }

            /////////////////  FIXME Short-form needs a big refactor. However, it first needs to work...

            val isNot = adjustedTargetId.startsWith("not ")
            val targetIdentifier = if (isNot) adjustedTargetId.substring(4) else adjustedTargetId

            if (ParseUtils.parseNumber(targetIdentifier) != null) {  // Is numeric?
                throw exceptions.runtime("Numeric values cannot be used with simple asserts")
            }

            // Create 'payload'
            val assertSpecs = AssertSpecs( mapOf("that" to inCmd.string), /* Short-form */ true)

            val registryResult = lookupsRegistry.lookupValueForTargetIdentifier(targetIdentifier, assertSpecs)
            if (registryResult != null) {
                for (eachVal in getTargetValues(registryResult)) {

                    if (eachVal.equals("true", ignoreCase = true)) {
                        if (isNot) {
                            throw exceptions.runtime("Simple assert unexpectedly returned <true> at least once")
                        }
                        // else OK
                    } else if (eachVal.equals("false", ignoreCase = true)) {
                        if (!isNot) {
                            throw exceptions.runtime("Simple assert unexpectedly returned <false> at least once")
                        }
                        // else OK
                    } else {
                        throw exceptions.runtime("Simple assert returned <$eachVal> not <true|false>")
                    }
                }
            } else if (isNot) {
                try {
                    val targetReq = findWith(targetIdentifier)

                    if (runProps.isLogAssertions) {
                        log.info("< Assert: check {}{}{} doesn't exist >", TerminalColours.assertClauseColour(), targetReq, TerminalColours.reset())
                    }

                    targetReq.first()
                    throw exceptions.runtime("Didn't expect to find: $targetReq")
                } catch (e: NoSuchElementException) {
                    // Correct - this is what we want.
                } catch (e: NoSuchWebElementException) {
                }

            } else {
                val targetReq = findWith(targetIdentifier)

                if (runProps.isLogAssertions) {
                    log.info("< Assert: check {}{}{} exists >", TerminalColours.assertClauseColour(), targetReq, TerminalColours.reset())
                }

                targetReq.first()
            }// Fall back to Web element finding...
        }
        else
            /* Long-form, property-based */ {

            // We shallow clone the Map, because modifying in-place could break for: loop
            val assertSpecs = AssertSpecs(inCmd.asMap())

            var targetTexts = getTargetValues(lookups.getAssertionTargets(assertSpecs))

            assertSpecs.remove("with")
            assertSpecs.remove("withAttr")
            assertSpecs.remove("withClass")
            assertSpecs.remove("ignore404")  // Ugh

            val ignoreReplacements = arrayListOf<String>()

            // Handle 'ignore'
            for ((key, value) in assertSpecs.entrySet()) {
                if (key != "ignore") {
                    continue
                }

                if (value is Map<*, *>) {
                    val map = value as Map<String, Any>
                    ignoreReplacements.addAll( /* All ignore: matchers */map.keys)
                } else if (value is String) {
                    ignoreReplacements.add(value)
                }
            }

            targetTexts = removeIgnorableText(targetTexts, ignoreReplacements) as Collection<String>

            // Now do assertions
            for (eachProperty in assertSpecs.entrySet()) {
                if (eachProperty.key == "ignore") {
                    continue  // Already processed!
                }

                if (eachProperty.value is Map<*,*>) {
                    val resolvedExpectationVals = ArrayList<String?>()

                    for (eachKey in (eachProperty.value as Map<Any?, Any>).keys.filter { it != null }) {
                        resolvedExpectationVals.add( obtainExpectedContent( ctxt.resolve(eachKey.toString() as String?) ))
                    }

                    validateAssertProperty(eachProperty, targetTexts, removeIgnorableText(resolvedExpectationVals, ignoreReplacements) as Collection<String>)
                } else {
                    val resolvedExpectationVal = obtainExpectedContent(ctxt.resolve( eachProperty.value?.toString() ?: "" as String? ))  // null => ""
                    validateAssertProperty(eachProperty, targetTexts, listOf(removeIgnorableText(resolvedExpectationVal, ignoreReplacements)))
                }
            }

            if (assertSpecs.isEmpty) {
                assert.that(targetTexts.size, !equalTo(0))
            }
        }
    }

    // Allow overrides
    private fun findWith(inSemSelector: String): FindRequest {
        return finders.with(inSemSelector)
    }

    private fun isNumericComparison(inPotentialNumber: Double?, inMatcherName: String): Boolean {
        return inPotentialNumber != null && inMatcherName !in NON_NUMERIC_MATCHERS
    }

    private fun validateAssertProperty(inAssertProperty: Entry<String, Any?>,
                                       targetTexts: Collection<String>,
                                       inValueToCheck: Collection<String>) {

        val potentialNumber = if (inValueToCheck.isEmpty()) null else ParseUtils.parseAsDouble(inValueToCheck.first())

        val matcher: Matcher<*>  // FIXME So ugly

        val matcherName: String
        val numericComparison: Boolean

        if (inAssertProperty.key.startsWith("not ")) {
            matcherName = inAssertProperty.key.substring(4).trim()
            numericComparison = isNumericComparison(potentialNumber, matcherName)

            matcher = if (numericComparison) {
                !getMatcher(matcherName, potentialNumber!!)
            } else {
                !getMatcher(matcherName, inValueToCheck)
            }
        } else {
            matcherName = inAssertProperty.key
            numericComparison = isNumericComparison(potentialNumber, matcherName)

            matcher = if (numericComparison) {
                getMatcher(matcherName, potentialNumber!!)
            } else {
                getMatcher(matcherName, inValueToCheck)
            }
        }

        val parsedNumberFound = if (numericComparison) ParseUtils.parseAsDouble(targetTexts.first()) else null

        if (numericComparison) {
            if (runProps.isLogAssertions) {
                log.info("< Assert: match {}{}{} against number: {} >", TerminalColours.assertClauseColour(), matcher.description, TerminalColours.reset(), parsedNumberFound)
            }

            performMatch(matcher, parsedNumberFound)
        } else {
            if (runProps.isLogAssertions) {
                log.info("< Assert: match {}{}{} against strings: {}", TerminalColours.assertClauseColour(), matcher.description, TerminalColours.reset(), toJavaSyntax(targetTexts))
            }

            performMatch(matcher, targetTexts)
        }
    }

    private fun performMatch(matcher: Matcher<*>, target: Any?) {
        assert.that(target, matcher as Matcher<Any?>) { /* Prefix errors */ lineNumbers.status() }
    }

    private fun getMatcher(inMatcherName: String, inValuesToCheck: Collection<String>): Matcher<Iterable<String>> {
        // In other words, whether there is an element for which all register Matchers pass
        // Obviously that can't be expected of *all* elements, as that would suggest all content on a page was identical
        return anyElement( getCollectionMatcher( inMatcherName, inValuesToCheck) )
    }

    private fun getCollectionMatcher(matcherName: String, valuesToCheck: Collection<String>): Matcher<String> {
        when (matcherName) {
            "eq", "equals" -> return allOfTheseValues(valuesToCheck) { equalTo(it) }

            "either", "anyOf" -> return anyOf( theseValues(valuesToCheck) { equalTo(it) })

            "eqIgnoreCase", "equalsIgnoreCase" -> return allOfTheseValues(valuesToCheck) { equalToIgnoringCase(it) }

            "startsWith" -> return allOfTheseValues(valuesToCheck) { startsWith(it) }
            "startsWithIgnoreCase" -> return allOfTheseValues(valuesToCheck) { startsWith(it).caseInsensitive() }

            "endsWith" -> return allOfTheseValues(valuesToCheck) { endsWith(it) }
            "endsWithIgnoreCase" -> return allOfTheseValues(valuesToCheck) { endsWith(it).caseInsensitive() }

            "contains" -> return allOfTheseValues(valuesToCheck) { containsSubstring(it) }
            "containsIgnoreCase" -> return allOfTheseValues(valuesToCheck) { containsSubstring(it).caseInsensitive() }

            "anyContain" -> return anyOf( theseValues(valuesToCheck) { containsSubstring(it) })

            "containsIgnoreWhitespace" -> return allOfTheseValues(valuesToCheck) { StringMatcher(::stripSpaces, collapseWhitespace(it)) }
            "containsIgnoreCaseWhitespace" -> return allOfTheseValues(valuesToCheck) { StringMatcher(::stripSpaces, collapseWhitespace(it)).caseInsensitive() }

            "matches" -> return allOfTheseValues(valuesToCheck) {
                contains( Regex(it, RegexOption.DOT_MATCHES_ALL) )
            }
            "matchesIgnoreCase" -> return allOfTheseValues(valuesToCheck) {
                contains( Regex(it, setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)) )
            }

            else -> throw exceptions.runtime("Unknown matcher '$matcherName' for text values")
        }
    }

    private fun getMatcher(inMatcherName: String, inValueToCheck: Double): Matcher<Double> {
        return when (inMatcherName) {
            "gt" -> greaterThan(inValueToCheck)
            "gte" -> greaterThanOrEqualTo(inValueToCheck)
            "lt" -> lessThan(inValueToCheck)
            "lte" -> lessThanOrEqualTo(inValueToCheck)
            "eq", "equals" -> equalTo(inValueToCheck)
            else -> throw exceptions.runtime("Unknown matcher '$inMatcherName' for numeric values")  // Not strictly true...
        }
    }

    @Throws(IOException::class)
    private fun obtainExpectedContent(inRawValue: String?): String? {
        if (inRawValue == null || inRawValue.isEmpty()) {
            return inRawValue
        }

        val cp = inRawValue.indexOf(':')
        if (cp < 0) {
            return inRawValue
        }

        val contentType = inRawValue.substring(0, cp).trim()
        val valueArg = inRawValue.substring(cp + 1).trim()

        when (contentType) {
            "file" -> {
                if (runProps.isLogAssertions) {
                    log.info("< Assert: loading expectations via file: from: '{}' >", valueArg)
                }
                return File(valueArg).readText(Charsets.UTF_8).trim()
            }
        }

        return inRawValue
    }

    private fun getTargetValues(inTargets: Collection<AssertTarget>): Collection<String> {
        return inTargets.map { it.text }
    }

    private fun removeIgnorableText(inOriginal: String?, ignorableReplacements: Collection<String>): String {
        var text = inOriginal
        for (eachReplacement in ignorableReplacements) {
            text = text!!.replace(eachReplacement.toRegex(), "")
        }
        return text!!.trim()
    }

    private fun removeIgnorableText(targetTexts: Collection<String?>, ignorableReplacements: Collection<String?>): Collection<String?> {
        var texts = targetTexts
        for (eachReplacement in ignorableReplacements) {
            texts = removeIgnorableText(texts, eachReplacement!!)
        }
        return texts
    }

    private fun removeIgnorableText(targetTexts: Collection<String?>, inRemovePattern: String): Collection<String> {
        return targetTexts.map { it!!.replace(inRemovePattern.toRegex(), "").trim() }
    }

    companion object {

        private val NON_NUMERIC_MATCHERS : Array<String> = arrayOf("startsWith", "endsWith", "contains", "matches")

        private fun stripSpaces(actual : CharSequence, expected: String, caseSensitivity: Boolean) : Boolean {
            return collapseWhitespace(actual).contains(expected, caseSensitivity)
        }

        private fun theseValues(inValuesToCheck: Collection<String>,
                                individualMatcher: (String) -> Matcher<String>): List<Matcher<String>> {
            return inValuesToCheck.map(individualMatcher)
        }

        private fun allOfTheseValues(inValuesToCheck: Collection<String>,
                                     individualMatcher: (String) -> Matcher<String>): Matcher<String> {
            return allOf( theseValues(inValuesToCheck, individualMatcher) )
        }

        fun validatePayload(inCmd: CommandSpec) {
            if (inCmd.isEmpty) {
                throw ValidationException("Missing body for: $inCmd")
            }

            if (!inCmd.hasString() &&             // If it has a string, it's short-form and we can't validate
                !inCmd.variablesResolved &&       // If it doesn't have a string because all vars were resolved away, ignore too
                inCmd.missing("that"))  // If it's therefore long-form, and "that" is missing, that's wrong
            {
                throw ValidationException("Missing 'that:' for: $inCmd")
            }

            // FIXME Can we check matchers at this stage? Or do we really need to know whether DOM element/string, or numeric value?
        }
    }
}
