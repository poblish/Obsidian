package org.hiatusuk.obsidian.utils

import org.apache.commons.lang.SystemUtils

object TerminalColours {

    private val IS_UNIX = SystemUtils.IS_OS_UNIX

    fun green(): String {
        return if (IS_UNIX) {
            "\u001b[0;32m"
        } else ""
    }

    fun greenBold(): String {
        return if (IS_UNIX) {
            "\u001b[0;1;32m"
        } else ""
    }

    fun darkGreenBI(): String {
        return if (IS_UNIX) {
            "\u001b[0;1;4;38;5;28m"
        } else ""
    }

    fun reset(): String {
        return if (IS_UNIX) {
            "\u001b[0;39m"
        } else ""
    }

    fun assertClauseColour(): Any {
        return if (IS_UNIX) {
            "\u001b[0;38;5;178m"
        } else ""
    }

    fun error(): Any {
        return if (IS_UNIX) {
            "\u001b[0;31m"
        } else ""
    }

    fun complete(): Any {
        return if (IS_UNIX) {
            "\u001b[104;97m"
        } else ""
    }
}
