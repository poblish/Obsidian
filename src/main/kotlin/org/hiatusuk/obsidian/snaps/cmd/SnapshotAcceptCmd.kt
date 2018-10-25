package org.hiatusuk.obsidian.snaps.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.slf4j.Logger
import java.io.File
import javax.inject.Inject

@ScenarioScope
@Command("snapshot accept")
class SnapshotAcceptCmd @Inject
constructor(private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val path = inCmd.string

        val current = File(path + "_actual.png")

        log.debug("SNAPSHOT Accept: Renaming 'actual' ('{}') to accept it", current)
        current.renameTo(File("$path.png"))

        val oldExpected = File(path + "_expected.png")
        if (oldExpected.exists()) {
            log.debug("SNAPSHOT Accept: Deleting old expectation ('{}')", oldExpected)
            oldExpected.delete()
        }

        val diffFile = File(path + "_diff.png")
        if (diffFile.exists()) {
            log.debug("SNAPSHOT Accept: Delete old diff")
            diffFile.delete()
        }
    }
}
