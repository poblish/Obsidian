package org.hiatusuk.obsidian.remote.aws.s3.cmd

import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.remote.aws.s3.delegates.S3Environments
import org.slf4j.Logger
import java.io.File
import java.util.*
import javax.inject.Inject

@ScenarioScope
@Command("AWS.S3:put")
class S3PutCmd @Inject
constructor(private val s3: S3Environments, private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val bucketName = inCmd.getString("bucket")
        val keyName = inCmd.getString("as")
        val filePath = inCmd.optString("file").orElse(null)
        val ttl = inCmd.optDuration("expires").orElse(null)

        val metadata = ObjectMetadata()
        if (ttl != null) {
            val d = Date(System.currentTimeMillis() + ttl.toMillis())
            log.info("Setting expiry date to '{}'", d)
            // metadata.setCacheControl("max-age=" + ParseUtils.parseDuration(ttl).toSeconds());  // ??? Does this work better ???
            metadata.expirationTime = d
        }

        if (filePath != null) {
            val req = PutObjectRequest(bucketName, keyName, File(filePath))
            req.metadata = metadata
            //            metadata.setLastModified( new Date(20000L) );

            val result = s3["main"].putObject(req)

            log.info("PUT contents of '{}' as ETag '{}', expire at '{}'", filePath, result.eTag, result.expirationTime)
        }
    }
}
