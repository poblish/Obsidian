package org.hiatusuk.obsidian.user.login.delegates

import com.codahale.metrics.MetricRegistry
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.Lifecycle
import org.hiatusuk.obsidian.run.events.AfterBrowserCompletedScenario
import org.hiatusuk.obsidian.run.events.BeforeLogin
import org.hiatusuk.obsidian.run.events.BeforeLogout
import org.hiatusuk.obsidian.run.events.BeforeScenario
import org.hiatusuk.obsidian.run.external.ExternalHandlers
import org.slf4j.Logger
import java.util.*
import javax.inject.Inject

@ScenarioScope
class LoggedInUserState @Inject
constructor(private val loginLogoutHandlers: ExternalHandlers,
            private val lifecycle: Lifecycle,
            private val varCtxt: VariablesContext,
            private val metrics: MetricRegistry,
            private val log: Logger) {

    private var loggedIn: Boolean = false
    private var loggedOut: Boolean = false
    private var customerId: String? = null
    private var userName = Optional.empty<String>()

    @BeforeScenario
    fun resetForScenario() {
        customerId = null
        userName = Optional.empty()
    }

    //    public String getCustomerId() {
    //        return customerId;
    //    }

    //    public Optional<String> getUserName() {
    //        return userName;
    //    }

    fun setCustomerId(inCustomer: String) {
        customerId = inCustomer
    }

    fun performLogin(inCmd: CommandSpec) {
        val userName = inCmd.getString("user")

        if (!userName.isEmpty()) {
            val pass = inCmd.getString("pass")

            this.userName = Optional.of(userName)

            log.info(": Try user/pass login: {user:'" + userName + ", pass:'" + pass + "', id:'" + this.customerId + "'}")

            runBeforeLoginHandlers()

            varCtxt.store("login:username", userName)
            varCtxt.store("login:password", pass)

            var handlerResult = false

            metrics.timer("login > $userName").time().use { handlerResult = loginLogoutHandlers.callHandler("login") }

            if (!handlerResult) {
                log.warn("WARNING. Login requested, but no login handler was configured!")
            }
        } else {
            log.info(": Assume IP-auth: {}", inCmd)
        }

        loggedIn = true
        loggedOut = false
    }

    @AfterBrowserCompletedScenario
    fun attemptLogout() {
        if (loggedOut || !loggedIn) {
            // login has never succeeded - or already logged-out - so logout cannot do anything
            return
        }

        log.info(": Try logging out \"{}\"...", this.userName.get())

        lifecycle.call(BeforeLogout::class.java)

        loginLogoutHandlers.callHandler("logout")

        loggedOut = true
        loggedIn = false

        resetForScenario()
    }

    private fun runBeforeLoginHandlers() {
        lifecycle.call(BeforeLogin::class.java, customerId!!)
    }
}