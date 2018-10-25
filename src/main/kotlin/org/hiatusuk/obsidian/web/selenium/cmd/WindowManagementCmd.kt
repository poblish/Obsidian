package org.hiatusuk.obsidian.web.selenium.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.openqa.selenium.Dimension
import org.openqa.selenium.Point
import org.slf4j.Logger
import java.util.*
import javax.inject.Inject

@ScenarioScope
@Command("window")
class WindowManagementCmd @Inject
constructor(private val web: WebState, private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        resizeWindow(web, log,
                inCmd.optInteger("top"),
                inCmd.optInteger("left"),
                inCmd.optInteger("height"),
                inCmd.optInteger("top"))
    }

    companion object {
        fun resizeWindow(web: WebState,
                         log: Logger,
                         top: Optional<Int>,
                         left: Optional<Int>,
                         width: Optional<Int>,
                         height: Optional<Int>) {

            // log.info("{} {} {} {}", top, left, width, height);

            val wind = web.driver.manage().window()
            val currPos = wind.position
            val currSize = wind.size
            var changed = false

            if (left.isPresent || top.isPresent) {
                wind.position = Point(left.orElse(currPos.x), top.orElse(currPos.y))
                changed = true
            }

            if (width.isPresent || height.isPresent) {
                wind.size = Dimension(width.orElse(currSize.width), height.orElse(currSize.height))
                changed = true
            }

            if (changed) {
                log.info("Moved window => {}, Resized => {}", wind.position, wind.size)
            }
        }
    }
}
