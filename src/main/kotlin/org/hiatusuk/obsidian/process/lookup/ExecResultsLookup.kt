package org.hiatusuk.obsidian.process.lookup

import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.asserts.lookups.AssertLookup
import org.hiatusuk.obsidian.asserts.lookups.AssertLookups
import org.hiatusuk.obsidian.asserts.lookups.LookupUtils
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.process.ExecProcessHandler
import org.hiatusuk.obsidian.process.ProcessUtils
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject

@AssertLookup("exec\\(")
@ScenarioScope
class ExecResultsLookup @Inject
internal constructor(private val varCtxt: VariablesContext,
                     private val exceptions: RuntimeExceptions) {

    fun lookup(targetIdentifier: String): Collection<AssertTarget> {
        val p = Pattern.compile("exec" + AssertLookups.METHOD_HANDLER_PATTERN_STR, Pattern.CASE_INSENSITIVE)
        val m = p.matcher(targetIdentifier)
        if (!m.find()) {
            throw exceptions.runtime("Malformed Exec Assert: '$targetIdentifier'")
        }

        val args = m.group(1).split(' ').filter { it.isNotBlank() }
        val handler = ExecProcessHandler()
        val pb = ProcessUtils.processBuilder(args, handler)

        val exitCode = pb.start().waitFor(0, TimeUnit.SECONDS)

        val propertyName = m.group(2)

        varCtxt.store("lastExec", ProcessResultsWrapper(exitCode, handler))

        return when (propertyName.toLowerCase()) {
            "result" -> LookupUtils.singleTarget(exitCode)
            "out" -> LookupUtils.singleTarget(handler.stdout)
            "err" -> LookupUtils.singleTarget(handler.stderr)
            else -> throw exceptions.runtime("Unknown property: $propertyName")
        }
    }

    internal class ProcessResultsWrapper(private val exitCode: Int, private val handler: ExecProcessHandler) {

        fun out(): String {
            return handler.stdout
        }

        @Suppress("unused")  // Potentially used by EL
        fun err(): String {
            return handler.stderr
        }

        fun result(): Int {
            return exitCode
        }
    }
}