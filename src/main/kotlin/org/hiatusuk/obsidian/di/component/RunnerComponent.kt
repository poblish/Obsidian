package org.hiatusuk.obsidian.di.component

import dagger.BindsInstance
import dagger.Component
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.di.includes.AssertLookupsComponent
import org.hiatusuk.obsidian.di.includes.CommandsComponent
import org.hiatusuk.obsidian.di.includes.ConfigsComponent
import org.hiatusuk.obsidian.di.includes.EventsComponent
import org.hiatusuk.obsidian.di.modules.RunnerModule
import org.hiatusuk.obsidian.run.RunInputs
import org.hiatusuk.obsidian.run.RunProperties
import org.hiatusuk.obsidian.run.ScenarioRunner

@ScenarioScope
@Component(dependencies = [ApplicationComponent::class], modules = [RunnerModule::class])
interface RunnerComponent {

    val scenarioRunner: ScenarioRunner

    @Component.Builder
    interface Builder {

        fun application(app: ApplicationComponent): Builder

        @BindsInstance
        fun props(runProps: RunProperties): Builder

        @BindsInstance
        fun inputs(runProps: RunInputs): Builder

        fun build(): RunnerComponent
    }

    // Includes...
    fun newEventsComponent(): EventsComponent
    fun newCommandsComponent(): CommandsComponent
    fun newConfigsComponent(): ConfigsComponent
    fun newAssertLookupsComponent(): AssertLookupsComponent
}
