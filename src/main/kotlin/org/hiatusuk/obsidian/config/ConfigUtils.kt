package org.hiatusuk.obsidian.config

import java.util.*

object ConfigUtils {

    fun mapEntries(inPayload: Any?): Map<String, Any> {
        if (inPayload == null) {
            return emptyMap()
        }

        if (inPayload is String) {
            return stringToMap(inPayload)
        }

        if (inPayload is List<*>) {
            val map = LinkedHashMap<String, Any?>()
            for (eachName in (inPayload as List<String>?)!!) {
                map[eachName] = null
            }
            return map as Map<String,Any>
        }

        return inPayload as Map<String, Any>
    }

    fun mapElements(inPayload: Any?): Collection<MutableMap<String, Any>> {
        if (inPayload == null) {
            return emptyList()
        }

        if (inPayload is String) {
            return setOf(stringToMap((inPayload as String?)!!))
        }

        return if (inPayload is Map<*, *>) {
            setOf(inPayload as MutableMap<String, Any>)
        } else (inPayload as Collection<MutableMap<String, Any>>).filter{ Objects.nonNull(it) }
    }

    private fun stringToMap(str: String): MutableMap<String, Any> {
        return Collections.singletonMap<String, Any>(str, null)
    }
}
