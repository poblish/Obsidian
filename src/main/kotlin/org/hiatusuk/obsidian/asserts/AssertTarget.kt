package org.hiatusuk.obsidian.asserts

import com.google.common.base.MoreObjects

/**
 * The target of an assertion
 */
class AssertTarget {

    val text: String

    constructor(inText: String) {
        text = inText
    }

    constructor(textElems: List<String>) {
        text = join(textElems)
    }

    constructor(textElems: Set<String>) {
        text = join(textElems)
    }

    constructor(inValue: Boolean) {
        text = java.lang.Boolean.toString(inValue)
    }

    constructor(inValue: Int) {
        text = Integer.toString(inValue)
    }

    constructor(inValue: Long) {
        text = java.lang.Long.toString(inValue)
    }

    constructor(inValue: Double) {
        text = java.lang.Double.toString(inValue)
    }

    private fun join(texts: Collection<String>) : String {
        return texts.joinToString(separator = ",")
    }

    override fun toString(): String {
        return MoreObjects.toStringHelper(this).add("text", text).toString()
    }
}
