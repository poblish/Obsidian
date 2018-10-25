package org.hiatusuk.obsidian.remote.aws.s3.delegates

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.events.AfterScenario
import javax.inject.Inject

@ScenarioScope
class S3Environments @Inject
internal constructor()
{

    private val envtsMapping = HashMap<String, AmazonS3>()

    @AfterScenario
    fun resetAfterScenario() {
        envtsMapping.clear()
    }

    fun put(name: String, creds: BasicAWSCredentials,
            endpointConfig: AwsClientBuilder.EndpointConfiguration,
            pathStyleAccessEnabled: Boolean) {

        val client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(endpointConfig)
                .withPathStyleAccessEnabled(pathStyleAccessEnabled)
                .withCredentials(AWSStaticCredentialsProvider(creds)).build()

        require(envtsMapping.put(name, client) == null) {"Envt already exists"}
    }

    operator fun get(name: String): AmazonS3 {
        return checkNotNull(envtsMapping[name]) {"Unknown S3"}
    }
}