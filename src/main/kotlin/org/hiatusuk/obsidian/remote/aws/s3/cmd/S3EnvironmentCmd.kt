package org.hiatusuk.obsidian.remote.aws.s3.cmd

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.remote.aws.s3.delegates.S3Environments
import javax.inject.Inject

@ScenarioScope
@Command("AWS.S3:envt")
class S3EnvironmentCmd @Inject
constructor(private val state: S3Environments) : CommandIF {

    override fun run(inCmd: CommandSpec) {
        val endpointUrl = inCmd.optString("endpointUrl").orElse(null)
        val signingRegion = inCmd.optString("signingRegion").orElse("eu-west-1")
        val endpointConfig = if (endpointUrl != null) EndpointConfiguration(endpointUrl, signingRegion) else null

        val pathStyleAccessEnabled = inCmd.optBoolean("pathStyle").orElse(false)

        // Defaults probably only make sense for our demos
        val accessKey = inCmd.optString("accessKey").orElse("unused")
        val secret = inCmd.optString("secret").orElse("unused")

        state.put( /* Only one supported at a time */"main",
                BasicAWSCredentials(accessKey, secret),
                endpointConfig!!,
                pathStyleAccessEnabled)
    }
}
