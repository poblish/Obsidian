package org.hiatusuk.obsidian.snaps.cmd

import com.codahale.metrics.MetricRegistry
import com.google.common.io.Files
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.snaps.ImageComparisons
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.slf4j.Logger
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import javax.inject.Inject

@ScenarioScope
@Command("snapshot")
class SnapshotImageCmd @Inject
constructor(private val web: WebState,
            private val metrics: MetricRegistry,
            private val log: Logger) : CommandIF {

    @Throws(IOException::class)
    override fun run(inCmd: CommandSpec) {
        val path = inCmd.string

        val current = File("$path.png")
        Files.createParentDirs(current)

        if (current.exists()) {
            val newFile = File(path + "_actual.png")  // FIXME Should really write to /tmp !
            web.takeScreenShot(newFile.absolutePath)

            log.debug("SNAPSHOT: Compare existing ('{}') against 'actual' ('{}')...", current, newFile)

            val beforeImg = ImageIO.read(current)
            val afterImg = ImageIO.read(newFile)

            var diff2: BufferedImage? = null

            metrics.timer("ImageComparisons.diff.png").time().use { diff2 = ImageComparisons.getDifferenceImage2(beforeImg, afterImg) }

            if (diff2 == null) {
                // No change, delete new screenshot
                log.debug("SNAPSHOT: New image ('{}') is identical - delete it", newFile)
                newFile.delete()

                val diffFile = File(path + "_diff.png")
                if (diffFile.exists()) {
                    log.debug("SNAPSHOT: Delete old diff")
                    diffFile.delete()
                }
            } else {
                // Change detected, rename 'current' to 'expected', keep 'actual'
                log.debug("SNAPSHOT: Renaming old 'current' ('{}') to become expectation", current)
                current.renameTo(File(path + "_expected.png"))

                log.debug("SNAPSHOT: Writing out diff...")
                ImageIO.write(diff2, "png", File(path + "_diff.png"))
            }
        } else {
            // FIXME Do we need to delete old / create new 'diff' ???

            val newFile = File(path + "_actual.png")  // FIXME Should really write to /tmp !
            if (newFile.exists()) {
                log.debug("SNAPSHOT: Missing 'current', overwrite existing 'actual': $newFile")
            } else {
                log.debug("SNAPSHOT: Missing 'current', create new 'actual': $newFile")
            }
            web.takeScreenShot(newFile.absolutePath)
        }
    }
}
