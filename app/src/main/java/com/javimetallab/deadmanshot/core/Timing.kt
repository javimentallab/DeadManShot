package com.javimetallab.deadmanshot.core

import kotlin.math.max
import kotlin.math.min

/**
 * Everything the clock needs, and nothing else, so the rules can be tested
 * without Android.
 *
 * The countdown starts from [baseAt] (the last check-in, or when the switch was
 * turned on) or from the end of the last pause, whichever is later: coming back
 * from a week away must not send the email on the spot.
 */
data class TimerState(
    val enabled: Boolean,
    val baseAt: Long,
    val pausedUntil: Long,
    val intervalMs: Long,
    /** Reminder offsets before the deadline, in ms. Ordered high-to-low. */
    val reminderOffsets: List<Long>,
    /** When the deadline was missed; 0 while it has not been. */
    val firedAt: Long,
    /** Offsets whose reminder has already been shown for the current deadline. */
    val remindersShown: Set<Long>,
    /** When the watch raised an alert (heart rate too low, or silent); 0 if none. */
    val pulseAlertAt: Long = 0,
    /** Why the watch raised the alert; meaningful only with [pulseAlertAt]. */
    val pulseAlertReason: FireReason = FireReason.HEART_RATE,
)

sealed interface Status {
    data object Off : Status
    data class Paused(val until: Long) : Status
    /** [deadline] already takes a watch alert into account. */
    data class Running(val deadline: Long) : Status
    data class Fired(val at: Long) : Status
}

sealed interface Due {
    data object Nothing : Due
    data class Fire(val reason: FireReason) : Due
    data class Remind(val offsets: List<Long>) : Due
}

object Timing {
    /** Time to press "I'm OK" after a watch alert before the switch fires. */
    const val PULSE_GRACE_MS = 10 * 60_000L

    /** The deadline of the daily check-in, ignoring the watch. */
    fun deadline(t: TimerState): Long = max(t.baseAt, t.pausedUntil) + t.intervalMs

    /** When a watch alert fires the switch, or null if there is none. */
    fun pulseDeadline(t: TimerState): Long? =
        if (t.pulseAlertAt > 0) t.pulseAlertAt + PULSE_GRACE_MS else null

    /** Whichever comes first: the check-in deadline or the end of a watch alert. */
    fun effectiveDeadline(t: TimerState): Long =
        pulseDeadline(t)?.let { min(it, deadline(t)) } ?: deadline(t)

    /**
     * The reminders that make sense for this interval. One as long as the
     * interval or longer would go off the moment you check in, and one of zero
     * would never show, because at the deadline the switch fires instead.
     */
    fun usableOffsets(t: TimerState): List<Long> =
        t.reminderOffsets.filter { it in 1 until t.intervalMs }

    fun status(t: TimerState, now: Long): Status = when {
        !t.enabled -> Status.Off
        t.firedAt > 0 -> Status.Fired(t.firedAt)
        t.pausedUntil > now -> Status.Paused(t.pausedUntil)
        else -> Status.Running(effectiveDeadline(t))
    }

    private fun pendingReminders(t: TimerState, now: Long): List<Long> {
        val offsets = usableOffsets(t)
        if (offsets.isEmpty()) return emptyList()
        val deadline = deadline(t)
        return offsets
            .filter { it !in t.remindersShown && now >= deadline - it && now < deadline }
            .sortedDescending()
    }

    /** What has to happen right now. Calling it twice in a row is harmless. */
    fun due(t: TimerState, now: Long): Due {
        if (status(t, now) !is Status.Running) return Due.Nothing
        val pulse = pulseDeadline(t)
        if (pulse != null && now >= pulse && pulse <= deadline(t)) return Due.Fire(t.pulseAlertReason)
        if (now >= deadline(t)) return Due.Fire(FireReason.DEADLINE)
        val pending = pendingReminders(t, now)
        return if (pending.isEmpty()) Due.Nothing else Due.Remind(pending)
    }

    /** When to wake up next, or null if nothing is waiting. */
    fun nextWake(t: TimerState, now: Long): Long? = when (val s = status(t, now)) {
        is Status.Off, is Status.Fired -> null
        is Status.Paused -> s.until
        is Status.Running -> {
            val deadline = deadline(t)
            val nextReminder = usableOffsets(t)
                .filter { it !in t.remindersShown }
                .map { deadline - it }
                .filter { it > now }
                .minOrNull()
            listOfNotNull(nextReminder, deadline, pulseDeadline(t)).min()
        }
    }
}
