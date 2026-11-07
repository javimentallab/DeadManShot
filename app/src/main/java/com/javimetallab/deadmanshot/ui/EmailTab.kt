package com.javimetallab.deadmanshot.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import com.javimetallab.deadmanshot.R
import com.javimetallab.deadmanshot.core.Mailer
import com.javimetallab.deadmanshot.core.Snapshot
import com.javimetallab.deadmanshot.core.Store
import com.javimetallab.deadmanshot.mail.Security
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SmtpPreset(
    val labelRes: Int,
    val host: String,
    val port: Int,
    val security: Security,
    val noteRes: Int,
)

private val PRESETS = listOf(
    SmtpPreset(R.string.preset_gmail, "smtp.gmail.com", 465, Security.SSL, R.string.preset_gmail_note),
    SmtpPreset(R.string.preset_yahoo, "smtp.mail.yahoo.com", 465, Security.SSL, R.string.preset_yahoo_note),
    SmtpPreset(R.string.preset_icloud, "smtp.mail.me.com", 587, Security.STARTTLS, R.string.preset_icloud_note),
    SmtpPreset(R.string.preset_proton, "smtp.protonmail.ch", 587, Security.STARTTLS, R.string.preset_proton_note),
)

@Composable
fun EmailTab(s: Snapshot, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        EmailCard(s)
        AccountCard(s)
    }
}

@Composable
private fun EmailCard(s: Snapshot) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uris.forEach {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        Store.setAttachments(
            (Store.snapshot().email.attachments + uris.map { it.toString() }).distinct()
        )
    }

    Section(stringResource(R.string.email_title)) {
        SavedField(
            initial = s.email.recipients,
            label = stringResource(R.string.recipients),
            onSave = Store::setRecipients,
            keyboardType = KeyboardType.Email,
            supporting = stringResource(R.string.recipients_hint),
        )
        SavedField(s.email.subject, stringResource(R.string.subject), Store::setSubject)
        SavedField(
            initial = s.email.body,
            label = stringResource(R.string.body),
            onSave = Store::setBody,
            singleLine = false,
            minLines = 6,
        )

        Text(stringResource(R.string.attachments), style = MaterialTheme.typography.titleSmall)
        if (s.email.attachments.isEmpty()) {
            Text(
                stringResource(R.string.no_attachments),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        s.email.attachments.forEach { raw ->
            val name = remember(raw) { Mailer.displayName(context, Uri.parse(raw)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AttachFile, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(name, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    runCatching {
                        context.contentResolver.releasePersistableUriPermission(
                            Uri.parse(raw), Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    Store.setAttachments(Store.snapshot().email.attachments - raw)
                }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove))
                }
            }
        }
        OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }) {
            Icon(Icons.Filled.AttachFile, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.add_attachment))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountCard(s: Snapshot) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sending by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var port by rememberSaveable(s.smtp.port) { mutableStateOf(s.smtp.port.toString()) }
    var showPresets by rememberSaveable { mutableStateOf(false) }

    Section(stringResource(R.string.account_title)) {
        Text(
            stringResource(R.string.account_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = { showPresets = true }) {
            Icon(Icons.Filled.AutoFixHigh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.smtp_fill_preset))
        }
        SavedField(
            s.smtp.host, stringResource(R.string.smtp_host),
            Store::setHost, keyboardType = KeyboardType.Uri,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Security.SSL to 465, Security.STARTTLS to 587).forEach { (sec, defaultPort) ->
                FilterChip(
                    selected = s.smtp.security == sec,
                    onClick = {
                        Store.setSecurity(sec)
                        port = defaultPort.toString()
                        Store.setPort(defaultPort)
                    },
                    label = {
                        Text(
                            if (sec == Security.SSL) stringResource(R.string.security_ssl)
                            else stringResource(R.string.security_starttls)
                        )
                    },
                )
            }
        }
        OutlinedTextField(
            value = port,
            onValueChange = { v ->
                port = v.filter(Char::isDigit).take(5)
                port.toIntOrNull()?.let(Store::setPort)
            },
            label = { Text(stringResource(R.string.smtp_port)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        SavedField(
            s.smtp.user, stringResource(R.string.smtp_user),
            Store::setUser, keyboardType = KeyboardType.Email,
        )
        val note = Mailer.accountNote(s.smtp.host)
        var typedPassword by remember { mutableStateOf(Store.password() ?: "") }
        SavedField(
            initial = remember { Store.password() ?: "" },
            label = stringResource(R.string.smtp_password),
            onSave = {
                typedPassword = it
                Store.setPassword(it)
            },
            isPassword = true,
        )
        when (note) {
            is Mailer.AccountNote.AppPassword -> {
                Text(stringResource(R.string.app_password_needed), style = MaterialTheme.typography.bodySmall)
                if (typedPassword.isNotEmpty() && !Mailer.looksLikeAppPassword(typedPassword)) {
                    Text(
                        stringResource(R.string.app_password_suspicious),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OpenPageButton(note.page, stringResource(R.string.app_password_open))
            }
            is Mailer.AccountNote.ProtonToken -> {
                Text(stringResource(R.string.proton_token_needed), style = MaterialTheme.typography.bodySmall)
                OpenPageButton(note.page, stringResource(R.string.proton_token_open))
            }
            Mailer.AccountNote.MicrosoftOAuth -> Text(
                stringResource(R.string.microsoft_unsupported),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            null -> Unit
        }
        SavedField(
            initial = s.smtp.from,
            label = stringResource(R.string.smtp_from),
            onSave = Store::setFrom,
            keyboardType = KeyboardType.Email,
            supporting = stringResource(R.string.smtp_from_hint),
        )

        Button(
            enabled = !sending,
            onClick = {
                sending = true
                result = null
                scope.launch {
                    val r = withContext(Dispatchers.IO) {
                        runCatching { Mailer.send(context, Store.snapshot(), test = true) }
                    }
                    sending = false
                    val from = Store.snapshot().smtp.let { it.from.ifBlank { it.user } }
                    result = r.fold(
                        onSuccess = { skipped ->
                            true to (context.getString(R.string.test_ok, from) +
                                if (skipped.isEmpty()) "" else "\n" +
                                    context.getString(R.string.test_skipped, skipped.joinToString(", ")))
                        },
                        onFailure = { e ->
                            false to context.getString(
                                R.string.test_failed, Mailer.explain(context, e.message ?: e.javaClass.simpleName)
                            )
                        },
                    )
                }
            },
        ) {
            if (sending) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.send_test))
        }
        result?.let { (ok, text) ->
            Text(text, color = if (ok) RunningColor else MaterialTheme.colorScheme.error)
        }
    }

    if (showPresets) {
        PresetDialog(
            onDismiss = { showPresets = false },
            onPick = { preset ->
                Store.applySmtpPreset(preset.host, preset.port, preset.security)
                port = preset.port.toString()
                showPresets = false
            },
        )
    }
}

@Composable
private fun PresetDialog(onDismiss: () -> Unit, onPick: (SmtpPreset) -> Unit) {
    var selected by remember { mutableStateOf(PRESETS.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.smtp_preset_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PRESETS.forEach { preset ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected == preset, onClick = { selected = preset })
                            .padding(vertical = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selected == preset, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(preset.labelRes),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(
                            stringResource(preset.noteRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 32.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(selected) }) { Text(stringResource(R.string.smtp_preset_apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun OpenPageButton(url: String, label: String) {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }) {
        Icon(Icons.Filled.OpenInNew, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
