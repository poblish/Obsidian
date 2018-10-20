package org.hiatusuk.obsidian.benchmarks

object BenchmarkRunner {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        org.openjdk.jmh.Main.main(args)
    }
}