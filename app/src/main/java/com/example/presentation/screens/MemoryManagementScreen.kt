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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
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
import com.example.data.remote.dto.MemoryAuditEventDto
import com.example.data.remote.dto.MemoryRecordDto
import com.example.domain.model.Phase7To13UiState
import com.example.presentation.components.GoluGlassCard
import com.example.ui.theme.GoluCoralRed
import com.example.ui.theme.GoluElectricBlue
import com.example.ui.theme.GoluEmerald
import com.example.ui.theme.GoluNeonCyan

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemoryManagementScreen(
    uiState: Phase7To13UiState,
    memories: List<MemoryRecordDto>,
    categories: List<String>,
    auditEvents: List<MemoryAuditEventDto>,
    onRefresh: (String?) -> Unit,
    onCreateMemory: (category: String, content: String, familyShared: Boolean) -> Unit,
    onUpdateMemory: (id: String, category: String, content: String, familyShared: Boolean) -> Unit,
    onDeleteMemory: (String) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var newCategory by remember { mutableStateOf(categories.firstOrNull() ?: "Preferences") }
    var newContent by remember { mutableStateOf("") }
    var familyShared by remember { mutableStateOf(false) }

    var editingMemory by remember { mutableStateOf<MemoryRecordDto?>(null) }
    var editContent by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("Preferences") }
    var editFamilyShared by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onRefresh(null)
    }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("memory_back_button"),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Column {
                            Text(
                                text = "Smart Personal Memory",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = "\"GOLU, ye yaad rakhna...\" • Explicit confirmation only",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoluNeonCyan,
                            )
                        }
                    }
                    if (memories.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showClearDialog = true },
                            border = BorderStroke(1.dp, GoluCoralRed.copy(alpha = 0.7f)),
                            modifier = Modifier.testTag("memory_clear_all_button"),
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = GoluCoralRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear", color = GoluCoralRed)
                        }
                    }
                }

                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                uiState.errorMessage?.let { err ->
                    Surface(
                        color = GoluCoralRed.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoluCoralRed),
                    ) {
                        Text(err, color = GoluCoralRed, modifier = Modifier.padding(12.dp))
                    }
                }
                uiState.statusMessage?.let { status ->
                    Surface(
                        color = GoluEmerald.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoluEmerald),
                    ) {
                        Text(status, color = GoluEmerald, modifier = Modifier.padding(12.dp))
                    }
                }

                // Privacy Rule Banner
                GoluGlassCard(accentBorder = GoluNeonCyan.copy(alpha = 0.5f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = "Memory Privacy", tint = GoluNeonCyan)
                        Text(
                            text = "GOLU never silently saves chat messages. Passwords, OTPs, and API tokens are automatically blocked from memory.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Add New Explicit Memory Card
                GoluGlassCard {
                    Text(
                        text = "Add Confirmed Memory",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = newCategory == cat,
                                onClick = { newCategory = cat },
                                label = { Text(cat) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text("What should GOLU remember, Boss?") },
                        placeholder = { Text("e.g. Meri favourite music app Spotify hai") },
                        modifier = Modifier.fillMaxWidth().testTag("memory_content_input"),
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Allow opt-in Family sharing",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Switch(checked = familyShared, onCheckedChange = { familyShared = it })
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newContent.isNotBlank()) {
                                onCreateMemory(newCategory, newContent.trim(), familyShared)
                                newContent = ""
                            }
                        },
                        enabled = newContent.trim().length >= 2,
                        modifier = Modifier.fillMaxWidth().testTag("memory_save_button"),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Confirm & Save Memory")
                    }
                }

                // Filter Category Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = {
                            selectedCategory = null
                            onRefresh(null)
                        },
                        label = { Text("All (${memories.size})") },
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = cat
                                onRefresh(cat)
                            },
                            label = { Text(cat) },
                        )
                    }
                }
            }
        }

        if (memories.isEmpty()) {
            item {
                GoluGlassCard(modifier = Modifier.widthIn(max = 680.dp).padding(top = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Psychology, contentDescription = null, tint = GoluElectricBlue)
                        Text(
                            text = "No saved memories yet, Boss. Say \"GOLU, ye yaad rakhna...\" or add one above.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(memories, key = { it.id }) { mem ->
                GoluGlassCard(modifier = Modifier.widthIn(max = 680.dp).padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mem.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = GoluNeonCyan,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = mem.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (mem.familyShared) "Private + Opt-in Shared" else "Strictly Private",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        Row {
                            IconButton(
                                onClick = {
                                    editingMemory = mem
                                    editContent = mem.content
                                    editCategory = mem.category
                                    editFamilyShared = mem.familyShared
                                },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("memory_edit_${mem.id}"),
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit memory")
                            }
                            IconButton(
                                onClick = { onDeleteMemory(mem.id) },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("memory_delete_${mem.id}"),
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete memory", tint = GoluCoralRed)
                            }
                        }
                    }
                }
            }
        }

        if (auditEvents.isNotEmpty()) {
            item {
                GoluGlassCard(modifier = Modifier.widthIn(max = 680.dp).padding(top = 12.dp)) {
                    Text(
                        text = "Recent Memory Audit Events",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    auditEvents.take(5).forEach { ev ->
                        Text(
                            text = "• [${ev.action}] ${ev.category}: ${ev.detail}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }

    editingMemory?.let { mem ->
        AlertDialog(
            onDismissRequest = { editingMemory = null },
            title = { Text("Edit Memory") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("Category") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("Content") },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Family shared")
                        Switch(checked = editFamilyShared, onCheckedChange = { editFamilyShared = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editContent.isNotBlank()) {
                        onUpdateMemory(mem.id, editCategory, editContent.trim(), editFamilyShared)
                        editingMemory = null
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingMemory = null }) { Text("Cancel") } },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Memories?") },
            text = { Text("This permanently deletes all your saved personal memories from GOLU.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = GoluCoralRed),
                    onClick = {
                        showClearDialog = false
                        onClearAll()
                    },
                ) { Text("Delete All") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } },
        )
    }
}
