package org.hiatusuk.obsidian.remote.aws.s3.lookups

import com.amazonaws.services.s3.model.S3ObjectSummary
import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.LookupUtils
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.remote.aws.s3.delegates.S3Environments
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.slf4j.Logger
import java.util.TreeMap
import java.util.regex.Pattern
import javax.inject.Inject
import kotlin.collections.LinkedHashMap
import kotlin.collections.set

@AssertLookup("s3\\.bucket\\(")
@ScenarioScope
class BucketLookups @Inject
constructor(private val s3: S3Environments,
            private val exceptions: RuntimeExceptions,
            private val log: Logger) {

    fun lookup(targetIdentifier: String): Collection<AssertTarget> {
        val p = Pattern.compile("s3.bucket\\((.*)\\)\\.([A-Z\\-]*)\\(\\)", Pattern.CASE_INSENSITIVE)
        val m = p.matcher(targetIdentifier)
        if (!m.find()) {
            throw exceptions.runtime("Malformed Bucket Assert: '$targetIdentifier'")
        }

        val bucketName = m.group(1)
        val objs = s3["main"].listObjects(bucketName)

        val propertyName = m.group(2)

        val summaries = makeDeterminate(objs.objectSummaries)
        log.trace("Bucket '{}', summaries = {}", bucketName, summaries)

        val texts = arrayListOf<String>()

        val targetText: String

        when (propertyName.toLowerCase()) {
            "contents" -> {
                for (eachObj in summaries) {
                    val obj = s3["main"].getObject(bucketName, eachObj.key)
                    texts.add( obj.objectContent.bufferedReader().use { it.readText() } )
                }
                targetText = texts.joinToString(separator = ",").trim()
            }

            "list" -> targetText = summaries.joinToString(separator = ",") {
                val eachDesc = LinkedHashMap<String, String>()
                eachDesc["key"] = it.key
                eachDesc["etag"] = it.eTag
                eachDesc["bytes"] = it.size.toString()
                eachDesc["lastMod"] = it.lastModified.toString()
                eachDesc.toString()
            }

            "keys" -> {
                for (eachObj in summaries) {
                    texts.add(eachObj.key)
                }
                targetText = texts.joinToString(separator = ",")
            }
            "sizes" -> {
                for (eachObj in summaries) {
                    texts.add(java.lang.Long.toString(eachObj.size))
                }
                targetText = texts.joinToString(separator = ",")
            }
            "etags" -> {
                for (eachObj in summaries) {
                    texts.add(eachObj.eTag)
                }
                targetText = texts.joinToString(separator = ",")
            }
            "count" -> targetText = Integer.toString(summaries.size)
            else -> throw exceptions.runtime("Unknown property")
        }

        return LookupUtils.singleTarget(targetText)
    }

    // Ordinarily comes back in ?arbitrary? order
    private fun makeDeterminate(objectSummaries: List<S3ObjectSummary>): Collection<S3ObjectSummary> {
        val map = TreeMap<String, S3ObjectSummary>()

        for (each in objectSummaries) {
            // Key is fugly, but more important to be *consistent* rather than attempt to guess at original insert order
            if (map.put(each.size.toString() + "_" + each.lastModified, each) != null) {
                throw exceptions.runtime("Cannot maintain consistent order")
            }
        }

        return map.values
    }
}
