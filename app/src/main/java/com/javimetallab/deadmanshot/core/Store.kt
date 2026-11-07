package com.javimetallab.deadmanshot.core

import android.content.Context
import android.content.SharedPreferences
import com.javimetallab.deadmanshot.mail.Security
import com.javimetallab.deadmanshot.pc.PcOs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

enum class SendState { NONE, PENDING, SENT, FAILED }

enum class FireReason { DEADLINE, HEART_RATE, NO_PULSE, MANUAL }

data class EmailConfig(
    val recipients: String,
    val subject: String,
    val body: String,
    /** content:// URIs with a persisted read grant. */
    val attachments: List<String>,
)

data class SmtpConfig(
    val host: String,
    val port: Int,
    val security: Security,
    val user: String,
    /** Blank means the same as [user]. */
    val from: String,
    val hasPassword: Boolean,
)

data class PulseConfig(
    val enabled: Boolean,
    val thresholdBpm: Int,
    /** The file Gadgetbridge exports its database to (content:// URI with a persisted grant). */
    val exportUri: String,
    val exportName: String,
    /** Fire when the watch sends no heart rate for this long; 0 = off. */
    val noSignalMs: Long,
    val checkIntervalMin: Int,
    val lastBpm: Int,
    val lastBpmAt: Long,
    val lastCheckAt: Long,
    val lastError: String,
)

data class PcConfig(
    val os: PcOs,
    val enableWol: Boolean,
    val mac: String,
    val broadcast: String,
    val wolWaitSeconds: Int,
    /** A command of the user's own, run over SSH right after Wake on LAN. */
    val enableCommand: Boolean,
    val command: String,
    val enableDestroyKey: Boolean,
    val keyPath: String,
    val enableShred: Boolean,
    val folder: String,
    val includeSubdirs: Boolean,
    val enableSshShutdown: Boolean,
    val sshHost: String,
    val sshPort: Int,
    val sshUser: String,
    val hasSshPassword: Boolean,
    /** OpenSSH public key line of the app's own key, or blank if there is none. */
    val sshPublicKey: String,
)

data class Snapshot(
    val timer: TimerState,
    val lastCheckIn: Long,
    val firedReason: FireReason,
    val sendState: SendState,
    val sentAt: Long,
    val lastError: String,
    val email: EmailConfig,
    val smtp: SmtpConfig,
    val pulse: PulseConfig,
    val pc: PcConfig,
)

/**
 * The only place that reads or writes the settings. Every write goes through
 * here, so [state] is always up to date without listening for changes.
 */
object Store {
    const val HOUR = 3_600_000L
    const val DAY = 24 * HOUR
    const val MINUTE = 60_000L

    private lateinit var prefs: SharedPreferences
    private val _state = MutableStateFlow<Snapshot?>(null)
    val state: StateFlow<Snapshot?> = _state.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("switch", Context.MODE_PRIVATE)
        migrate()
        _state.value = read()
    }

    fun snapshot(): Snapshot = read()

    /**
     * [sync] writes to disk before returning. Timer changes need it, because a
     * receiver's process may be killed right after; text fields do not.
     */
    @android.annotation.SuppressLint("ApplySharedPref") // commit() on purpose, see above
    @Synchronized
    private fun edit(sync: Boolean = true, block: SharedPreferences.Editor.() -> Unit) {
        val editor = prefs.edit().apply(block)
        if (sync) editor.commit() else editor.apply()
        _state.value = read()
    }

    /** Upgrades the single-reminder scheme to the multi-reminder one on first run. */
    private fun migrate() {
        if (!prefs.contains(K.REMINDER_LEGACY)) return
        val legacy = prefs.getLong(K.REMINDER_LEGACY, 0)
        val offsets = if (legacy > 0) listOf(legacy) else emptyList()
        prefs.edit()
            .putString(K.REMINDERS, JSONArray(offsets).toString())
            .remove(K.REMINDER_LEGACY)
            .apply()
        if (prefs.contains(K.REMINDER_FOR_LEGACY)) {
            prefs.edit().remove(K.REMINDER_FOR_LEGACY).apply()
        }
    }

    private fun read(): Snapshot = with(prefs) {
        Snapshot(
            timer = TimerState(
                enabled = getBoolean(K.ENABLED, false),
                baseAt = getLong(K.BASE_AT, 0),
                pausedUntil = getLong(K.PAUSED_UNTIL, 0),
                intervalMs = getLong(K.INTERVAL, DAY),
                reminderOffsets = readLongList(K.REMINDERS, listOf(2 * HOUR)),
                firedAt = getLong(K.FIRED_AT, 0),
                remindersShown = readShownReminders(),
                pulseAlertAt = getLong(K.PULSE_ALERT_AT, 0),
                pulseAlertReason = enumValueOrNull<FireReason>(getString(K.PULSE_ALERT_REASON, null))
                    ?: FireReason.HEART_RATE,
            ),
            lastCheckIn = getLong(K.LAST_CHECK_IN, 0),
            firedReason = enumValueOrNull<FireReason>(getString(K.FIRED_REASON, null))
                ?: FireReason.DEADLINE,
            sendState = enumValueOrNull<SendState>(getString(K.SEND_STATE, null))
                ?: SendState.NONE,
            sentAt = getLong(K.SENT_AT, 0),
            lastError = getString(K.LAST_ERROR, "") ?: "",
            email = EmailConfig(
                recipients = getString(K.TO, "") ?: "",
                subject = getString(K.SUBJECT, "") ?: "",
                body = getString(K.BODY, "") ?: "",
                attachments = getString(K.ATTACHMENTS, null)?.let { raw ->
                    val arr = JSONArray(raw)
                    List(arr.length()) { arr.getString(it) }
                } ?: emptyList(),
            ),
            smtp = SmtpConfig(
                host = getString(K.HOST, "smtp.gmail.com") ?: "",
                port = getInt(K.PORT, 465),
                security = enumValueOrNull<Security>(getString(K.SECURITY, null)) ?: Security.SSL,
                user = getString(K.USER, "") ?: "",
                from = getString(K.FROM, "") ?: "",
                hasPassword = contains(K.PASSWORD),
            ),
            pulse = PulseConfig(
                enabled = getBoolean(K.PULSE_ENABLED, false),
                thresholdBpm = getInt(K.PULSE_THRESHOLD, 40),
                exportUri = getString(K.PULSE_EXPORT_URI, "") ?: "",
                exportName = getString(K.PULSE_EXPORT_NAME, "") ?: "",
                noSignalMs = getLong(K.PULSE_NO_SIGNAL, 0),
                checkIntervalMin = getInt(K.PULSE_INTERVAL, 30),
                lastBpm = getInt(K.PULSE_LAST_BPM, 0),
                lastBpmAt = getLong(K.PULSE_LAST_BPM_AT, 0),
                lastCheckAt = getLong(K.PULSE_LAST_CHECK, 0),
                lastError = getString(K.PULSE_LAST_ERROR, "") ?: "",
            ),
            pc = PcConfig(
                os = enumValueOrNull<PcOs>(getString(K.PC_OS, null)) ?: PcOs.WINDOWS,
                enableWol = getBoolean(K.PC_WOL_ENABLED, false),
                mac = getString(K.PC_MAC, "") ?: "",
                broadcast = getString(K.PC_BROADCAST, "255.255.255.255") ?: "",
                wolWaitSeconds = getInt(K.PC_WOL_WAIT, 60),
                enableCommand = getBoolean(K.PC_CMD_ENABLED, false),
                command = getString(K.PC_CMD, "") ?: "",
                enableDestroyKey = getBoolean(K.PC_KEY_ENABLED, false),
                keyPath = getString(K.PC_KEY_PATH, "") ?: "",
                enableShred = getBoolean(K.PC_SHRED_ENABLED, false),
                folder = getString(K.PC_FOLDER, "") ?: "",
                includeSubdirs = getBoolean(K.PC_SUBDIRS, true),
                enableSshShutdown = getBoolean(K.PC_SHUTDOWN_ENABLED, false),
                sshHost = getString(K.PC_SSH_HOST, "") ?: "",
                sshPort = getInt(K.PC_SSH_PORT, 22),
                sshUser = getString(K.PC_SSH_USER, "") ?: "",
                hasSshPassword = contains(K.PC_SSH_PASSWORD),
                sshPublicKey = if (contains(K.PC_SSH_KEY_PRIVATE)) getString(K.PC_SSH_KEY_PUBLIC, "") ?: "" else "",
            ),
        )
    }

    private fun readLongList(key: String, default: List<Long>): List<Long> =
        prefs.getString(key, null)?.let { raw ->
            runCatching {
                val arr = JSONArray(raw)
                List(arr.length()) { arr.getLong(it) }.sortedDescending()
            }.getOrDefault(default)
        } ?: default

    private fun readShownReminders(): Set<Long> {
        val raw = prefs.getString(K.REMINDERS_SHOWN, null) ?: return emptySet()
        val sep = raw.indexOf(':')
        if (sep <= 0) return emptySet()
        val storedDeadline = raw.substring(0, sep).toLongOrNull() ?: return emptySet()
        val currentDeadline = Timing.deadline(
            TimerState(
                enabled = prefs.getBoolean(K.ENABLED, false),
                baseAt = prefs.getLong(K.BASE_AT, 0),
                pausedUntil = prefs.getLong(K.PAUSED_UNTIL, 0),
                intervalMs = prefs.getLong(K.INTERVAL, DAY),
                reminderOffsets = emptyList(),
                firedAt = 0, remindersShown = emptySet(),
            )
        )
        if (storedDeadline != currentDeadline) return emptySet()
        return raw.substring(sep + 1).split(',')
            .mapNotNull { it.trim().toLongOrNull() }
            .toSet()
    }

    // --- Timer ---

    fun setEnabled(on: Boolean, now: Long) = edit {
        putBoolean(K.ENABLED, on)
        if (on) {
            putLong(K.BASE_AT, now)
            putLong(K.PAUSED_UNTIL, 0)
            clearFired()
        }
        remove(K.REMINDERS_SHOWN)
    }

    fun checkIn(now: Long) = edit {
        putLong(K.BASE_AT, now)
        putLong(K.LAST_CHECK_IN, now)
        clearFired()
        remove(K.REMINDERS_SHOWN)
    }

    fun pauseUntil(until: Long) = edit {
        putLong(K.PAUSED_UNTIL, until)
        remove(K.REMINDERS_SHOWN)
        remove(K.PULSE_ALERT_AT)
    }

    fun setPulseAlert(now: Long, reason: FireReason) = edit {
        putLong(K.PULSE_ALERT_AT, now)
        putString(K.PULSE_ALERT_REASON, reason.name)
    }

    fun clearPulseAlert() = edit { remove(K.PULSE_ALERT_AT) }

    fun setInterval(ms: Long) = edit { putLong(K.INTERVAL, ms) }

    fun setReminderOffsets(list: List<Long>) = edit {
        putString(K.REMINDERS, JSONArray(list.sortedDescending()).toString())
        remove(K.REMINDERS_SHOWN)
    }

    fun markReminderShown(deadline: Long, offset: Long) = edit {
        val raw = prefs.getString(K.REMINDERS_SHOWN, null)
        val current = mutableSetOf<Long>()
        if (raw != null) {
            val sep = raw.indexOf(':')
            if (sep > 0 && raw.substring(0, sep).toLongOrNull() == deadline) {
                raw.substring(sep + 1).split(',')
                    .mapNotNull { it.trim().toLongOrNull() }
                    .forEach { current += it }
            }
        }
        current += offset
        putString(K.REMINDERS_SHOWN, "$deadline:" + current.joinToString(","))
    }

    fun markFired(now: Long, reason: FireReason) = edit {
        putLong(K.FIRED_AT, now)
        putString(K.FIRED_REASON, reason.name)
        putString(K.SEND_STATE, SendState.PENDING.name)
        putString(K.LAST_ERROR, "")
    }

    fun markSent(now: Long) = edit {
        putString(K.SEND_STATE, SendState.SENT.name)
        putLong(K.SENT_AT, now)
        putString(K.LAST_ERROR, "")
    }

    fun markFailed(error: String) = edit {
        putString(K.SEND_STATE, SendState.FAILED.name)
        putString(K.LAST_ERROR, error)
    }

    private fun SharedPreferences.Editor.clearFired() {
        remove(K.PULSE_ALERT_AT)
        putLong(K.FIRED_AT, 0)
        putString(K.SEND_STATE, SendState.NONE.name)
        putString(K.LAST_ERROR, "")
    }

    // --- Email ---

    fun setRecipients(v: String) = edit(sync = false) { putString(K.TO, v) }
    fun setSubject(v: String) = edit(sync = false) { putString(K.SUBJECT, v) }
    fun setBody(v: String) = edit(sync = false) { putString(K.BODY, v) }

    fun setAttachments(uris: List<String>) = edit {
        putString(K.ATTACHMENTS, JSONArray(uris).toString())
    }

    // --- Sending account ---

    fun setHost(v: String) = edit(sync = false) { putString(K.HOST, v.trim()) }
    fun setPort(v: Int) = edit(sync = false) { putInt(K.PORT, v) }
    fun setSecurity(v: Security) = edit(sync = false) { putString(K.SECURITY, v.name) }
    fun setUser(v: String) = edit(sync = false) { putString(K.USER, v.trim()) }
    fun setFrom(v: String) = edit(sync = false) { putString(K.FROM, v.trim()) }

    fun setPassword(v: String) = edit(sync = false) {
        if (v.isEmpty()) remove(K.PASSWORD) else putString(K.PASSWORD, SecretStore.encrypt(v))
    }

    fun password(): String? = prefs.getString(K.PASSWORD, null)?.let(SecretStore::decrypt)

    fun applySmtpPreset(host: String, port: Int, security: Security) = edit {
        putString(K.HOST, host)
        putInt(K.PORT, port)
        putString(K.SECURITY, security.name)
    }

    // --- Pulse ---

    fun setPulseEnabled(v: Boolean) = edit { putBoolean(K.PULSE_ENABLED, v) }
    fun setPulseThreshold(v: Int) = edit(sync = false) { putInt(K.PULSE_THRESHOLD, v.coerceIn(10, 40)) }
    fun setPulseExport(uri: String, name: String) = edit {
        putString(K.PULSE_EXPORT_URI, uri)
        putString(K.PULSE_EXPORT_NAME, name)
    }
    /** Counts one more failed check in a row and returns the count. */
    fun addPulseFailure(): Int {
        val n = prefs.getInt(K.PULSE_FAILS, 0) + 1
        edit(sync = false) { putInt(K.PULSE_FAILS, n) }
        return n
    }
    fun clearPulseFailures() = edit(sync = false) { remove(K.PULSE_FAILS) }
    fun setPulseNoSignal(ms: Long) = edit { putLong(K.PULSE_NO_SIGNAL, ms) }
    fun setPulseInterval(min: Int) = edit { putInt(K.PULSE_INTERVAL, min.coerceIn(15, 240)) }
    fun setPulseReading(bpm: Int, at: Long, checkedAt: Long) = edit(sync = false) {
        putInt(K.PULSE_LAST_BPM, bpm)
        putLong(K.PULSE_LAST_BPM_AT, at)
        putLong(K.PULSE_LAST_CHECK, checkedAt)
    }
    fun setPulseLastError(v: String) = edit(sync = false) { putString(K.PULSE_LAST_ERROR, v) }

    // --- PC ---

    fun setPcOs(v: PcOs) = edit { putString(K.PC_OS, v.name) }
    fun setPcWolEnabled(v: Boolean) = edit { putBoolean(K.PC_WOL_ENABLED, v) }
    fun setPcMac(v: String) = edit(sync = false) { putString(K.PC_MAC, v.trim()) }
    fun setPcBroadcast(v: String) = edit(sync = false) { putString(K.PC_BROADCAST, v.trim()) }
    fun setPcWolWait(v: Int) = edit(sync = false) { putInt(K.PC_WOL_WAIT, v.coerceIn(0, 600)) }
    fun setPcCommandEnabled(v: Boolean) = edit { putBoolean(K.PC_CMD_ENABLED, v) }
    fun setPcCommand(v: String) = edit(sync = false) { putString(K.PC_CMD, v) }
    fun setPcKeyEnabled(v: Boolean) = edit { putBoolean(K.PC_KEY_ENABLED, v) }
    fun setPcKeyPath(v: String) = edit(sync = false) { putString(K.PC_KEY_PATH, v.trim()) }
    fun setPcShredEnabled(v: Boolean) = edit { putBoolean(K.PC_SHRED_ENABLED, v) }
    fun setPcFolder(v: String) = edit(sync = false) { putString(K.PC_FOLDER, v.trim()) }
    fun setPcSubdirs(v: Boolean) = edit { putBoolean(K.PC_SUBDIRS, v) }
    fun setPcShutdownEnabled(v: Boolean) = edit { putBoolean(K.PC_SHUTDOWN_ENABLED, v) }
    fun setPcSshHost(v: String) = edit(sync = false) { putString(K.PC_SSH_HOST, v.trim()) }
    fun setPcSshPort(v: Int) = edit(sync = false) { putInt(K.PC_SSH_PORT, v.coerceIn(1, 65535)) }
    fun setPcSshUser(v: String) = edit(sync = false) { putString(K.PC_SSH_USER, v.trim()) }

    fun setPcSshPassword(v: String) = edit(sync = false) {
        if (v.isEmpty()) remove(K.PC_SSH_PASSWORD)
        else putString(K.PC_SSH_PASSWORD, SecretStore.encrypt(v))
    }

    /** The app's own SSH key. The private half is stored encrypted, like the passwords. */
    fun setSshKey(privatePem: String, publicLine: String) = edit {
        putString(K.PC_SSH_KEY_PRIVATE, SecretStore.encrypt(privatePem))
        putString(K.PC_SSH_KEY_PUBLIC, publicLine)
    }

    fun deleteSshKey() = edit {
        remove(K.PC_SSH_KEY_PRIVATE)
        remove(K.PC_SSH_KEY_PUBLIC)
    }

    fun sshPrivateKey(): String? = prefs.getString(K.PC_SSH_KEY_PRIVATE, null)?.let(SecretStore::decrypt)

    fun sshPassword(): String? = prefs.getString(K.PC_SSH_PASSWORD, null)?.let(SecretStore::decrypt)

    fun savedSshHostKey(host: String, port: Int): String? =
        prefs.getString(K.PC_SSH_HOSTKEY_PREFIX + "$host:$port", null)

    fun saveSshHostKey(host: String, port: Int, fingerprint: String) = edit(sync = false) {
        putString(K.PC_SSH_HOSTKEY_PREFIX + "$host:$port", fingerprint)
    }

    // --- Demo (debug builds only, for screenshots) ---

    /** True while the invented demo data is in place: nothing is sent or run then. */
    val isDemo: Boolean get() = prefs.getBoolean(K.DEMO, false)

    /** Replaces everything with invented data. Only reachable from debug builds. */
    fun seedDemo(now: Long) = edit {
        clear()
        putBoolean(K.DEMO, true)
        putBoolean(K.ENABLED, true)
        putLong(K.BASE_AT, now - 3 * HOUR - 20 * MINUTE)
        putLong(K.LAST_CHECK_IN, now - 3 * HOUR - 20 * MINUTE)
        putLong(K.INTERVAL, DAY)
        putString(K.REMINDERS, JSONArray(listOf(2 * HOUR, 15 * MINUTE)).toString())
        putString(K.TO, "alex@example.com, sam@example.org")
        putString(K.SUBJECT, "If you are reading this")
        putString(
            K.BODY,
            "Hi both,\n\nIf this reaches you, I have not checked in for a day. " +
                "The letter attached explains where everything is.\n\nTake care.",
        )
        putString(K.HOST, "smtp.example.com")
        putInt(K.PORT, 465)
        putString(K.USER, "me@example.com")
        putString(K.PASSWORD, SecretStore.encrypt("demo"))
        putBoolean(K.PULSE_ENABLED, true)
        putInt(K.PULSE_THRESHOLD, 40)
        putString(K.PULSE_EXPORT_URI, "content://demo/Gadgetbridge.db")
        putString(K.PULSE_EXPORT_NAME, "Gadgetbridge.db")
        putInt(K.PULSE_INTERVAL, 30)
        putLong(K.PULSE_NO_SIGNAL, 8 * HOUR)
        putInt(K.PULSE_LAST_BPM, 64)
        putLong(K.PULSE_LAST_BPM_AT, now - 7 * MINUTE)
        putLong(K.PULSE_LAST_CHECK, now - 4 * MINUTE)
        putString(K.PC_OS, "WINDOWS")
        putBoolean(K.PC_WOL_ENABLED, true)
        putString(K.PC_MAC, "00:11:22:33:44:55")
        putString(K.PC_BROADCAST, "192.168.1.255")
        putInt(K.PC_WOL_WAIT, 90)
        putBoolean(K.PC_CMD_ENABLED, true)
        putString(K.PC_CMD, "Write-Output \"Fired at \$(Get-Date)\"")
        putBoolean(K.PC_KEY_ENABLED, true)
        putString(K.PC_KEY_PATH, "D:\\Vault\\keys\\vault.key")
        putBoolean(K.PC_SHRED_ENABLED, false)
        putString(K.PC_FOLDER, "C:\\Users\\alex\\Documents\\Private")
        putBoolean(K.PC_SUBDIRS, true)
        putBoolean(K.PC_SHUTDOWN_ENABLED, true)
        putString(K.PC_SSH_HOST, "192.168.1.20")
        putInt(K.PC_SSH_PORT, 22)
        putString(K.PC_SSH_USER, "alex")
        putString(K.PC_SSH_PASSWORD, SecretStore.encrypt("demo"))
    }

    private inline fun <reified E : Enum<E>> enumValueOrNull(name: String?): E? =
        enumValues<E>().firstOrNull { it.name == name }

    private object K {
        const val ENABLED = "enabled"
        const val BASE_AT = "base_at"
        const val PAUSED_UNTIL = "paused_until"
        const val INTERVAL = "interval_ms"
        const val REMINDER_LEGACY = "reminder_ms"
        const val REMINDER_FOR_LEGACY = "reminder_for"
        const val REMINDERS = "reminder_offsets"
        const val REMINDERS_SHOWN = "reminders_shown"
        const val FIRED_AT = "fired_at"
        const val FIRED_REASON = "fired_reason"
        const val LAST_CHECK_IN = "last_check_in"
        const val SEND_STATE = "send_state"
        const val SENT_AT = "sent_at"
        const val LAST_ERROR = "last_error"
        const val TO = "to"
        const val SUBJECT = "subject"
        const val BODY = "body"
        const val ATTACHMENTS = "attachments"
        const val HOST = "smtp_host"
        const val PORT = "smtp_port"
        const val SECURITY = "smtp_security"
        const val USER = "smtp_user"
        const val FROM = "smtp_from"
        const val PASSWORD = "smtp_password"
        const val PULSE_ENABLED = "pulse_enabled"
        const val PULSE_THRESHOLD = "pulse_threshold"
        const val PULSE_EXPORT_URI = "pulse_export_uri"
        const val PULSE_EXPORT_NAME = "pulse_export_name"
        const val PULSE_NO_SIGNAL = "pulse_no_signal_ms"
        const val PULSE_INTERVAL = "pulse_interval_min"
        const val PULSE_LAST_BPM = "pulse_last_bpm"
        const val PULSE_LAST_BPM_AT = "pulse_last_bpm_at"
        const val PULSE_LAST_CHECK = "pulse_last_check"
        const val PULSE_FAILS = "pulse_fails"
        const val PULSE_ALERT_AT = "pulse_alert_at"
        const val PULSE_ALERT_REASON = "pulse_alert_reason"
        const val PULSE_LAST_ERROR = "pulse_last_error"
        const val DEMO = "demo"
        const val PC_OS = "pc_os"
        const val PC_WOL_ENABLED = "pc_wol_enabled"
        const val PC_MAC = "pc_mac"
        const val PC_BROADCAST = "pc_broadcast"
        const val PC_WOL_WAIT = "pc_wol_wait"
        const val PC_CMD_ENABLED = "pc_cmd_enabled"
        const val PC_CMD = "pc_cmd"
        const val PC_KEY_ENABLED = "pc_key_enabled"
        const val PC_KEY_PATH = "pc_key_path"
        const val PC_SHRED_ENABLED = "pc_shred_enabled"
        const val PC_FOLDER = "pc_folder"
        const val PC_SUBDIRS = "pc_subdirs"
        const val PC_SHUTDOWN_ENABLED = "pc_shutdown_enabled"
        const val PC_SSH_HOST = "pc_ssh_host"
        const val PC_SSH_PORT = "pc_ssh_port"
        const val PC_SSH_USER = "pc_ssh_user"
        const val PC_SSH_PASSWORD = "pc_ssh_password"
        const val PC_SSH_HOSTKEY_PREFIX = "pc_ssh_hk_"
        const val PC_SSH_KEY_PRIVATE = "pc_ssh_key_private"
        const val PC_SSH_KEY_PUBLIC = "pc_ssh_key_public"
    }
}
