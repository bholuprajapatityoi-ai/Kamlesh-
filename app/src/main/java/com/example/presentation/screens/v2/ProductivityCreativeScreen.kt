package com.example.presentation.screens.v2

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.remote.dto.RecordingResponseDto
import com.example.presentation.components.GoluGlassCard
import com.example.presentation.components.StatusPill
import com.example.presentation.viewmodel.GoluV2ViewModel
import com.example.ui.theme.GoluAmber
import com.example.ui.theme.GoluCoralRed
import com.example.ui.theme.GoluElectricBlue
import com.example.ui.theme.GoluEmerald
import com.example.ui.theme.GoluNeonCyan

@Composable
fun ProductivityCreativeScreen(
    v2ViewModel: GoluV2ViewModel,
    initialTab: Int = 0,
    onBack: () -> Unit,
) {
    val uiState by v2ViewModel.uiState.collectAsStateWithLifecycle()
    val plannerProposal by v2ViewModel.plannerProposal.collectAsStateWithLifecycle()
    val codingHistory by v2ViewModel.codingHistory.collectAsStateWithLifecycle()
    val creativeAssets by v2ViewModel.creativeAssets.collectAsStateWithLifecycle()
    val recordings by v2ViewModel.recordings.collectAsStateWithLifecycle()
    val isRecordingAudio by v2ViewModel.isRecordingAudio.collectAsStateWithLifecycle()
    val lastRecordedPath by v2ViewModel.lastRecordedPath.collectAsStateWithLifecycle()

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            1 -> v2ViewModel.loadCodingHistory()
            2 -> v2ViewModel.loadCreativeHistory()
            3 -> v2ViewModel.loadRecordings()
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
                    title = "Productivity & Creative",
                    subtitle = "Life Planner, Coding AI, Creative Studio & Voice Recorder",
                    onBack = onBack,
                )
                Golu2SegmentedTabs(
                    tabs = listOf("Life Planner", "Coding AI", "Creative Studio", "Voice Recorder"),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                    prefixTag = "prod",
                )
                Golu2StatusBanners(uiState = uiState, onDismiss = v2ViewModel::clearFeedback)

                when (selectedTab) {
                    0 -> LifePlannerSection(v2ViewModel = v2ViewModel, proposal = plannerProposal)
                    1 -> CodingAssistantSection(v2ViewModel = v2ViewModel, history = codingHistory)
                    2 -> CreativeStudioSection(v2ViewModel = v2ViewModel, assets = creativeAssets)
                    3 -> VoiceRecorderSection(
                        v2ViewModel = v2ViewModel,
                        recordings = recordings,
                        isRecording = isRecordingAudio,
                        lastPath = lastRecordedPath,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LifePlannerSection(
    v2ViewModel: GoluV2ViewModel,
    proposal: com.example.data.remote.dto.PlannerSuggestResponseDto?,
) {
    var focus by remember { mutableStateOf("Finish college assignment, workout 45m, family dinner") }
    var hours by remember { mutableIntStateOf(6) }

    GoluGlassCard(accentBorder = GoluEmerald.copy(alpha = 0.5f)) {
        Text("Life Planner AI (Approval-Gated)", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "GOLU analyzes your tasks, calendar & timetable and suggests a plan — saved only when you tap Approve.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = focus,
            onValueChange = { focus = it },
            label = { Text("Today's Focus / Priorities") },
            modifier = Modifier.fillMaxWidth().testTag("v2_planner_focus_input"),
        )
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2, 4, 6, 8).forEach { h ->
                FilterChip(selected = hours == h, onClick = { hours = h }, label = { Text("${h}h available") })
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { v2ViewModel.suggestPlan(focus, hours) },
            enabled = focus.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("v2_planner_suggest_btn"),
        ) {
            Text("Generate AI Plan Proposal")
        }
    }

    proposal?.let { prop ->
        Spacer(Modifier.height(10.dp))
        GoluGlassCard(accentBorder = GoluNeonCyan) {
            Text("Proposed Schedule", style = MaterialTheme.typography.titleMedium, color = GoluNeonCyan)
            Text(prop.summary, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            prop.proposals.forEach { item ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("[${item.itemType} • ${item.priority}] ${item.title}", style = MaterialTheme.typography.labelLarge)
                        Text("${item.suggestedSlot} — ${item.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { v2ViewModel.approveCurrentPlan() },
                colors = ButtonDefaults.buttonColors(containerColor = GoluEmerald),
                modifier = Modifier.fillMaxWidth().testTag("v2_planner_approve_btn"),
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Approve & Save to Daily Life")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CodingAssistantSection(
    v2ViewModel: GoluV2ViewModel,
    history: List<com.example.data.remote.dto.CodingAssistResponseDto>,
) {
    var language by remember { mutableStateOf("kotlin") }
    var taskType by remember { mutableStateOf("EXPLAIN") }
    var prompt by remember { mutableStateOf("") }

    GoluGlassCard(accentBorder = GoluNeonCyan.copy(alpha = 0.5f)) {
        Text("Coding Assistant", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("kotlin", "python", "sql", "typescript", "bash").forEach { lang ->
                FilterChip(selected = language == lang, onClick = { language = lang }, label = { Text(lang) })
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("EXPLAIN", "GENERATE", "DEBUG", "REVIEW").forEach { task ->
                FilterChip(selected = taskType == task, onClick = { taskType = task }, label = { Text(task) })
            }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Paste code or describe what you want to build/debug") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().testTag("v2_coding_prompt"),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { v2ViewModel.assistCoding(prompt, language, taskType) },
            enabled = prompt.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("v2_coding_submit_btn"),
        ) {
            Icon(Icons.Filled.Code, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Run Coding Assistant")
        }
    }

    history.take(5).forEach { item ->
        Spacer(Modifier.height(8.dp))
        GoluGlassCard {
            Text("${item.language.uppercase()} • ${item.taskType}", style = MaterialTheme.typography.labelSmall, color = GoluNeonCyan)
            Text(item.explanation, style = MaterialTheme.typography.bodyMedium)
            if (item.codeOutput.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = item.codeOutput,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
            if (item.securityNote.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("Security: ${item.securityNote}", style = MaterialTheme.typography.labelSmall, color = GoluAmber)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreativeStudioSection(
    v2ViewModel: GoluV2ViewModel,
    assets: List<com.example.data.remote.dto.CreativeAssetResponseDto>,
) {
    var assetType by remember { mutableStateOf("IMAGE") }
    var style by remember { mutableStateOf("Futuristic Navy & Cyan") }
    var prompt by remember { mutableStateOf("") }

    GoluGlassCard(accentBorder = GoluElectricBlue.copy(alpha = 0.5f)) {
        Text("Creative Studio", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("IMAGE", "POSTER_IDEA", "DESIGN_CONCEPT").forEach { t ->
                FilterChip(selected = assetType == t, onClick = { assetType = t }, label = { Text(t) })
            }
        }
        OutlinedTextField(
            value = style,
            onValueChange = { style = it },
            label = { Text("Visual Style") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Describe artwork, poster, or concept") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).testTag("v2_creative_prompt"),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { v2ViewModel.generateCreative(assetType, prompt, style) },
            enabled = prompt.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("v2_creative_generate_btn"),
        ) {
            Icon(Icons.Filled.Brush, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Generate ${assetType.replace("_", " ")}")
        }
    }

    assets.forEach { asset ->
        Spacer(Modifier.height(8.dp))
        GoluGlassCard(
            accentBorder = if (asset.status == "FAILED") GoluCoralRed else GoluNeonCyan.copy(alpha = 0.4f)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${asset.assetType} • ${asset.status}", style = MaterialTheme.typography.labelSmall, color = if (asset.status == "FAILED") GoluCoralRed else GoluEmerald)
                    Text(asset.prompt, style = MaterialTheme.typography.titleSmall)
                }
                IconButton(
                    onClick = { v2ViewModel.deleteCreative(asset.id) },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete creative asset", tint = GoluCoralRed)
                }
            }
            if (asset.imageUrl.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = asset.imageUrl,
                    contentDescription = asset.prompt,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
                )
            }
            if (asset.designNotes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(asset.designNotes, style = MaterialTheme.typography.bodySmall)
            }
            if (asset.errorDetail.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(asset.errorDetail, style = MaterialTheme.typography.bodySmall, color = GoluCoralRed)
            }
        }
    }
}

@Composable
private fun VoiceRecorderSection(
    v2ViewModel: GoluV2ViewModel,
    recordings: List<RecordingResponseDto>,
    isRecording: Boolean,
    lastPath: String,
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("Meeting Notes") }
    var transcript by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var editingRec by remember { mutableStateOf<RecordingResponseDto?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editTranscript by remember { mutableStateOf("") }

    val micPermLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) v2ViewModel.startAudioRecording()
        }

    GoluGlassCard(accentBorder = if (isRecording) GoluCoralRed else GoluElectricBlue.copy(alpha = 0.5f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(
                label = if (isRecording) "RECORDING ON DEVICE" else "RECORDER IDLE",
                icon = Icons.Filled.Mic,
                tint = if (isRecording) GoluCoralRed else GoluNeonCyan,
                modifier = Modifier.testTag("v2_recorder_status_pill"),
            )
            if (lastPath.isNotBlank() && !isRecording) {
                OutlinedButton(onClick = { v2ViewModel.playLastRecording() }) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Play Last")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Recording Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = transcript,
            onValueChange = { transcript = it },
            label = { Text("Transcript / Lecture Notes (editable)") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        if (!isRecording) {
            Button(
                onClick = {
                    val ok =
                        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED
                    if (ok) v2ViewModel.startAudioRecording()
                    else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                modifier = Modifier.fillMaxWidth().testTag("v2_recorder_start_btn"),
            ) {
                Icon(Icons.Filled.Mic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start Audio Recording")
            }
        } else {
            Button(
                onClick = { v2ViewModel.stopAndSaveRecording(title, transcript) },
                colors = ButtonDefaults.buttonColors(containerColor = GoluCoralRed),
                modifier = Modifier.fillMaxWidth().testTag("v2_recorder_stop_btn"),
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Stop, Save & Summarize")
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Search Recordings") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = { v2ViewModel.loadRecordings(search.ifBlank { null }) }) {
            Text("Filter")
        }
    }

    recordings.forEach { rec ->
        Spacer(Modifier.height(8.dp))
        GoluGlassCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${rec.title} (${rec.durationSeconds}s)", style = MaterialTheme.typography.titleMedium, color = GoluNeonCyan)
                    Text("Summary: ${rec.summary}", style = MaterialTheme.typography.bodySmall)
                    if (rec.lectureNotes.isNotBlank()) {
                        Text("Notes: ${rec.lectureNotes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row {
                    if (rec.localFileHint.isNotBlank()) {
                        IconButton(
                            onClick = { v2ViewModel.playRecordingPath(rec.localFileHint) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play recording", tint = GoluEmerald)
                        }
                    }
                    IconButton(
                        onClick = {
                            editingRec = rec
                            editTitle = rec.title
                            editTranscript = rec.transcript
                        },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit transcript")
                    }
                    IconButton(
                        onClick = { v2ViewModel.deleteRecording(rec) },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete recording", tint = GoluCoralRed)
                    }
                }
            }
        }
    }

    editingRec?.let { rec ->
        AlertDialog(
            onDismissRequest = { editingRec = null },
            title = { Text("Edit Transcript & Re-summarize") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Title") })
                    OutlinedTextField(value = editTranscript, onValueChange = { editTranscript = it }, label = { Text("Transcript") }, minLines = 3)
                }
            },
            confirmButton = {
                Button(onClick = {
                    v2ViewModel.updateRecordingTranscript(rec.id, editTitle, editTranscript)
                    editingRec = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingRec = null }) { Text("Cancel") } },
        )
    }
}
