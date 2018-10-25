package org.hiatusuk.obsidian.protocol.http.cookie.cmd

import org.apache.http.impl.cookie.BasicClientCookie
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.protocol.http.cookie.delegates.HttpCookiesState
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.slf4j.LoggerFactory
import javax.inject.Inject

@ScenarioScope
@Command("cookie delete")
class DeleteCookieCmd @Inject
constructor(private val cookiesState: HttpCookiesState, private val web: WebState) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val cookieName = inCmd.string

        for (each in cookiesState.cookieStore.cookies) {
            if (each.name == cookieName) {

                LOG.debug("Expiring {} from CookieStore...", each)

                val bc = BasicClientCookie(each.name, "<deleted>")
                bc.expiryDate = java.util.Date(0)

                cookiesState.cookieStore.addCookie(bc)  // I *assume* this actually works...
                break
            }
        }

        web.driver.manage().deleteCookieNamed(cookieName)
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(DeleteCookieCmd::class.java)
    }
}
