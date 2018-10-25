package org.hiatusuk.obsidian.jdbc.delegates

import org.hiatusuk.obsidian.di.ScenarioScope
import org.slf4j.Logger
import javax.inject.Inject
import javax.sql.DataSource

@ScenarioScope
class JdbcEnvironments @Inject
constructor(private val log: Logger) {

    // Pretend we support multiple named environments. Which we could do...
    private val sources = HashMap<String, DataSource>()

    fun put(name: String, envt: DataSource) {
        if (sources.put(name, envt) != null) {
            log.info("Replaced previous DataSource")
        }
    }

    operator fun get(name: String): DataSource {
        return checkNotNull(sources[name]) {"Unknown DataSource"}
    }
}
