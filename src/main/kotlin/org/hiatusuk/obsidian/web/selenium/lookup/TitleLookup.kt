package org.hiatusuk.obsidian.web.selenium.lookup

import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.LookupUtils
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import javax.inject.Inject

@AssertLookup("title\\(\\)")
@ScenarioScope
class TitleLookup @Inject
constructor(private val web: WebState) {

    fun lookup(@Suppress("UNUSED_PARAMETER") targetIdentifier: String): Collection<AssertTarget> {
        return LookupUtils.singleTarget(web.driver.title)
    }
}
