package org.hiatusuk.obsidian.remote.aws.s3.cmd

import com.amazonaws.services.s3.model.CreateBucketRequest
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.remote.aws.s3.delegates.S3Environments
import org.slf4j.Logger
import javax.inject.Inject

@ScenarioScope
@Command("AWS.S3:create")
class S3CreateBucketCmd @Inject constructor(private val s3: S3Environments, private val log: Logger) : CommandIF {
    override fun run(inCmd: CommandSpec) {
        val bucketName = inCmd.getString("bucket")

        if (!s3["main"].doesBucketExistV2(bucketName)) {
            log.debug("Creating '{}'", bucketName)
            s3["main"].createBucket(CreateBucketRequest(bucketName))
        } else {
            log.info("Bucket '{}' already exists", bucketName)
        }
    }
}
