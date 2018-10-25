package org.hiatusuk.obsidian.remote.redis.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.remote.redis.delegates.RedisEnvironments
import redis.clients.jedis.Jedis
import javax.inject.Inject

@ScenarioScope
@Command("redis:envt")
class RedisEnvironmentCmd @Inject
internal constructor(private val envts: RedisEnvironments) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val host = inCmd.optString("host").orElse("localhost")
        //        final int port = ParseUtils.valueToInt(spec.get("port"), ctxt, 6379);  // FIXME FIXME Support properly!

        envts.put( /* Only one supported at a time */"main", Jedis(host))
    }
}
