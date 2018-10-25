package org.hiatusuk.obsidian.cucumber

import cucumber.runtime.Backend
import cucumber.runtime.Glue
import cucumber.runtime.snippets.FunctionNameGenerator
import gherkin.pickles.PickleStep
import org.hiatusuk.obsidian.run.ScenarioRunner
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.run.RunInputs
import org.hiatusuk.obsidian.run.ScenarioParsing
import org.hiatusuk.obsidian.run.ScriptletCall
import org.yaml.snakeyaml.Yaml
import java.io.IOException
import java.util.regex.Pattern

class ObsidianBackend(inCukeInputs: CucumberInputs, private val tester: ScenarioRunner, private val varCtxt: VariablesContext) : Backend {

    private val allCalls = ArrayList<ScriptletCall>()
    private val yamlInst = Yaml()
    private val handlers = getHandlers(RunInputs(inCukeInputs.implementations))

    @Throws(IOException::class)
    private fun getHandlers(inInputs: RunInputs): Map<CucumberRuleList, Any> {
        val handlers = LinkedHashMap<CucumberRuleList, Any>()

        for (eachInput in inInputs) {
            // System.out.println(eachScenarioFile);
            val yamlStr = ScenarioParsing.handleYamlIncludes(eachInput)

            var gherkinSectionNo = 0  // A simple dumb index within the scenario file. Not even a line count...

            // First pass, to populate VariablesContext
            for (eachDoc in yamlInst.loadAll(yamlStr)) {
                for ((_, value) in ScenarioParsing.elements(eachDoc, 0, ScenarioParsing.SETTERS)) {
                    val setMap = value as Map<String, Any>
                    for ((key, value1) in setMap) {
                        varCtxt.store(key, value1)
                    }
                }
            }

            // Second pass
            for (eachDoc in yamlInst.loadAll(yamlStr)) {
                for ((key, value) in ScenarioParsing.elements(eachDoc, 0) { it.key.startsWith("^") }) {
                    val resolvedVal = varCtxt.resolve(key.substring(1).trim() as String?)
                    handlers[CucumberRuleList(resolvedVal!!, gherkinSectionNo++)] = value
                }
            }
        }

        return handlers
    }

    override fun loadGlue(glue: Glue, gluePaths: List<String>) {
        for ((key) in handlers) {
            val pats = ArrayList<Pattern>()
            for (eachB in key) {
                pats.add(eachB.pattern)
            }

            //System.out.println("> " + each);
            glue.addStepDefinition(ObsidianStepDefinition(this.varCtxt, allCalls, pats, key))
        }
    }

    override fun getSnippet(step: PickleStep, keyword: String, functionNameGenerator: FunctionNameGenerator): List<String> {
        return arrayListOf("^" + keyword + "(\"" + step.text.trim().replace("\"", "\\\"") + "\"): <newline>- FIXME: Something here" + "\r")
    }

    override fun buildWorld() {
        allCalls.clear()
    }

    override fun disposeWorld() {
        if (allCalls.isEmpty()) {
            throw RuntimeException("NOTHING to run, though handlers = $handlers")
        }

        tester.startExternalHandlersRun(*allCalls.toTypedArray())
    }

    fun quit() {
        tester.quit()
    }
}