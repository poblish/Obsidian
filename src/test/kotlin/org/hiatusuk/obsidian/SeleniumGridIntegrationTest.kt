package org.hiatusuk.obsidian

import org.hiatusuk.obsidian.di.component.DaggerRunnerComponent
import org.hiatusuk.obsidian.run.RunInputs
import org.hiatusuk.obsidian.run.RunProperties
import org.junit.jupiter.api.Test
import org.openqa.selenium.remote.DesiredCapabilities
import org.testcontainers.containers.BrowserWebDriverContainer
import java.io.File
import java.util.*

class SeleniumGridIntegrationTest {

    @Test
    fun testPageWithChromeSeleniumGrid() {
        testPageWithSeleniumGrid(DesiredCapabilities.chrome())
    }

    private fun testPageWithSeleniumGrid(caps: DesiredCapabilities) {
        MyBrowserWebDriverContainer().withDesiredCapabilities(caps).use {
            it.start()

            val props = RunProperties()
            props.seleniumGridUrl = Optional.of(it.seleniumAddress)
            props.isUseSeleniumGrid = true
            props.setDefaultDesiredCapabilities(caps)
            props.isLogMetrics = true

            DaggerRunnerComponent.builder().props(props).build().scenarioRunner.use { r ->
                r.inputs = RunInputs( File("demos/google.yaml"))
                r.start()
            }
        }
    }

    // See: https://github.com/testcontainers/testcontainers-java/issues/238
    private class MyBrowserWebDriverContainer : BrowserWebDriverContainer<MyBrowserWebDriverContainer>()
}