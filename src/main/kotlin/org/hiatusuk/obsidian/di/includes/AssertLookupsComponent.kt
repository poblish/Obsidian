package org.hiatusuk.obsidian.di.includes

import org.hiatusuk.obsidian.remote.aws.s3.lookups.BucketLookups
import org.hiatusuk.obsidian.files.lookups.LocalFileLookups
import org.hiatusuk.obsidian.protocol.http.cookie.lookup.CookieNamesLookup
import org.hiatusuk.obsidian.protocol.http.cookie.lookup.NamedCookieLookup
import org.hiatusuk.obsidian.remote.googleapi.lookup.PageSpeedLookups
import org.hiatusuk.obsidian.protocol.http.har.lookup.HarLookups
import org.hiatusuk.obsidian.protocol.http.lookup.HttpLookups
import org.hiatusuk.obsidian.javascript.jasmine.lookup.JasmineFailsLookup
import org.hiatusuk.obsidian.javascript.jasmine.lookup.JasminePassesLookup
import org.hiatusuk.obsidian.javascript.lookup.JavaScriptLookups
import org.hiatusuk.obsidian.jdbc.lookup.JdbcSelectLookups
import org.hiatusuk.obsidian.json.lookup.JsonLookups
import org.hiatusuk.obsidian.process.lookup.ExecResultsLookup
import org.hiatusuk.obsidian.remote.redis.lookups.RedisGetLookups
import org.hiatusuk.obsidian.remote.redis.lookups.RedisKeysLookups
import org.hiatusuk.obsidian.web.selenium.lookup.*
import org.hiatusuk.obsidian.xml.lookup.XmlLookups

import dagger.Subcomponent
import org.hiatusuk.obsidian.docker.lookups.DockerExecOutputLookup

@Subcomponent
interface AssertLookupsComponent {

    val bucketLookups: BucketLookups
    val colourContrastLookups: ColourContrastLookups
    val cookieNamesLookup: CookieNamesLookup
    val cssLookups: CssLookups
    val currentUrlLookups: CurrentUrlLookups
    val dockerExecOutputLookup: DockerExecOutputLookup
    val domAttributeLookups: DomAttributeLookups
    val elementCountLookups: ElementCountLookups
    val elementOrderingLookups: ElementOrderingLookups
    val execResultsLookup: ExecResultsLookup
    val harLookups: HarLookups
    val httpLookups: HttpLookups
    val jasmineFailsLookup: JasmineFailsLookup
    val jasminePassesLookup: JasminePassesLookup
    val javaScriptLookups: JavaScriptLookups
    val jdbcSelectLookups: JdbcSelectLookups
    val jsonLookups: JsonLookups
    val localFileLookups: LocalFileLookups
    val metaLookups: MetaLookups
    val namedCookieLookup: NamedCookieLookup
    val pageSpeedLookups: PageSpeedLookups
    val redisGetLookups: RedisGetLookups
    val redisKeysLookups: RedisKeysLookups
    val textLengthLookups: TextLengthLookups
    val titleLookup: TitleLookup
    val webSourceLookups: WebSourceLookups
    val xmlLookups: XmlLookups
}
