package com.javimetallab.deadmanshot.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.javimetallab.deadmanshot.R
import com.javimetallab.deadmanshot.core.Engine
import com.javimetallab.deadmanshot.core.Gadgetbridge
import com.javimetallab.deadmanshot.core.Mailer
import com.javimetallab.deadmanshot.core.PulseRule
import com.javimetallab.deadmanshot.core.Snapshot
import com.javimetallab.deadmanshot.core.Store
import com.javimetallab.deadmanshot.core.Timing

private val NO_SIGNAL_OPTIONS = listOf(0L, 30 * Store.MINUTE) + listOf(1L, 2L, 4L, 8L, 12L).map { it * Store.HOUR }
private val CHECK_OPTIONS = listOf(15, 30, 60)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PulseTab(s: Snapshot, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pickExport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) chooseExport(context, uri)
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = PausedColor.copy(alpha = 0.16f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.pulse_requirements_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(stringResource(R.string.pulse_requirements_body))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { openGadgetbridge(context) }) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.pulse_open_gadgetbridge))
                    }
                    OutlinedButton(onClick = { pickExport.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.pulse_choose_file))
                    }
                }
                Text(
                    if (s.pulse.exportUri.isBlank()) stringResource(R.string.pulse_no_file)
                    else stringResource(R.string.pulse_file, s.pulse.exportName),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (s.pulse.exportUri.isBlank()) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Section(stringResource(R.string.pulse_title)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = FiredColor)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.pulse_enable),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                )
                Switch(
                    checked = s.pulse.enabled,
                    enabled = s.pulse.enabled || s.pulse.exportUri.isNotBlank(),
                    onCheckedChange = { Engine.setPulseEnabled(context, it) },
                )
            }
            var value by remember(s.pulse.thresholdBpm) { mutableFloatStateOf(s.pulse.thresholdBpm.toFloat()) }
            Text(
                stringResource(R.string.pulse_threshold, value.toInt()),
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = value,
                onValueChange = { value = it },
                onValueChangeFinished = { Store.setPulseThreshold(value.toInt()) },
                valueRange = 10f..40f,
                steps = 29,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                pluralStringResource(
                    R.plurals.pulse_rule, PulseRule.LOW_READINGS,
                    PulseRule.LOW_READINGS, (Timing.PULSE_GRACE_MS / 60_000L).toInt(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(stringResource(R.string.pulse_no_signal_label), style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NO_SIGNAL_OPTIONS.forEach { ms ->
                    FilterChip(
                        selected = s.pulse.noSignalMs == ms,
                        onClick = { Store.setPulseNoSignal(ms) },
                        label = {
                            Text(
                                if (ms == 0L) stringResource(R.string.pulse_no_signal_off)
                                else formatDuration(context, ms)
                            )
                        },
                    )
                }
            }
            Text(
                stringResource(R.string.pulse_no_signal_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(stringResource(R.string.pulse_interval_label), style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CHECK_OPTIONS.forEach { min ->
                    FilterChip(
                        selected = s.pulse.checkIntervalMin == min,
                        onClick = { Engine.setPulseInterval(context, min) },
                        label = { Text(formatDuration(context, min * Store.MINUTE)) },
                    )
                }
            }
        }

        Section(stringResource(R.string.pulse_status_title)) {
            Text(
                when {
                    s.pulse.lastCheckAt == 0L -> stringResource(R.string.pulse_never_checked)
                    s.pulse.lastBpmAt == 0L -> stringResource(
                        R.string.pulse_no_reading, formatWhen(context, s.pulse.lastCheckAt)
                    )
                    else -> stringResource(
                        R.string.pulse_last, s.pulse.lastBpm,
                        formatWhen(context, s.pulse.lastBpmAt), formatWhen(context, s.pulse.lastCheckAt),
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (s.pulse.lastError.isNotBlank()) {
                Text(
                    s.pulse.lastError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedButton(
                enabled = s.pulse.exportUri.isNotBlank(),
                onClick = { Engine.checkPulseNow(context) },
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.pulse_check_now))
            }
            Text(
                stringResource(R.string.pulse_check_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun chooseExport(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    // Let go of the previous file, if it was a different one.
    val old = Store.snapshot().pulse.exportUri
    if (old.isNotBlank() && old != uri.toString()) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(Uri.parse(old), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    Store.setPulseExport(uri.toString(), Mailer.displayName(context, uri))
    Store.setPulseLastError("")
}

private fun openGadgetbridge(context: Context) {
    val pkg = Gadgetbridge.installed(context)
    val launch = pkg?.let { context.packageManager.getLaunchIntentForPackage(it) }
    if (launch != null) {
        context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } else {
        Store.setPulseLastError(context.getString(R.string.pulse_gadgetbridge_missing))
    }
}
