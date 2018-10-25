package org.hiatusuk.obsidian.cucumber

import com.google.common.base.MoreObjects
import cucumber.runtime.StepDefinition
import io.cucumber.stepexpression.Argument
import gherkin.pickles.PickleStep
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.run.ScriptletCall
import java.util.regex.Pattern

class ObsidianStepDefinition
internal constructor(private val varCtxt: VariablesContext,
                     private val calls: MutableList<ScriptletCall>,
                     private val patterns: List<Pattern>,  // Why? Only way to do AND when a Step has multiple rules
                     private val coll: CucumberRuleList) : StepDefinition {

    override fun matchedArguments(step: PickleStep): List<Argument>? {
        val arguments = arrayListOf<Argument>()
        var gotMatch = false

        for (eachPattern in patterns) {
            val matcher = eachPattern.matcher(step.text)
            val matched = matcher.lookingAt()

            if (matched) {
                gotMatch = true
                for (i in 1..matcher.groupCount()) {
                    val startIndex = matcher.start(i)
                    if (startIndex >= 0) {
                        arguments.add(Argument { matcher.group(i) })
                    }
                }

                break
//                if (!MULTIPLE_RULES_MEANS_AND_NOT_OR) {
//                    break
//                }
            }
            /* else if (MULTIPLE_RULES_MEANS_AND_NOT_OR) {
                if (!arguments.isEmpty()) {
                    println("FAIL existing matched args ($arguments) as follow-up pattern doesn't match: $eachPattern")
                }
                return null  // If *any* fail, reject the lot, i.e. AND
            } */
        }

        return if (!gotMatch) {
            null
        } else arguments
    }

    override fun getLocation(detail: Boolean): String {
        return MoreObjects.toStringHelper("Location").add("idx", coll.sectionNo).add("Pattern", coll.first().pattern).toString()
    }

    override fun getParameterCount(): Int? {
        return null  // NOOP
    }

    override fun execute(args: Array<Any>) {
        for (each in coll) {
            calls.add(CucumberScriptletCall(this.varCtxt, each, args))
        }
    }

    override fun isDefinedAt(elem: StackTraceElement): Boolean {
        return false
    }

    override fun getPattern(): String {
        return this.patterns.first().pattern()  // Ugh, FIXME. Think this is just for logging/debug tho
    }

    override fun isScenarioScoped(): Boolean {
        return false
    }

//    companion object {
//        private const val MULTIPLE_RULES_MEANS_AND_NOT_OR = false
//    }
}