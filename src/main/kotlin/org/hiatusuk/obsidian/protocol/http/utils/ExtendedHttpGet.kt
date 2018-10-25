package org.hiatusuk.obsidian.protocol.http.utils

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase

/**
 * A GET implementation (a copy of PUT) that supports entities, just like curl does
 *
 */
class ExtendedHttpGet : HttpEntityEnclosingRequestBase() {

    override fun getMethod(): String {
        return METHOD_NAME
    }

    companion object {
        const val METHOD_NAME = "GET"
    }
}