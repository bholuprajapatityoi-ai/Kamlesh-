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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
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
import androidx.compose.ui.unit.dp
import com.example.data.remote.dto.FamilyDashboardDto
import com.example.domain.model.Phase7To13UiState
import com.example.presentation.components.GoluGlassCard
import com.example.ui.theme.GoluCoralRed
import com.example.ui.theme.GoluElectricBlue
import com.example.ui.theme.GoluEmerald
import com.example.ui.theme.GoluNeonCyan

@Composable
fun FamilyModeScreen(
    uiState: Phase7To13UiState,
    dashboard: FamilyDashboardDto?,
    latestInviteCode: String?,
    onRefresh: () -> Unit,
    onToggleFatherMode: (Boolean) -> Unit,
    onCreateGroup: (String) -> Unit,
    onInviteMember: (groupId: String, email: String, displayLabel: String, fatherMode: Boolean) -> Unit,
    onAcceptInvite: (String) -> Unit,
    onUpdateMemberPrivacy: (memberId: String, fatherMode: Boolean?, shareReminders: Boolean?) -> Unit,
    onAddSharedReminder: (groupId: String, title: String, notes: String) -> Unit,
    onDeleteSharedReminder: (String) -> Unit,
    onFatherTalkToGolu: () -> Unit,
    onFatherQuickCall: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var groupName by remember { mutableStateOf("Sharma Family") }
    var inviteEmail by remember { mutableStateOf("") }
    var inviteLabel by remember { mutableStateOf("Papa") }
    var inviteFatherMode by remember { mutableStateOf(true) }
    var acceptCode by remember { mutableStateOf("") }
    var sharedTitle by remember { mutableStateOf("") }

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
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("family_back_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text("Family Mode & Papa Interface", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "Shared family reminders • Strict private memory isolation",
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

                // Strict Privacy Isolation Guarantee
                GoluGlassCard(accentBorder = GoluNeonCyan.copy(alpha = 0.6f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Lock, contentDescription = "Family Privacy", tint = GoluNeonCyan)
                        Text(
                            text = "Privacy Guarantee: Each family member uses their own account. Group admins CANNOT view your private memories, private reminders, or chat history.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Papa / Father-Friendly Mode Card
                GoluGlassCard(accentBorder = GoluEmerald.copy(alpha = 0.6f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Papa / Elder-Friendly Mode", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "Extra-large buttons, simple Hindi/Hinglish shortcuts & distraction-free layout.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.fatherFriendlyMode,
                            onCheckedChange = onToggleFatherMode,
                            modifier = Modifier.testTag("father_mode_switch"),
                        )
                    }

                    if (uiState.fatherFriendlyMode) {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = onFatherTalkToGolu,
                            modifier = Modifier.fillMaxWidth().height(58.dp).testTag("father_talk_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = GoluElectricBlue),
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("GOLU से बात करें / Talk to GOLU", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onFatherQuickCall,
                            modifier = Modifier.fillMaxWidth().height(58.dp).testTag("father_call_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = GoluEmerald),
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("फ़ोन लगाएं / Quick Call", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                // Create Group / Accept Invitation
                GoluGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.FamilyRestroom, contentDescription = null, tint = GoluElectricBlue)
                        Text("Family Groups & Invitations", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("New Group Name") }, singleLine = true, modifier = Modifier.weight(1f))
                        Button(onClick = { if (groupName.isNotBlank()) onCreateGroup(groupName.trim()) }) { Text("Create") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = acceptCode, onValueChange = { acceptCode = it }, label = { Text("Enter Invite Code") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = {
                            if (acceptCode.isNotBlank()) {
                                onAcceptInvite(acceptCode.trim())
                                acceptCode = ""
                            }
                        }) { Text("Join") }
                    }

                    val primaryGroup = dashboard?.groups?.firstOrNull()
                    if (primaryGroup != null) {
                        Spacer(Modifier.height(10.dp))
                        Text("Invite Member to ${primaryGroup.name}", style = MaterialTheme.typography.labelLarge, color = GoluNeonCyan)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = inviteEmail, onValueChange = { inviteEmail = it }, label = { Text("Member Email") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = inviteLabel, onValueChange = { inviteLabel = it }, label = { Text("Label (Papa)") }, singleLine = true, modifier = Modifier.weight(0.7f))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Enable Father-Friendly layout for invitee", style = MaterialTheme.typography.bodySmall)
                            Switch(checked = inviteFatherMode, onCheckedChange = { inviteFatherMode = it })
                        }
                        Button(
                            onClick = {
                                if (inviteEmail.isNotBlank()) {
                                    onInviteMember(primaryGroup.id, inviteEmail.trim(), inviteLabel.ifBlank { "Member" }, inviteFatherMode)
                                    inviteEmail = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Generate Secure Invite Code") }
                    }

                    latestInviteCode?.let { code ->
                        Spacer(Modifier.height(8.dp))
                        Surface(color = GoluElectricBlue.copy(alpha = 0.16f), shape = RoundedCornerShape(10.dp)) {
                            Text("Invite Code (share privately): $code", color = GoluNeonCyan, modifier = Modifier.padding(10.dp))
                        }
                    }

                    dashboard?.members?.forEach { m ->
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${m.displayLabel} (${m.fullName})", style = MaterialTheme.typography.bodyMedium)
                                Text("Role: ${m.role} • Father UI: ${if (m.fatherFriendlyMode) "ON" else "OFF"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            Switch(
                                checked = m.shareSharedReminders,
                                onCheckedChange = { onUpdateMemberPrivacy(m.id, null, it) },
                            )
                        }
                    }
                }

                // Shared Family Reminders
                dashboard?.groups?.firstOrNull()?.let { grp ->
                    GoluGlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = GoluEmerald)
                            Text("Shared Family Reminders (${dashboard.sharedReminders.size})", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = sharedTitle, onValueChange = { sharedTitle = it }, label = { Text("Medicine / Family Reminder") }, singleLine = true, modifier = Modifier.weight(1f))
                            Button(onClick = {
                                if (sharedTitle.isNotBlank()) {
                                    onAddSharedReminder(grp.id, sharedTitle.trim(), "")
                                    sharedTitle = ""
                                }
                            }) { Text("Share") }
                        }
                        dashboard.sharedReminders.forEach { sr ->
                            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("• ${sr.title}", style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = { onDeleteSharedReminder(sr.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete shared reminder", tint = GoluCoralRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
