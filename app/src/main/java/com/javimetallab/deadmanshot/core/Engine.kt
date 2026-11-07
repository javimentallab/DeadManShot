package com.javimetallab.deadmanshot.core

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.javimetallab.deadmanshot.pc.PcActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Every change of state goes through here: it updates the store, then sets the
 * next alarm. [tick] can be called at any moment and only does what is due.
 */
object Engine {
    private const val SEND_WORK = "send"
    private const val PC_WORK = "pc"
    private const val WATCHDOG_WORK = "watchdog"
    private const val PULSE_WORK = "pulse"
    private const val PULSE_NOW_WORK = "pulse-now"

    @Synchronized
    fun tick(context: Context) {
        val now = System.currentTimeMillis()
        val t = Store.snapshot().timer
        when (val due = Timing.due(t, now)) {
            is Due.Fire -> fire(context, due.reason)
            is Due.Remind -> {
                val deadline = Timing.deadline(t)
                for (offset in due.offsets) {
                    Store.markReminderShown(deadline, offset)
                    Notifications.showReminder(context, deadline, offset)
                }
            }
            Due.Nothing -> Unit
        }
        schedule(context)
    }

    /** "Fire now (test)": does exactly what a missed deadline does, if the switch is running. */
    @Synchronized
    fun fireNow(context: Context) {
        if (Timing.status(Store.snapshot().timer, System.currentTimeMillis()) !is Status.Running) return
        fire(context, FireReason.MANUAL)
        schedule(context)
    }

    /** The deadline was missed (or the heart rate fell): send the email and run PC actions. */
    @Synchronized
    fun fire(context: Context, reason: FireReason) {
        Store.markFired(System.currentTimeMillis(), reason)
        Notifications.cancelReminders(context)
        Notifications.cancelPulseAlert(context)
        val send = OneTimeWorkRequestBuilder<SendWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(SEND_WORK, ExistingWorkPolicy.KEEP, send)

        val pc = Store.snapshot().pc
        if (pc.enableWol || pc.enableCommand || pc.enableDestroyKey || pc.enableShred || pc.enableSshShutdown) {
            val pcWork = OneTimeWorkRequestBuilder<PcActionsWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(PC_WORK, ExistingWorkPolicy.KEEP, pcWork)
        }
    }

    fun checkIn(context: Context) {
        Store.checkIn(System.currentTimeMillis())
        afterReset(context)
    }

    fun setEnabled(context: Context, on: Boolean) {
        Store.setEnabled(on, System.currentTimeMillis())
        afterReset(context)
    }

    fun pause(context: Context, durationMs: Long) {
        Store.pauseUntil(System.currentTimeMillis() + durationMs)
        Notifications.cancelReminders(context)
        schedule(context)
    }

    fun resume(context: Context) {
        Store.pauseUntil(System.currentTimeMillis())
        tick(context)
    }

    fun setInterval(context: Context, ms: Long) {
        Store.setInterval(ms)
        tick(context)
    }

    fun setReminderOffsets(context: Context, offsets: List<Long>) {
        Store.setReminderOffsets(offsets)
        tick(context)
    }

    // --- Watch ---

    fun setPulseEnabled(context: Context, on: Boolean) {
        Store.setPulseEnabled(on)
        if (!on) clearPulseAlert(context)
        tick(context)
    }

    fun setPulseInterval(context: Context, minutes: Int) {
        Store.setPulseInterval(minutes)
        tick(context)
    }

    /** "Check now" in the Watch tab. */
    fun checkPulseNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            PULSE_NOW_WORK, ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<PulseWorker>()
                .setInputData(androidx.work.workDataOf(PulseWorker.KEY_MANUAL to true))
                .build(),
        )
    }

    /**
     * What the watch says. A worrying reading raises an alert, which fires the
     * switch [Timing.PULSE_GRACE_MS] later unless you check in; a normal reading
     * taken after the alert calls it off.
     */
    @Synchronized
    fun onPulseVerdict(context: Context, verdict: PulseVerdict) {
        val s = Store.snapshot()
        val now = System.currentTimeMillis()
        if (!s.pulse.enabled || Timing.status(s.timer, now) !is Status.Running) return
        val t = s.timer
        if (t.pulseAlertAt > 0) {
            if (PulseRule.recovered(verdict, t.pulseAlertAt, s.pulse.thresholdBpm, t.pulseAlertReason)) {
                clearPulseAlert(context)
                tick(context)
            }
            return
        }
        val (reason, bpm) = when (verdict) {
            is PulseVerdict.Low -> FireReason.HEART_RATE to verdict.latest.bpm
            is PulseVerdict.NoSignal -> FireReason.NO_PULSE to (verdict.latest?.bpm ?: 0)
            else -> return
        }
        Store.setPulseAlert(now, reason)
        Notifications.showPulseAlert(context, now + Timing.PULSE_GRACE_MS, reason, bpm)
        tick(context)
    }

    private fun clearPulseAlert(context: Context) {
        Store.clearPulseAlert()
        Notifications.cancelPulseAlert(context)
    }

    private fun afterReset(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(SEND_WORK)
        wm.cancelUniqueWork(PC_WORK)
        Notifications.cancelPulseAlert(context)
        Notifications.cancelReminders(context)
        Notifications.cancelResult(context)
        tick(context)
    }

    /** One alarm for the next thing that has to happen, plus a slow watchdog in case it is lost. */
    private fun schedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 0, Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val wm = WorkManager.getInstance(context)
        val snap = Store.snapshot()
        if (snap.pulse.enabled && snap.timer.enabled && snap.timer.firedAt == 0L) {
            wm.enqueueUniquePeriodicWork(
                PULSE_WORK, ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<PulseWorker>(
                    snap.pulse.checkIntervalMin.toLong().coerceAtLeast(15), TimeUnit.MINUTES
                ).build(),
            )
        } else {
            wm.cancelUniqueWork(PULSE_WORK)
        }
        val next = Timing.nextWake(snap.timer, System.currentTimeMillis())
        if (next == null) {
            am.cancel(pi)
            wm.cancelUniqueWork(WATCHDOG_WORK)
            return
        }
        if (am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
        }
        wm.enqueueUniquePeriodicWork(
            WATCHDOG_WORK, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WatchdogWorker>(15, TimeUnit.MINUTES).build()
        )
    }

    fun exactAlarmsAllowed(context: Context): Boolean =
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
}

class SendWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (Store.isDemo) return@withContext Result.success() // invented data: never send or run anything
        val s = Store.snapshot()
        if (!s.timer.enabled || s.timer.firedAt == 0L || s.sendState == SendState.SENT) {
            return@withContext Result.success()
        }
        try {
            val skipped = Mailer.send(applicationContext, s, test = false)
            Store.markSent(System.currentTimeMillis())
            Notifications.showSent(applicationContext, skipped)
            Result.success()
        } catch (e: Exception) {
            val msg = Mailer.explain(applicationContext, e.message ?: e.javaClass.simpleName)
            Store.markFailed(msg)
            Notifications.showFailed(applicationContext, msg.trimEnd('.'))
            Result.retry()
        }
    }
}

class PcActionsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (Store.isDemo) return@withContext Result.success() // invented data: never send or run anything
        val s = Store.snapshot()
        if (!s.timer.enabled || s.timer.firedAt == 0L) return@withContext Result.success()
        PcActions.runOnFire(applicationContext, s.pc)
        Result.success()
    }
}

class WatchdogWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Engine.tick(applicationContext)
        return Result.success()
    }
}
