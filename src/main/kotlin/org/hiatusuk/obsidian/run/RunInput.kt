package org.hiatusuk.obsidian.run

import com.google.common.base.Charsets.UTF_8
import java.io.File
import java.io.IOException
import java.util.*

class RunInput(private val scenarioFile: Optional<File>, private val scratchPad: Optional<String>) {

    // Only used for publishing position over Socket.io etc.
    val location: String
        get() = if (scratchPad.isPresent) {
            ""
        } else scenarioFile.get().absolutePath

    val text: String
        @Throws(IOException::class)
        get() = if (scratchPad.isPresent) {
            scratchPad.get().trim()
        } else scenarioFile.get().readText(UTF_8)


    constructor(scenarioFile: File) : this(Optional.of<File>(scenarioFile), Optional.empty<String>())

    fun sibling(inSiblingName: String): RunInput {
        if (scratchPad.isPresent) {
            throw UnsupportedOperationException()
        }

        return RunInput(File(scenarioFile.get().parentFile, inSiblingName))
    }

    override fun toString(): String {
        return if (scratchPad.isPresent) {
            "<ScratchPad>"
        } else scenarioFile.get().toString()

    }
}
