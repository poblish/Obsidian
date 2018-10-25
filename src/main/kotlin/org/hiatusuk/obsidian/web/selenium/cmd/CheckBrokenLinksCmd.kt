package org.hiatusuk.obsidian.web.selenium.cmd

import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpHead
import org.apache.http.client.utils.URIBuilder
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.openqa.selenium.By
import org.slf4j.Logger
import java.io.IOException
import java.net.URISyntaxException
import javax.inject.Inject

@ScenarioScope
@Command("checkBrokenLinks")
class CheckBrokenLinksCmd @Inject
internal constructor(private val web: WebState,
                     private val http: HttpClient,
                     private val exceptions: RuntimeExceptions,
                     private val log: Logger) : CommandIF {

    @Throws(IOException::class)
    override fun run(inCmd: CommandSpec) {
        val expectFail = inCmd.optBoolean("expectFail").orElse(false)
        val hrefs = web.driver.findElements(By.tagName("a")).map { it.getAttribute("href").trim() }.toSet()

        if (hrefs.isEmpty()) {
            if (expectFail) {
                log.info("No links found on page")
                return
            }
            throw exceptions.runtime("Unexpectedly found no links on page")
        }

        if (hrefs.size == 1) {
            log.info("Testing one distinct URL...")
        } else {
            log.info("Testing {} distinct URLs...", hrefs.size)
        }

        val method = inCmd.optString("method").orElse("get")

        for (href in hrefs) {
            try {
                if (href.isEmpty() || href.endsWith("/#")) {
                    log.warn("Skipping blank/# (self) URL")
                    continue
                }

                var skip = false
                for (proto in SKIP_PROTOCOLS) {
                    if (href.startsWith(proto)) {
                        log.warn("Skipping {} protocol link: '{}'", proto, href)
                        skip = true
                        break
                    }
                }

                if (skip) {
                    continue
                }

                log.debug("> Requesting: {}", href)

                //////////////////////////////////////////////////////////////

                val theUri = URIBuilder(href).build()
                val resp: HttpResponse

                try {
                    resp = when (method.toLowerCase()) {
                        "get" -> http.execute(HttpGet(theUri))
                        "head" -> http.execute(HttpHead(theUri))
                        else -> throw exceptions.runtime("Unexpected HTTP verb: $method")
                    }
                } catch (e: IOException) {
                    if (expectFail) {
                        log.info("Expected to fail '{}'", theUri)
                    } else {
                        if (e.cause != null && e.cause is IOException) {
                            throw e.cause as IOException
                        }
                        throw e
                    }

                    continue
                }

                //////////////////////////////////////////////////////////////

                val code = resp.statusLine.statusCode

                if (expectFail) {
                    if (code == HttpStatus.SC_OK) {
                        throw exceptions.runtime("Unexpectedly got HTTP 200 for '$theUri'")
                    }

                    if (code != HttpStatus.SC_NOT_FOUND) {
                        log.warn("Unexpected {} status found for '{}'", code, theUri)
                    }
                } else {
                    if (code == HttpStatus.SC_NOT_FOUND) {
                        throw exceptions.runtime("404 error found for '$theUri'")
                    }

                    if (code != HttpStatus.SC_OK) {
                        log.warn("Unexpected {} status found for '{}'", code, theUri)
                    }
                }
            } catch (e: URISyntaxException) {
                throw exceptions.runtime(e)
            }
        }
    }

    companion object {
        private val SKIP_PROTOCOLS = arrayListOf("file:", "mailto:", "tel:")
    }
}
