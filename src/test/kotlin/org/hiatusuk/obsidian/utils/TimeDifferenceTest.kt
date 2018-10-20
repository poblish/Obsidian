package org.hiatusuk.obsidian.utils

import com.natpryce.hamkrest.assertion.assert
import com.natpryce.hamkrest.equalTo
import org.hiatusuk.obsidian.utils.TimeDifference.getFormattedTimeDiff
import org.hiatusuk.obsidian.utils.TimeDifference.getFormattedTimeNanosDiff
import org.testng.annotations.Test

class TimeDifferenceTest {

    @Test
    fun testGetFormattedTimeNanosDiff() {
        assert.that(getFormattedTimeNanosDiff(100), equalTo("100 nanos"))
        assert.that(getFormattedTimeNanosDiff(50000), equalTo("50 micros"))
        assert.that(getFormattedTimeNanosDiff(5000000), equalTo("5 msecs"))

        assert.that(getFormattedTimeDiff(1), equalTo("1 msecs"))
        assert.that(getFormattedTimeDiff(737), equalTo("737 msecs"))
        assert.that(getFormattedTimeDiff(737, false, false), equalTo("< 1 second"))
        assert.that(getFormattedTimeDiff(1000), equalTo("1 second"))
        assert.that(getFormattedTimeDiff(1200), equalTo("1.2 seconds"))
        assert.that(getFormattedTimeDiff(2000, true, false), equalTo("2 seconds"))
        assert.that(getFormattedTimeDiff(60000), equalTo("1 minute"))
        assert.that(getFormattedTimeDiff(120000), equalTo("2 mins"))
        assert.that(getFormattedTimeDiff(121000), equalTo("2 mins, 1 sec"))
        assert.that(getFormattedTimeDiff(122000, true, false), equalTo("2 mins, 2 secs"))
        assert.that(getFormattedTimeDiff(3600000), equalTo("1 hour"))
        assert.that(getFormattedTimeDiff(3601000), equalTo("1 hour, 1 sec"))
        assert.that(getFormattedTimeDiff(4920000), equalTo("1 hour, 22 mins"))
        assert.that(getFormattedTimeDiff(8920000), equalTo("2 hrs, 28 mins, 40 secs"))
        assert.that(getFormattedTimeDiff(86400000), equalTo("1 day"))
        assert.that(getFormattedTimeDiff(86400001), equalTo("1 day, 0.001 secs"))
        assert.that(getFormattedTimeDiff(86401000), equalTo("1 day, 1 sec"))
        assert.that(getFormattedTimeDiff(86410000, false, false), equalTo("1 day"))
        assert.that(getFormattedTimeDiff(86410000, true, false), equalTo("1 day, 10 secs"))
        assert.that(getFormattedTimeDiff(86410000, false, true), equalTo("1 day"))
        assert.that(getFormattedTimeDiff(86520000), equalTo("1 day, 2 mins"))
        assert.that(getFormattedTimeDiff(92000000), equalTo("1 day, 1 hour, 33 mins, 20 secs"))
        assert.that(getFormattedTimeDiff(172800000), equalTo("2 days"))
    }

}