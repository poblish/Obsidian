package org.hiatusuk.obsidian.asserts.lookups

import kotlin.collections.Map.Entry

class AssertSpecs(inSpecs: Map<String, Any>, val isShortForm: Boolean) {

    private val assertSpecs: MutableMap<String, Any?>

    val isEmpty: Boolean
        get() = assertSpecs.isEmpty()

    constructor(inSpecs: Map<String, Any>) : this(HashMap<String, Any>(inSpecs), false)

    init {
        assertSpecs = HashMap(inSpecs)
    }

    fun getString(propName: String): String? {
        val value = assertSpecs[propName]
        return value?.toString()
    }

    fun getBoolean(propName: String): Boolean {
        return java.lang.Boolean.parseBoolean(getString(propName))
    }

    fun remove(propName: String) {
        assertSpecs.remove(propName)
    }

    fun entrySet(): Iterable<Entry<String, Any?>> {
        return assertSpecs.entries
    }
}
