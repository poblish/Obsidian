package org.hiatusuk.obsidian.remote.redis.lookups

import com.codahale.metrics.MetricRegistry
import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.LookupUtils
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.remote.redis.delegates.RedisEnvironments
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import java.util.regex.Pattern
import javax.inject.Inject

@AssertLookup("redis:keys\\(")
@ScenarioScope
class RedisKeysLookups @Inject
internal constructor(private val envts: RedisEnvironments,
                     private val exceptions: RuntimeExceptions,
                     private val metrics: MetricRegistry) {

    fun lookup(targetIdentifier: String): Collection<AssertTarget> {
        val p = Pattern.compile("redis:keys\\(\\)", Pattern.CASE_INSENSITIVE)
        val m = p.matcher(targetIdentifier)
        if (!m.find()) {
            throw exceptions.runtime("Malformed Redis Assert: '$targetIdentifier'")
        }

        metrics.timer("RedisKeysLookups.keys").time().use {
            val allKeys = envts["main"].keys("*")
            allKeys.remove("ElastiCacheMasterReplicationTimestamp")  // Remove 'pseudo' key
            return LookupUtils.singleTarget(allKeys)
        }
    }
}
