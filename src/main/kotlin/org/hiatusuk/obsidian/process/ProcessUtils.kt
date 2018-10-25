package org.hiatusuk.obsidian.process

import com.zaxxer.nuprocess.NuAbstractProcessHandler
import com.zaxxer.nuprocess.NuProcessBuilder
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

object ProcessUtils {

    private val LOG = LoggerFactory.getLogger("ProcessUtils")

    fun processBuilder(inArgs: Iterable<String>, inHandler: NuAbstractProcessHandler): NuProcessBuilder {
        return try {
            NuProcessBuilder(inHandler, correctExecutableArgs(inArgs))
        } catch (e: ExecutableNotOnPathException) {
            NuProcessBuilder(inHandler, inArgs.toList())
        }
    }

    @Throws(ExecutableNotOnPathException::class)
    private fun correctExecutableArgs(inArgs: Iterable<String>): ArrayList<String?>? {
        val orig = inArgs.toMutableList()
        val newList = arrayListOf(getPathForExecutable(orig.removeAt(0)))
        newList.addAll(orig)
        return newList
    }

    @Throws(ExecutableNotOnPathException::class)
    fun getPathForExecutable(inExecName: String): String? {
        val path = System.getenv("PATH")
        val possiblePaths = path.split(File.pathSeparator)

        var binPath: String? = null
        var foundPath = false

        for (possPath in possiblePaths) {
            binPath = (if (possPath.endsWith(File.separator)) possPath else possPath + File.separator) + inExecName
            LOG.trace("Looking for... `{}`", binPath)
            if (File(binPath).exists()) {
                foundPath = true
                break
            }
        }

        if (!foundPath) {
            throw ExecutableNotOnPathException("Couldn't find `$inExecName` on PATH: $path")
        }

        return binPath
    }
}
