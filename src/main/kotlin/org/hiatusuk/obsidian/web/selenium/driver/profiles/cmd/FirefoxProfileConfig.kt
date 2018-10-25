package org.hiatusuk.obsidian.web.selenium.driver.profiles.cmd

import com.google.common.base.MoreObjects
import org.hiatusuk.obsidian.config.FeatureConfiguration
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.selenium.driver.profiles.delegates.ProfilesState
import org.openqa.selenium.firefox.FirefoxProfile
import org.slf4j.Logger
import java.io.File
import javax.inject.Inject

@FeatureConfiguration(value = "profile:firefox" /*, successors = [RemoteDrivers::class] */)
@ScenarioScope
class FirefoxProfileConfig @Inject
internal constructor(private val profiles: ProfilesState, private val log: Logger) {

    fun configure(map: Map<String,Any>) {

        val profile = FirefoxProfile()
        val toStringHelper = MoreObjects.toStringHelper("FirefoxProfile")

        if (map.containsKey("extensions")) {
            val exts = (map["extensions"] as Map<String, Any>).keys

            if (!exts.isEmpty()) {
                val extensionsAdded = arrayListOf<String>()  // For logging only

                for (eachExtensionPath in exts) {
                    profile.addExtension(File(eachExtensionPath))
                    extensionsAdded.add(eachExtensionPath)
                }

                toStringHelper.add("extensions", extensionsAdded)
            }
        }

        val prefs = map.toMutableMap()
        prefs.remove("extensions")

        if (!prefs.isEmpty()) {
            toStringHelper.add("prefs", prefs)

            for ((key, value) in prefs) {
                profile.setPreference(key, value.toString())
            }
        }

        log.info("Using {}", toStringHelper)

        profiles.firefoxProfile = profile
    }
}
