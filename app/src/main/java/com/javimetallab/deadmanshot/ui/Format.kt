package com.javimetallab.deadmanshot.ui

import android.content.Context
import android.text.format.DateUtils
import com.javimetallab.deadmanshot.R
import com.javimetallab.deadmanshot.core.Store

fun formatWhen(context: Context, millis: Long): String = DateUtils.formatDateTime(
    context, millis,
    DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or
        DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL
)

/** A length of time as people say it: "2 hours", "3 days", "45 minutes". */
fun formatDuration(context: Context, ms: Long): String {
    val r = context.resources
    val minutes = ms / 60_000
    return when {
        ms >= Store.DAY && ms % Store.DAY == 0L ->
            (ms / Store.DAY).toInt().let { r.getQuantityString(R.plurals.days, it, it) }
        ms >= Store.HOUR && ms % Store.HOUR == 0L ->
            (ms / Store.HOUR).toInt().let { r.getQuantityString(R.plurals.hours, it, it) }
        else -> minutes.toInt().let { r.getQuantityString(R.plurals.minutes, it, it) }
    }
}

/** Time left, rounded down to the minute: "1 d 3 h", "5 h 12 min", "8 min". */
fun formatRemaining(context: Context, ms: Long): String {
    val totalMin = (ms.coerceAtLeast(0) / 60_000)
    val d = totalMin / (24 * 60)
    val h = (totalMin / 60) % 24
    val m = totalMin % 60
    return when {
        d > 0 -> context.getString(R.string.remaining_dh, d, h)
        h > 0 -> context.getString(R.string.remaining_hm, h, m)
        else -> context.getString(R.string.remaining_m, m)
    }
}
