package org.hiatusuk.obsidian.di.includes

import org.hiatusuk.obsidian.run.external.ExternalHandlers
import org.hiatusuk.obsidian.api.wiremock.delegates.WireMockServerState
import org.hiatusuk.obsidian.cases.delegates.CasesState
import org.hiatusuk.obsidian.remote.aws.s3.delegates.S3Environments
import org.hiatusuk.obsidian.cmd.DebugLoggingCmd
import org.hiatusuk.obsidian.config.ConfigurationRegistry
import org.hiatusuk.obsidian.run.reports.ProjectReportsConfig
import org.hiatusuk.obsidian.protocol.http.cookie.delegates.HttpCookiesState
import org.hiatusuk.obsidian.cucumber.delegates.CucumberRunStats
import org.hiatusuk.obsidian.run.delegates.*
import org.hiatusuk.obsidian.docker.delegates.DockerEnvironments
import org.hiatusuk.obsidian.javascript.jasmine.delegates.JasmineRunStatus
import org.hiatusuk.obsidian.run.state.LineNumbersState
import org.hiatusuk.obsidian.run.state.ScenarioState
import org.hiatusuk.obsidian.run.state.SuppressedErrors
import org.hiatusuk.obsidian.user.login.delegates.LoggedInUserState
import org.hiatusuk.obsidian.web.selenium.delegates.NewlyCreatedWindowState
import org.hiatusuk.obsidian.web.selenium.delegates.Screenshots
import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.hiatusuk.obsidian.web.server.jetty.delegates.JettyServers
import org.hiatusuk.obsidian.asserts.lookups.AssertLookupsRegistry
import org.hiatusuk.obsidian.web.proxy.delegates.WebProxies
import org.hiatusuk.obsidian.remote.redis.delegates.RedisEnvironments
import org.hiatusuk.obsidian.web.selenium.config.RemoteDrivers
import org.hiatusuk.obsidian.web.selenium.driver.profiles.delegates.ProfilesState
import org.hiatusuk.obsidian.xml.delegates.XmlNamespaces

import dagger.Subcomponent
import org.hiatusuk.obsidian.web.selenium.delegates.WindowSizeDefaults
import org.hiatusuk.obsidian.xml.lookup.XmlLookups

// Things that listen to events like @BeforeScenario etc.
@Subcomponent
interface EventsComponent {

    val assertLookupsRegistry: AssertLookupsRegistry
    val casesState: CasesState
    val commandMappings: CommandMappings
    val commandNamespaces: CommandNamespaces
    val configurationRegistry: ConfigurationRegistry
    val cucumberRunStats: CucumberRunStats
    val debugLoggingCmd: DebugLoggingCmd  // Iffy, here because we modify RunProperties + need to reset after scenario finished
    val dockerEnvironments: DockerEnvironments
    val duplicateScenarioNameChecker : DuplicateScenarioNameChecker
    val externalHandlers: ExternalHandlers
    val httpCookiesState: HttpCookiesState
    val jasmineRunStatus: JasmineRunStatus
    val jettyServers: JettyServers
    val lineNumbersState: LineNumbersState
    val loggedInUserState: LoggedInUserState
    val metricsLogger: MetricsLogger
    val newlyCreatedWindowState: NewlyCreatedWindowState
    val browserProfilesState: ProfilesState
    val projectReportsConfig: ProjectReportsConfig  // Iffy, prob shouldn't have events within configs...
    val redisEnvironments: RedisEnvironments
    val remoteDrivers: RemoteDrivers
    val scenarioDefaultsContext: ScenarioDefaultsContext
    val scenarioState: ScenarioState
    val scenarioStatusPublisher: ScenarioStatusPublisher
    val screenshots: Screenshots
    val s3Environments: S3Environments
    val suppressedErrors: SuppressedErrors
    val webProxies: WebProxies
    val webState: WebState
    val windowSizeDefaults: WindowSizeDefaults
    val wireMockServerState: WireMockServerState
    val xmlLookups: XmlLookups
    val xmlNamespaces: XmlNamespaces
}
