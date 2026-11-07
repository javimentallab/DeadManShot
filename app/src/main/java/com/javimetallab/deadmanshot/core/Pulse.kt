package com.javimetallab.deadmanshot.core

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.javimetallab.deadmanshot.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** One heart-rate reading from the watch. */
data class HrSample(val atMs: Long, val bpm: Int)

sealed interface PulseVerdict {
    /** Nothing wrong; [latest] is the newest reading, if any. */
    data class Ok(val latest: HrSample?) : PulseVerdict
    data class Low(val latest: HrSample) : PulseVerdict
    data class NoSignal(val latest: HrSample?) : PulseVerdict
    /** Gadgetbridge did not export recently, so the data says nothing either way. */
    data object Stale : PulseVerdict
}

/**
 * When the watch should raise an alert. A single odd reading must never fire
 * the switch: it takes [LOW_READINGS] readings in a row under the threshold, the
 * newest one recent, and even then the user gets [Timing.PULSE_GRACE_MS] to
 * answer before anything happens.
 */
object PulseRule {
    const val LOW_READINGS = 3
    const val RECENT_MS = 30 * 60_000L
    /** An export older than this means Gadgetbridge is not working, not that you are dead. */
    const val STALE_EXPORT_MS = 90 * 60_000L

    fun isValid(bpm: Int) = bpm in 1..250

    fun evaluate(
        samples: List<HrSample>, now: Long, thresholdBpm: Int, noSignalMs: Long, exportAgeMs: Long,
    ): PulseVerdict {
        if (exportAgeMs > STALE_EXPORT_MS) return PulseVerdict.Stale
        val valid = samples.filter { isValid(it.bpm) && it.atMs <= now + 60_000L }.sortedByDescending { it.atMs }
        val latest = valid.firstOrNull()
        if (noSignalMs > 0 && (latest == null || now - latest.atMs > noSignalMs)) {
            return PulseVerdict.NoSignal(latest)
        }
        if (latest == null) return PulseVerdict.Ok(null)
        val lastFew = valid.take(LOW_READINGS)
        if (lastFew.size == LOW_READINGS && lastFew.all { it.bpm < thresholdBpm } &&
            now - latest.atMs <= RECENT_MS
        ) {
            return PulseVerdict.Low(latest)
        }
        return PulseVerdict.Ok(latest)
    }

    /**
     * Whether an alert raised at [alertAt] can be called off: a normal reading
     * taken after it (for a low pulse), or any reading after it (for silence).
     */
    fun recovered(verdict: PulseVerdict, alertAt: Long, thresholdBpm: Int, reason: FireReason): Boolean {
        val latest = (verdict as? PulseVerdict.Ok)?.latest ?: return false
        if (latest.atMs <= alertAt) return false
        return reason == FireReason.NO_PULSE || latest.bpm >= thresholdBpm
    }

    /** Gadgetbridge stores seconds in most tables and milliseconds in a few. */
    fun toMillis(timestamp: Long): Long = if (timestamp < 100_000_000_000L) timestamp * 1000 else timestamp
}

/** Talks to Gadgetbridge: asks it to sync and export, then reads the export. */
object Gadgetbridge {
    val PACKAGES = listOf(
        "nodomain.freeyourgadget.gadgetbridge",
        "nodomain.freeyourgadget.gadgetbridge.nightly",
        "com.espruino.gadgetbridge.banglejs",
    )
    private const val ACTION_SYNC = "nodomain.freeyourgadget.gadgetbridge.command.ACTIVITY_SYNC"
    private const val ACTION_EXPORT = "nodomain.freeyourgadget.gadgetbridge.command.TRIGGER_EXPORT"
    private const val SYNC_WAIT_MS = 25_000L
    private const val EXPORT_WAIT_MS = 90_000L

    fun installed(context: Context): String? = PACKAGES.firstOrNull { pkg ->
        runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
    }

    fun lastModified(context: Context, uri: Uri): Long? = runCatching {
        context.contentResolver.query(
            uri, arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED), null, null, null
        )?.use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null }
    }.getOrNull()

    /** Asks for a sync and a fresh export, and waits for the file to change. */
    suspend fun refresh(context: Context, pkg: String, uri: Uri) {
        val before = lastModified(context, uri) ?: 0L
        context.sendBroadcast(Intent(ACTION_SYNC).setPackage(pkg))
        delay(SYNC_WAIT_MS)
        context.sendBroadcast(Intent(ACTION_EXPORT).setPackage(pkg))
        val until = System.currentTimeMillis() + EXPORT_WAIT_MS
        while (System.currentTimeMillis() < until) {
            delay(3_000)
            if ((lastModified(context, uri) ?: 0L) > before) {
                delay(3_000) // let the write finish
                return
            }
        }
    }

    /** The newest readings of every table that holds a heart rate. */
    fun readSamples(context: Context, uri: Uri, perTable: Int = 20): List<HrSample> {
        val copy = File(context.cacheDir, "gadgetbridge-export.db")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                copy.outputStream().use { input.copyTo(it, 1 shl 16) }
            } ?: throw IOException("cannot open the export")
            // Our own copy, so it may be opened read-write (needed if it is in WAL mode).
            val db = SQLiteDatabase.openDatabase(copy.path, null, SQLiteDatabase.OPEN_READWRITE)
            db.use {
                val tables = mutableListOf<String>()
                it.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
                    while (c.moveToNext()) tables += c.getString(0)
                }
                val out = mutableListOf<HrSample>()
                for (t in tables) {
                    // Daily aggregates (resting, maximum) are not live readings.
                    if (t.contains("RESTING") || t.contains("_MAX_")) continue
                    val cols = mutableSetOf<String>()
                    it.rawQuery("PRAGMA table_info(\"$t\")", null).use { c ->
                        while (c.moveToNext()) cols += c.getString(1)
                    }
                    if ("TIMESTAMP" !in cols || "HEART_RATE" !in cols) continue
                    it.rawQuery(
                        "SELECT TIMESTAMP, HEART_RATE FROM \"$t\" WHERE HEART_RATE BETWEEN 1 AND 250 " +
                            "ORDER BY TIMESTAMP DESC LIMIT $perTable", null
                    ).use { c ->
                        while (c.moveToNext()) out += HrSample(PulseRule.toMillis(c.getLong(0)), c.getInt(1))
                    }
                }
                return out
            }
        } finally {
            copy.delete()
            File(copy.path + "-wal").delete()
            File(copy.path + "-shm").delete()
            File(copy.path + "-journal").delete()
        }
    }
}

/**
 * Every few minutes while the switch runs: refresh Gadgetbridge's export and check the heart rate.
 * Started by hand ([KEY_MANUAL]), it also runs with the switch off, to try the reading: it then
 * only shows the result and never raises an alert.
 */
class PulseWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    companion object {
        const val KEY_MANUAL = "manual"
        /** Failed checks in a row before the user is told the watch is not being watched. */
        const val FAILS_TO_WARN = 3
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (Store.isDemo) return@withContext Result.success() // invented data: never send or run anything
        val ctx = applicationContext
        val s = Store.snapshot()
        val manual = inputData.getBoolean(KEY_MANUAL, false)
        val armed = s.pulse.enabled && Timing.status(s.timer, System.currentTimeMillis()) is Status.Running
        if (!armed && !manual) return@withContext Result.success()

        fun failed(message: String) {
            Store.setPulseLastError(message)
            if (armed && !manual && Store.addPulseFailure() >= FAILS_TO_WARN) {
                Notifications.showPulseProblem(ctx, message)
            }
        }

        if (s.pulse.exportUri.isBlank()) {
            failed(ctx.getString(R.string.pulse_err_no_file))
            return@withContext Result.success()
        }
        val pkg = Gadgetbridge.installed(ctx)
        if (pkg == null) {
            failed(ctx.getString(R.string.pulse_gadgetbridge_missing))
            return@withContext Result.success()
        }
        val uri = Uri.parse(s.pulse.exportUri)
        try {
            Gadgetbridge.refresh(ctx, pkg, uri)
            val now = System.currentTimeMillis()
            val modified = Gadgetbridge.lastModified(ctx, uri) ?: 0L
            val samples = Gadgetbridge.readSamples(ctx, uri)
            val verdict = PulseRule.evaluate(
                samples, now, s.pulse.thresholdBpm, s.pulse.noSignalMs, now - modified,
            )
            val latest = when (verdict) {
                is PulseVerdict.Ok -> verdict.latest
                is PulseVerdict.Low -> verdict.latest
                is PulseVerdict.NoSignal -> verdict.latest
                PulseVerdict.Stale -> null
            }
            Store.setPulseReading(latest?.bpm ?: 0, latest?.atMs ?: 0, now)
            if (verdict == PulseVerdict.Stale) {
                failed(ctx.getString(R.string.pulse_err_stale))
            } else {
                Store.setPulseLastError("")
                Store.clearPulseFailures()
                Notifications.cancelPulseProblem(ctx)
            }
            if (armed) Engine.onPulseVerdict(ctx, verdict)
        } catch (e: Exception) {
            failed(ctx.getString(R.string.pulse_err_read, e.message ?: e.javaClass.simpleName))
        }
        Result.success()
    }
}
