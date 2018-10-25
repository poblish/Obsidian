package org.hiatusuk.obsidian.protocol.http.cookie.delegates

import org.apache.http.client.CookieStore
import org.apache.http.impl.client.BasicCookieStore
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.events.AfterScenario
import org.slf4j.Logger
import javax.inject.Inject

@ScenarioScope
class HttpCookiesState @Inject
constructor(private val log: Logger) {

    val cookieStore: CookieStore = BasicCookieStore()

    @AfterScenario
    fun resetAfterScenario() {
        if (!cookieStore.cookies.isEmpty()) {
            log.info("Clearing CookieStore...")
            cookieStore.clear()
        }
    }
}