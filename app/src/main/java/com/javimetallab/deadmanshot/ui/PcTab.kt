package com.javimetallab.deadmanshot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.javimetallab.deadmanshot.R
import com.javimetallab.deadmanshot.core.Snapshot
import com.javimetallab.deadmanshot.core.Store
import com.javimetallab.deadmanshot.pc.PcActions
import com.javimetallab.deadmanshot.pc.PcOs
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import com.javimetallab.deadmanshot.pc.Wol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PcTab(s: Snapshot, scope: CoroutineScope, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = FiredColor.copy(alpha = 0.12f))) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = FiredColor)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        stringResource(R.string.pc_warning_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.pc_warning_body),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Section(stringResource(R.string.pc_os_title)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PcOs.entries.forEach { os ->
                    FilterChip(
                        selected = s.pc.os == os,
                        onClick = { Store.setPcOs(os) },
                        label = {
                            Text(stringResource(if (os == PcOs.WINDOWS) R.string.pc_os_windows else R.string.pc_os_linux))
                        },
                    )
                }
            }
        }

        val pathHint = stringResource(
            if (s.pc.os == PcOs.WINDOWS) R.string.pc_path_hint else R.string.pc_path_hint_linux
        )

        Section(stringResource(R.string.pc_wol_title)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.NetworkPing, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.pc_wol_enable),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Switch(checked = s.pc.enableWol, onCheckedChange = { Store.setPcWolEnabled(it) })
            }
            SavedField(s.pc.mac, stringResource(R.string.pc_mac), Store::setPcMac)
            SavedField(
                s.pc.broadcast, stringResource(R.string.pc_broadcast),
                Store::setPcBroadcast,
                supporting = stringResource(R.string.pc_broadcast_hint),
            )
            NumberField(
                initial = s.pc.wolWaitSeconds.toString(),
                label = stringResource(R.string.pc_wol_wait),
                onSave = { it.toIntOrNull()?.let(Store::setPcWolWait) },
            )
            OutlinedButton(
                enabled = Wol.isMacValid(s.pc.mac),
                onClick = {
                    scope.launch {
                        busy = true
                        val r = withContext(Dispatchers.IO) {
                            runCatching { Wol.send(s.pc.mac, s.pc.broadcast.ifBlank { "255.255.255.255" }) }
                        }
                        busy = false
                        result = r.fold(
                            onSuccess = { true to context.getString(R.string.pc_wol_sent) },
                            onFailure = { false to context.getString(R.string.pc_wol_failed, it.message ?: "") },
                        )
                    }
                },
            ) {
                Icon(Icons.Filled.NetworkPing, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.pc_wol_test))
            }
        }

        Section(stringResource(R.string.pc_cmd_title)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Terminal, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.pc_cmd_enable),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Switch(checked = s.pc.enableCommand, onCheckedChange = { Store.setPcCommandEnabled(it) })
            }
            Text(
                stringResource(
                    if (s.pc.os == PcOs.WINDOWS) R.string.pc_cmd_body_windows else R.string.pc_cmd_body_linux
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SavedField(
                s.pc.command, stringResource(R.string.pc_cmd_field),
                Store::setPcCommand,
                singleLine = false,
                minLines = 3,
                keyboardType = KeyboardType.Ascii,
            )
        }

        Section(stringResource(R.string.pc_ssh_title)) {
            SavedField(s.pc.sshHost, stringResource(R.string.pc_ssh_host), Store::setPcSshHost, keyboardType = KeyboardType.Uri)
            NumberField(
                initial = s.pc.sshPort.toString(),
                label = stringResource(R.string.pc_ssh_port),
                onSave = { it.toIntOrNull()?.let(Store::setPcSshPort) },
            )
            SavedField(s.pc.sshUser, stringResource(R.string.pc_ssh_user), Store::setPcSshUser)
            SshKeyBlock(s)
            SavedField(
                initial = remember { Store.sshPassword() ?: "" },
                label = stringResource(R.string.pc_ssh_password),
                onSave = Store::setPcSshPassword,
                isPassword = true,
            )
            Button(
                enabled = !busy && s.pc.sshHost.isNotBlank() && s.pc.sshUser.isNotBlank() && (s.pc.hasSshPassword || s.pc.sshPublicKey.isNotBlank()),
                onClick = {
                    scope.launch {
                        busy = true
                        val r = withContext(Dispatchers.IO) {
                            runCatching {
                                PcActions.testSsh(
                                    s.pc.sshHost, s.pc.sshPort, s.pc.sshUser,
                                    Store.sshPassword().orEmpty(),
                                )
                            }
                        }
                        busy = false
                        result = r.fold(
                            onSuccess = { out ->
                                out.ok to (if (out.ok)
                                    context.getString(R.string.pc_ssh_ok, out.stdout.trim())
                                else context.getString(R.string.pc_ssh_failed, out.stderr.trim()))
                            },
                            onFailure = { false to context.getString(R.string.pc_ssh_failed, it.message ?: "") },
                        )
                    }
                },
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.pc_ssh_test))
            }
            result?.let { (ok, text) ->
                Text(text, color = if (ok) RunningColor else MaterialTheme.colorScheme.error)
            }
        }

        Section(stringResource(R.string.pc_key_title)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.pc_key_enable),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Switch(checked = s.pc.enableDestroyKey, onCheckedChange = { Store.setPcKeyEnabled(it) })
            }
            Text(
                stringResource(R.string.pc_key_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SavedField(
                s.pc.keyPath, stringResource(R.string.pc_key_path),
                Store::setPcKeyPath,
                supporting = pathHint,
            )
        }

        Section(stringResource(R.string.pc_shred_title)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.pc_shred_enable),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Switch(checked = s.pc.enableShred, onCheckedChange = { Store.setPcShredEnabled(it) })
            }
            Text(
                stringResource(R.string.pc_shred_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SavedField(
                s.pc.folder, stringResource(R.string.pc_folder),
                Store::setPcFolder,
                supporting = pathHint,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = s.pc.includeSubdirs,
                    onCheckedChange = { Store.setPcSubdirs(it) },
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.pc_subdirs))
            }
        }

        Section(stringResource(R.string.pc_shutdown_title)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PowerSettingsNew, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.pc_shutdown_enable),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Switch(checked = s.pc.enableSshShutdown, onCheckedChange = { Store.setPcShutdownEnabled(it) })
            }
            Text(
                stringResource(
                    if (s.pc.os == PcOs.WINDOWS) R.string.pc_shutdown_body else R.string.pc_shutdown_body_linux
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NumberField(initial: String, label: String, onSave: (String) -> Unit) {
    var text by rememberSaveable(initial) { mutableStateOf(initial) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.filter(Char::isDigit).take(6)
            onSave(text)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The app's own SSH key: create it, copy its public half for the PC, or delete it. */
@Composable
private fun SshKeyBlock(s: Snapshot) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    Text(
        stringResource(R.string.pc_ssh_key_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (s.pc.sshPublicKey.isBlank()) {
        OutlinedButton(
            enabled = !working,
            onClick = {
                working = true
                scope.launch {
                    val key = withContext(Dispatchers.Default) {
                        runCatching { com.javimetallab.deadmanshot.pc.SshRunner.generateKey("deadmanshot") }.getOrNull()
                    }
                    key?.let { (prv, pub) -> Store.setSshKey(prv, pub) }
                    working = false
                }
            },
        ) {
            if (working) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(R.string.pc_ssh_key_generate))
        }
    } else {
        Text(
            s.pc.sshPublicKey.take(48) + "…",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        )
        Text(
            stringResource(
                if (s.pc.os == PcOs.WINDOWS) R.string.pc_ssh_key_where_windows else R.string.pc_ssh_key_where_linux
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                clipboard.setText(androidx.compose.ui.text.AnnotatedString(s.pc.sshPublicKey))
                android.widget.Toast.makeText(
                    context, context.getString(R.string.pc_ssh_key_copied), android.widget.Toast.LENGTH_SHORT
                ).show()
            }) { Text(stringResource(R.string.pc_ssh_key_copy)) }
            TextButton(onClick = { Store.deleteSshKey() }) { Text(stringResource(R.string.pc_ssh_key_delete)) }
        }
    }
}
