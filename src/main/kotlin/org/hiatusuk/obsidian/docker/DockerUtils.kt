package org.hiatusuk.obsidian.docker

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.exceptions.DockerException
import org.slf4j.Logger
import org.slf4j.MDC

object DockerUtils {

    fun pull(client: DockerClient, image: String, log: Logger) {
        log.info("Pulling: '{}'", image)

        val currentMsg = MDC.get("driver")

        try {
            // Auth not supported at present
            client.pull(image) { message ->
                MDC.put("driver", "$image,")
                log.info(message.status())
            }
        } catch (e: DockerException) {
            throw RuntimeException(e)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        } finally {
            MDC.put("driver", currentMsg)
        }
    }
}
