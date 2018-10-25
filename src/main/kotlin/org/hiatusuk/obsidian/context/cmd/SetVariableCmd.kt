package org.hiatusuk.obsidian.context.cmd

import org.hiatusuk.obsidian.asserts.lookups.AssertLookupsRegistry
import org.hiatusuk.obsidian.asserts.lookups.AssertSpecs
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.delegates.ExtractionHandler
import org.hiatusuk.obsidian.utils.ParseUtils
import org.hiatusuk.obsidian.utils.TerminalColours
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.hiatusuk.obsidian.web.selenium.find.InvalidCssException
import org.hiatusuk.obsidian.web.selenium.utils.WebDriverUtils
import org.slf4j.Logger
import java.io.IOException
import javax.inject.Inject

@ScenarioScope
@Command(value = "set \\S+", regex = true)
class SetVariableCmd @Inject
internal constructor(private val lookupsRegistry: AssertLookupsRegistry,
                     private val finders: ElementFinders,
                     private val varCtxt: VariablesContext,
                     private val log: Logger) : CommandIF {

    @Throws(IOException::class)
    override fun run(inCmd: CommandSpec) {

        val whenFilter = inCmd.optBoolean("when")  // Return if an (optional) boolean "when" clause resolves to false
        if (!whenFilter.orElse(true)) {
            return
        }

        val varName = inCmd.name.substring(4).trim()
        val strValue = inCmd.string

        val extractResult = ExtractionHandler().matchString(strValue)

        if (extractResult.matched()) {
            val thingToExtractFrom = extractResult.contents!!

            if (strValue.startsWith("!")) {
                varCtxt.store(varName, extractResult.extractString(thingToExtractFrom))
                return
            }

            val registryResult = lookupsRegistry.lookupValueForTargetIdentifier(thingToExtractFrom, AssertSpecs(emptyMap()))
            if (registryResult != null) {
                varCtxt.store(varName, extractResult.extractString(/* Yuk, API abuse */ registryResult.first().text))
                return
            }

            try {
                val req = finders.with(thingToExtractFrom)
                log.info("< Setting variable '{}' to text of {}{}{} that matches regex '{}' >", varName, TerminalColours.assertClauseColour(), req, TerminalColours.reset(), extractResult.pattern)
                varCtxt.store(varName, extractResult.extractString( WebDriverUtils.elementToString(req.first()) ))
            }
            catch (e: InvalidCssException) {
                // We thought it might be a CSS path, it wasn't. So just return the value as-is.
                varCtxt.store(varName, extractResult.extractString(thingToExtractFrom))
            }

            return
        }

        ///////////////////////////////////////////////  Non-extract-based

        if (strValue.startsWith("!")) {  // Is plain text
            varCtxt.store(varName, strValue.substring(1))
        }
        else if (ParseUtils.parseAsDouble(strValue) != null) {  // Is numeric?
            varCtxt.store(varName, strValue)
        }
        else
        /* Any other lookup! */ {
            val registryResult = lookupsRegistry.lookupValueForTargetIdentifier(strValue, AssertSpecs(emptyMap()))
            if (registryResult != null) {
                varCtxt.store(varName, /* Yuk, API abuse */ registryResult.first().text)
            } else {  // Assume a CSS path or equiv
                try {
                    val req = finders.with(strValue)
                    log.info("< Setting variable '{}' to text of {}{}{} >", varName, TerminalColours.assertClauseColour(), req, TerminalColours.reset())
                    varCtxt.store(varName, WebDriverUtils.elementToString(req.first()))
                }
                catch (e: InvalidCssException) {
                    // We thought it might be a CSS path, it wasn't. So just return the value as-is.
                    varCtxt.store(varName, strValue)
                }
            }
        }
    }
}
