package org.hiatusuk.obsidian.user.login.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.user.login.delegates.LoggedInUserState
import javax.inject.Inject

@ScenarioScope
@Command("logout")
class UserLogoutCmd @Inject
constructor(private val userState: LoggedInUserState) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        userState.attemptLogout()
    }
}