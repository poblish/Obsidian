package org.hiatusuk.obsidian.javascript.jasmine.lookup

import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.LookupUtils
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.javascript.jasmine.delegates.JasmineRunStatus
import javax.inject.Inject

@AssertLookup("jasmine.passed\\(")
@ScenarioScope
class JasminePassesLookup @Inject
internal constructor(private val jasmineStatus: JasmineRunStatus) {

    fun lookup(targetIdentifier: String): Collection<AssertTarget> {
        check(jasmineStatus.hasRunCompleted()) {"No Jasmine run has completed"}
        return LookupUtils.singleTarget(jasmineStatus.passes)
    }
}