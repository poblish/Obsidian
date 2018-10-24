package org.hiatusuk.obsidian.di.component

import com.codahale.metrics.MetricRegistry
import dagger.Component
import org.hiatusuk.obsidian.di.modules.ApplicationModule
import org.hiatusuk.obsidian.run.delegates.ApplicationShutdownState
import org.hiatusuk.obsidian.run.delegates.ReflectionsState
import org.slf4j.Logger
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class])
interface ApplicationComponent {

    fun logger(): Logger

    fun metricRegistry(): MetricRegistry

    @Named("outputDir")
    fun outputDir(): File

    // Singleton/App-scope deps we expose to ScenarioScope / RunnerComponent must be listed here
    fun reflectionsState(): ReflectionsState
    fun applicationShutdownState(): ApplicationShutdownState
}
