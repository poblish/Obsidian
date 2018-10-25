package org.hiatusuk.obsidian.remote.aws.s3.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.remote.aws.s3.delegates.S3Environments
import org.slf4j.Logger
import javax.inject.Inject

@ScenarioScope
@Command("AWS.S3:clear")
class S3ClearBucketCmd @Inject constructor(private val s3: S3Environments, private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val bucketName = inCmd.getString("bucket")
        log.debug("Clearing bucket... {}", bucketName)

        val s3Client = s3["main"]
        var items = s3Client.listObjects(bucketName)
        var itemListTruncated: Boolean
        var numItemsDeleted = 0

        do {
            val summaries = items.objectSummaries

            for (summary in summaries) {
                log.debug("> Deleting '{}'", summary.key)
                s3Client.deleteObject(bucketName, summary.key)
            }

            numItemsDeleted += summaries.size

            itemListTruncated = items.isTruncated

            if (itemListTruncated) {
                items = s3Client.listNextBatchOfObjects(items)
            }

        } while (itemListTruncated)

        if (numItemsDeleted > 0) {
            log.debug("Deleted: {}", numItemsDeleted)
        }
    }
}
