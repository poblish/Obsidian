package org.hiatusuk.obsidian.docker.delegates

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.exceptions.DockerException
import com.spotify.docker.client.exceptions.DockerRequestException
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.events.ApplicationQuit
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.slf4j.Logger
import javax.inject.Inject

@ScenarioScope
class DockerEnvironments @Inject
constructor(private val exceptions: RuntimeExceptions,
            private val log: Logger) {

    internal val dockerClient = DefaultDockerClient.fromEnv().build()
    internal val containerIds = hashMapOf<String,String>()

    // Container names *must* be unique
    fun recordId(containerName: String, inId: String) {
        if (containerIds.containsKey(containerName)) {
            exceptions.runtime("Container Id already recorded for $containerName")
        }
        containerIds[containerName] = inId
    }

    @ApplicationQuit
    fun shutdownAll() {
        for ((name, id) in HashMap(containerIds)) {
            containerIds.remove(name)

            try {
                log.info("Killing... {}", id)
                dockerClient.killContainer(id)
            } catch (e: DockerRequestException) {
                log.debug("Caught during kill request: {}", e.responseBody)
            } catch (e: InterruptedException) {
                log.debug("Caught during kill request: {}", e.message)
            } catch (e: DockerException) {
                log.debug("Caught during kill request: {}", e.message)
            }

            try {
                log.info("Removing from container... {}", id)
                dockerClient.removeContainer(id)
            } catch (e: DockerRequestException) {
                log.debug("Caught during remove request: {}", e.responseBody)
            } catch (e: InterruptedException) {
                log.debug("Caught during remove request: {}", e.message)
            } catch (e: DockerException) {
                log.debug("Caught during remove request: {}", e.message)
            }
        }
    }
}
