package org.hiatusuk.obsidian.di.includes

import org.hiatusuk.obsidian.api.wiremock.cmd.WireMockApiCmd
import org.hiatusuk.obsidian.asserts.cmd.AssertCmd
import org.hiatusuk.obsidian.cases.cmd.CasesCmd
import org.hiatusuk.obsidian.files.cmd.CleanFileDirectoryCmd
import org.hiatusuk.obsidian.protocol.http.cmd.BandwidthCmd
import org.hiatusuk.obsidian.remote.aws.s3.cmd.*
import org.hiatusuk.obsidian.cmd.*
import org.hiatusuk.obsidian.context.cmd.DecrementVariableCmd
import org.hiatusuk.obsidian.context.cmd.IncrementVariableCmd
import org.hiatusuk.obsidian.context.cmd.SetVariableCmd
import org.hiatusuk.obsidian.protocol.http.cookie.cmd.DeleteCookieCmd
import org.hiatusuk.obsidian.protocol.http.cookie.cmd.SetCookieCmd
import org.hiatusuk.obsidian.docker.cmd.*
import org.hiatusuk.obsidian.remote.googleapi.cmd.PageSpeedInsightsCmd
import org.hiatusuk.obsidian.protocol.http.har.cmd.HarCopyCmd
import org.hiatusuk.obsidian.protocol.http.har.cmd.HarDumpCmd
import org.hiatusuk.obsidian.protocol.http.har.cmd.HarStartCmd
import org.hiatusuk.obsidian.javascript.cmd.JavaScriptAsyncCmd
import org.hiatusuk.obsidian.javascript.cmd.JavaScriptCmd
import org.hiatusuk.obsidian.javascript.cmd.JavaScriptLogCmd
import org.hiatusuk.obsidian.javascript.jasmine.cmd.RunJasmineSpecCmd
import org.hiatusuk.obsidian.jdbc.cmd.*
import org.hiatusuk.obsidian.user.login.cmd.UserCmd
import org.hiatusuk.obsidian.user.login.cmd.UserLogoutCmd
import org.hiatusuk.obsidian.web.selenium.cmd.*
import org.hiatusuk.obsidian.web.server.jetty.cmd.JettyConfigCmd
import org.hiatusuk.obsidian.web.server.jetty.cmd.JettyDeployCmd
import org.hiatusuk.obsidian.process.cmd.ExecCmd
import org.hiatusuk.obsidian.web.proxy.cmd.ProxyStopCmd
import org.hiatusuk.obsidian.remote.redis.cmd.RedisDeleteCmd
import org.hiatusuk.obsidian.remote.redis.cmd.RedisEnvironmentCmd
import org.hiatusuk.obsidian.remote.redis.cmd.RedisFlushAllCmd
import org.hiatusuk.obsidian.remote.redis.cmd.RedisSetCmd
import org.hiatusuk.obsidian.snaps.cmd.SnapshotAcceptCmd
import org.hiatusuk.obsidian.snaps.cmd.SnapshotImageCmd
import org.hiatusuk.obsidian.threads.cmd.ThreadScheduleCmd
import org.hiatusuk.obsidian.threads.cmd.ThreadSubmitCmd

import dagger.Subcomponent
import org.hiatusuk.obsidian.asserts.cmd.HopeCmd

@Subcomponent
interface CommandsComponent {

    val alertCmd: AlertCmd
    val alertCloseCmd: AlertCloseCmd
    val assertCmd: AssertCmd
    val bandwidthCmd: BandwidthCmd
    val callSubroutineCmd: CallSubroutineCmd
    val casesCmd: CasesCmd
    val checkBrokenLinksCmd: CheckBrokenLinksCmd
    val checkedCmd: CheckedCmd
    val cleanFileDirectory: CleanFileDirectoryCmd
    val clearInputCmd: ClearInputCmd
    val clickCmd: ClickCmd
    val decrementVariableCmd: DecrementVariableCmd
    val delay: Delay
    val delayUntilCmd: DelayUntilCmd
    val debugLoggingCmd: DebugLoggingCmd
    val deleteCookieCmd: DeleteCookieCmd
    val dockerContainerCmd: DockerContainerCmd
    val domDump: DomDump
    val echoCmd: EchoCmd
    val execCmd: ExecCmd
    val failCmd: FailCmd
    val fixmeCmd: FixmeCmd
    val formSubmitCmd: FormSubmitCmd
    val goToUrl: GoToUrl
    val harCopyCmd: HarCopyCmd
    val harDumpCmd: HarDumpCmd
    val harStartCmd: HarStartCmd
    var hopeCmd : HopeCmd
    val incrementVariableCmd: IncrementVariableCmd
    val javaScriptCmd: JavaScriptCmd
    val javaScriptAsyncCmd: JavaScriptAsyncCmd
    val javaScriptLogCmd: JavaScriptLogCmd
    val jdbcConnectCmd: JdbcConnectCmd
    val jdbcDeleteCmd: JdbcDeleteCmd
    val jdbcLoadCmd: JdbcLoadCmd
    val jdbcSelectCmd: JdbcSelectCmd
    val jdbcUpdateCmd: JdbcUpdateCmd
    val jettyConfigCmd: JettyConfigCmd
    val jettyDeployCmd: JettyDeployCmd
    val navigateBackCmd: NavigateBackCmd
    val navigateForwardCmd: NavigateForwardCmd
    val navigateRefreshCmd: NavigateRefreshCmd
    val pageSpeedInsightsCmd: PageSpeedInsightsCmd
    val popupClickCmd: PopupClickCmd
    val popupReturnCmd: PopupReturnCmd
    val popupSelectCmd: PopupSelectCmd
    val proxyStopCmd: ProxyStopCmd
    val redisDeleteCmd: RedisDeleteCmd
    val redisEnvironmentCmd: RedisEnvironmentCmd
    val redisFlushAllCmd: RedisFlushAllCmd
    val redisSetCmd: RedisSetCmd
    val runJasmineSpecCmd: RunJasmineSpecCmd
    val s3ClearBucketCmd: S3ClearBucketCmd
    val s3CreateBucketCmd: S3CreateBucketCmd
    val s3DeleteCmd: S3DeleteCmd
    val s3EnvironmentCmd: S3EnvironmentCmd
    val s3PutCmd: S3PutCmd
    val screenshotCmd: ScreenshotCmd
    val scrollCmd: ScrollCmd
    val selectDropdownCmd: SelectDropdownCmd
    val setCookieCmd: SetCookieCmd
    val setVariableCmd: SetVariableCmd
    val snapshotAcceptCmd: SnapshotAcceptCmd
    val snapshotImageCmd: SnapshotImageCmd
    val switchToFrame: SwitchToFrame
    val tabOrderCmd: TabOrderCmd
    val threadScheduleCmd: ThreadScheduleCmd
    val threadSubmitCmd: ThreadSubmitCmd
    val typeCmd: TypeCommand
    val uncheckedCmd: UncheckedCmd
    val userCmd: UserCmd
    val userLogoutCmd: UserLogoutCmd
    val waitForCmd: WaitForCmd
    val windowCloseCmd: WindowCloseCmd
    val windowManagementCmd: WindowManagementCmd
    val wireMockApiCmd: WireMockApiCmd
}
