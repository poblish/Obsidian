package org.hiatusuk.obsidian.web.selenium.driver.profiles.delegates

import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.events.BeforeScenario
import org.hiatusuk.obsidian.web.selenium.driver.profiles.cmd.ChromeOptionsConfig
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxProfile
import javax.inject.Inject

@ScenarioScope
class ProfilesState @Inject
constructor()
{
    var firefoxProfile: FirefoxProfile? = null
    private var chromeOpts: ChromeOptions? = null

    var chromeOptions: ChromeOptions?
        get() {
            if (chromeOpts == null) {
                chromeOpts = ChromeOptionsConfig.defaultOptions()
            }

            return chromeOpts
        }
        set(opts) {
            chromeOpts = opts!!
        }

    @BeforeScenario
    fun initialiseBeforeScenario() {
        firefoxProfile = null
        chromeOpts = null
    }
}
