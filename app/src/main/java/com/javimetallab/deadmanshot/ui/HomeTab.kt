package com.javimetallab.deadmanshot.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.javimetallab.deadmanshot.R
import com.javimetallab.deadmanshot.core.Engine
import com.javimetallab.deadmanshot.core.FireReason
import com.javimetallab.deadmanshot.core.Mailer
import com.javimetallab.deadmanshot.core.SendState
import com.javimetallab.deadmanshot.core.Snapshot
import com.javimetallab.deadmanshot.core.Status
import com.javimetallab.deadmanshot.core.Store
import com.javimetallab.deadmanshot.core.Timing

private val PAUSE_OPTIONS = listOf(
    1, 2, 4, 8, 12, 24, 48, 72, 7 * 24, 14 * 24, 30 * 24
).map { it * Store.HOUR }

private val INTERVAL_OPTIONS_MIN = listOf(10L, 15L, 30L).map { it * Store.MINUTE }
private val INTERVAL_OPTIONS_HOUR = listOf(1L, 2L, 4L, 8L, 12L, 24L, 48L, 72L).map { it * Store.HOUR }
private val INTERVAL_OPTIONS = INTERVAL_OPTIONS_MIN + INTERVAL_OPTIONS_HOUR

private val REMINDER_OPTIONS = listOf(
    5 * Store.MINUTE, 10 * Store.MINUTE, 15 * Store.MINUTE, 30 * Store.MINUTE,
    Store.HOUR, 2 * Store.HOUR, 4 * Store.HOUR,
)

@Composable
fun HomeTab(
    s: Snapshot,
    tick: Int,
    permissionsTick: Int,
    askNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = remember(tick, s) { System.currentTimeMillis() }
    val status = Timing.status(s.timer, now)

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatusCard(s, status, now, askNotifications)
        CheckInSection(s, status)
        PermissionsCard(permissionsTick, askNotifications)
        TimingCard(s)
    }
}

@Composable
private fun StatusCard(s: Snapshot, status: Status, now: Long, askNotifications: () -> Unit) {
    val context = LocalContext.current
    var showPause by rememberSaveable { mutableStateOf(false) }
    var showFireNow by rememberSaveable { mutableStateOf(false) }
    var missing by remember { mutableStateOf(emptyList<String>()) }

    val color = when (status) {
        Status.Off -> FiredColor   // Stopped = red
        is Status.Paused -> PausedColor
        is Status.Running -> RunningColor
        is Status.Fired -> FiredColor
    }
    val titleRes = when (status) {
        Status.Off -> R.string.status_off
        is Status.Paused -> R.string.status_paused
        is Status.Running -> R.string.status_running
        is Status.Fired -> R.string.status_fired
    }

    val detail = when (status) {
        Status.Off -> stringResource(R.string.detail_off)
        is Status.Running -> {
            val running = stringResource(
                R.string.detail_running,
                formatWhen(context, status.deadline), formatRemaining(context, status.deadline - now)
            )
            val pulse = Timing.pulseDeadline(s.timer)
            if (pulse != null) running + "\n" + stringResource(
                R.string.detail_pulse_alert,
                android.text.format.DateUtils.formatDateTime(context, pulse, android.text.format.DateUtils.FORMAT_SHOW_TIME),
            )
            else running
        }
        is Status.Paused -> stringResource(
            R.string.detail_paused,
            formatWhen(context, status.until), formatRemaining(context, status.until - now)
        )
        is Status.Fired -> {
            val why = when (s.firedReason) {
                FireReason.HEART_RATE -> R.string.fired_heart_rate
                FireReason.NO_PULSE -> R.string.fired_no_pulse
                FireReason.MANUAL -> R.string.fired_manual
                FireReason.DEADLINE -> R.string.fired_deadline
            }
            val sending = when (s.sendState) {
                SendState.SENT -> stringResource(R.string.send_sent, formatWhen(context, s.sentAt))
                SendState.FAILED -> stringResource(R.string.send_failed, s.lastError)
                else -> stringResource(R.string.send_pending)
            }
            stringResource(why, formatWhen(context, status.at)) + "\n" + sending + "\n" +
                stringResource(R.string.fired_reset)
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.16f))) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusGlyph(status, color)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(titleRes),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(detail, style = MaterialTheme.typography.bodyLarge)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.switch_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                )
                Switch(
                    checked = s.timer.enabled,
                    onCheckedChange = { on ->
                        missing = if (on) Mailer.missing(context, Store.snapshot()) else emptyList()
                        if (missing.isEmpty()) {
                            if (on) askNotifications()
                            Engine.setEnabled(context, on)
                        }
                    },
                )
            }
            if (missing.isNotEmpty()) {
                Text(
                    stringResource(R.string.cannot_enable) + "\n" +
                        missing.joinToString("\n") { "• $it" },
                    color = MaterialTheme.colorScheme.error,
                )
            }

            when (status) {
                is Status.Running -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showPause = true }) {
                        Icon(Icons.Filled.Pause, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.pause))
                    }
                    OutlinedButton(
                        onClick = { showFireNow = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FiredColor),
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.fire_now))
                    }
                }
                is Status.Paused -> Button(onClick = { Engine.resume(context) }) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.resume))
                }
                else -> Unit
            }
        }
    }

    if (showFireNow) {
        FireNowDialog(
            s = s,
            onDismiss = { showFireNow = false },
            onConfirm = {
                Engine.fireNow(context)
                showFireNow = false
            },
        )
    }

    if (showPause) {
        PauseDialog(
            now = now,
            onDismiss = { showPause = false },
            onPick = {
                Engine.pause(context, it)
                showPause = false
            },
        )
    }
}

@Composable
private fun StatusGlyph(status: Status, color: Color) {
    when (status) {
        is Status.Running -> {
            val transition = rememberInfiniteTransition(label = "heart")
            val alpha by transition.animateFloat(
                initialValue = 1f, targetValue = 0.35f,
                animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
                label = "pulse",
            )
            Icon(
                Icons.Filled.Favorite, contentDescription = null,
                tint = color, modifier = Modifier.size(28.dp).alpha(alpha),
            )
        }
        is Status.Fired -> Icon(
            Icons.Filled.Error, contentDescription = null,
            tint = color, modifier = Modifier.size(28.dp),
        )
        Status.Off -> Icon(
            Icons.Filled.PowerSettingsNew, contentDescription = null,
            tint = color, modifier = Modifier.size(28.dp),
        )
        is Status.Paused -> Box(
            Modifier.size(20.dp).clip(CircleShape).background(color)
        )
    }
}

@Composable
private fun FireNowDialog(s: Snapshot, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val actions = buildList {
        if (s.pc.enableWol) add(stringResource(R.string.fire_action_wol))
        if (s.pc.enableCommand && s.pc.command.isNotBlank()) add(stringResource(R.string.fire_action_cmd))
        if (s.pc.enableDestroyKey) add(stringResource(R.string.fire_action_key))
        if (s.pc.enableShred) add(stringResource(R.string.fire_action_shred, s.pc.folder))
        if (s.pc.enableSshShutdown) add(stringResource(R.string.fire_action_shutdown))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Bolt, contentDescription = null, tint = FiredColor) },
        title = { Text(stringResource(R.string.fire_now_title)) },
        text = {
            Text(
                stringResource(
                    R.string.fire_now_body,
                    Mailer.parseRecipients(s.email.recipients).joinToString(", "),
                    if (actions.isEmpty()) stringResource(R.string.fire_now_no_pc)
                    else actions.joinToString(", "),
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = FiredColor),
            ) { Text(stringResource(R.string.fire_now_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun PauseDialog(now: Long, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val context = LocalContext.current
    var selected by remember { mutableLongStateOf(Store.DAY) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pause_title)) },
        text = {
            Column {
                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    PAUSE_OPTIONS.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(selected == option, onClick = { selected = option })
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected == option, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(formatDuration(context, option))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.pause_until, formatWhen(context, now + selected)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(selected) }) { Text(stringResource(R.string.pause)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun CheckInSection(s: Snapshot, status: Status) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { Engine.checkIn(context) },
            enabled = status != Status.Off,
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RunningColor, contentColor = Color.White),
        ) {
            Icon(Icons.Filled.Favorite, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.check_in), style = MaterialTheme.typography.titleLarge)
        }
        Text(
            if (s.lastCheckIn > 0) stringResource(R.string.last_check_in, formatWhen(context, s.lastCheckIn))
            else stringResource(R.string.never_checked_in),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PermissionsCard(refresh: Int, askNotifications: () -> Unit) {
    val context = LocalContext.current
    val notifications = remember(refresh) { notificationsAllowed(context) }
    val exact = remember(refresh) { Engine.exactAlarmsAllowed(context) }
    val battery = remember(refresh) {
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    }
    if (notifications && exact && battery) return

    val pkg = Uri.parse("package:" + context.packageName)
    Section(stringResource(R.string.permissions_title)) {
        if (!notifications) {
            PermissionRow(stringResource(R.string.perm_notifications), askNotifications)
        }
        if (!exact) {
            PermissionRow(stringResource(R.string.perm_exact)) {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg))
            }
        }
        if (!battery) {
            PermissionRow(stringResource(R.string.perm_battery)) {
                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }
}

@Composable
private fun PermissionRow(text: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onClick) { Text(stringResource(R.string.allow)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimingCard(s: Snapshot) {
    val context = LocalContext.current
    Section(stringResource(R.string.timing_title)) {
        Text(stringResource(R.string.interval_label), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            INTERVAL_OPTIONS.forEach { ms ->
                FilterChip(
                    selected = s.timer.intervalMs == ms,
                    onClick = { Engine.setInterval(context, ms) },
                    label = { Text(formatDuration(context, ms)) },
                )
            }
        }
        Text(stringResource(R.string.reminder_label), style = MaterialTheme.typography.bodyMedium)
        Text(
            stringResource(R.string.reminder_multi_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val selected = s.timer.reminderOffsets.toSet()
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            REMINDER_OPTIONS.forEach { ms ->
                FilterChip(
                    selected = ms in selected,
                    enabled = ms < s.timer.intervalMs,
                    onClick = {
                        val next = if (ms in selected) selected - ms else selected + ms
                        Engine.setReminderOffsets(context, next.toList().sortedDescending())
                    },
                    label = {
                        Text(formatDuration(context, ms))
                    },
                )
            }
        }
    }
}
