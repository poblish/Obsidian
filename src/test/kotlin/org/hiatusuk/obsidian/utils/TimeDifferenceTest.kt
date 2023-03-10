package org.hiatusuk.obsidian.utils

import org.hiatusuk.obsidian.utils.TimeDifference.getFormattedTimeDiff
import org.hiatusuk.obsidian.utils.TimeDifference.getFormattedTimeNanosDiff
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class TimeDifferenceTest {

    @Test
    fun testGetFormattedTimeNanosDiff() {
        expectThat(getFormattedTimeNanosDiff(100)).isEqualTo("100 nanos")
        expectThat(getFormattedTimeNanosDiff(50000)).isEqualTo("50 micros")
        expectThat(getFormattedTimeNanosDiff(5000000)).isEqualTo("5 msecs")

        expectThat(getFormattedTimeDiff(1)).isEqualTo("1 msecs")
        expectThat(getFormattedTimeDiff(737)).isEqualTo("737 msecs")
        expectThat(getFormattedTimeDiff(737, false, false)).isEqualTo("< 1 second")
        expectThat(getFormattedTimeDiff(1000)).isEqualTo("1 second")
        expectThat(getFormattedTimeDiff(1200)).isEqualTo("1.2 seconds")
        expectThat(getFormattedTimeDiff(2000, true, false)).isEqualTo("2 seconds")
        expectThat(getFormattedTimeDiff(60000)).isEqualTo("1 minute")
        expectThat(getFormattedTimeDiff(120000)).isEqualTo("2 mins")
        expectThat(getFormattedTimeDiff(121000)).isEqualTo("2 mins, 1 sec")
        expectThat(getFormattedTimeDiff(122000, true, false)).isEqualTo("2 mins, 2 secs")
        expectThat(getFormattedTimeDiff(3600000)).isEqualTo("1 hour")
        expectThat(getFormattedTimeDiff(3601000)).isEqualTo("1 hour, 1 sec")
        expectThat(getFormattedTimeDiff(4920000)).isEqualTo("1 hour, 22 mins")
        expectThat(getFormattedTimeDiff(8920000)).isEqualTo("2 hrs, 28 mins, 40 secs")
        expectThat(getFormattedTimeDiff(86400000)).isEqualTo("1 day")
        expectThat(getFormattedTimeDiff(86400001)).isEqualTo("1 day, 0.001 secs")
        expectThat(getFormattedTimeDiff(86401000)).isEqualTo("1 day, 1 sec")
        expectThat(getFormattedTimeDiff(86410000, false, false)).isEqualTo("1 day")
        expectThat(getFormattedTimeDiff(86410000, true, false)).isEqualTo("1 day, 10 secs")
        expectThat(getFormattedTimeDiff(86410000, false, true)).isEqualTo("1 day")
        expectThat(getFormattedTimeDiff(86520000)).isEqualTo("1 day, 2 mins")
        expectThat(getFormattedTimeDiff(92000000)).isEqualTo("1 day, 1 hour, 33 mins, 20 secs")
        expectThat(getFormattedTimeDiff(172800000)).isEqualTo("2 days")
    }

}