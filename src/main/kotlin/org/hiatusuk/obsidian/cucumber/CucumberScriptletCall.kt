package org.hiatusuk.obsidian.cucumber

import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.run.ScriptletCall
import java.util.Arrays
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.indices
import kotlin.collections.set

class CucumberScriptletCall internal constructor(private val varCtxt: VariablesContext,
                                                 private val command: CucumberRule,
                                                 args: Array<Any>) : ScriptletCall() {

    private val args: Array<Any> = args.clone()
    private var prevArgValues: Map<String, Any>? = null

    override fun matchesScenario(inScenarioName: String): Boolean {
        // Use contains because we might be matching...
        //    ^Given(".*searched for \"(.*)\"")
        // within a scenario named:
        //     ^Given(".*searched for \"(.*)\"") ^When(".*search(?:es)? for \"(.*)\""):
        return inScenarioName.contains(command.getFullStr())
    }

    override fun arguments(): List<Any> {
        return Arrays.asList(*args)
    }

    override fun enterFilter(fullMethodNameWithArgs: String) {

        // Build the Map of filled-in input params to push to the VarCtxt
        val replacementCtxtValues = HashMap<String, Any>()
        for (i in args.indices) {
            replacementCtxtValues[(i + 1).toString()] = args[i]
        }

        prevArgValues = this.varCtxt.push(replacementCtxtValues)  // Return the original state for re-popping
    }

    override fun exitFilter() {
        this.varCtxt.pop(prevArgValues!!)
    }
}
