package org.hiatusuk.obsidian.docker.cmd

import com.spotify.docker.client.DockerClient.LogsParam
import com.spotify.docker.client.LogStream
import com.spotify.docker.client.exceptions.DockerException
import com.spotify.docker.client.exceptions.DockerRequestException
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.HostConfig
import com.spotify.docker.client.messages.PortBinding
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.docker.DockerUtils
import org.hiatusuk.obsidian.docker.delegates.DockerEnvironments
import org.hiatusuk.obsidian.files.FileUtils
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.utils.StringUtils
import org.slf4j.Logger
import java.util.*
import javax.inject.Inject

@ScenarioScope
@Command("docker:container")
class DockerContainerCmd @Inject
constructor(private val envts: DockerEnvironments,
            private val exceptions: RuntimeExceptions,
            private val varCtxt: VariablesContext,
            private val log: Logger) : CommandIF {

    override fun run(inCmd: CommandSpec) {

        val imgName = inCmd.getString("image")

        val bdr = ContainerConfig.builder().image(imgName)
        val hostBdr = HostConfig.builder()

        val portBindings = HashMap<String, List<PortBinding>>()

        if (inCmd.has("ports")) {
            val ports = inCmd.getStrings("ports")
            val exposedPorts = HashSet<String>()

            for (port in ports) {
                val colonPos = port.indexOf(':')

                if (colonPos >= 0) {  // 80 ==> 0.0.0.0:9080
                    val hostPort = Integer.parseInt(port.substring(0, colonPos))
                    val containerPort = Integer.parseInt(port.substring(colonPos + 1)).toString()

                    portBindings[containerPort] = arrayListOf(PortBinding.of("0.0.0.0", hostPort))
                    exposedPorts.add(containerPort)
                } else {  // 9200 ==> 0.0.0.0:9200
                    portBindings[port] = arrayListOf(PortBinding.of("0.0.0.0", port))
                    exposedPorts.add(port)
                }
            }

            bdr.exposedPorts(exposedPorts)
        }

        if (inCmd.has("binds")) {
            val bindings = arrayListOf<String>()

            // See: https://github.com/spotify/docker-client/issues/228
            for (eachBindsEntry in inCmd.getList("binds")) {
                bindings.add(FileUtils.resolveTildes(eachBindsEntry["local"] as String) + ":" + eachBindsEntry["container"])
            }

            log.info("Binding: {}", bindings)
            hostBdr.binds(bindings)
        }

// Unused at present:
//
//        if (inCmd.has("links")) {
//            val links = arrayListOf<String>()
//
//            for ((key, value) in inCmd.getMap("links")) {
//                links.add("$key:$value")
//            }
//
//            log.info("Add links... {}", links)
//            hostBdr.links(links)
//        }

        if (inCmd.has("env")) {
            bdr.env(inCmd.getStrings("env"))
        }

        if (inCmd.has("cmd")) {
            bdr.cmd(inCmd.getStrings("cmd"))
        }

        bdr.hostConfig(hostBdr.autoRemove(true).portBindings(portBindings).build())

        DockerUtils.pull(envts.dockerClient, imgName, log)

        startContainer(inCmd.optString("as").orElse( generateContainerName(imgName) ),
                       inCmd.optString("setAs"),
                       bdr.build())
    }

    private fun startContainer(proposedNewCtrName: String,
                               storeAs: Optional<String>,
                               containerConf: ContainerConfig) {
        try {
            val creation = try {
                envts.dockerClient.createContainer(containerConf, proposedNewCtrName)
            } catch (e: DockerRequestException) {
                // See https://github.com/docker/docker/issues/3036, perhaps
                if (e.cause!!.message!!.contains("HTTP 409 Conflict")) {
                    log.warn("Could not rename container to `{}` due to a conflict", proposedNewCtrName)
                    envts.dockerClient.createContainer(containerConf)
                } else {
                    throw e
                }
            }

            val id = creation.id()
            envts.recordId(proposedNewCtrName, id!!)

            envts.dockerClient.startContainer(id)

            storeAs.ifPresent {
                varCtxt.store(it, ContainerResultsBean( envts.dockerClient.logs(id, LogsParam.stdout(), LogsParam.stderr()) )) }

        } catch (e: DockerRequestException) {
            // See: https://github.com/spotify/docker-client/issues/130
            throw exceptions.runtime("DockerRequestException: " + e.responseBody!!)
        } catch (e: DockerException) {
            throw exceptions.runtime("Docker " + e.message)  // Less noisy error
        }
    }

    internal class ContainerResultsBean(private val logStream: LogStream) {

        @Suppress("unused")  // Used by EL
        fun output(): String {
            return StringUtils.collapseWhitespace(logStream.readFully())
        }
    }

    companion object {
        private var CTR_COUNT = 1

        @Synchronized
        private fun generateContainerName(imgName: String): String? {
            return imgName.replace(Regex("[/:]"), "-") + "." + (CTR_COUNT++)
        }
    }
}
