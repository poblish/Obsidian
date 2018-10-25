package org.hiatusuk.obsidian.web.selenium.lookup

import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.LookupUtils
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.utils.StringUtils
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.hiatusuk.obsidian.web.selenium.utils.WebDriverUtils
import java.util.regex.Pattern
import javax.inject.Inject

@AssertLookup("ordering\\(")
@ScenarioScope
class ElementOrderingLookups @Inject
internal constructor(private val finders: ElementFinders,
                     private val exceptions: RuntimeExceptions) {

    fun lookup(targetIdentifier: String): Collection<AssertTarget> {
        val m = ORDERING_HANDLER_PATTERN.matcher(targetIdentifier)
        if (!m.find()) {
            throw exceptions.runtime("Malformed Assert: $targetIdentifier")
        }

        val parentPath = StringUtils.stripQuotes(m.group(1))
        val child1Path = StringUtils.stripQuotes(m.group(2))
        val child2Path = StringUtils.stripQuotes(m.group(3))

        if (child1Path == child2Path) {
            return LookupUtils.singleTarget("same")  // Not so great, how to detect they match the same thing?
        }

        val pm = finders.with(parentPath).first()
        val match1 = finders.with("$parentPath $child1Path").first()
        val match2 = finders.with("$parentPath $child2Path").first()
        val firstFoundMatch = WebDriverUtils.findEarliest(pm, match1, match2)

        return when {
            firstFoundMatch == null -> throw exceptions.runtime("No child element matched")
            firstFoundMatch === match1 -> LookupUtils.singleTarget("before")
            firstFoundMatch === match2 -> LookupUtils.singleTarget("after")
            else -> throw exceptions.runtime("Unexpected WebElement matched")
        }
    }

    companion object {

        private val ORDERING_HANDLER_PATTERN = Pattern.compile("ordering" + "\\(" + "(.*)\\s*,\\s*(.*),\\s*(.*)" + "\\)", Pattern.CASE_INSENSITIVE)
    }
}
