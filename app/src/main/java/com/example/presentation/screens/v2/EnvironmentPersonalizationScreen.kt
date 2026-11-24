package com.example.presentation.screens.v2

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.dto.SmartHomeDeviceResponseDto
import com.example.presentation.components.GoluGlassCard
import com.example.presentation.components.StatusPill
import com.example.presentation.viewmodel.GoluV2ViewModel
import com.example.ui.theme.GoluAmber
import com.example.ui.theme.GoluCoralRed
import com.example.ui.theme.GoluElectricBlue
import com.example.ui.theme.GoluEmerald
import com.example.ui.theme.GoluIndigoAccent
import com.example.ui.theme.GoluNeonCyan
import kotlin.math.roundToInt

@Composable
fun EnvironmentPersonalizationScreen(
    v2ViewModel: GoluV2ViewModel,
    initialTab: Int = 0,
    onBack: () -> Unit,
) {
    val uiState by v2ViewModel.uiState.collectAsStateWithLifecycle()
    val smartHomeConfig by v2ViewModel.smartHomeConfig.collectAsStateWithLifecycle()
    val smartHomeDevices by v2ViewModel.smartHomeDevices.collectAsStateWithLifecycle()
    val expenseSummary by v2ViewModel.expenseSummary.collectAsStateWithLifecycle()
    val personality by v2ViewModel.personality.collectAsStateWithLifecycle()
    val installedApps by v2ViewModel.installedApps.collectAsStateWithLifecycle()
    val appShortcuts by v2ViewModel.appShortcuts.collectAsStateWithLifecycle()

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0 -> v2ViewModel.loadSmartHome()
            1 -> v2ViewModel.loadExpenses()
            2 -> v2ViewModel.loadPersonality()
            3 -> v2ViewModel.refreshSmartApps()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 700.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Golu2HeaderBar(
                    title = "Environment & Personalization",
                    subtitle = "Smart Home, Expenses, AI Personality & Smart Apps",
                    onBack = onBack,
                )
                Golu2SegmentedTabs(
                    tabs = listOf("Smart Home", "Expenses", "Personality", "Smart Apps"),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                    prefixTag = "env",
                )
                Golu2StatusBanners(uiState = uiState, onDismiss = v2ViewModel::clearFeedback)

                when (selectedTab) {
                    0 -> SmartHomeSection(v2ViewModel = v2ViewModel, config = smartHomeConfig, devices = smartHomeDevices)
                    1 -> ExpenseTrackerSection(v2ViewModel = v2ViewModel, summary = expenseSummary)
                    2 -> PersonalitySection(v2ViewModel = v2ViewModel, personality = personality)
                    3 -> SmartAppsSection(v2ViewModel = v2ViewModel, apps = installedApps, shortcuts = appShortcuts)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartHomeSection(
    v2ViewModel: GoluV2ViewModel,
    config: com.example.data.remote.dto.SmartHomeConfigResponseDto?,
    devices: List<SmartHomeDeviceResponseDto>,
) {
    var hubUrl by remember(config) { mutableStateOf(config?.hubUrl.orEmpty()) }
    var token by remember { mutableStateOf("") }
    var devName by remember { mutableStateOf("") }
    var devRoom by remember { mutableStateOf("Living Room") }
    var devEntity by remember { mutableStateOf("") }
    var devType by remember { mutableStateOf("LIGHT") }
    var pendingSensitive by remember { mutableStateOf<SmartHomeDeviceResponseDto?>(null) }

    GoluGlassCard(accentBorder = GoluNeonCyan.copy(alpha = 0.5f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Authorized Smart Home Hub", style = MaterialTheme.typography.titleMedium)
            StatusPill(
                label = config?.connectionState ?: "DISCONNECTED",
                icon = Icons.Filled.Home,
                tint = if (config?.isAuthorized == true) GoluEmerald else GoluAmber,
                modifier = Modifier.testTag("v2_smarthome_status_pill"),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = config?.detail ?: "Connect an authorized HTTPS Home Assistant / Smart Home hub. Fake devices are never created.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = hubUrl,
            onValueChange = { hubUrl = it },
            label = { Text("HTTPS Hub URL (e.g. https://home.example.com)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Hub Long-Lived Access Token") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { v2ViewModel.authorizeSmartHome(hubUrl, token) },
            enabled = hubUrl.startsWith("https://") && token.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("v2_smarthome_auth_btn"),
        ) {
            Text("Authorize Smart Home Hub")
        }
    }

    Spacer(Modifier.height(8.dp))
    GoluGlassCard {
        Text("Register Real Hub Entity", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(value = devName, onValueChange = { devName = it }, label = { Text("Device Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(value = devRoom, onValueChange = { devRoom = it }, label = { Text("Room") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(value = devEntity, onValueChange = { devEntity = it }, label = { Text("Entity ID (e.g. light.living_room)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("LIGHT", "SWITCH", "LOCK", "THERMOSTAT", "GARAGE").forEach { t ->
                FilterChip(selected = devType == t, onClick = { devType = t }, label = { Text(t) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                v2ViewModel.addSmartHomeDevice(devName, devRoom, devEntity, devType)
                devName = ""
                devEntity = ""
            },
            enabled = devName.isNotBlank() && devEntity.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Register Hub Device")
        }
    }

    devices.forEach { dev ->
        Spacer(Modifier.height(8.dp))
        GoluGlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${dev.name} (${dev.room})", style = MaterialTheme.typography.titleMedium)
                    Text("${dev.deviceType} • ${dev.externalEntityId} • ${dev.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = dev.isOn,
                        onCheckedChange = {
                            val sensitive = dev.deviceType in setOf("LOCK", "GARAGE", "ALARM", "SECURITY", "THERMOSTAT")
                            if (sensitive) {
                                pendingSensitive = dev
                            } else {
                                v2ViewModel.toggleSmartHomeDevice(dev, confirmedSensitive = false)
                            }
                        },
                        modifier = Modifier.testTag("v2_smarthome_toggle_${dev.id}"),
                    )
                    IconButton(
                        onClick = { v2ViewModel.deleteSmartHomeDevice(dev.id) },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("v2_smarthome_delete_${dev.id}"),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete smart device", tint = GoluCoralRed)
                    }
                }
            }
        }
    }

    pendingSensitive?.let { dev ->
        AlertDialog(
            onDismissRequest = { pendingSensitive = null },
            title = { Text("Confirm Sensitive Smart Home Action") },
            text = { Text("Confirm changing ${dev.deviceType} '${dev.name}' (${dev.externalEntityId})?") },
            confirmButton = {
                Button(onClick = {
                    v2ViewModel.toggleSmartHomeDevice(dev, confirmedSensitive = true)
                    pendingSensitive = null
                }) { Text("Confirm Action") }
            },
            dismissButton = { TextButton(onClick = { pendingSensitive = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpenseTrackerSection(
    v2ViewModel: GoluV2ViewModel,
    summary: com.example.data.remote.dto.ExpenseSummaryResponseDto?,
) {
    val context = LocalContext.current
    var budgetInput by remember(summary?.budgetLimit) { mutableStateOf((summary?.budgetLimit ?: 15000.0).toInt().toString()) }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var amountInput by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    GoluGlassCard(accentBorder = GoluEmerald.copy(alpha = 0.5f)) {
        Text("Monthly Budget & Expense Tracker", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Manual entry & analytics only — GOLU never requests bank credentials.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        summary?.let { sum ->
            val pct = if (sum.budgetLimit > 0) (sum.totalSpent / sum.budgetLimit).toFloat().coerceIn(0f, 1f) else 0f
            Text(
                text = "Spent: ₹${"%.0f".format(sum.totalSpent)} / ₹${"%.0f".format(sum.budgetLimit)} (${sum.monthKey})",
                style = MaterialTheme.typography.titleSmall,
                color = if (sum.remainingBudget < 0) GoluCoralRed else GoluEmerald,
            )
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            if (sum.byCategory.isNotEmpty()) {
                Text("Category Breakdown:", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                val palette = listOf(GoluNeonCyan, GoluEmerald, GoluElectricBlue, GoluAmber, GoluIndigoAccent)
                val entries = sum.byCategory.entries.toList()
                val maxVal = entries.maxOfOrNull { it.value }?.coerceAtLeast(1.0) ?: 1.0
                Canvas(modifier = Modifier.fillMaxWidth().height((entries.size * 26 + 8).dp)) {
                    val barHeight = 14.dp.toPx()
                    val gap = 12.dp.toPx()
                    entries.forEachIndexed { idx, entry ->
                        val top = idx * (barHeight + gap)
                        val ratio = (entry.value / maxVal).toFloat().coerceIn(0.04f, 1f)
                        drawRoundRect(
                            color = palette[idx % palette.size],
                            topLeft = Offset(0f, top),
                            size = Size(size.width * ratio, barHeight),
                            cornerRadius = CornerRadius(8f, 8f),
                        )
                    }
                }
                entries.forEachIndexed { idx, entry ->
                    Text(
                        text = "• ${entry.key}: ₹${"%.0f".format(entry.value)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette[idx % palette.size],
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = {
                    v2ViewModel.exportExpensesCsv { csv ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "GOLU Expenses ${sum.monthKey}")
                            putExtra(Intent.EXTRA_TEXT, csv)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        runCatching {
                            context.startActivity(
                                Intent.createChooser(shareIntent, "Export Expense CSV")
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("v2_expense_export_btn"),
            ) {
                Icon(Icons.Filled.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export Expenses CSV")
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = budgetInput,
                onValueChange = { budgetInput = it },
                label = { Text("Budget (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = {
                budgetInput.toDoubleOrNull()?.let { v2ViewModel.setMonthlyBudget(it) }
            }) { Text("Set") }
        }
    }

    Spacer(Modifier.height(8.dp))
    GoluGlassCard {
        Text("Add Expense Entry", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Expense Title") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("v2_expense_title"))
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Food", "Travel", "Bills", "Shopping", "Health", "General").forEach { cat ->
                FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
            }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = amountInput,
            onValueChange = { amountInput = it },
            label = { Text("Amount (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("v2_expense_amount"),
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val amt = amountInput.toDoubleOrNull() ?: return@Button
                v2ViewModel.addExpense(title, category, amt, notes)
                title = ""
                amountInput = ""
                notes = ""
            },
            enabled = title.isNotBlank() && (amountInput.toDoubleOrNull() ?: 0.0) > 0.0,
            modifier = Modifier.fillMaxWidth().testTag("v2_expense_add_btn"),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Save Expense")
        }
    }

    summary?.items?.forEach { item ->
        Spacer(Modifier.height(6.dp))
        GoluGlassCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${item.title} • ₹${item.amount}", style = MaterialTheme.typography.titleSmall)
                    Text("${item.category} • ${item.notes.ifBlank { item.spentAt.take(10) }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { v2ViewModel.deleteExpense(item.id) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete expense", tint = GoluCoralRed)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonalitySection(
    v2ViewModel: GoluV2ViewModel,
    personality: com.example.data.remote.dto.PersonalityResponseDto?,
) {
    var style by remember(personality) { mutableStateOf(personality?.responseStyle ?: "Hinglish") }
    var humor by remember(personality) { mutableFloatStateOf((personality?.humorLevel ?: 70).coerceIn(0, 100).toFloat()) }
    var length by remember(personality) { mutableStateOf(personality?.responseLength ?: "MEDIUM") }
    var avatar by remember(personality) { mutableStateOf(personality?.avatarStyle ?: "CYBER_BOT") }
    var greeting by remember(personality) { mutableStateOf(personality?.greetingMessage ?: "Hello! Boss, GOLU ready hai!") }
    var notes by remember(personality) { mutableStateOf(personality?.customSystemNotes ?: "") }

    GoluGlassCard(accentBorder = GoluElectricBlue.copy(alpha = 0.5f)) {
        Text("Custom AI Personality (Boss Mode)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("Response Style:", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Hinglish", "Playful", "Formal", "Concise").forEach { s ->
                FilterChip(selected = style == s, onClick = { style = s }, label = { Text(s) })
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Humor Level: ${humor.roundToInt()}%", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = humor,
            onValueChange = { humor = it },
            valueRange = 0f..100f,
            modifier = Modifier.testTag("v2_personality_humor_slider"),
        )
        Text("Response Length:", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("SHORT", "MEDIUM", "DETAILED").forEach { len ->
                FilterChip(selected = length == len, onClick = { length = len }, label = { Text(len) })
            }
        }
        Text("Avatar Style:", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("CYBER_BOT", "CYBER_ORB", "MINIMAL_RING", "RETRO_BOT").forEach { av ->
                FilterChip(selected = avatar == av, onClick = { avatar = av }, label = { Text(av) })
            }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = greeting,
            onValueChange = { greeting = it },
            label = { Text("Activation Greeting") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Custom Tone Instructions") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { v2ViewModel.updatePersonality(style, humor.roundToInt(), length, greeting, avatar, notes) },
            modifier = Modifier.fillMaxWidth().testTag("v2_personality_save_btn"),
        ) {
            Text("Save GOLU Personality")
        }
    }
}

@Composable
private fun SmartAppsSection(
    v2ViewModel: GoluV2ViewModel,
    apps: List<com.example.domain.model.InstalledAppInfo>,
    shortcuts: List<com.example.data.remote.dto.SmartAppPreferenceResponseDto>,
) {
    var search by remember { mutableStateOf("") }
    val launchCounts = remember(shortcuts) { shortcuts.associateBy { it.packageName } }
    val filtered = remember(apps, search) {
        if (search.isBlank()) apps.take(25)
        else apps.filter {
            it.appName.contains(search, ignoreCase = true) ||
                it.packageName.contains(search, ignoreCase = true)
        }.take(25)
    }

    GoluGlassCard(accentBorder = GoluAmber.copy(alpha = 0.5f)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Smart App Intelligence (${apps.size} apps)", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { v2ViewModel.refreshSmartApps() }) { Text("Refresh") }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Filter installed launcher apps") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("v2_smart_apps_search"),
        )
    }

    filtered.forEach { app ->
        val stat = launchCounts[app.packageName]
        Spacer(Modifier.height(6.dp))
        GoluGlassCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.appName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "${app.packageName}${if (stat != null) " • ${stat.launchCount} launches" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (stat != null) {
                        IconButton(
                            onClick = { v2ViewModel.deleteAppShortcut(stat.id) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Reset shortcut stats", tint = GoluCoralRed)
                        }
                    }
                    OutlinedButton(onClick = { v2ViewModel.launchAndTrackApp(app) }) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = "Launch ${app.appName}")
                        Spacer(Modifier.width(4.dp))
                        Text("Open")
                    }
                }
            }
        }
    }
}
