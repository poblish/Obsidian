package org.hiatusuk.obsidian.cucumber

class CucumberRuleList(fullStr: String, val sectionNo: Int) : Iterable<CucumberRule> {

    private val rules = fullStr.split(" ^").map { CucumberRule(it) }

    override fun iterator(): Iterator<CucumberRule> {
        return rules.iterator()
    }
}
