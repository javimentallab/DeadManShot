package com.javimetallab.deadmanshot.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.javimetallab.deadmanshot.MainActivity
import com.javimetallab.deadmanshot.R

object Notifications {
    private const val CH_REMINDER = "reminder"
    private const val CH_RESULT = "result"
    private const val ID_RESULT = 10
    private const val ID_PULSE = 20
    private const val ID_PULSE_PROBLEM = 21
    private const val ID_REMINDER_BASE = 1000

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CH_REMINDER, context.getString(R.string.channel_reminder),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CH_RESULT, context.getString(R.string.channel_result),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    private fun openApp(context: Context) = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun post(context: Context, id: Int, builder: NotificationCompat.Builder) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        try {
            nm.notify(id, builder.build())
        } catch (_: SecurityException) {
            // Permission withdrawn between the check and the call.
        }
    }

    private fun reminderId(offsetMs: Long): Int =
        ID_REMINDER_BASE + (offsetMs / 60_000L).coerceIn(0, 1_000_000).toInt()

    fun showReminder(context: Context, deadline: Long, offsetMs: Long) {
        val checkIn = PendingIntent.getBroadcast(
            context, offsetMs.hashCode(), Intent(context, CheckInReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val time = DateUtils.formatDateTime(
            context, deadline,
            DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL
        )
        val remaining = DateUtils.getRelativeTimeSpanString(
            deadline, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
        ).toString()
        val title = context.getString(R.string.reminder_title)
        post(
            context, reminderId(offsetMs),
            NotificationCompat.Builder(context, CH_REMINDER)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(context.getString(R.string.reminder_text, time, remaining))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(openApp(context))
                .addAction(0, context.getString(R.string.check_in), checkIn)
                .setAutoCancel(true)
        )
    }

    /** The watch raised an alert: the switch fires at [fireAt] unless you check in. */
    fun showPulseAlert(context: Context, fireAt: Long, reason: FireReason, bpm: Int) {
        val checkIn = PendingIntent.getBroadcast(
            context, ID_PULSE, Intent(context, CheckInReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val time = DateUtils.formatDateTime(context, fireAt, DateUtils.FORMAT_SHOW_TIME)
        val text = if (reason == FireReason.NO_PULSE) context.getString(R.string.pulse_alert_silent, time)
        else context.getString(R.string.pulse_alert_low, bpm, time)
        post(
            context, ID_PULSE,
            NotificationCompat.Builder(context, CH_REMINDER)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.pulse_alert_title))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(openApp(context))
                .addAction(0, context.getString(R.string.check_in), checkIn)
                .setOngoing(true)
        )
    }

    fun cancelPulseAlert(context: Context) = NotificationManagerCompat.from(context).cancel(ID_PULSE)

    /** The heart rate cannot be read: the watch is not watching, and the user should know. */
    fun showPulseProblem(context: Context, error: String) {
        post(
            context, ID_PULSE_PROBLEM,
            NotificationCompat.Builder(context, CH_RESULT)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.pulse_problem_title))
                .setContentText(error)
                .setStyle(NotificationCompat.BigTextStyle().bigText(error))
                .setOnlyAlertOnce(true)
                .setContentIntent(openApp(context))
        )
    }

    fun cancelPulseProblem(context: Context) = NotificationManagerCompat.from(context).cancel(ID_PULSE_PROBLEM)

    fun cancelReminders(context: Context) {
        val nm = NotificationManagerCompat.from(context)
        for (i in 0..1440) nm.cancel(ID_REMINDER_BASE + i)
    }

    fun cancelResult(context: Context) = NotificationManagerCompat.from(context).cancel(ID_RESULT)

    fun showSent(context: Context, skipped: List<String>) {
        val text = if (skipped.isEmpty()) context.getString(R.string.sent_text)
        else context.getString(R.string.sent_text_skipped, skipped.joinToString(", "))
        post(
            context, ID_RESULT,
            NotificationCompat.Builder(context, CH_RESULT)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.sent_title))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(openApp(context))
        )
    }

    fun showFailed(context: Context, error: String) {
        val text = context.getString(R.string.failed_text, error)
        post(
            context, ID_RESULT,
            NotificationCompat.Builder(context, CH_RESULT)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.failed_title))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setOnlyAlertOnce(true)
                .setContentIntent(openApp(context))
        )
    }

    fun showPcResult(context: Context, title: String, body: String) {
        post(
            context, ID_RESULT + 1,
            NotificationCompat.Builder(context, CH_RESULT)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(openApp(context))
        )
    }
}
