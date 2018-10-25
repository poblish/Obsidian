package org.hiatusuk.obsidian.run

import java.io.File
import java.net.URL
import java.util.*

class GuiRunRequest(inFiles: Collection<File>,
                    inScratchPad: Optional<String>,
                    val failFast: Boolean,
                    val logAssertions: Boolean,
                    val logMetrics: Boolean,
                    val useSeleniumGrid: Boolean,
                    val seleniumGridUrl: Optional<URL>) {

    val inputs: RunInputs = RunInputs(inFiles, inScratchPad)
}
