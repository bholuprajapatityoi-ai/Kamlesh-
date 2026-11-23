package com.example.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.remote.dto.SecurityDashboardDto
import com.example.domain.model.Phase7To13UiState
import com.example.presentation.components.GoluGlassCard
import com.example.ui.theme.GoluAmber
import com.example.ui.theme.GoluCoralRed
import com.example.ui.theme.GoluEmerald
import com.example.ui.theme.GoluNeonCyan

@Composable
fun SecurityDashboardScreen(
    uiState: Phase7To13UiState,
    dashboard: SecurityDashboardDto?,
    onRefresh: () -> Unit,
    onSetPin: (String) -> Unit,
    onDisablePin: () -> Unit,
    onUpdateRemoteFlags: (confirmSensitive: Boolean?, micAllowed: Boolean?, wakeAllowed: Boolean?) -> Unit,
    onRevokeSession: (String) -> Unit,
    onWipePersonalData: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var newPin by remember { mutableStateOf("") }
    var confirmWipeStep1 by remember { mutableStateOf(false) }
    var confirmWipeStep2 by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { onRefresh() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("security_back_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text("Complete Security System", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "Keystore PIN Lock • Session Audit • Privacy Controls",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoluNeonCyan,
                        )
                    }
                }

                uiState.statusMessage?.let { msg ->
                    Surface(
                        color = GoluEmerald.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoluEmerald),
                    ) { Text(msg, color = GoluEmerald, modifier = Modifier.padding(12.dp)) }
                }
                uiState.errorMessage?.let { err ->
                    Surface(
                        color = GoluCoralRed.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoluCoralRed),
                    ) { Text(err, color = GoluCoralRed, modifier = Modifier.padding(12.dp)) }
                }

                // Voice Auth Policy Notice
                GoluGlassCard(accentBorder = GoluAmber.copy(alpha = 0.6f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = GoluAmber)
                        Text(
                            text = "Security Rule: Voice recognition is never used as sole authentication. App unlock requires your Keystore-encrypted 4–6 digit PIN or account password.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // PIN Lock Vault Card
                GoluGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = GoluNeonCyan)
                        Text(
                            text = if (uiState.pinLockEnabled) "App PIN Lock: ENABLED" else "App PIN Lock: OFF",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) newPin = it },
                            label = { Text("4–6 digit PIN") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.weight(1f).testTag("security_pin_input"),
                        )
                        Button(
                            onClick = {
                                onSetPin(newPin)
                                newPin = ""
                            },
                            enabled = newPin.length in 4..6,
                            modifier = Modifier.testTag("security_set_pin_btn"),
                        ) { Text("Enable") }
                    }
                    if (uiState.pinLockEnabled) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = onDisablePin,
                            modifier = Modifier.fillMaxWidth().testTag("security_disable_pin_btn"),
                        ) { Text("Disable App PIN Lock") }
                    }
                }

                // Mic / Wake-Word / Sensitive Action Controls
                dashboard?.settings?.let { sec ->
                    GoluGlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Security, contentDescription = null, tint = GoluEmerald)
                            Text("Permission & Safety Controls", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Confirm sensitive actions", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = sec.confirmSensitiveActions, onCheckedChange = { onUpdateRemoteFlags(it, null, null) })
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Allow voice microphone", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = sec.microphoneAllowed, onCheckedChange = { onUpdateRemoteFlags(null, it, null) })
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Allow \"Hey Golu\" wake-word", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = sec.wakeWordMonitoringAllowed, onCheckedChange = { onUpdateRemoteFlags(null, null, it) })
                        }
                    }
                }

                // Active Sessions
                GoluGlassCard {
                    Text("Active Sessions (${dashboard?.activeSessions?.size ?: 0})", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    if (dashboard?.activeSessions.isNullOrEmpty()) {
                        Text("Sign in to inspect and revoke active JWT sessions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    } else {
                        dashboard?.activeSessions?.forEach { s ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(if (s.isCurrent) "Current Device Session" else "Session ${s.id.take(8)}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Expires: ${s.expiresAt}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                if (!s.revoked) {
                                    OutlinedButton(onClick = { onRevokeSession(s.id) }) { Text("Revoke") }
                                } else {
                                    Text("Revoked", color = GoluCoralRed, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                // Recent Security Audit Events
                GoluGlassCard {
                    Text("Security Event Audit Log", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    if (dashboard?.recentEvents.isNullOrEmpty()) {
                        Text("No security events recorded yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    } else {
                        dashboard?.recentEvents?.take(8)?.forEach { ev ->
                            Text(
                                text = "• [${ev.severity}] ${ev.eventType}: ${ev.detail}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }

                // Wipe Personal Data
                Button(
                    onClick = { confirmWipeStep1 = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GoluCoralRed),
                    modifier = Modifier.fillMaxWidth().testTag("security_wipe_data_btn"),
                ) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Wipe All Personal Memories & Routines")
                }
            }
        }
    }

    if (confirmWipeStep1) {
        AlertDialog(
            onDismissRequest = { confirmWipeStep1 = false },
            title = { Text("Wipe Personal Data (Step 1/2)?") },
            text = { Text("This deletes your personal memories, tasks, reminders, timetable, calendar events, and automation rules.") },
            confirmButton = {
                Button(onClick = { confirmWipeStep1 = false; confirmWipeStep2 = true }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { confirmWipeStep1 = false }) { Text("Cancel") } },
        )
    }

    if (confirmWipeStep2) {
        AlertDialog(
            onDismissRequest = { confirmWipeStep2 = false },
            title = { Text("Final Confirmation (Step 2/2)") },
            text = { Text("Cannot be undone. Confirm permanent personal data wipe?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = GoluCoralRed),
                    onClick = { confirmWipeStep2 = false; onWipePersonalData() },
                ) { Text("Wipe Permanently") }
            },
            dismissButton = { TextButton(onClick = { confirmWipeStep2 = false }) { Text("Cancel") } },
        )
    }
}
