package org.hiatusuk.obsidian.run.state

import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.events.AcceptCommand
import org.hiatusuk.obsidian.run.events.AfterBrowserCompletedScenario
import org.hiatusuk.obsidian.run.events.AfterScenariosFailed
import org.hiatusuk.obsidian.run.events.BeforeScenarioElement
import org.slf4j.MDC
import java.util.regex.Pattern
import javax.inject.Inject

@ScenarioScope
class LineNumbersState @Inject constructor() {

    private val linePosArray = arrayListOf<Int>()
    private var originalScenarioContent = ""
    private var lastCommandSearchPos: Int = 0
    private var currentLineNumber = -1

    fun acceptScenario(yamlStr: String) {
        originalScenarioContent = Pattern.compile("\\r\\n|\\r").matcher(yamlStr).replaceAll("\n")
        if (!originalScenarioContent.endsWith("\n")) {
            originalScenarioContent += "\n"
        }

        resetForScenarioFile()
    }

    @BeforeScenarioElement
    fun resetForScenarioFile() {
        lastCommandSearchPos = 0

        linePosArray.clear()
        clear()

        // Build the array of char-positions for each line
        val m = Pattern.compile("[\\r\\n]").matcher(originalScenarioContent)
        while (m.find()) {
            linePosArray.add(m.start(0) + 1)
        }
    }

    @AfterBrowserCompletedScenario
    @AfterScenariosFailed
    fun clear() {
        currentLineNumber = -1
        lastCommandSearchPos = 0
        MDC.remove("line")
    }


    fun currentLine(): Int {
        return currentLineNumber
    }

    fun status(): String {
        return if (currentLineNumber >= 0) {
            "[Line #$currentLineNumber]: "
        } else ""
    }

    @AcceptCommand
    fun acceptNextCommand(currentCmd: Map<String, Any>) {
        val currCmdName = currentCmd.keys.first()

        // Start searching for the current command from the last-found position...
        val m = Pattern.compile("-\\s+" + Pattern.quote(currCmdName) + ":").matcher(originalScenarioContent)
        while (m.find(lastCommandSearchPos)) {
            val foundPos = m.start(0)
            val match = getLineNumber(originalScenarioContent, foundPos)

            lastCommandSearchPos = match.endOfLinePos

            if (match.lineStr.startsWith("#")) {  // *Do not* count commented-out references to that command!
                continue
            }

            currentLineNumber = match.lineIdx
            if (currentLineNumber < 0) {
                throw RuntimeException("Could not find command '$currCmdName'")
            }

            MDC.put("line", "@$currentLineNumber")
            return
        }

        clear()
    }

    private fun getLineNumber(inContent: String, idx: Int): LineMatch {
        var i = 1
        var prevIdx = 0
        for (linePos in linePosArray) {
            if (idx < linePos) {
                return LineMatch(inContent.substring(prevIdx, linePos).trim(), i, linePos)
            }
            prevIdx = linePos
            i++
        }

        return LineMatch("", -1, -1)
    }

    private class LineMatch internal constructor(val lineStr: String, val lineIdx: Int, val endOfLinePos: Int)
}
