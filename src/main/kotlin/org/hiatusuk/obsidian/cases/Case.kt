package org.hiatusuk.obsidian.cases

import com.google.common.base.MoreObjects
import java.util.LinkedHashSet

class Case {

    val id: String
    val subtypes = LinkedHashSet<String>()

    constructor(inId: String) {
        id = inId
    }

    constructor(inId: String, inUser: String) {
        id = inId
        subtypes.add(inUser)
    }

    constructor(inId: String, inUsers: Set<String>) {
        id = inId
        subtypes.addAll(inUsers)
    }

    override fun toString(): String {
        return MoreObjects.toStringHelper(this).omitNullValues().add("id", id).add("subtypes", if (subtypes.isEmpty()) null else subtypes).toString()
    }
}