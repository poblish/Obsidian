package org.hiatusuk.obsidian.di.includes

import dagger.Subcomponent
import org.hiatusuk.obsidian.run.external.ExternalHandlers
import org.hiatusuk.obsidian.api.wiremock.config.WireMockConfig
import org.hiatusuk.obsidian.protocol.http.basic.BasicAuthConfig
import org.hiatusuk.obsidian.protocol.ssl.config.SslConfig
import org.hiatusuk.obsidian.remote.googleapi.config.GoogleApiConfig
import org.hiatusuk.obsidian.run.config.BaseUrlConfig
import org.hiatusuk.obsidian.run.config.DefaultsConfig
import org.hiatusuk.obsidian.run.config.SystemPropertiesConfig
import org.hiatusuk.obsidian.run.delegates.CommandNamespaces
import org.hiatusuk.obsidian.run.profiles.ScenarioProfilesConfig
import org.hiatusuk.obsidian.run.reports.ProjectReportsConfig
import org.hiatusuk.obsidian.web.proxy.config.WebProxyConfig
import org.hiatusuk.obsidian.web.selenium.config.RemoteDrivers
import org.hiatusuk.obsidian.web.selenium.driver.profiles.cmd.ChromeOptionsConfig
import org.hiatusuk.obsidian.web.selenium.driver.profiles.cmd.FirefoxProfileConfig
import org.hiatusuk.obsidian.xml.delegates.XmlNamespaces

@Subcomponent
interface ConfigsComponent {

    val baseUrlConfig: BaseUrlConfig
    val basicAuthConfig: BasicAuthConfig
    val chromeOptionsConfig: ChromeOptionsConfig
    val commandNamespaces: CommandNamespaces
    val defaultsConfig: DefaultsConfig
    val externalHandlers: ExternalHandlers
    val firefoxProfileConfig: FirefoxProfileConfig
    val googleApiConfig: GoogleApiConfig
    val projectReportsConfig: ProjectReportsConfig
    val remoteDrivers: RemoteDrivers
    val scenarioProfiles: ScenarioProfilesConfig
    val sslConfig: SslConfig
    val systemPropertiesConfig: SystemPropertiesConfig
    val webProxyConfig: WebProxyConfig
    val wireMockConfig: WireMockConfig
    val xmlNamespaces: XmlNamespaces
}