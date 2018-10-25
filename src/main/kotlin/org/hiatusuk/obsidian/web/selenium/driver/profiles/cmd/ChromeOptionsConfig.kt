package org.hiatusuk.obsidian.web.selenium.driver.profiles.cmd

import com.google.common.base.MoreObjects
import org.hiatusuk.obsidian.config.FeatureConfiguration
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.selenium.driver.profiles.delegates.ProfilesState
import org.openqa.selenium.chrome.ChromeOptions
import org.slf4j.Logger
import java.io.File
import javax.inject.Inject

@FeatureConfiguration(value = "profile:chrome" /*, successors = [RemoteDrivers::class] */)
@ScenarioScope
class ChromeOptionsConfig @Inject
constructor(private val profiles: ProfilesState, private val log: Logger) {

    fun configure(map: Map<String,Any>) {

        val opts = ChromeOptions()
        val toStringHelper = MoreObjects.toStringHelper("ChromeOptions")

        if (map.containsKey("extensions")) {
            val extensionPaths = (map["extensions"] as MutableMap<String, Any>).keys

            toStringHelper.add("extensions", extensionPaths)

            opts.addExtensions(extensionPaths.map(::File))
        }

        if (map.containsKey("args")) {
            val argNames = (map["args"] as Map<String, Any>).keys

            toStringHelper.add("args", argNames)

            opts.addArguments(ArrayList(argNames))
        }

        val prefs = map.toMutableMap()
        prefs.remove("extensions")
        prefs.remove("args")

        if (!map.isEmpty()) {
            toStringHelper.add("experimentalOptions", prefs)
            opts.setExperimentalOption("prefs", prefs)
        }

        log.info("Using {}", toStringHelper)

        profiles.chromeOptions = opts
    }

    companion object {

        fun vanillaOptions(): ChromeOptions {
            return ChromeOptions()
        }

        fun defaultOptions(): ChromeOptions {
            return vanillaOptions().addArguments("--headless")
        }
    }
}
