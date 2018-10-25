package org.hiatusuk.obsidian.protocol.http.cookie.cmd

import org.apache.http.impl.cookie.BasicClientCookie
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.protocol.http.cookie.delegates.HttpCookiesState
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.openqa.selenium.Cookie
import org.slf4j.Logger
import java.util.*
import javax.inject.Inject

@ScenarioScope
@Command("cookie set")
class SetCookieCmd @Inject
internal constructor(private val cookiesState: HttpCookiesState,
                     private val web: WebState,
                     private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val name = inCmd.getString("name")
        val value = inCmd.getString("value")
        val domain = inCmd.optString("domain").orElse(null)
        val path = inCmd.optString("path").orElse("/")
        val expiry: Date? = null // FIXME Make configurable
        val isSecure = inCmd.optBoolean("secure").orElse(false)
        val isHttpOnly = inCmd.optBoolean("httpOnly").orElse(false)

        val bc = BasicClientCookie(name, value)
        bc.domain = domain
        bc.path = path
        bc.expiryDate = expiry
        bc.isSecure = isSecure!!

        log.debug("Adding {} to CookieStore...", bc)

        cookiesState.cookieStore.addCookie(bc)

        try {
            web.driver.manage().addCookie(Cookie(name, value, domain, path, expiry, isSecure, isHttpOnly!!))
        } catch (e: UnsupportedOperationException) {
            log.warn("Current browser does not support adding cookies")
        }

    }
}
