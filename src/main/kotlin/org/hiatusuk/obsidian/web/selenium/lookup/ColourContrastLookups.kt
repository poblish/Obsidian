package org.hiatusuk.obsidian.web.selenium.lookup

import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.LookupUtils
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.RunProperties
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.web.selenium.find.ElementFinders
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.slf4j.Logger
import java.util.regex.Pattern
import javax.inject.Inject

@AssertLookup("contrast\\(")
@ScenarioScope
class ColourContrastLookups @Inject
constructor(private val finders: ElementFinders, private val runProps: RunProperties, private val exceptions: RuntimeExceptions, private val log: Logger) {

    fun lookup(targetIdentifier: String): Collection<AssertTarget> {
        val p = Pattern.compile("contrast\\((.*)\\)\\.([A-Z\\-]*)\\(\\)", Pattern.CASE_INSENSITIVE)
        val m = p.matcher(targetIdentifier)
        if (!m.find()) {
            throw exceptions.runtime("Malformed Contrast Assert: '$targetIdentifier'")
        }

        val req = finders.with(m.group(1).trim())

        if (runProps.isLogAssertions) {
            log.info("< Asserting against $req >")
        }

        val elem = req.first()

        val bgImage = elem.getCssValue("background-image")
        if (bgImage != null && !bgImage.equals("none", ignoreCase = true)) {
            throw exceptions.runtime("Cannot calculate contrast-ratio with 'background-image:$bgImage'")
        }

        val propertyName = m.group(2)

        val fgColor = getInheritedColor(elem, "color")
        val bgColor = getInheritedColor(elem, "background-color")

        val fgLum = getLuminance(fgColor)
        val bgLum = getLuminance(bgColor)
        val ratio = Math.round((Math.max(fgLum, bgLum) + 0.05) / (Math.min(fgLum, bgLum) + 0.05) * 100) / 100.0  // 2 dp

        log.trace("fgColor: {}, bgColor: {}, => {}, {} => ratio: {}", fgColor, bgColor, fgLum, bgLum, ratio)

        if (propertyName == "ratio") {
            return LookupUtils.singleTarget(ratio)
        }

        if (propertyName != "checkWcagAA" && propertyName != "checkWcagAAA") {
            throw RuntimeException("Unknown property: $propertyName")
        }

        // See: http://www.w3.org/TR/UNDERSTANDING-WCAG20/visual-audio-contrast-contrast.html

        val fontSizePx = getInheritedSize(elem, "font-size")
        val fontSizePt = java.lang.Double.parseDouble(fontSizePx.replace("px", "")) / 96.0 * 72.0

        val fontWeight = getInheritedFontWeight(elem)
        val isBold = fontWeight.startsWith("bold") || fontWeight == "700" || fontWeight == "800" || fontWeight == "900"
        val isLarge = fontSizePt >= 18 || isBold && fontSizePt >= 14

        log.trace("fontSizePx: {}, fontSizePt: {}, fontWeight: {}, isBold: {}, isLarge: {}", fontSizePx, fontSizePt, fontWeight, isBold, isLarge)

        if (propertyName == "checkWcagAA") {
            if (isLarge && ratio < 3 || ratio < 4.5) {
                return LookupUtils.singleTarget("fail")
            }
        } else /* AAA */ if (isLarge && ratio < 4.5 || ratio < 7) {
            return LookupUtils.singleTarget("fail")
        }

        return LookupUtils.singleTarget("pass")
    }

    // FIXME Factor out somewhere
    private fun getInheritedFontWeight(inElem: WebElement): String {
        val weight = inElem.getCssValue("font-weight")
        if (weight != null && weight != "initial" && weight != "inherited") {
            return weight
        }

        val parent = inElem.findElement(By.xpath(".."))
        return if (parent != null) {
            getInheritedFontWeight(parent)
        } else ""

    }

    // FIXME Factor out somewhere
    private fun getInheritedColor(inElem: WebElement, cssName: String): String {
        val name = inElem.getCssValue(cssName)
        if (name != null && !isTransparentColor(name) && name != "inherited") {
            return name
        }

        val parent = inElem.findElement(By.xpath(".."))
        return if (parent != null) {
            getInheritedColor(parent, cssName)
        } else ""

    }

    // FIXME Surely we should handle different opacity, not opaque vs transparent only?!
    private fun isTransparentColor(value: String): Boolean {
        if (value == "transparent") {
            return true
        }

        val rgba = parseRgba(value)
        return rgba.size > 3 && rgba[3].trim() == "0"
    }

    // FIXME Factor out somewhere
    private fun getInheritedSize(inElem: WebElement, cssName: String): String {
        val cssValue = inElem.getCssValue(cssName)
        if (cssValue != null && cssValue != "inherited") {
            return cssValue
        }

        val parent = inElem.findElement(By.xpath(".."))
        return if (parent != null) {
            getInheritedSize(parent, cssName)
        } else ""

    }

    private fun parseRgba(value: String): Array<String> {
        // See: http://stackoverflow.com/a/21307354/954442
        return value.replace("rgba(", "").replace(")", "").split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    }

    private fun getLuminance(colourStr: String?): Double {
        if (colourStr == null || colourStr.isEmpty() || /* Hmmm? */ colourStr == "transparent") {
            // FIXME For time being assume 100% luminant background, but should check parent...?
            return 1.0
        }

        val numbers = parseRgba(colourStr)
        val r = Integer.parseInt(numbers[0].trim())
        val g = Integer.parseInt(numbers[1].trim())
        val b = Integer.parseInt(numbers[2].trim())

        // See: http://stackoverflow.com/a/596243/954442
        return 0.2126 * getsRGB(r / 255.0) + 0.7152 * getsRGB(g / 255.0) + 0.0722 * getsRGB(b / 255.0)
    }

    private fun getsRGB(rgbValZeroOne: Double): Double {
        return if (rgbValZeroOne <= 0.03928) rgbValZeroOne / 12.92 else Math.pow((rgbValZeroOne + 0.055) / 1.055, 2.4)
    }
}
