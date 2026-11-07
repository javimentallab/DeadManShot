package com.javimetallab.deadmanshot

import com.javimetallab.deadmanshot.core.Due
import com.javimetallab.deadmanshot.core.FireReason
import com.javimetallab.deadmanshot.core.HrSample
import com.javimetallab.deadmanshot.core.PulseRule
import com.javimetallab.deadmanshot.core.PulseVerdict
import com.javimetallab.deadmanshot.core.Status
import com.javimetallab.deadmanshot.core.TimerState
import com.javimetallab.deadmanshot.core.Timing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PulseTest {
    private val m = 60_000L
    private val h = 60 * m
    private val now = 1_800_000_000_000L
    private fun readings(vararg bpmMinutesAgo: Pair<Int, Int>) =
        bpmMinutesAgo.map { (bpm, ago) -> HrSample(now - ago * m, bpm) }

    private fun eval(samples: List<HrSample>, noSignal: Long = 0, exportAge: Long = 5 * m) =
        PulseRule.evaluate(samples, now, thresholdBpm = 40, noSignalMs = noSignal, exportAgeMs = exportAge)

    @Test fun normalPulseIsOk() {
        val v = eval(readings(70 to 2, 68 to 12, 72 to 22))
        assertTrue(v is PulseVerdict.Ok)
        assertEquals(70, (v as PulseVerdict.Ok).latest!!.bpm)
    }

    @Test fun oneLowReadingIsNotEnough() {
        assertTrue(eval(readings(30 to 2, 68 to 12, 72 to 22)) is PulseVerdict.Ok)
        assertTrue(eval(readings(30 to 2, 31 to 12, 72 to 22)) is PulseVerdict.Ok)
    }

    @Test fun threeLowReadingsInARowAlert() {
        val v = eval(readings(30 to 2, 31 to 12, 35 to 22, 70 to 32))
        assertTrue(v is PulseVerdict.Low)
        assertEquals(30, (v as PulseVerdict.Low).latest.bpm)
    }

    @Test fun oldLowReadingsDoNotAlert() {
        // The low readings are from an hour ago: something newer is needed.
        assertTrue(eval(readings(30 to 60, 31 to 70, 35 to 80)) is PulseVerdict.Ok)
    }

    @Test fun invalidValuesAreIgnored() {
        // 0 and 255 are what watches store when they could not measure.
        val v = eval(readings(0 to 1, 255 to 2, 30 to 3, 70 to 13, 31 to 23))
        assertTrue(v is PulseVerdict.Ok)
        assertEquals(30, (v as PulseVerdict.Ok).latest!!.bpm)
    }

    @Test fun staleExportSaysNothing() {
        assertEquals(PulseVerdict.Stale, eval(readings(30 to 2, 31 to 12, 35 to 22), exportAge = 3 * h))
        assertEquals(PulseVerdict.Stale, eval(emptyList(), noSignal = h, exportAge = 3 * h))
    }

    @Test fun silenceOnlyFiresWhenAsked() {
        val old = readings(70 to 3 * 60)
        assertTrue(eval(old) is PulseVerdict.Ok)
        assertTrue(eval(old, noSignal = 2 * h) is PulseVerdict.NoSignal)
        assertTrue(eval(old, noSignal = 4 * h) is PulseVerdict.Ok)
        assertTrue(eval(emptyList(), noSignal = 2 * h) is PulseVerdict.NoSignal)
    }

    @Test fun recoveryNeedsANewNormalReading() {
        val alertAt = now - 5 * m
        val before = eval(readings(70 to 10))
        val after = eval(readings(70 to 1))
        val stillLow = eval(readings(30 to 1, 70 to 10))
        assertFalse(PulseRule.recovered(before, alertAt, 40, FireReason.HEART_RATE))
        assertTrue(PulseRule.recovered(after, alertAt, 40, FireReason.HEART_RATE))
        assertFalse(PulseRule.recovered(stillLow, alertAt, 40, FireReason.HEART_RATE))
        // Silence is over with any new reading.
        assertTrue(PulseRule.recovered(eval(readings(30 to 1, 70 to 10)), alertAt, 40, FireReason.NO_PULSE))
    }

    @Test fun timestampsInSecondsOrMillis() {
        assertEquals(1_793_519_040_000L, PulseRule.toMillis(1_793_519_040L))
        assertEquals(1_793_519_040_000L, PulseRule.toMillis(1_793_519_040_000L))
    }

    // --- How an alert moves the countdown ---

    private val day = 24 * h
    private val start = now - h
    private fun timer(alertAt: Long = 0, reason: FireReason = FireReason.HEART_RATE) =
        TimerState(true, start, 0, day, emptyList(), 0, emptySet(), alertAt, reason)

    @Test fun alertFiresAfterTheGrace() {
        val t = timer(alertAt = now)
        val fireAt = now + Timing.PULSE_GRACE_MS
        assertEquals(Status.Running(fireAt), Timing.status(t, now))
        assertEquals(fireAt, Timing.nextWake(t, now))
        assertEquals(Due.Nothing, Timing.due(t, fireAt - 1))
        assertEquals(Due.Fire(FireReason.HEART_RATE), Timing.due(t, fireAt))
        assertEquals(Due.Fire(FireReason.NO_PULSE), Timing.due(timer(now, FireReason.NO_PULSE), fireAt))
    }

    @Test fun anEarlierDeadlineStillWins() {
        // Alert raised 5 minutes before the daily deadline: the deadline comes first.
        val t = timer(alertAt = start + day - 5 * m)
        assertEquals(Due.Fire(FireReason.DEADLINE), Timing.due(t, start + day))
    }

    @Test fun noAlertNoChange() {
        val t = timer()
        assertEquals(Status.Running(start + day), Timing.status(t, now))
    }
}
