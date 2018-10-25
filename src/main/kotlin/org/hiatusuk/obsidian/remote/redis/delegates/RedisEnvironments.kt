package org.hiatusuk.obsidian.remote.redis.delegates

import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.events.AfterScenario
import redis.clients.jedis.Jedis
import javax.inject.Inject

@ScenarioScope
class RedisEnvironments @Inject constructor() {
    private val envtsMapping = HashMap<String, Jedis>()

    @AfterScenario
    fun resetAfterScenario() {
        envtsMapping.clear()
    }

    fun put(name: String, envt: Jedis) {
        require(envtsMapping.put(name, envt) == null) {"Jedis envt already exists"}
    }

    operator fun get(name: String): Jedis {
        return checkNotNull(envtsMapping[name]) {"Unknown Jedis server"}
    }
}