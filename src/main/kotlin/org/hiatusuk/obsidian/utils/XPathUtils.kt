package org.hiatusuk.obsidian.utils

import java.util.regex.Pattern

object XPathUtils {

    // Basically anything starting with // or xpath( or xpath=(
    private val XPATH_PATTERN = Pattern.compile("^(\\/\\/|xpath\\s*\\()", Pattern.CASE_INSENSITIVE)

    fun isXPath(inStr: String): Boolean {
        return XPATH_PATTERN.matcher(inStr).find()
    }

    fun unwrapXPath(inStr: String): String {
        if (inStr.startsWith("xpath")) {
            var s = inStr.substring(5)
            val idx = s.indexOf('(')
            if (idx >= 0) {
                s = s.substring(idx + 1)
            }
            if (s.endsWith(")")) {
                s = s.substring(0, s.length - 1)
            }
            return s.trim()
        }
        return inStr
    }
}
