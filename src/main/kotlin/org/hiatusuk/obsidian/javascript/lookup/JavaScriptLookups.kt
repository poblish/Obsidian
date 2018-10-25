package org.hiatusuk.obsidian.javascript.lookup

import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.LookupUtils
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.openqa.selenium.JavascriptExecutor
import java.util.concurrent.Executors
import java.util.regex.Pattern
import javax.inject.Inject

@AssertLookup("javascript\\(")
@ScenarioScope
class JavaScriptLookups @Inject
internal constructor(private val web: WebState, private val exceptions: RuntimeExceptions) {

    fun lookup(targetIdentifier: String): Collection<AssertTarget> {
        val p = Pattern.compile("javascript\\((.*)\\)", Pattern.CASE_INSENSITIVE)  // Greedy...
        val m = p.matcher(targetIdentifier)
        if (!m.find()) {
            throw exceptions.runtime("Malformed JavaScript Assert: '$targetIdentifier'")
        }

        try {
            return Executors.newFixedThreadPool(1).submit<Collection<AssertTarget>> {
                val result = (web.driver as JavascriptExecutor).executeScript("return " + m.group(1) + ";")

                if (result == null) {
                    LookupUtils.singleTarget("<null>")
                }
                else {
                    LookupUtils.singleTarget( result.toString() )
                }
            }.get()
        } catch (e: InterruptedException) {
            throw exceptions.runtime(e)
        }
    }
}