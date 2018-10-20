package org.hiatusuk.obsidian

import org.hiatusuk.obsidian.run.ApplicationRunner

// Main class
object Obsidian {

    @JvmStatic
    fun main(args: Array<String>) {
        ApplicationRunner(args).startUp()
    }
}
