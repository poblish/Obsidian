package org.hiatusuk.obsidian.di.modules

import com.codahale.metrics.MetricRegistry
import dagger.Module
import dagger.Provides
import org.hiatusuk.obsidian.run.delegates.ReflectionsState
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Module
class ApplicationModule {

    @Provides
    @Singleton
    internal fun logger(): Logger {
        return LoggerFactory.getLogger("Main")
    }

    @Provides
    @Singleton
    internal fun metricRegistry(): MetricRegistry {
        return MetricRegistry()
    }

    // Define explicitly to ensure we can inject into @ScenarioScope
    @Provides
    @Singleton
    internal fun reflectionsState(metrics: MetricRegistry): ReflectionsState {
        return ReflectionsState(metrics)
    }

    @Provides
    @Singleton
    @Named("outputDir")
    internal fun outputDir(): File {
        val f = File(System.getProperty("user.home"), "Obsidian Output")
        f.mkdirs()
        return f
    }
}
