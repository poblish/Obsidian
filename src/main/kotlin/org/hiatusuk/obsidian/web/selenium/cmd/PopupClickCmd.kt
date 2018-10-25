package org.hiatusuk.obsidian.web.selenium.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.web.selenium.delegates.NewlyCreatedWindowState
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import javax.inject.Inject

//TODO Should really add a @Validate handler
@ScenarioScope
@Command("popup click")
class PopupClickCmd @Inject
constructor(private val click: ClickCmd,
            private val web: WebState,
            private val createdWindowState: NewlyCreatedWindowState,
            private val exceptions: RuntimeExceptions) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        createdWindowState.originalWindowHandle = web.driver.windowHandle

        val origWindows = web.driver.windowHandles

        click.run(inCmd)

        val currWindows = web.driver.windowHandles
        currWindows.removeAll(origWindows)

        if (currWindows.isEmpty()) {
            throw exceptions.runtime("No new window discovered")
        } else if (currWindows.size > 1) {
            throw exceptions.runtime("More than one window discovered")
        }

        createdWindowState.setLastWindowHandle(currWindows.first())
    }
}