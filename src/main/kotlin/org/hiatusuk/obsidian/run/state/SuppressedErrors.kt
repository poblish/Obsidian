package org.hiatusuk.obsidian.run.state

import com.google.common.base.Throwables
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import org.hiatusuk.obsidian.cases.delegates.CasesState
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.FailFastMode
import org.hiatusuk.obsidian.run.Lifecycle
import org.hiatusuk.obsidian.run.RunProperties
import org.hiatusuk.obsidian.run.events.AfterScenariosPassed
import org.hiatusuk.obsidian.run.events.OnSuppressedError
import org.hiatusuk.obsidian.run.events.OutputFinalReports
import org.hiatusuk.obsidian.run.events.ScenarioStartup
import org.hiatusuk.obsidian.run.exceptions.LifecycleMethodException
import org.hiatusuk.obsidian.run.exceptions.TerminationException
import org.hiatusuk.obsidian.utils.TerminalColours
import org.slf4j.Logger
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.util.*
import javax.inject.Inject
import javax.inject.Named

@ScenarioScope
class SuppressedErrors @Inject
constructor(private val eventBus: EventBus,
            private val runProps: RunProperties,
            private val errors: RecordedErrors,
            private val lifecycle: Lifecycle,
            private val cases: CasesState,
            @param:Named("outputDir") private val outputDir: File,
            private val LOG: Logger) {

    private val errorsList = arrayListOf<RecordedError>()
    private var lastErrorFound: String? = null
    private var eventBusRegistered: Boolean = false

    @ScenarioStartup
    fun clearBeforeScenariosRun() {
        if (!eventBusRegistered) {
            eventBus.register(this)
            eventBusRegistered = true
        }

        errorsList.clear()
        lastErrorFound = null
    }

    @Subscribe // EventBus! Only for non-FailFast
    fun suppressLifecycleMethodException(error: LifecycleMethodException) {
        errorFound(error.cause!!, /* Don't fail cases...? */ false)
    }

    @Throws(RuntimeException::class, Error::class)
    fun onError(t: Throwable) { //throws IOException  {
        if (runProps.failFastMode == FailFastMode.FAIL_ON_FIRST_MISMATCH) {
            if (t is RuntimeException) {
                throw t
            }
            if (t is Error) {
                throw t
            }
            Throwables.throwIfUnchecked(t)
            throw RuntimeException(t)
        } else
        /* if ( failFastMode == FailFastMode.FAIL_ON_LAST_MISMATCH) */ {
            errorFound(t)
        }
    }

    private fun errorFound(t: Throwable) {
        errorFound(t, true)
    }

    private fun errorFound(inError: Throwable, inFailCases: Boolean) {
        try {
            logError(inError)

            if (inFailCases) {
                cases.failCurrentCases(/* FIXME */null)
            }

            lifecycle.call(OnSuppressedError::class.java, inError)
        } catch (innerError: Throwable) {
            if (runProps.failFastMode == FailFastMode.FAIL_ON_FIRST_MISMATCH) {
                throw innerError
            } else {
                logError(innerError)
            }
        }

    }

    private fun logError(inError: Throwable) {

        errorsList.add(this.errors.newError(inError))

        lastErrorFound = inError.message

        LOG.error("{}Suppressed error #{}: {}{}", TerminalColours.error(), errorsList.size, inError.message, TerminalColours.reset())
    }

    fun hasErrors(): Boolean {
        return !errorsList.isEmpty()
    }

    fun errorStrings(): List<String?> {
        return errorsList.map { it.throwable.message }
    }

    @AfterScenariosPassed
    fun reportFinalErrors() {
        if (lastErrorFound != null) {
            val msg = "Rethrowing final failure (of " + errorsList.size + ") ... " + lastErrorFound

            lastErrorFound = null  // Don't throw this again...

            throw TerminationException(msg)
        }
    }

    //    @OutputFinalReports
    //    public void outputResultsToLog() {
    //    	if (errorsList.isEmpty()) {
    //    		return;
    //    	}
    //
    //    	LOG.error("All errorsList...");
    //
    //    	int idx = 1;
    //    	for (Error each : errorsList) {
    //    		LOG.error("[#" + (idx++) + "/" + errorsList.size() + "] in \"" + each.currentScenarioName + "\" @ " + each.currentScenarioPath, each.throwable);
    //    	}
    //    }

    @OutputFinalReports
    fun outputResultsToFile() {
        if (errorsList.isEmpty()) {
            return
        }

        try {
            val timeVal = Date().time.toString().substring(6)  // Strip off pointless prefix

            val outputFile = File(outputDir, "TestResults_$timeVal.txt")

            LOG.warn("{}Writing {} errorsList to {}...{}", TerminalColours.error(), errorsList.size, outputFile, TerminalColours.reset())

            try {
                PrintWriter(FileWriter(outputFile)).use { pw ->
                    var idx = 1
                    for (each in errorsList) {
                        pw.println("[#" + idx++ + "/" + errorsList.size + "] in \"" + each.currentScenarioName + "\" @ " + each.currentScenarioPath)
                        each.throwable.printStackTrace(pw)
                    }
                }
            } catch (e: IOException) {
                LOG.error("Could not write results to {}", outputFile)
            }

        } finally {
            reportFinalErrors()  // Just in case @AfterScenariosPassed failed
        }
    }
}
