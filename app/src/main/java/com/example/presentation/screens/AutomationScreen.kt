package com.example.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.example.data.remote.dto.AutomationDto
import com.example.data.remote.dto.AutomationExecutionLogDto
import com.example.domain.model.Phase7To13UiState
import com.example.presentation.components.GoluGlassCard
import com.example.ui.theme.GoluCoralRed
import com.example.ui.theme.GoluElectricBlue
import com.example.ui.theme.GoluEmerald
import com.example.ui.theme.GoluNeonCyan

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutomationScreen(
    uiState: Phase7To13UiState,
    rules: List<AutomationDto>,
    history: List<AutomationExecutionLogDto>,
    onRefresh: () -> Unit,
    onCreateRule: (
        name: String,
        triggerType: String,
        triggerValue: String,
        conditionType: String?,
        conditionValue: String?,
        actionType: String,
        actionPayload: String,
        confirmedSensitive: Boolean,
    ) -> Unit,
    onUpdateRule: (
        ruleId: String,
        name: String,
        triggerType: String,
        triggerValue: String,
        actionType: String,
        actionPayload: String,
    ) -> Unit,
    onToggleEnabled: (AutomationDto) -> Unit,
    onRunNow: (AutomationDto, Boolean) -> Unit,
    onDeleteRule: (String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var name by remember { mutableStateOf("") }
    var triggerType by remember { mutableStateOf("TIME") }
    var triggerValue by remember { mutableStateOf("07:00") }
    var conditionType by remember { mutableStateOf("DEVICE_STATE") }
    var conditionValue by remember { mutableStateOf("BATTERY_GT_15") }
    var actionType by remember { mutableStateOf("SPEAK_MESSAGE") }
    var actionPayload by remember { mutableStateOf("Good morning Boss! Aaj ka plan shuru karte hain.") }
    var confirmedSensitive by remember { mutableStateOf(false) }

    var editingRule by remember { mutableStateOf<AutomationDto?>(null) }
    var editName by remember { mutableStateOf("") }
    var editTriggerValue by remember { mutableStateOf("") }
    var editActionPayload by remember { mutableStateOf("") }

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
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("automation_back_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text("Smart Automation Builder", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "Safe Trigger → Condition → Action rules • No arbitrary code execution",
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

                GoluGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.AutoFixHigh, contentDescription = null, tint = GoluElectricBlue)
                        Text("Create Automation Rule", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Rule Name (e.g. Morning Briefing)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("automation_name_input"),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Trigger Type", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("TIME", "DAY", "REMINDER", "COMMAND", "ANDROID_EVENT").forEach { t ->
                            FilterChip(selected = triggerType == t, onClick = { triggerType = t }, label = { Text(t) })
                        }
                    }
                    OutlinedTextField(
                        value = triggerValue,
                        onValueChange = { triggerValue = it },
                        label = { Text("Trigger Value (e.g. 07:00 or Monday)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = conditionValue,
                        onValueChange = { conditionValue = it },
                        label = { Text("Condition Value (e.g. BATTERY_GT_15, optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Safe Action Type", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("SPEAK_MESSAGE", "SHOW_NOTIFICATION", "OPEN_APP", "CREATE_REMINDER", "READ_TIMETABLE", "ANDROID_ACTION").forEach { a ->
                            FilterChip(selected = actionType == a, onClick = { actionType = a }, label = { Text(a) })
                        }
                    }
                    OutlinedTextField(
                        value = actionPayload,
                        onValueChange = { actionPayload = it },
                        label = { Text("Action Payload") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (actionType in listOf("OPEN_APP", "ANDROID_ACTION")) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = confirmedSensitive, onCheckedChange = { confirmedSensitive = it })
                            Text("Confirm sensitive device action trigger", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = {
                            if (name.trim().length >= 2 && triggerValue.isNotBlank()) {
                                onCreateRule(
                                    name.trim(),
                                    triggerType,
                                    triggerValue.trim(),
                                    if (conditionValue.isNotBlank()) conditionType else null,
                                    conditionValue.trim().takeIf { it.isNotBlank() },
                                    actionType,
                                    actionPayload.trim(),
                                    confirmedSensitive,
                                )
                                name = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("automation_create_btn"),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Save Automation Rule")
                    }
                }
            }
        }

        items(rules, key = { it.id }) { rule ->
            GoluGlassCard(modifier = Modifier.widthIn(max = 680.dp).padding(top = 8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(rule.name, style = MaterialTheme.typography.titleMedium)
                        Text("${rule.triggerType}: ${rule.triggerValue}", style = MaterialTheme.typography.labelSmall, color = GoluNeonCyan)
                        rule.actions.firstOrNull()?.let { act ->
                            Text("Action: ${act.actionType} (${act.actionPayload})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(checked = rule.isEnabled, onCheckedChange = { onToggleEnabled(rule) })
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onRunNow(rule, true) }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Test Run")
                    }
                    TextButton(onClick = {
                        editingRule = rule
                        editName = rule.name
                        editTriggerValue = rule.triggerValue
                        editActionPayload = rule.actions.firstOrNull()?.actionPayload.orEmpty()
                    }) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit")
                    }
                    TextButton(onClick = { onDeleteRule(rule.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = GoluCoralRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", color = GoluCoralRed)
                    }
                }
            }
        }

        if (history.isNotEmpty()) {
            item {
                GoluGlassCard(modifier = Modifier.widthIn(max = 680.dp).padding(top = 10.dp)) {
                    Text("Execution History", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    history.take(8).forEach { log ->
                        Text("• [${log.status}] ${log.summary}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    editingRule?.let { rule ->
        AlertDialog(
            onDismissRequest = { editingRule = null },
            title = { Text("Edit Automation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name") })
                    OutlinedTextField(value = editTriggerValue, onValueChange = { editTriggerValue = it }, label = { Text("Trigger Value") })
                    OutlinedTextField(value = editActionPayload, onValueChange = { editActionPayload = it }, label = { Text("Action Payload") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val actType = rule.actions.firstOrNull()?.actionType ?: "SPEAK_MESSAGE"
                    onUpdateRule(rule.id, editName.trim(), rule.triggerType, editTriggerValue.trim(), actType, editActionPayload.trim())
                    editingRule = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingRule = null }) { Text("Cancel") } },
        )
    }
}
