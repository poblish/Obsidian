package org.hiatusuk.obsidian.utils

import java.util.concurrent.TimeUnit


class Duration(private val amount: Int, private val unit: TimeUnit) {

    fun toNanos(): Long {
        return unit.toNanos(amount.toLong())
    }

    fun toMillis(): Long {
        return unit.toMillis(amount.toLong())
    }

    override fun toString(): String {
        return amount.toString() + " " + unit
    }

    companion object {
        val ZERO = Duration(0, TimeUnit.MILLISECONDS)
    }
}
