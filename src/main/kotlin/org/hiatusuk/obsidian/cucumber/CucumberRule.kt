package org.hiatusuk.obsidian.cucumber

import org.hiatusuk.obsidian.utils.StringUtils
import java.util.regex.Pattern

class CucumberRule internal constructor(private val fullStr: String) {
    val pattern: Pattern

    init {
        val m = SPLIT.matcher(fullStr)

        if (!m.find()) {
            throw RuntimeException("Could not parse Gherkin rule: '$fullStr'")
        }

        // final String type = m.group(1);
        pattern = Pattern.compile(StringUtils.replace(m.group(2), "?;", "?:"))
    }

    internal fun getFullStr(): String {
        return "^$fullStr"
    }

    companion object {
        private val SPLIT = Pattern.compile("(Given|When|Then|And|But)\\(\"(.*)\"\\)")
    }
}
