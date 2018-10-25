package org.hiatusuk.obsidian.web.selenium.cmd

import org.hiatusuk.obsidian.cmd.Delay
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.delegates.ScenarioDefaultsContext
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.utils.StringUtils
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.openqa.selenium.ElementNotVisibleException
import org.openqa.selenium.Keys
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions
import org.slf4j.Logger
import javax.inject.Inject

// TODO Should really add a @Validate handler
@ScenarioScope
@Command("type")
class TypeCommand @Inject
internal constructor(private val web: WebState,
                     private val finders: ElementFinders,
                     private val delayCmd: Delay,
                     private val scenarioDefaults: ScenarioDefaultsContext,
                     private val exceptions: RuntimeExceptions, private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val typedText = inCmd.string
        val inField = inCmd.optString("in").orElse("")  // not-null

        // FIXME Need demo, need to use Robot to dismiss dalogs, as per http://stackoverflow.com/a/11539854/954442

        try {
            // In theory, should support anything from Keys enum, but be cautious and fast vs slow
            if (!handleSpecialKeySuffixes(inField, typedText, "return", "tab", "escape", "alt", "up", "down")) {
                if (!inField.isEmpty()) {
                    log.info("< Selenium type '$typedText' in '$inField' >")

                    val elem = getUsableWebElement(inField)
                    elem.clear()
                    elem.sendKeys(workAroundSendKeysText(typedText))
                } else {
                    log.info("< Selenium type '$typedText' using Actions >")
                    Actions(web.driver).sendKeys(workAroundSendKeysText(typedText)).perform()
                }
            }
        } catch (e: NoSuchElementException) {  // Less noisy error
            throw exceptions.noSuchWebElement("Could not find $inField")
        } catch (e: ElementNotVisibleException) {  // Less noisy error
            throw exceptions.webElementNotVisible("Element '$inField' exists, but is not visible and cannot be used")
        }

    }

    // Workaround for https://code.google.com/p/selenium/issues/detail?id=1723 etc.
    private fun workAroundSendKeysText(orig: String): String {
        return StringUtils.replace(orig, "(", Keys.chord(Keys.SHIFT, "9"))
    }

    private fun handleSpecialKeySuffixes(inField: String?, inTypedText: String, vararg specialSuffixesToCheck: String): Boolean {
        for (eachSuffix in specialSuffixesToCheck) {

            val adjText = if (inTypedText.endsWith(" $eachSuffix")) {
                inTypedText.substring(0, inTypedText.length - eachSuffix.length - 1)
            } else if (inTypedText == "$$eachSuffix") {
                ""
            } else {
                continue
            }

            if (inField != null && !inField.isEmpty()) {
                log.info("< Selenium type '" + adjText + " <" + eachSuffix.toUpperCase() + ">' in '" + inField + "' >")

                val elem = getUsableWebElement(inField)
                val elemTagName = elem.tagName.toLowerCase()

                // Ensure a better error message if people try to type into a <div> or something
                require(elemTagName == "input" || elemTagName == "textarea") {"Can only type into <input> or <textarea>, not <$elemTagName>"}

                elem.clear()
                elem.sendKeys(workAroundSendKeysText(adjText))
                elem.sendKeys(Keys.valueOf(eachSuffix.toUpperCase()))

                delayCmd.delayFor(scenarioDefaults.getElse("type", "thenWait", 0).toLong())
            } else {
                if (adjText.isEmpty()) {
                    log.info("< Selenium type <" + eachSuffix.toUpperCase() + "> using Actions >")
                } else {
                    log.info("< Selenium type '" + adjText + " <" + eachSuffix.toUpperCase() + ">' using Actions >")
                }

                Actions(web.driver)
                        .sendKeys(workAroundSendKeysText(adjText))
                        .sendKeys(Keys.valueOf(eachSuffix.toUpperCase()))
                        .perform()
            }

            return true
        }

        return false
    }

    private fun getUsableWebElement(identifier: String): WebElement {
        val endTime = System.currentTimeMillis() + /* FIXME Arbitrary timeout */ 5000
        var count = 0

        while (true) {

            try {
                val elem = finders.with(identifier).first()
                if (elem.isDisplayed && elem.isEnabled) {
                    return elem
                }
            } catch (ee: NoSuchElementException) {
                // Fall through
            } catch (ee: ElementNotVisibleException) {
            } catch (t: Throwable) {
                throw t  // Throw unanticipated exception
            }

            if (System.currentTimeMillis() < endTime) {
                if (count++ % 3 == 1) {  // Less noisy, i.e. about once per second
                    log.info("< Waiting for text element '{}' to become usable... >", identifier)
                }

                // Ignore, give another chance!
                Thread.sleep(334)
            } else
            /* Timed out!! Do what AssertCmd does */ {
                throw exceptions.runtime("Timeout waiting for editable element '$identifier' to become available")
            }
        }
    }
}
