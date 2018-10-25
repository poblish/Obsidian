package org.hiatusuk.obsidian.user.login.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.user.login.delegates.LoggedInUserState
import org.hiatusuk.obsidian.web.selenium.config.RequiresBrowser
import javax.inject.Inject

@ScenarioScope
@RequiresBrowser
@Command("user")
class UserCmd @Inject
internal constructor(private val userState: LoggedInUserState) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        if (inCmd.hasString() && inCmd.string == "inherit") {
            // Ignore
        } else {
            userState.attemptLogout()

            userState.setCustomerId(inCmd.optString("id").orElse("user-" + System.nanoTime()))

            userState.performLogin(inCmd)
        }
    }
}
