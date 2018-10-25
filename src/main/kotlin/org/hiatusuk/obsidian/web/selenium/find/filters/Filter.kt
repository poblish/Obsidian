package org.hiatusuk.obsidian.web.selenium.find.filters

class Filter(customAttribute: String, private val expectedValue: String) {
    private val attr: String = customAttribute.toLowerCase()

    fun matches(valueToTest: String): Boolean {
        return valueToTest == expectedValue
    }

    fun attr(): String {
        return this.attr
    }

    override fun toString(): String {
        return "[" + this.attr + "=\"" + expectedValue + "\"]"
    }
}
