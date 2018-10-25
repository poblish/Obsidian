package org.hiatusuk.obsidian.web.selenium.lookup

import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.LookupUtils
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Returns text length for **first match only**.
 */
@AssertLookup("text_length\\(")
@ScenarioScope
class TextLengthLookups @Inject
internal constructor(private val finders: ElementFinders, private val exceptions: RuntimeExceptions) {

    fun lookup(targetIdentifier: String): Collection<AssertTarget> {
        val m = TEXT_LENGTH_HANDLER_PATTERN.matcher(targetIdentifier)
        if (!m.find()) {
            throw exceptions.runtime("Malformed Assert: $targetIdentifier")
        }

        return LookupUtils.singleTarget(finders.with(m.group(1)).first().text.length)
    }

    companion object {

        private val TEXT_LENGTH_HANDLER_PATTERN = Pattern.compile("text_length" + "\\((.*)\\)", Pattern.CASE_INSENSITIVE)
    }
}
