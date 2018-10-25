package org.hiatusuk.obsidian.web.selenium.delegates

import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.delegates.ScenarioDefaultsContext
import org.hiatusuk.obsidian.run.events.AfterGoToUrl
import org.hiatusuk.obsidian.web.selenium.cmd.WindowManagementCmd
import org.slf4j.Logger
import javax.inject.Inject

@ScenarioScope
class WindowSizeDefaults @Inject
internal constructor(private val web: WebState,
                     private val defaults: ScenarioDefaultsContext,
                     private val log: Logger) {

    @AfterGoToUrl
    fun resizeWindowAfterLoading() {
        WindowManagementCmd.resizeWindow(web, log,
                defaults.getOptional("window", "top"),
                defaults.getOptional("window", "left"),
                defaults.getOptional("window", "width"),
                defaults.getOptional("window", "height"))
    }
}
