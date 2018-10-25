package org.hiatusuk.obsidian.remote.redis.cmd

import com.codahale.metrics.MetricRegistry
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.remote.redis.delegates.RedisEnvironments
import javax.inject.Inject

@ScenarioScope
@Command("redis:delete")
class RedisDeleteCmd @Inject
internal constructor(private val envts: RedisEnvironments, private val metrics: MetricRegistry) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val key = inCmd.getString("key")

        metrics.timer("Redis.deleteKey").time().use {
            val delCount = envts["main"].del(key)!!

            if (!inCmd.has("optional")) {
                check(delCount > 0) {"No records deleted for: $key"}
            }
        }
    }
}
