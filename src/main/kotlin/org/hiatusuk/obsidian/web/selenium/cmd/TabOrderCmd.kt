package org.hiatusuk.obsidian.web.selenium.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.openqa.selenium.Keys
import java.util.*
import javax.inject.Inject

@ScenarioScope
@Command("tab-order")
class TabOrderCmd @Inject
constructor(private val web: WebState, private val finders: ElementFinders) : CommandIF {

    override fun run(inCmd: CommandSpec) {

        // Essentially we build a list of unique descriptors for the expected tab-order elements...
        val expectedElementsState = LinkedHashSet<String>()
        val expectedElemIdentifiers = inCmd.propertyNames()

        for (eachIdentifier in expectedElemIdentifiers) {
            val expectedElem = finders.with(eachIdentifier).first()
            val almostUniqueKey = DomDump.toString(web.driver, expectedElem)
            expectedElementsState.add(almostUniqueKey)
        }

        // Now work through the actual tab-order, building an actual set of unique descriptors... which should match
        val elementsState = LinkedHashSet<String>()
        var idx = 0

        while (idx < expectedElemIdentifiers.size) {
            val e = web.driver.switchTo().activeElement()

            if (e.tagName == "body" || !e.isDisplayed || !e.isEnabled) {
                continue
            }

            val almostUniqueKey = DomDump.toString(web.driver, e)
            elementsState.add(almostUniqueKey)

            e.sendKeys(Keys.TAB)
            idx++
        }

        if (expectedElementsState != elementsState) {
            throw RuntimeException("Tab-order mismatch. Expected: $expectedElementsState, found: $elementsState")
        }
    }
}