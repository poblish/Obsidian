package org.hiatusuk.obsidian.cmd.api

import com.codahale.metrics.MetricRegistry
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.delegates.CommandNamespaces
import javax.inject.Inject

@ScenarioScope
class CommandSpecFactory @Inject
internal constructor(private val ns: CommandNamespaces, private val ctxt: VariablesContext, private val metrics: MetricRegistry) {

    fun create(inCmdName: String, inValue: Any): CommandSpec {
        metrics.timer("CommandSpecFactory.createForObject").time().use { return CommandSpec(ctxt, ns, inCmdName, inValue) }
    }

    fun create(inData: Map<String, Any>): CommandSpec {
        metrics.timer("CommandSpecFactory.createForMap").time().use { return CommandSpec(ctxt, ns, inData, false) }
    }

    fun createForValidation(inData: Map<String, Any>): CommandSpec {
        metrics.timer("CommandSpecFactory.createForValidation").time().use { return CommandSpec(ctxt, ns, inData, true) }
    }
}
