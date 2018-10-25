package org.hiatusuk.obsidian.asserts.lookups

import org.hiatusuk.obsidian.asserts.AssertTarget

object LookupUtils {

    fun singleTarget(input: String): Collection<AssertTarget> {
        return listOf(AssertTarget(input))
    }

    fun singleTarget(input: List<String>): Collection<AssertTarget> {
        return listOf(AssertTarget(input))
    }

    fun singleTarget(input: Set<String>): Collection<AssertTarget> {
        return listOf(AssertTarget(input))
    }

    fun singleTarget(input: Boolean): Collection<AssertTarget> {
        return listOf(AssertTarget(input))
    }

    fun singleTarget(input: Int): Collection<AssertTarget> {
        return listOf(AssertTarget(input))
    }

    fun singleTarget(input: Long): Collection<AssertTarget> {
        return listOf(AssertTarget(input))
    }

    fun singleTarget(input: Double): Collection<AssertTarget> {
        return listOf(AssertTarget(input))
    }
}