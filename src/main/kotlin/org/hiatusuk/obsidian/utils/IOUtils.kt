package org.hiatusuk.obsidian.utils

import java.util.LinkedHashMap

object IOUtils {

    fun deepCloneMap(inMap: Map<Any?, Any>): MutableMap<String, Any> {
        val newMap = LinkedHashMap(inMap)

        for ((key, value) in inMap) {
            if (key == null) continue

            if (value is Map<*, *>) {
                newMap[key.toString()] = deepCloneMap(value as Map<Any?, Any>)
            } else {
                // No actual need to clone the raw value, purely the Map structure
                newMap[key.toString()] = value
            }
        }

        return newMap as MutableMap<String, Any>
    }
}
