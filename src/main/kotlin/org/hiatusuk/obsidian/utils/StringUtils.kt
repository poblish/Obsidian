package org.hiatusuk.obsidian.utils

import java.util.function.Supplier

object StringUtils {

    fun titleCase(s: String?): String? {
        if (s.isNullOrEmpty()) {
            return s
        }

        return if (s!!.length == 1) {
            s.toUpperCase()
        } else Character.toTitleCase(s[0]) + s.substring(1)

    }

    fun camelCase(s: String?): String? {
        if (s.isNullOrEmpty()) {
            return s
        }

        return if (s!!.length == 1) {
            s.toLowerCase()
        } else Character.toLowerCase(s[0]) + s.substring(1)

    }

    fun firstLine(s: String?): String? {
        if (s.isNullOrEmpty()) {
            return s
        }

        val idx = s!!.indexOf('\n')
        return if (idx >= 0) s.substring(0, idx) else s
    }

    @JvmOverloads
    fun checkSet(reference: String, message: String = "String cannot be null or empty"): String {
        require(reference.isNotBlank()) { message }
        return reference
    }

    fun replace(inString: String, oldPattern: String, newPattern: String?): String {
        if (inString.isEmpty() || oldPattern.isEmpty() || newPattern == null) {
            return inString
        }
        return inString.replace(oldPattern, newPattern)
    }

    fun replace(inString: String, oldPattern: String, newPatternSupplier: Supplier<String>?): String {
        if (inString.isEmpty() || oldPattern.isEmpty() || newPatternSupplier == null) {
            return inString
        }
        val sb = StringBuilder()
        var pos = 0 // our position in the old string
        var index = inString.indexOf(oldPattern)
        // the index of an occurrence we've found, or -1
        val patLen = oldPattern.length
        while (index >= 0) {
            sb.append(inString, pos, index)
            sb.append(newPatternSupplier.get())
            pos = index + patLen
            index = inString.indexOf(oldPattern, pos)
        }
        sb.append(inString.substring(pos))
        // remember to append any characters to the right of a match
        return sb.toString()
    }

    // Adapted from Hamcrest's BaseDescription
    private fun toJavaSyntax(unformatted: String): StringBuilder {
        val sb = StringBuilder()
        sb.append('"')
        for (i in 0 until unformatted.length) {
            toJavaSyntax(sb, unformatted[i])
        }
        return sb.append('"')
    }

    fun toJavaSyntax(inCSs: Collection<String>): List<String> {
        return inCSs.map { StringUtils.toJavaSyntax(it).toString() }
    }

    private fun toJavaSyntax(sb: StringBuilder, ch: Char) {
        when (ch) {
            '"' -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> sb.append(ch)
        }
    }

    fun stripQuotes(s: String?): String {
        if (s.isNullOrEmpty()) {
            return ""
        }
        return if (s!!.startsWith("\"") && s.endsWith("\"") || s.startsWith("'") && s.endsWith("'")) {
            stripQuotes(s.substring(1, s.length - 1))  // Any more?
        } else s
    }

     fun collapseWhitespace(toBeStripped: CharSequence): String {
        val result = StringBuilder()
        var lastWasSpace = true
        for (i in 0 until toBeStripped.length) {
            val c = toBeStripped[i]
            if (Character.isWhitespace(c)) {
                if (!lastWasSpace) {
                    result.append(' ')
                }
                lastWasSpace = true
            } else {
                result.append(c)
                lastWasSpace = false
            }
        }
        return result.toString().trim()
    }
}
