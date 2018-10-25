package org.hiatusuk.obsidian.web.utils

import org.hiatusuk.obsidian.utils.StringUtils.replace

object EncodeUtils {

    fun fixQueryEncoding(inUrl: String): String {
        val qIdx = inUrl.indexOf('?')
        if (qIdx < 0) {
            return inUrl
        }

        val qStr = replace(replace(inUrl.substring(qIdx + 1), "+", "%20"), "\"", "%22")
        return inUrl.substring(0, qIdx + 1) + qStr
    }
}
