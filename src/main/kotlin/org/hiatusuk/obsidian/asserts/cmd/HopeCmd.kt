package org.hiatusuk.obsidian.asserts.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.cmd.api.Validate
import org.hiatusuk.obsidian.di.ScenarioScope
import org.slf4j.Logger
import java.io.IOException
import javax.inject.Inject

@ScenarioScope
@Command("hope")
class HopeCmd @Inject
constructor(private val assertCmd: AssertCmd,
            private val log: Logger) : CommandIF {

    @Validate
    fun validate(inCmd: CommandSpec) {
        AssertCmd.validatePayload(inCmd)
    }

    @Throws(IOException::class)
    override fun run(inCmd: CommandSpec) {
        try {
            assertCmd.doAssert(inCmd)
        } catch (e: AssertionError) {
            log.error("hope() was misplaced, as an assertion failed:", e)
        }
    }
}
