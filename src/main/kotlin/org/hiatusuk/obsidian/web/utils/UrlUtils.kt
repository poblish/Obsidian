package org.hiatusuk.obsidian.web.utils

import java.io.File
import java.net.URI
import java.net.URISyntaxException

import org.hiatusuk.obsidian.utils.StringUtils

object UrlUtils {

    fun expandAndCorrectUrl(inRawUrl: String, inCurrentUrl: String?): String {
        val url = EncodeUtils.fixQueryEncoding( StringUtils.stripQuotes(inRawUrl) )

        if (url.startsWith("local:")) {
            // E.g. local:src/test/resources/Demo/static_pages/ipsum.html => file:///Users/me/Dir/obsidian/src/test/resources/Demo/static_pages/ipsum.html
            return File(url.substring(6)).toURI().toString()
        }

        // Could this be a relative path?
        if (inCurrentUrl != null && !inCurrentUrl.isEmpty()) {
            try {
                if (!URI(inRawUrl).isAbsolute) {  // Is a relative URL
                    val lastSlashIdx = inCurrentUrl.lastIndexOf('/')
                    if (lastSlashIdx >= 0) {  // Sanity check
                        val parent = inCurrentUrl.substring(0, lastSlashIdx)
                        return parent + if (inRawUrl.startsWith("/")) inRawUrl else "/$inRawUrl"
                    }
                }
            } catch (e: URISyntaxException) {
                // Ignore, not worth the hassle
            }

        }

        return url
    }
}
