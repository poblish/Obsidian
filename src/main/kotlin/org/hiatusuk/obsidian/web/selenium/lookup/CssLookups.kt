package org.hiatusuk.obsidian.web.selenium.lookup

import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.LookupUtils
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.RunProperties
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.utils.StringUtils
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.slf4j.Logger
import java.util.regex.Pattern
import javax.inject.Inject

@AssertLookup("css\\(")
@ScenarioScope
class CssLookups @Inject
internal constructor(private val finders: ElementFinders, private val runProps: RunProperties, private val exceptions: RuntimeExceptions, private val log: Logger) {

    fun lookup(targetIdentifier: String): Collection<AssertTarget> {
        val p = Pattern.compile("css\\((.*)\\)\\.([A-Z\\-]*)\\(\\)", Pattern.CASE_INSENSITIVE)
        val m = p.matcher(targetIdentifier)
        if (m.find()) {
            val cssPath = m.group(1)
            val propertyName = m.group(2)

            val req = finders.with(cssPath)

            if (runProps.isLogAssertions) {
                log.info("< Asserting against $req >")
            }

            val elem = req.first()

            return LookupUtils.singleTarget( StringUtils.stripQuotes(elem.getCssValue(propertyName)) )
        } else {
            throw exceptions.runtime("Malformed CSS Assert: '$targetIdentifier'")
        }
    }
}
