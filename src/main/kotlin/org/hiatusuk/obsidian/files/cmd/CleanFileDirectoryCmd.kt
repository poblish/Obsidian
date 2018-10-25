package org.hiatusuk.obsidian.files.cmd

import org.apache.commons.io.FileUtils
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.slf4j.Logger
import java.nio.file.Paths
import javax.inject.Inject

@ScenarioScope
@Command("file:clean")
class CleanFileDirectoryCmd @Inject
constructor(private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val fileOrDir = Paths.get(inCmd.string).toFile()

        if (fileOrDir.isDirectory) {
            FileUtils.deleteDirectory(fileOrDir)
        } else if (!fileOrDir.delete()) {
            log.warn("Could not delete $fileOrDir")
        }
    }
}
