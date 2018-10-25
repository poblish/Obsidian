package org.hiatusuk.obsidian.remote.aws.s3.cmd

import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.remote.aws.s3.delegates.S3Environments
import org.slf4j.Logger
import javax.inject.Inject

@ScenarioScope
@Command("AWS.S3:delete")
class S3DeleteCmd @Inject
constructor(private val s3: S3Environments, private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val bucketName = inCmd.getString("bucket")

        log.debug("Deleting bucket '{}'", bucketName)
        s3["main"].deleteBucket(bucketName)
    }
}
