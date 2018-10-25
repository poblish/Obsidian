package org.hiatusuk.obsidian.web.selenium.lookup

import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.LookupUtils
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.RunProperties
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.run.state.ScenarioState
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.openqa.selenium.WebElement
import org.slf4j.Logger
import java.util.regex.Pattern
import javax.inject.Inject

@AssertLookup("attr\\(")
@ScenarioScope
class DomAttributeLookups @Inject
internal constructor(private val finders: ElementFinders,
                     private val state: ScenarioState,
                     private val runProps: RunProperties,
                     private val exceptions: RuntimeExceptions,
                     private val log: Logger) {

    fun lookup(targetIdentifier: String): Collection<AssertTarget> {
        val p = Pattern.compile("attr\\((.*)\\)\\.([A-Z\\-]*)\\(\\)", Pattern.CASE_INSENSITIVE)
        val m = p.matcher(targetIdentifier)
        if (m.find()) {
            val cssPath = m.group(1)
            val propertyName = m.group(2)

            val req = state.defaultTarget.orElse(finders.with(cssPath))

            if (runProps.isLogAssertions) {
                log.info("< Asserting against $req >")
            }

            val elem = req.first()

            // log.info("> '" + cssPath + "', " + DomDump.toString(elem));

            val targetText = when (propertyName) {
                "text" -> elem.text
                "displayed" -> elem.isDisplayed.toString()
                "enabled" -> elem.isEnabled.toString()
                "selected" -> elem.isSelected.toString()
                "editable" -> isEditable(elem).toString()
                else -> elem.getAttribute(propertyName).toString()
            }

            return LookupUtils.singleTarget(targetText)
        } else {
            throw exceptions.runtime("Malformed Attribute Assert: '$targetIdentifier'")
        }
    }

    // Adapted impl from http://bit.ly/1yK5q6D
    private fun isEditable(elem: WebElement): Boolean {
        if (!elem.isEnabled) {
            return false
        }

        val tagName = elem.tagName.toLowerCase()
        return if ("input" != tagName && "select" != tagName && "textarea" != tagName) {
            false
        } else "input" != tagName || elem.getAttribute("readonly") == null

        // "readonly" seems to be fatal for inputs in practice
    }
}
