package org.hiatusuk.obsidian.run.delegates

import org.hiatusuk.obsidian.utils.StringUtils.stripQuotes
import org.slf4j.LoggerFactory
import java.util.regex.Pattern
import java.util.regex.Pattern.CASE_INSENSITIVE

class ExtractionHandler {

    fun matchString(inInput: String): Result {
        val trimmed = inInput.trim()

        val prefixMatcher = START_PATT.matcher(trimmed)
        if (!prefixMatcher.find()) {
            return Result()
        }

        val extractPattMatcher = END_PATT.matcher(trimmed)
        return if (!extractPattMatcher.find()) {
            Result()
        } else Result(stripQuotes(trimmed.substring(prefixMatcher.end(), extractPattMatcher.start()).trim()), extractPattMatcher.group(1))

    }

    class Result {
        private val matched: Boolean
        val contents: String?
        val pattern: String?

        internal constructor(contents: String, pattern: String) {
            this.contents = contents
            this.pattern = pattern
            this.matched = true
        }

        internal constructor() {  // "null" object
            this.contents = null
            this.pattern = null
            this.matched = false
        }

        fun matched(): Boolean {
            return matched
        }

        fun extractString(rawOutput: String): String {
            if (!matched || pattern == null) {
                return rawOutput
            }

            if (pattern == "(.*)") {
                LOG.warn("Pattern (.*) matches all, so extract(...) call is unnecessary")
                // Could simply return 'rawOutput' but let's not introduce a separate code path for a merely helpful warning
            }

            val outputMatcher = Pattern.compile(pattern, Pattern.DOTALL).matcher(rawOutput)

            if (!outputMatcher.find()) {
                throw RuntimeException("Could not find pattern <$pattern> within <$rawOutput>")
            }

            if (outputMatcher.groupCount() == 0) {
                throw RuntimeException("Matching pattern found, but no capture group specified for <$pattern> within <$rawOutput>")
            }
            return outputMatcher.group(1) ?: ""
        }
    }

    companion object {

        private val LOG = LoggerFactory.getLogger("Main")

        private val START_PATT = Pattern.compile("extract\\(", CASE_INSENSITIVE)
        private val END_PATT = Pattern.compile(",\\s*['\"]([^,]*)['\"]\\s*\\)$", CASE_INSENSITIVE)  // Pretty iffy... [^,]* !
    }
}
