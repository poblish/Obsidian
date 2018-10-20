package org.hiatusuk.obsidian.utils

import java.text.NumberFormat
import java.util.Locale

object TimeDifference {

    private val SECONDS_FORMAT: NumberFormat = NumberFormat.getNumberInstance(Locale.UK)

    init {
        SECONDS_FORMAT.maximumFractionDigits = 3
    }

    fun getFormattedTimeNanosDiff(inDiffNanos: Long): String {
        return getFormattedTimeNanosDiff(inDiffNanos, true, true)
    }

    fun getFormattedTimeNanosDiff(inDiffNanos: Long, inUseSeconds: Boolean, inUseMSecs: Boolean): String {
        if (inDiffNanos < 1000L) {
            return StringBuilder(11).append(inDiffNanos).append(" nanos").toString()
        }

        val theMicros = inDiffNanos / 1000L

        return if (theMicros < 1000L) {
            StringBuilder(11).append(theMicros).append(" micros").toString()
        } else getFormattedTimeDiff(theMicros / 1000L, inUseSeconds, inUseMSecs)

    }

    @JvmOverloads
    fun getFormattedTimeDiff(inDiffMSecs: Long, inUseSeconds: Boolean = true, inUseMSecs: Boolean = true): String {
        val theBuf = StringBuilder(200)

        if (inDiffMSecs >= 1000L) {
            var theSecs = inDiffMSecs / 1000.0
            val wantMins = theSecs >= 60.0

            if (wantMins) {
                var theMins = (theSecs / 60L).toLong()

                theSecs -= (theMins * 60L).toDouble()

                if (theMins >= 60L) {
                    var theHrs = theMins / 60L

                    theMins %= 60L

                    if (theHrs >= 24L) {
                        val theDays = theHrs / 24L

                        if (theDays == 1L) {
                            theBuf.append("1 day")
                        } else {
                            theBuf.append(theDays).append(" days")
                        }

                        theHrs %= 24L
                    }

                    if (theHrs >= 1L && theBuf.isNotEmpty()) {
                        theBuf.append(", ")
                    }

                    if (theHrs == 1L) {
                        theBuf.append("1 hour")
                    } else if (theHrs > 1L) {
                        theBuf.append(theHrs).append(" hrs")
                    }

                }

                if (theMins >= 1L && theBuf.isNotEmpty()) {
                    theBuf.append(", ")
                }

                if (theMins == 1L) {
                    theBuf.append("1 minute")
                } else if (theMins > 1L) {
                    theBuf.append(theMins).append(" mins")
                }

            }

            if (inUseSeconds && theSecs > 0.0) {
                if (theBuf.isNotEmpty()) {
                    theBuf.append(", ")
                }

                if (theSecs > 0.999999 && theSecs < 1.000001) {
                    theBuf.append(if (wantMins) "1 sec" else "1 second")
                } else if (inUseMSecs) {
                    theBuf.append(SECONDS_FORMAT.format(theSecs)).append(if (wantMins) " secs" else " seconds")
                } else {
                    theBuf.append(Integer.toString(theSecs.toInt())).append(if (wantMins) " secs" else " seconds")
                }
            }

        } else if (inUseMSecs) {
            theBuf.append(inDiffMSecs).append(" msecs")
        } else {
            theBuf.append("< 1 second")
        }

        return theBuf.toString()
    }
}
