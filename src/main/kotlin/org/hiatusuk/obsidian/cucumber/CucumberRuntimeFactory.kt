package org.hiatusuk.obsidian.cucumber

import cucumber.api.Result.Type.*
import cucumber.api.event.TestCaseFinished
import cucumber.api.event.TestStepFinished
import cucumber.runner.TimeService
import cucumber.runner.TimeServiceEventBus
import cucumber.runtime.Runtime
import cucumber.runtime.RuntimeOptionsFactory
import cucumber.runtime.io.FileResource
import cucumber.runtime.io.ResourceLoader
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.cucumber.delegates.CucumberRunStats
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CucumberRuntimeFactory @Inject
internal constructor(private val varCtxt: VariablesContext,
                     private val stats: CucumberRunStats) {

    fun create(backend: ObsidianBackend, inCukeInputs: CucumberInputs): Runtime {
        return CucumberRuntime.create(backend, inCukeInputs, this.varCtxt, this.stats)
    }

    object CucumberRuntime {

        fun create(backend: ObsidianBackend,
                   inCukeInputs: CucumberInputs,
                   varCtxt: VariablesContext,
                   stats: CucumberRunStats): Runtime {

            val runtimeOptions = RuntimeOptionsFactory(CucumberRuntime::class.java).create()
            runtimeOptions.featurePaths.addAll( inCukeInputs.scenarios.map { it.absolutePath } )

            ///////////////////////////////////////////////////////  Start Stats collectors

            val bus = TimeServiceEventBus(TimeService.SYSTEM)  // Same as Cucumber default

            bus.registerHandlerFor(TestCaseFinished::class.java) {
                stats.scenarioStats[it.result.status] = stats.scenarioStats.getOrDefault(it.result.status, 0) + 1  // yuk!
                stats.scenarioCount++
            }
            bus.registerHandlerFor(TestStepFinished::class.java) {
                stats.stepStats[it.result.status] = stats.stepStats.getOrDefault(it.result.status, 0) + 1  // yuk!
                stats.stepCount++
            }

            stats.clear()

            varCtxt.store("cucumberStats", CucumberStatsBean(stats))

            //////////////////////////////////////////////////////////////////////////////////

            return Runtime.builder()
                    .withEventBus(bus)
                    .withRuntimeOptions(runtimeOptions)
                    .withBackendSupplier { arrayListOf(backend) }
                    .withResourceLoader((ResourceLoader {
                        _, _ ->
                        inCukeInputs.scenarios.map { it -> FileResource.createFileResource(it,it) } }))
                    .build()
        }
    }

    @Suppress("unused")
    internal class CucumberStatsBean(private val stats: CucumberRunStats) {

        fun scenariosPassed(): Int {
            return stats.scenarioStats.getOrDefault(PASSED, 0)
        }

        fun scenarioErrors(): Int {
            return stats.scenarioStats.getOrDefault(FAILED, 0)
        }

        fun stepsPassed(): Int {
            return stats.stepStats.getOrDefault(PASSED, 0)
        }

        fun stepsSkipped(): Int {
            return stats.stepStats.getOrDefault(SKIPPED, 0)
        }

        fun stepErrors(): Int {
            return stats.stepStats.getOrDefault(FAILED, 0)
        }
    }
}
