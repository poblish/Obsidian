package org.hiatusuk.obsidian.run

import org.hiatusuk.obsidian.files.FileUtils
import java.io.File
import java.util.*
import javax.inject.Inject

class RunInputs : Iterable<RunInput> {

    private val scenarioFilesOrFolders = arrayListOf<File>()
    private val scratchPad: Optional<String>

    val isEmpty: Boolean
        get() = scratchPad.map { it.trim().isEmpty() }.orElseGet { scenarioFilesOrFolders.isEmpty() }

    val simpleNames: List<String>
        get() {
            if (scratchPad.isPresent) {
                return arrayListOf(scratchPad.get().trim())
            }

            return scenarioFilesOrFolders.map { FileUtils.getFriendlyFileName(it) }
        }

    @Inject constructor() {
        scratchPad = Optional.empty()
    }

    constructor(inScenarioFileOrFolder: File) : this( listOf<File>(inScenarioFileOrFolder) )

    @JvmOverloads
    constructor(inScenarioFilesOrFolders: Collection<File>, inScratchPad: Optional<String> = Optional.empty()) {

        for (eachFileOrDir in inScenarioFilesOrFolders) {
            scenarioFilesOrFolders.addAll(FileUtils.allYamlFiles(eachFileOrDir))
        }

        scratchPad = inScratchPad
    }

    override fun toString(): String {
        return if (scratchPad.isPresent) {
            "<ScratchPad>"
        } else scenarioFilesOrFolders.toString()
    }

    override fun iterator(): Iterator<RunInput> {
        return if (scratchPad.isPresent) {
            arrayOf(RunInput(Optional.empty(), scratchPad)).iterator()
        } else this.scenarioFilesOrFolders.map{ RunInput(it) }.iterator()
    }
}
