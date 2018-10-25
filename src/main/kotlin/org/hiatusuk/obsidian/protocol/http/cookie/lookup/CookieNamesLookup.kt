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

@AssertLookup("cookies.names\\(")
@ScenarioScope
class CookieNamesLookup @Inject
internal constructor(private val cookiesState: HttpCookiesState,
                     private val web: WebState,
                     private val exceptions: RuntimeExceptions) {

    fun lookup(targetIdentifier: String): Collection<AssertTarget> {
        val p = Pattern.compile("cookies.names\\(\\)", Pattern.CASE_INSENSITIVE)
        val m = p.matcher(targetIdentifier)
        if (!m.find()) {
            throw exceptions.runtime("Malformed Cookie Assert: '$targetIdentifier'")
        }

        val names = HashSet( cookiesState.cookieStore.cookies.map{ it.name } )
        names.addAll( web.driver.manage().cookies.map{ it.name }.toSet() )

        return LookupUtils.singleTarget(names)
    }
}
