package org.hiatusuk.obsidian.run

import java.util.regex.Pattern

class SimpleMatchingRule(inName: String) {
    val pattern: Pattern = Pattern.compile("\\^$inName", Pattern.MULTILINE)
}
