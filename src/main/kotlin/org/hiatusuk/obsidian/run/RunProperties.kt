package org.hiatusuk.obsidian.run

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.MoreObjects
import com.natpryce.konfig.Configuration
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.overriding
import org.openqa.selenium.MutableCapabilities
import java.io.File
import java.net.URL
import java.util.*
import javax.inject.Inject

// TODO Make a Data class? http://kotlinlang.org/docs/reference/data-classes.html
class RunProperties @Inject constructor()
{
    var failFastMode = FailFastMode.FAIL_ON_FIRST_MISMATCH
    var isLogAssertions: Boolean = false
    var isLogMetrics: Boolean = false
    var isDebug: Boolean = false
    var ignoreScenariosWithMissingSteps = false
    var parallelThreads: Int = 0

    var seleniumGridUrl = Optional.empty<URL>()
    var isUseSeleniumGrid: Boolean = false

    var specifiedProfiles: MutableSet<String> = linkedSetOf()

    var runtimeOverrides: MutableMap<String,Any> = linkedMapOf()

    var defaultConfig: Map<String,Any> = mapOf()
    var overrideConfig: Map<String,Any> = mapOf()

    var defaultDesiredCapabilities = Optional.empty<MutableCapabilities>()
        private set

    @VisibleForTesting
    var obsidianPropertiesFile = File(System.getProperty("user.home"), "obsidian.properties")

    val konfig: Configuration by lazy {
        ConfigurationProperties.systemProperties() overriding
            EnvironmentVariables() overriding
                ConfigurationProperties.fromOptionalFile(obsidianPropertiesFile)
    }

    fun setDefaultDesiredCapabilities(caps: MutableCapabilities) {
        defaultDesiredCapabilities = Optional.of(caps)
    }

    override fun toString(): String {
        return MoreObjects.toStringHelper(this).omitNullValues()
                .add("parallelThreads", if (parallelThreads < 1) null else parallelThreads)
                .add("failFast", failFastMode)
                .add("profiles", specifiedProfiles)
                .add("logAssertions", isLogAssertions)
                .add("logMetrics", isLogMetrics)
                .add("debug", isDebug)
                .add("useSeleniumGrid", isUseSeleniumGrid)
                .add("gridUrl", seleniumGridUrl.orElse(null))
                .toString()
    }
}
