package org.hiatusuk.obsidian.remote.googleapi.lookup

import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.LookupUtils
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.json.delegates.JsonLoader
import org.hiatusuk.obsidian.remote.googleapi.cmd.PageSpeedInsightsCmd
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import java.util.regex.Pattern
import javax.inject.Inject

@AssertLookup("insights\\.")
@ScenarioScope
class PageSpeedLookups @Inject
constructor(private val state: PageSpeedInsightsCmd,  // FIXME Should be a separate 'state' delegate for this!
            private val loader: JsonLoader,
            private val exceptions: RuntimeExceptions) {

    fun lookup(targetIdentifier: String): Collection<AssertTarget> {
        val m = HANDLER_PATTERN.matcher(targetIdentifier)
        if (!m.find()) {
            throw exceptions.runtime("Malformed PageSpeedLookup Assert: '$targetIdentifier'")
        }

        var path = m.group(2)
        var propertyName = m.group(1)

        // Friendlier names
        if ("score" == propertyName) {
            propertyName = "node"
            path = "$.ruleGroups.SPEED.score"
        } else if ("recommends" == propertyName) {
            propertyName = "exists"
            path = "$.formattedResults.ruleResults.$path"
        }

        return LookupUtils.singleTarget(loader.findString(state.jsonDoc!!, propertyName, path))
    }

    companion object {

        private val HANDLER_PATTERN = Pattern.compile("insights" + "\\.([A-Z]*)\\((.*)\\)", Pattern.CASE_INSENSITIVE)
    }
}
