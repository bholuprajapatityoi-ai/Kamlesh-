package com.example.presentation.screens.v2

import android.Manifest
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.golu2.LocationAndAppHelper
import com.example.domain.model.BackendDiagnostics
import com.example.domain.model.ConnectionPhase
import com.example.presentation.components.GoluGlassCard
import com.example.presentation.components.StatusPill
import com.example.presentation.viewmodel.GoluV2ViewModel
import com.example.ui.theme.GoluAmber
import com.example.ui.theme.GoluCoralRed
import com.example.ui.theme.GoluElectricBlue
import com.example.ui.theme.GoluEmerald
import com.example.ui.theme.GoluNeonCyan

@Composable
fun MobilitySystemScreen(
    v2ViewModel: GoluV2ViewModel,
    diagnostics: BackendDiagnostics,
    initialTab: Int = 0,
    onRetryConnection: () -> Unit,
    onBack: () -> Unit,
) {
    val uiState by v2ViewModel.uiState.collectAsStateWithLifecycle()
    val studySessions by v2ViewModel.studySessions.collectAsStateWithLifecycle()
    val travelPlans by v2ViewModel.travelPlans.collectAsStateWithLifecycle()
    val multiDeviceOverview by v2ViewModel.multiDeviceOverview.collectAsStateWithLifecycle()
    val diagnosticReport by v2ViewModel.diagnosticReport.collectAsStateWithLifecycle()

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0 -> v2ViewModel.loadStudySessions()
            1 -> v2ViewModel.loadTravelPlans()
            2 -> v2ViewModel.loadMultiDeviceOverview()
            3 -> v2ViewModel.runSelfDiagnostics()
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
                    title = "Learning, Mobility & System",
                    subtitle = "Study Tutor, Travel AI, Multi-Device Sync & Diagnostics",
                    onBack = onBack,
                )
                Golu2SegmentedTabs(
                    tabs = listOf("Study Tutor", "Travel AI", "Multi-Device", "Diagnostics"),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                    prefixTag = "mob",
                )
                Golu2StatusBanners(uiState = uiState, onDismiss = v2ViewModel::clearFeedback)

                when (selectedTab) {
                    0 -> StudyTutorSection(v2ViewModel = v2ViewModel, sessions = studySessions)
                    1 -> TravelIntelligenceSection(v2ViewModel = v2ViewModel, plans = travelPlans)
                    2 -> MultiDeviceSyncSection(v2ViewModel = v2ViewModel, overview = multiDeviceOverview)
                    3 -> SelfDiagnosticsSection(
                        v2ViewModel = v2ViewModel,
                        diagnostics = diagnostics,
                        report = diagnosticReport,
                        onRetryConnection = onRetryConnection,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudyTutorSection(
    v2ViewModel: GoluV2ViewModel,
    sessions: List<com.example.data.remote.dto.StudySessionResponseDto>,
) {
    var subject by remember { mutableStateOf("Computer Science") }
    var topic by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("Hinglish") }
    var difficulty by remember { mutableStateOf("Intermediate") }

    GoluGlassCard(accentBorder = GoluNeonCyan.copy(alpha = 0.5f)) {
        Text("Study Tutor Mode", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("Topic (e.g. Recursion, Photosynthesis)") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("v2_study_topic"))
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("English", "Hindi", "Hinglish").forEach { lang ->
                FilterChip(selected = language == lang, onClick = { language = lang }, label = { Text(lang) })
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Beginner", "Intermediate", "Advanced").forEach { diff ->
                FilterChip(selected = difficulty == diff, onClick = { difficulty = diff }, label = { Text(diff) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { v2ViewModel.explainStudyTopic(subject, topic, language, difficulty) },
            enabled = subject.isNotBlank() && topic.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("v2_study_ask_btn"),
        ) {
            Icon(Icons.Filled.School, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Teach Me Step-by-Step")
        }
    }

    sessions.forEach { sess ->
        Spacer(Modifier.height(8.dp))
        GoluGlassCard {
            Text("${sess.subject}: ${sess.topic} (${sess.language} • ${sess.difficulty})", style = MaterialTheme.typography.titleMedium, color = GoluNeonCyan)
            Spacer(Modifier.height(4.dp))
            Text(sess.explanation, style = MaterialTheme.typography.bodySmall)
            if (sess.practiceQuestions.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("Practice Questions:", style = MaterialTheme.typography.labelLarge, color = GoluEmerald)
                sess.practiceQuestions.forEach { q ->
                    Text("• $q", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Mastery: ${sess.masteryPct}%", style = MaterialTheme.typography.labelMedium)
                OutlinedButton(onClick = { v2ViewModel.bumpStudyProgress(sess) }) {
                    Text("+20% Progress")
                }
            }
            LinearProgressIndicator(progress = { sess.masteryPct / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
    }
}

@Composable
private fun TravelIntelligenceSection(
    v2ViewModel: GoluV2ViewModel,
    plans: List<com.example.data.remote.dto.TravelPlanResponseDto>,
) {
    val context = LocalContext.current
    var hasLoc by remember { mutableStateOf(LocationAndAppHelper.hasLocationPermission(context)) }
    val locLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            hasLoc = LocationAndAppHelper.hasLocationPermission(context)
        }

    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf("landmarks, food, metro") }

    GoluGlassCard(accentBorder = GoluElectricBlue.copy(alpha = 0.5f)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            StatusPill(
                label = if (hasLoc) "LOCATION READY" else "LOCATION OPTIONAL",
                icon = Icons.Filled.MyLocation,
                tint = if (hasLoc) GoluEmerald else GoluAmber,
            )
            if (!hasLoc) {
                OutlinedButton(
                    onClick = {
                        locLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                            )
                        )
                    }
                ) { Text("Grant GPS") }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = origin, onValueChange = { origin = it }, label = { Text("Origin City / Start (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(value = destination, onValueChange = { destination = it }, label = { Text("Destination (e.g. Jaipur, Delhi)") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("v2_travel_destination"))
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(value = interests, onValueChange = { interests = it }, label = { Text("Interests") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { v2ViewModel.planTravel(origin, destination, interests) },
            enabled = destination.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("v2_travel_plan_btn"),
        ) {
            Icon(Icons.Filled.Explore, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Plan Route & Nearby Places")
        }
    }

    plans.forEach { plan ->
        Spacer(Modifier.height(8.dp))
        GoluGlassCard {
            Text("${plan.origin.ifBlank { "Current Location" }} → ${plan.destination}", style = MaterialTheme.typography.titleMedium, color = GoluNeonCyan)
            Text(plan.routeSummary, style = MaterialTheme.typography.bodySmall)
            if (plan.nearbyPlaces.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("Real OpenStreetMap Places:", style = MaterialTheme.typography.labelLarge, color = GoluEmerald)
                plan.nearbyPlaces.forEach { p ->
                    Text("• ${p.name} (${p.category}) — ${p.address}", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (plan.itineraryNotes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(plan.itineraryNotes, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = GoluAmber.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, GoluAmber.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(plan.liveDataNotice, style = MaterialTheme.typography.bodySmall, color = GoluAmber, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiDeviceSyncSection(
    v2ViewModel: GoluV2ViewModel,
    overview: com.example.data.remote.dto.MultiDeviceOverviewResponseDto?,
) {
    var policy by remember { mutableStateOf("SERVER_WINS") }

    GoluGlassCard(accentBorder = GoluNeonCyan.copy(alpha = 0.5f)) {
        Text("Multi-Device Account Sync", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text("Conflict Policy:", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("SERVER_WINS", "CLIENT_WINS", "LATEST_TIMESTAMP").forEach { p ->
                FilterChip(selected = policy == p, onClick = { policy = p }, label = { Text(p) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { v2ViewModel.triggerDeviceSync(policy) },
            modifier = Modifier.fillMaxWidth().testTag("v2_sync_now_btn"),
        ) {
            Icon(Icons.Filled.Sync, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Sync Account Checkpoints Now")
        }
    }

    overview?.let { ov ->
        Spacer(Modifier.height(8.dp))
        GoluGlassCard {
            Text("Cloud Account Inventory", style = MaterialTheme.typography.titleMedium, color = GoluNeonCyan)
            Text("Memories: ${ov.memoriesCount} • Notes: ${ov.notesCount} • Expenses: ${ov.expensesCount}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text("Registered Devices (${ov.devices.size}):", style = MaterialTheme.typography.labelLarge)
            ov.devices.forEach { d ->
                Text("• ${d["device_name"] ?: "Android"} (${d["platform"] ?: "android"})", style = MaterialTheme.typography.bodySmall)
            }
            if (ov.checkpoints.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("Recent Checkpoints:", style = MaterialTheme.typography.labelLarge)
                ov.checkpoints.take(5).forEach { cp ->
                    Text("• ${cp.deviceIdentifier} [${cp.conflictPolicy}] — ${cp.syncedItemsCount} items", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SelfDiagnosticsSection(
    v2ViewModel: GoluV2ViewModel,
    diagnostics: BackendDiagnostics,
    report: com.example.data.remote.dto.DiagnosticReportResponseDto?,
    onRetryConnection: () -> Unit,
) {
    val isConnected = diagnostics.connectionPhase == ConnectionPhase.CONNECTED
    val statusTint = if (isConnected && report?.overallStatus == "HEALTHY") GoluEmerald else GoluAmber

    GoluGlassCard(accentBorder = statusTint.copy(alpha = 0.7f)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("GOLU Self-Diagnostic System", style = MaterialTheme.typography.titleMedium)
            StatusPill(
                label = if (isConnected) (report?.overallStatus ?: "CONNECTED") else diagnostics.connectionPhase.label.uppercase(),
                icon = if (isConnected) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                tint = if (isConnected) GoluEmerald else GoluCoralRed,
                modifier = Modifier.testTag("v2_diag_status_pill"),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = diagnostics.statusSummary,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isConnected) GoluEmerald else GoluAmber,
        )
        if (diagnostics.rootCauseDetail.isNotBlank()) {
            Text(diagnostics.rootCauseDetail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { v2ViewModel.runSelfDiagnostics() },
                modifier = Modifier.weight(1f).testTag("v2_diag_run_btn"),
            ) {
                Icon(Icons.Filled.HealthAndSafety, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Run Checks")
            }
            OutlinedButton(
                onClick = onRetryConnection,
                modifier = Modifier.weight(1f),
            ) {
                Text("Retry Backend")
            }
        }
    }

    report?.let { r ->
        Spacer(Modifier.height(8.dp))
        GoluGlassCard {
            Text("Real Subsystem Probes", style = MaterialTheme.typography.titleMedium, color = GoluNeonCyan)
            Spacer(Modifier.height(4.dp))
            Text("API Server: ${if (r.backendOk) "OK" else "UNREACHABLE"}", style = MaterialTheme.typography.bodySmall)
            Text("PostgreSQL Database: ${if (r.databaseOk) "CONNECTED" else "UNAVAILABLE"}", style = MaterialTheme.typography.bodySmall)
            Text("Redis Cache: ${if (r.redisOk) "CONNECTED" else "NOT CONNECTED"}", style = MaterialTheme.typography.bodySmall)
            Text("OpenAI Provider Key: ${if (r.openaiConfigured) "CONFIGURED" else "MISSING"}", style = MaterialTheme.typography.bodySmall)
            Text("Search API (Tavily): ${if (r.searchApiConfigured) "CONFIGURED" else "WIKIPEDIA FALLBACK"}", style = MaterialTheme.typography.bodySmall)
            if (r.troubleshootingSuggestions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Troubleshooting Suggestions:", style = MaterialTheme.typography.labelLarge, color = GoluAmber)
                r.troubleshootingSuggestions.forEach { tip ->
                    Text("• $tip", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
