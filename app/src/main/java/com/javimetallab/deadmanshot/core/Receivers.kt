package com.javimetallab.deadmanshot.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** The single alarm: whatever is due (reminder, deadline, end of pause) gets done. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Engine.tick(context)
}

/** The "I'm OK" button on the reminder notification. */
class CheckInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Engine.checkIn(context)
}

/** Alarms are lost on reboot and when the app is updated; the clock can jump. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in ACTIONS) Engine.tick(context)
    }

    private companion object {
        val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
