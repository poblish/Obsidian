package org.hiatusuk.obsidian.web.selenium.utils

import org.hiatusuk.obsidian.utils.StringUtils.titleCase
import org.openqa.selenium.By
import org.openqa.selenium.HasCapabilities
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.Select
import java.util.regex.Pattern

object WebDriverUtils {

    private val WS_PATTERN = Pattern.compile("\\s")

    fun getDriverName(driver: WebDriver): String? {
        return if (driver is HasCapabilities) titleCase((driver as HasCapabilities).capabilities.browserName) else "???"
    }

    fun elementToString(elem: WebElement): String {
        requireNotNull(elem) {"Element cannot be null"}

        val tagName = elem.tagName  // A remote call...

        if (tagName == "input") {
            val possText = elem.text
            // Let text() take precedence over value(). Don't know why - maybe a fool's errand
            return if (possText.isNullOrEmpty()) elem.getAttribute("value") ?: "" else possText
        }

        if (tagName == "select") {
            val texts = ArrayList<String>()
            for (eachSelection in Select(elem).allSelectedOptions) {
                texts.add(eachSelection.text)
            }
            return texts.joinToString(separator = ",")
        }

        if (tagName == "textarea") {  // Alarmingly needed fix on 7/8/2018
            return elem.getAttribute("value") ?: ""
        }

        val text = elem.text ?: ""

        // Replace any whitespace with spaces to tackle weird Safari internal line-breaking behaviour seen
        return WS_PATTERN.matcher(text).replaceAll(" ")
    }

    fun findEarliest(inParent: WebElement, inA: WebElement, inB: WebElement): WebElement? {
        if (inParent == inA) {
            return inA
        }

        if (inParent == inB) {
            return inB
        }

        // System.out.println(">>> Trying: " + inParent);

        for (eachChild in inParent.findElements(By.xpath("*"))) {
            val we = findEarliest(eachChild, inA, inB)
            if (we != null) {
                return we
            }
        }

        return null
    }
}
