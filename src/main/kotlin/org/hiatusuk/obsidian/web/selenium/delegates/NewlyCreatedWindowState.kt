package org.hiatusuk.obsidian.web.selenium.delegates

import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.events.BeforeScenario
import org.slf4j.Logger
import java.util.*
import javax.inject.Inject

@ScenarioScope
class NewlyCreatedWindowState @Inject
constructor(private val log: Logger) {

    var originalWindowHandle: String? = null
    var newWindowHandle = Optional.empty<String>()
        private set

    @BeforeScenario
    fun resetForScenario() {
        originalWindowHandle = null
        newWindowHandle = Optional.empty()
    }

    fun setLastWindowHandle(windowHandle: String) {
        this.newWindowHandle = Optional.of(windowHandle)

        log.info("< Selenium discovered new window '{}' >", windowHandle)
    }
}
