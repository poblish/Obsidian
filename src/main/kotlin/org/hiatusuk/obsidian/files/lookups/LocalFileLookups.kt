package org.hiatusuk.obsidian.files.lookups

import com.google.common.base.Charsets
import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.AssertLookups.Companion.METHOD_HANDLER_PATTERN_STR
import org.hiatusuk.obsidian.asserts.lookups.AssertLookups.Companion.SIMPLE_METHOD_HANDLER_PATTERN_STR
import org.hiatusuk.obsidian.asserts.lookups.LookupUtils
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import java.io.File
import java.io.IOException
import java.util.regex.Pattern
import java.util.regex.Pattern.CASE_INSENSITIVE
import javax.inject.Inject

@AssertLookup("file\\(")
@ScenarioScope
class LocalFileLookups @Inject
internal constructor(private val exceptions: RuntimeExceptions) {

    @Throws(IOException::class)
    fun lookup(targetIdentifier: String): Collection<AssertTarget> {
        var p = Pattern.compile(METHOD_HANDLER_PATTERN_STR, CASE_INSENSITIVE)
        var m = p.matcher(targetIdentifier)
        if (!m.find()) {

            // Try short-form
            p = Pattern.compile(SIMPLE_METHOD_HANDLER_PATTERN_STR, CASE_INSENSITIVE)
            m = p.matcher(targetIdentifier)

            if (!m.find()) {
                throw exceptions.runtime("Malformed Local file Assert: '$targetIdentifier'")
            }

            return LookupUtils.singleTarget(getContents(File(m.group(1))))
        }

        // Long-form

        val file = File(m.group(1))
        val propertyName = m.group(2)

        when (propertyName.toLowerCase()) {
            "exists" -> return LookupUtils.singleTarget(file.exists())
            "readable" -> return LookupUtils.singleTarget(file.canRead())
            "writeable" -> return LookupUtils.singleTarget(file.canWrite())
            "isfile" -> return LookupUtils.singleTarget(file.isFile)
            "isdir", "isdirectory" -> return LookupUtils.singleTarget(file.isDirectory)
            "size" -> {
                if (!file.exists()) {
                    throw exceptions.runtime("File not found: $file")
                }
                return LookupUtils.singleTarget(getContents(file).length)  // More reliable than File.length()
            }
            "contents" -> {
                if (!file.exists()) {
                    throw exceptions.runtime("File not found: $file")
                }
                return LookupUtils.singleTarget(getContents(file))
            }
            else -> throw exceptions.runtime("Unknown property: $propertyName")
        }
    }

    @Throws(IOException::class)
    private fun getContents(f: File): String {
        return f.readText(Charsets.UTF_8).trim()
    }
}
