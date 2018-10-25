package org.hiatusuk.obsidian.web.selenium.lookup

import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.LookupUtils
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.hiatusuk.obsidian.web.selenium.find.filters.Filter
import java.util.regex.Pattern
import javax.inject.Inject

@AssertLookup("meta\\(")
@ScenarioScope
class MetaLookups @Inject
internal constructor(private val finders : ElementFinders, private val exceptions: RuntimeExceptions) {

    fun lookup(targetIdentifier: String): Collection<AssertTarget> {
        val p = Pattern.compile("meta\\((.*)\\)\\.([A-Z\\-]*)\\(\\)", Pattern.CASE_INSENSITIVE)
        val m = p.matcher(targetIdentifier)
        if (!m.find()) {
            throw exceptions.runtime("Malformed Meta Assert: '$targetIdentifier'")
        }

        val metaName = m.group(1)
        val propertyName = m.group(2)

//        val elems = web.driver.findElements(By.tagName("meta").name(tagName))
        val elems = finders.with("meta").all( Filter("name", metaName) )

        when (propertyName) {
            "content" -> {
                val texts = arrayListOf<String>()
                for (each in elems) {
                    texts.add(each.getAttribute("content"))
                }
                return LookupUtils.singleTarget(texts)
            }
            else -> throw exceptions.runtime("Unexpected propertyName: $propertyName")
        }
    }
}
