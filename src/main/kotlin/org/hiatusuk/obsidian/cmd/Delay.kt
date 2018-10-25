package org.hiatusuk.obsidian.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.delegates.DelayHandler
import org.hiatusuk.obsidian.utils.ParseUtils
import javax.inject.Inject

@ScenarioScope
@Command("delay")
class Delay @Inject
constructor(private val delayHandler: DelayHandler, private val ctxt: VariablesContext) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val od = inCmd.optDuration()  // E.g. "2 minutes"
        if (od.isPresent) {
            delayHandler.doDelay(od.get().toMillis())
        } else {
            // Couldn't parse a proper duration value? Just fall back to millisecond value.
            // Oh, and make 1 sec the default for blank, rather than 'do nothing at all'
            delayFor(inCmd.optInteger().orElse(1000).toLong())
        }
    }

    fun run(inVal: Any?, inDefault: Int) {
        if (inVal == null) {
            delayFor(inDefault.toLong())
            return
        }

        delayFor(ParseUtils.valueToInt(inVal, ctxt).toLong())
    }

    fun delayFor(msecs: Long) {
        if (msecs <= 0) {
            return   // NOOP
        }

        delayHandler.doDelay(msecs)
    }
}
