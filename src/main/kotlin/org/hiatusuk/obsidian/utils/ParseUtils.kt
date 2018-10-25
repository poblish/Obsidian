package org.hiatusuk.obsidian.utils

import com.google.common.primitives.Doubles
import com.google.common.primitives.Ints
import com.google.common.primitives.Longs

import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

import org.hiatusuk.obsidian.context.VariablesContext

object ParseUtils {

    fun parseNumber(s: String): Number? {
        val str = StringUtils.replace(s, ",", "")
        val intVal = Ints.tryParse(str)
        if (intVal != null) {
            return intVal
        }
        val longVal = Longs.tryParse(str)
        return longVal ?: Doubles.tryParse(str)
    }

    fun parseAsDouble(s: String): Double? {
        return Doubles.tryParse(StringUtils.replace(s, ",", ""))
    }

    fun parseDuration(str: String): Duration {

        val m = Pattern.compile("([0-9]+)(?: ([A-Z]+))?", Pattern.CASE_INSENSITIVE).matcher(str.trim())
        if (!m.matches()) {
            throw RuntimeException("Could not parse: '$str'")
        }

        return Duration(Integer.parseInt(m.group(1)), ParseUtils.parseTimeUnit(m.group(2)))
    }

    fun valueToInt(inValue: Any, inVarCtxt: VariablesContext?): Int {
        if (inValue is Number) {
            return inValue.toInt()
        } else if (inVarCtxt != null) {
            return Integer.parseInt( inVarCtxt.resolve( inValue.toString() as String?) !!)
        }
        return Integer.parseInt(inValue.toString())  // Ugh, safe but inefficient
    }

    private fun parseTimeUnit(inStr: String): TimeUnit {
        val rawFormat = inStr.toUpperCase()
        val format = if (rawFormat.endsWith("S")) rawFormat else rawFormat + "S"

        if (!format.isEmpty()) {
            return when (format) {
                "SECS" -> TimeUnit.SECONDS
                "MSECS", "MILLIS" -> TimeUnit.MILLISECONDS
                "NANOS" -> TimeUnit.NANOSECONDS
                "MINS" -> TimeUnit.MINUTES
                "HRS" -> TimeUnit.HOURS
                else -> TimeUnit.valueOf(format)
            }
        }

        return TimeUnit.SECONDS
    }
}
