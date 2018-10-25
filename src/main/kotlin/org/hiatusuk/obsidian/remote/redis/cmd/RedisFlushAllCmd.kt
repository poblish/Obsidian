package org.hiatusuk.obsidian.remote.redis.cmd

import com.codahale.metrics.MetricRegistry
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.remote.redis.delegates.RedisEnvironments
import javax.inject.Inject

@ScenarioScope
@Command("redis:clear")
class RedisFlushAllCmd @Inject
internal constructor(private val envts: RedisEnvironments, private val metrics: MetricRegistry) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        metrics.timer("Redis.flushAll").time().use {
            val resp = envts["main"].flushAll()
            check(resp == "OK") {"Unexpected response: $resp"}
        }
    }
}
