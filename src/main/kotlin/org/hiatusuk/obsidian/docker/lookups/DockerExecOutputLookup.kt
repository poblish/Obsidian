package org.hiatusuk.obsidian.docker.lookups

import com.spotify.docker.client.DockerClient.ExecCreateParam
import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.LookupUtils
import org.hiatusuk.obsidian.cmd.api.CommandSpecFactory
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.docker.delegates.DockerEnvironments
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.hiatusuk.obsidian.utils.StringUtils
import org.yaml.snakeyaml.Yaml
import java.util.regex.Pattern
import javax.inject.Inject

@AssertLookup("docker:exec\\(")
@ScenarioScope
class DockerExecOutputLookup
@Inject constructor(private val envts: DockerEnvironments,
                    private val specs: CommandSpecFactory,
                    private val exceptions: RuntimeExceptions) {

    fun lookup(targetIdentifier: String): Collection<AssertTarget> {
        val p = Pattern.compile("docker:exec\\((.*)\\)", Pattern.CASE_INSENSITIVE)
        val m = p.matcher(targetIdentifier)
        if (!m.find()) {
            exceptions.runtime("Malformed Assert: '$targetIdentifier'")
        }

        // Looks horrible, but basically use the CommandSpec API to parse our payload and use its very helpful methods
        val map = hashMapOf<String,Any>("exec" to PAYLOAD_PARSER.load("{" + m.group(1) + "}"))
        val spec = specs.create(map)

        val execs: Array<ExecCreateParam> = spec.getStrings("attach").map {
            when {
                it.equals("out", ignoreCase = true) -> ExecCreateParam.attachStdout()
                it.equals("err", ignoreCase = true) -> ExecCreateParam.attachStderr()
                else -> throw RuntimeException("Unknown type $it")
            }
        }.toTypedArray()

        val cmds = spec.getString("cmd").split(' ').map { it.trim() }.toTypedArray()

        val execId = envts.dockerClient.execCreate(envts.containerIds[ spec.getString("target") ], cmds, *execs).id()

        envts.dockerClient.execStart(execId).use { return LookupUtils.singleTarget( StringUtils.collapseWhitespace( it.readFully()) ) }
    }

    companion object {
        private val PAYLOAD_PARSER = Yaml()
    }
}