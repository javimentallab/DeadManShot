package com.javimetallab.deadmanshot

import com.javimetallab.deadmanshot.core.Due
import com.javimetallab.deadmanshot.core.FireReason
import com.javimetallab.deadmanshot.core.Status
import com.javimetallab.deadmanshot.core.TimerState
import com.javimetallab.deadmanshot.core.Timing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimingTest {
    private val h = 3_600_000L
    private val day = 24 * h
    private val start = 1_000_000_000_000L

    private fun running(
        baseAt: Long = start, pausedUntil: Long = 0,
        reminders: List<Long> = listOf(2 * h),
        firedAt: Long = 0, shown: Set<Long> = emptySet(),
    ) = TimerState(true, baseAt, pausedUntil, day, reminders, firedAt, shown)

    @Test fun offDoesNothing() {
        val t = running().copy(enabled = false)
        assertEquals(Status.Off, Timing.status(t, start + 10 * day))
        assertEquals(Due.Nothing, Timing.due(t, start + 10 * day))
        assertNull(Timing.nextWake(t, start))
    }

    @Test fun deadlineIsOneIntervalAfterCheckIn() {
        val t = running()
        assertEquals(Status.Running(start + day), Timing.status(t, start + h))
        assertEquals(Due.Nothing, Timing.due(t, start + h))
        assertEquals(Due.Fire(FireReason.DEADLINE), Timing.due(t, start + day))
    }

    @Test fun reminderComesBeforeDeadlineAndOnlyOnce() {
        val t = running()
        assertEquals(start + day - 2 * h, Timing.nextWake(t, start))
        val due = Timing.due(t, start + day - h)
        assertTrue(due is Due.Remind && 2 * h in due.offsets)
        val shown = t.copy(remindersShown = setOf(2 * h))
        assertEquals(Due.Nothing, Timing.due(shown, start + day - h))
        assertEquals(start + day, Timing.nextWake(shown, start + day - h))
    }

    @Test fun missedReminderStillFires() {
        // Phone off through the reminder and the deadline: on boot it must send, not remind.
        assertEquals(Due.Fire(FireReason.DEADLINE), Timing.due(running(), start + 3 * day))
    }

    @Test fun threeOffsetsEachTriggerOnce() {
        val offs = listOf(2 * h, 30 * 60_000L, 5 * 60_000L)
        var t = running(reminders = offs)
        // Just after the 2 h reminder.
        var due = Timing.due(t, start + day - 2 * h + 1)
        assertTrue(due is Due.Remind && (due as Due.Remind).offsets == listOf(2 * h))
        t = t.copy(remindersShown = setOf(2 * h))
        // 30 min before.
        due = Timing.due(t, start + day - 20 * 60_000L)
        assertTrue(due is Due.Remind && (due as Due.Remind).offsets == listOf(30 * 60_000L))
        t = t.copy(remindersShown = setOf(2 * h, 30 * 60_000L))
        // 5 min before.
        due = Timing.due(t, start + day - 2 * 60_000L)
        assertTrue(due is Due.Remind && (due as Due.Remind).offsets == listOf(5 * 60_000L))
    }

    @Test fun reminderThatAlreadyPassedDoesNotFireOnEnable() {
        // Offset of 5 h ago when the window is 1 h away should not retroactively trigger.
        val t = running(reminders = listOf(30 * 60_000L))
        // Very close to the deadline, with no reminders shown.
        val due = Timing.due(t, start + day - 20 * 60_000L)
        assertTrue(due is Due.Remind)
    }

    @Test fun noReminderWhenEmpty() {
        val t = running(reminders = emptyList())
        assertEquals(start + day, Timing.nextWake(t, start))
        assertEquals(Due.Nothing, Timing.due(t, start + day - 1))
    }

    @Test fun pauseHoldsTheDeadline() {
        val t = running(pausedUntil = start + 7 * day)
        val during = start + 3 * day
        assertEquals(Status.Paused(start + 7 * day), Timing.status(t, during))
        assertEquals(Due.Nothing, Timing.due(t, during))
        assertEquals(start + 7 * day, Timing.nextWake(t, during))
    }

    @Test fun countdownRestartsWhenPauseEnds() {
        val t = running(pausedUntil = start + 7 * day)
        val after = start + 7 * day + h
        assertEquals(Status.Running(start + 8 * day), Timing.status(t, after))
        assertEquals(Due.Nothing, Timing.due(t, after))
    }

    @Test fun shortPauseKeepsALaterCheckIn() {
        // A pause that ended before the last check-in changes nothing.
        val t = running(baseAt = start + 2 * h, pausedUntil = start + h)
        assertEquals(start + 2 * h + day, Timing.deadline(t))
    }

    @Test fun firedStaysFired() {
        val t = running(firedAt = start + day)
        assertEquals(Status.Fired(start + day), Timing.status(t, start + 2 * day))
        assertEquals(Due.Nothing, Timing.due(t, start + 2 * day))
        assertNull(Timing.nextWake(t, start + 2 * day))
    }

    @Test fun remindersAsLongAsTheIntervalAreIgnored() {
        // 10-minute interval with 2 h, 15, 10 and 5 min reminders: only the 5-minute one makes sense.
        val m = 60_000L
        val t = TimerState(true, start, 0, 10 * m, listOf(2 * h, 15 * m, 10 * m, 5 * m), 0, emptySet())
        assertEquals(listOf(5 * m), Timing.usableOffsets(t))
        assertEquals(Due.Nothing, Timing.due(t, start))
        assertEquals(start + 5 * m, Timing.nextWake(t, start))
        val due = Timing.due(t, start + 5 * m)
        assertTrue(due is Due.Remind && (due as Due.Remind).offsets == listOf(5 * m))
    }

    @Test fun zeroReminderIsIgnored() {
        val t = running(reminders = listOf(0L))
        assertEquals(start + day, Timing.nextWake(t, start))
        assertEquals(Due.Fire(FireReason.DEADLINE), Timing.due(t, start + day))
    }
}
