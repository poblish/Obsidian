package org.hiatusuk.obsidian.run

import com.google.common.base.MoreObjects
import org.hiatusuk.obsidian.run.exceptions.MatchingScenarioNotFoundException
import java.util.regex.Pattern

open class ScriptletCall {

    private var matchingRule: SimpleMatchingRule? = null
    protected var pattern: Pattern? = null

    constructor()

    constructor(inPattern: Pattern) {
        this.pattern = requireNotNull(inPattern)
    }

    constructor(simpleMatchingRule: SimpleMatchingRule) {
        this.matchingRule = requireNotNull(simpleMatchingRule)
    }

    open fun matchesScenario(inScenarioName: String): Boolean {
        return if (this.pattern != null) {
            this.pattern!!.matcher(inScenarioName).matches()
        } else matchingRule!!.pattern.matcher(inScenarioName).matches()
    }

    open fun enterFilter(fullMethodNameWithArgs: String) {
        // NOOP for overriding
    }

    open fun exitFilter() {
        // NOOP for overriding
    }

    override fun toString(): String {
        return if (this.pattern != null) {
            MoreObjects.toStringHelper(this).add("pattern", this.pattern).toString()
        } else MoreObjects.toStringHelper(this).add("rule", matchingRule).toString()
    }

    open fun arguments(): List<Any> {
        return emptyList()
    }

    open fun handleNotFound() {
        throw MatchingScenarioNotFoundException("Could not find matching scenario for: " + this)
    }
}
