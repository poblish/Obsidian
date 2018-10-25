package org.hiatusuk.obsidian.remote.redis.cmd

import com.codahale.metrics.MetricRegistry
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.remote.redis.delegates.RedisEnvironments
import javax.inject.Inject

@ScenarioScope
@Command("redis:set")
class RedisSetCmd @Inject
internal constructor(private val envts: RedisEnvironments, private val metrics: MetricRegistry) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val key = inCmd.getString("key")
        val value = inCmd.getString("value")

        metrics.timer("RedisGetLookups.set").time().use {
            val resp = envts["main"].set(key, value)
            check(resp == "OK") {"Unexpected response: $resp"}
        }
    }
}
