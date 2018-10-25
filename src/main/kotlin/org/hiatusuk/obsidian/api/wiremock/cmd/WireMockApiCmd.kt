package org.hiatusuk.obsidian.api.wiremock.cmd

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.google.common.base.Charsets.UTF_8
import org.hiatusuk.obsidian.api.wiremock.delegates.WireMockServerState
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import javax.inject.Inject

@ScenarioScope
@Command("api:wireMockMappings")
class WireMockApiCmd
@Inject
internal constructor(private val state: WireMockServerState) : CommandIF {

    override fun run(inCmd: CommandSpec) {

        state.restart()

        val fileSource = inCmd.optFile("file")
        val payload = if (fileSource.isPresent) {
            fileSource.get().readText(UTF_8)
        } else {
            inCmd.string
        }

        state.wireMockServer!!.addStubMapping(StubMapping.buildFrom(payload))
    }
}
