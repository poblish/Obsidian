package org.hiatusuk.obsidian

import org.hiatusuk.obsidian.di.component.DaggerApplicationComponent
import org.hiatusuk.obsidian.run.RunInputs
import org.hiatusuk.obsidian.run.RunProperties
import org.openqa.selenium.remote.DesiredCapabilities
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testng.annotations.Test
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

            DaggerApplicationComponent.builder().runWith(props).build().scenarioRunner.use { r ->
                r.setInputs(RunInputs( File("demos/google.yaml")) )
                r.start()
            }
        }
    }

    // See: https://github.com/testcontainers/testcontainers-java/issues/238
    private class MyBrowserWebDriverContainer : BrowserWebDriverContainer<MyBrowserWebDriverContainer>()
}