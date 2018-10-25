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

@AssertLookup("redis:get\\(")
@ScenarioScope
class RedisGetLookups @Inject
internal constructor(private val envts: RedisEnvironments,
                     private val exceptions: RuntimeExceptions,
                     private val metrics: MetricRegistry) {

    fun lookup(targetIdentifier: String): Collection<AssertTarget> {
        val p = Pattern.compile("redis:get\\(([^\\(]*)\\)", Pattern.CASE_INSENSITIVE)
        val m = p.matcher(targetIdentifier)
        if (!m.find()) {
            throw exceptions.runtime("Malformed Redis Assert: '$targetIdentifier'")
        }

        val key = m.group(1)

        metrics.timer("RedisGetLookups.get").time().use { return LookupUtils.singleTarget(envts["main"].get(key) ?: "<empty>") }
    }
}
