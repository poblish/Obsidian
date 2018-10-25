package org.hiatusuk.obsidian.web.selenium.cmd

import com.google.common.base.MoreObjects.toStringHelper
import com.google.common.base.Strings
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.slf4j.Logger
import javax.inject.Inject

@ScenarioScope
@Command("dump")
class DomDump @Inject
constructor(private val web: WebState, private val finders: ElementFinders, private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val req = finders.with(inCmd.string)
        log.debug("Dumping {}...", req)
        for ((idx, each) in req.all().withIndex()) {
            log.debug("Dump: [{}], {}", idx, toString(web.driver, each))
        }
    }

    companion object {

        fun toString(inDriver: WebDriver, elem: WebElement): String {
            val js = inDriver as JavascriptExecutor

            val allAttrs = js.executeScript("var items = {}; for (index = 0; index < arguments[0].attributes.length; ++index) { items[arguments[0].attributes[index].name] = arguments[0].attributes[index].value }; return items;", elem) as Map<String, String>// *Much* much more efficient than keep calling element.attribute()

            val id = allAttrs["id"]
            val name = allAttrs["name"]
            val href = allAttrs["href"]
            val clazz = allAttrs["class"]
            val disabled = allAttrs["disabled"]

            var selected = false
            try {
                selected = elem.isSelected
            } catch (e: RuntimeException) {
                // Ignore. Seen a UnsupportedOperationException from HtmlUnit
            }

            val h = toStringHelper("Element").omitNullValues()
                    .add("tag", elem.tagName)
                    .add("id", Strings.emptyToNull(id))
                    .add("class", Strings.emptyToNull(clazz))
                    .add("name", Strings.emptyToNull(name))
                    .add("text", Strings.emptyToNull(elem.text))
                    .add("href", Strings.emptyToNull(href))
                    .add("size", elem.size)
                    .add("disabled", Strings.emptyToNull(disabled))
                    .add("displayed", if (elem.isDisplayed) null else "NO")
                    .add("selected", if (selected) "YES" else null)
                    .add("enabled", if (elem.isEnabled) null else "NO")
            return h.toString()
        }
    }
}
