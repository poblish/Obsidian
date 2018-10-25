package org.hiatusuk.obsidian.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.ScriptletCall
import org.hiatusuk.obsidian.run.exceptions.MatchingScenarioNotFoundException
import org.hiatusuk.obsidian.run.external.ExternalHandlers
import org.hiatusuk.obsidian.run.state.ScenarioState
import java.util.regex.Pattern
import javax.inject.Inject

@ScenarioScope
@Command("call")
class CallSubroutineCmd @Inject
internal constructor(private val scenario: ScenarioState,
                     private val handlers: ExternalHandlers,
                     private val varCtxt: VariablesContext) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val payload = inCmd.string
        val desiredMethodNameMatcher = METHOD_NAME.matcher(payload)
        if (!desiredMethodNameMatcher.find()) {
            throw RuntimeException("Could not parse method name")
        }

        val methodNameToFind = desiredMethodNameMatcher.group(1).trim()
        val methodNameSigMatchingPattern = Pattern.compile("\\^$methodNameToFind$SPACES_PATTERN\\([^\\)]*\\)")

        val suppliedArgsStr = desiredMethodNameMatcher.group(2).trim()
        val suppliedArgValues = if (suppliedArgsStr.isEmpty()) EMPTY else splitCommas(suppliedArgsStr)  // Empty strings *are* allowed

        val handleCall = fun() {

            val call = SubroutineScriptletCall(varCtxt, methodNameSigMatchingPattern, suppliedArgValues)

            // Try the current scenario file first...
            for (eachDoc in scenario.currentScenarioYaml()) {
                try {
                    handlers.handleScenarioElement(eachDoc, call)
                    return   // Found!!
                } catch (e: MatchingScenarioNotFoundException) {
                    if (handlers.hasHandlerScripts()) {
                        break  // Not in the current scenario, fine. Bail out, let's try the handler scripts
                    }

                    throw e  // Nothing left to try, must propagate 'missing method' error
                }

            }

            // Fall back to trying the registered handler scripts
            handlers.callHandler(call)
        }

        return handlers.wrapRunner(handleCall)
    }

    private class SubroutineScriptletCall internal constructor(private val varCtxt: VariablesContext, methodNameSigMatchingPattern: Pattern, private val suppliedArgValues: List<String>) : ScriptletCall(methodNameSigMatchingPattern) {

        private var prevArgValues: Map<String, Any>? = null

        override fun enterFilter(fullMethodNameWithArgs: String) {
            val methodSigMatcher = VALUES_PATTERN.matcher(fullMethodNameWithArgs)
            methodSigMatcher.find()

            // Decode the method declaration to extract the expected arg names
            val declaredArgNames = splitCommas(methodSigMatcher.group(1)).filter { it.isNotBlank() }  // Empty strings *not* allowed
            if (declaredArgNames.size != this.suppliedArgValues.size) {
                throw RuntimeException("Number of subroutine values: " + this.suppliedArgValues + " does not match parameters: " + declaredArgNames)
            }

            // Build the Map of filled-in input params to push to the VarCtxt
            val replacementCtxtValues = HashMap<String, Any>()
            for (argIdx in declaredArgNames.indices) {
                replacementCtxtValues[declaredArgNames[argIdx]] = this.suppliedArgValues[argIdx]
            }

            prevArgValues = this.varCtxt.push(replacementCtxtValues)  // Return the original state for re-popping
        }

        override fun exitFilter() {
            this.varCtxt.pop(prevArgValues!!)
        }
    }

    companion object {

        private const val SPACES_PATTERN = "\\s?"
        private const val VALUES_MATCH_PATTERN = "\\(([^\\)]*)\\)"

        private val VALUES_PATTERN = Pattern.compile(VALUES_MATCH_PATTERN)
        private val METHOD_NAME = Pattern.compile("(.*)$SPACES_PATTERN$VALUES_MATCH_PATTERN")

        private val EMPTY = emptyList<String>()

        private fun splitCommas(str: String): List<String> {
            return str.split(',').map { it.trim() }
        }
    }
}
