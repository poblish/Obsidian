package org.hiatusuk.obsidian.protocol.http.cookie.lookup

import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.LookupUtils
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.protocol.http.cookie.delegates.HttpCookiesState
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import java.util.regex.Pattern
import javax.inject.Inject

@AssertLookup("cookies.get\\(")
@ScenarioScope
class NamedCookieLookup @Inject
internal constructor(private val cookiesState: HttpCookiesState,
                     private val web: WebState,
                     private val exceptions: RuntimeExceptions) {

    fun lookup(targetIdentifier: String): Collection<AssertTarget> {
        val p = Pattern.compile("cookies.get\\(([^\\(]*)\\)", Pattern.CASE_INSENSITIVE)
        val m = p.matcher(targetIdentifier)
        if (!m.find()) {
            throw exceptions.runtime("Malformed Cookie Assert: '$targetIdentifier'")
        }

        val name = m.group(1)

        for (c in cookiesState.cookieStore.cookies) {
            if (c.name == name) {
                return LookupUtils.singleTarget(c.value ?: "<empty>")
            }
        }

        ///////////////////// No cookie, go to the web instead

        try {
            val c = web.driver.manage().getCookieNamed(name)
            if (c != null) {
                return LookupUtils.singleTarget(c.value ?: "<empty>")
            }
        } catch (e: UnsupportedOperationException) {
            // Just ignore. Probably the NullWebDriver
        }

        return LookupUtils.singleTarget("<none>")
    }
}