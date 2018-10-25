package org.hiatusuk.obsidian.remote.googleapi.cmd

import com.jayway.jsonpath.DocumentContext
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.json.delegates.JsonLoader
import org.hiatusuk.obsidian.remote.googleapi.delegates.GoogleApiAccounts
import org.slf4j.Logger
import java.io.IOException
import java.net.URISyntaxException
import javax.inject.Inject

@ScenarioScope
@Command("insights")
class PageSpeedInsightsCmd @Inject
internal constructor(private val api: GoogleApiAccounts,
                     private val loader: JsonLoader,
                     private val log: Logger) : CommandIF {

    var jsonDoc: DocumentContext? = null
        private set

    @Throws(IOException::class)
    override fun run(inCmd: CommandSpec) {
        val targetUrl = inCmd.getString("url")
        val strategyVal = inCmd.getString("strategy")

        val actualStrategy = if (strategyVal.isEmpty() || strategyVal.equals("desktop", ignoreCase = true)) {
            "desktop"
        } else if (strategyVal.equals("mobile", ignoreCase = true)) {
            "mobile"
        } else {
            throw RuntimeException("Unknown strategy: $strategyVal")
        }

        val uri = URIBuilder()
        uri.path = "https://www.googleapis.com/pagespeedonline/v2/runPagespeed"
        uri.setParameter("url", targetUrl)
        uri.setParameter("filter_third_party_resources", "true")  // I think...
        uri.setParameter("screenshot", "false")  // I think...
        uri.setParameter("strategy", actualStrategy)
        uri.setParameter("key", api.apiKey)

        log.debug("Calling Insights API: {}", uri)

        try {
            jsonDoc = loader.loadDocument(HttpGet(uri.build()))
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }

    }
}
