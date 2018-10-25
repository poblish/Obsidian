package org.hiatusuk.obsidian.files

import org.hiatusuk.obsidian.utils.StringUtils.replace
import java.io.File

object FileUtils {

    fun resolveTildes(path: String): String {
        if (path.contains("~")) {
            return requireNotNull(path).replaceFirst("^~".toRegex(), System.getProperty("user.home"))
        }

        val f = File(path)
        return if (f.exists()) f.absolutePath
        else path
    }

    fun allYamlFiles(inFileOrDir: File): List<File> {
        return filter(inFileOrDir) { it.name.endsWith(".yaml") || it.name.endsWith(".yml") }
    }

    fun allGherkinFiles(inFileOrDir: File): List<File> {
        return filter(inFileOrDir) { it.name.endsWith(".feature") }
    }

    private fun filter(inFileOrDir: File, predicate: (File) -> Boolean): List<File> {
        return inFileOrDir.walk().filter(predicate).toList()
    }

    fun getFriendlyFileName(file: File?): String {
        return if (file == null) {
            ""
        } else replace(
                replace(file.absolutePath, System.getProperty("user.dir"), "."), System.getProperty("user.home"), "~")
    }
}