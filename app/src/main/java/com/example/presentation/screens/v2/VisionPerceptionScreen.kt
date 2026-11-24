package com.example.presentation.screens.v2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.golu2.GoluAccessibilityVisionService
import com.example.domain.golu2.GoluNotificationListenerService
import com.example.presentation.components.GoluGlassCard
import com.example.presentation.components.StatusPill
import com.example.presentation.viewmodel.GoluV2ViewModel
import com.example.ui.theme.GoluAmber
import com.example.ui.theme.GoluCoralRed
import com.example.ui.theme.GoluElectricBlue
import com.example.ui.theme.GoluEmerald
import com.example.ui.theme.GoluNeonCyan
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisionPerceptionScreen(
    v2ViewModel: GoluV2ViewModel,
    initialTab: Int = 0,
    onBack: () -> Unit,
) {
    val uiState by v2ViewModel.uiState.collectAsStateWithLifecycle()
    val screenResult by v2ViewModel.screenResult.collectAsStateWithLifecycle()
    val cameraResult by v2ViewModel.cameraResult.collectAsStateWithLifecycle()
    val pickedDoc by v2ViewModel.pickedDoc.collectAsStateWithLifecycle()
    val documents by v2ViewModel.documents.collectAsStateWithLifecycle()
    val docAnswer by v2ViewModel.docAnswer.collectAsStateWithLifecycle()
    val notifSummary by v2ViewModel.notificationSummary.collectAsStateWithLifecycle()
    val bufferedNotifs by GoluNotificationListenerService.recentNotifications.collectAsStateWithLifecycle()

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 2) v2ViewModel.loadDocuments()
        if (selectedTab == 3) v2ViewModel.loadNotificationSummary()
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
                    title = "Vision & Perception",
                    subtitle = "Explicit Consent Required for Screen, Camera, Docs & Alerts",
                    onBack = onBack,
                )
                Golu2SegmentedTabs(
                    tabs = listOf("Screen AI", "Camera AI", "Documents", "Notifications"),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                    prefixTag = "vision",
                )
                Golu2StatusBanners(uiState = uiState, onDismiss = v2ViewModel::clearFeedback)

                when (selectedTab) {
                    0 -> ScreenUnderstandingSection(v2ViewModel = v2ViewModel, result = screenResult)
                    1 -> CameraAiSection(v2ViewModel = v2ViewModel, result = cameraResult)
                    2 -> DocumentIntelligenceSection(
                        v2ViewModel = v2ViewModel,
                        pickedDoc = pickedDoc,
                        documents = documents,
                        docAnswer = docAnswer,
                    )
                    3 -> NotificationIntelligenceSection(
                        v2ViewModel = v2ViewModel,
                        summary = notifSummary,
                        bufferedCount = bufferedNotifs.size,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScreenUnderstandingSection(
    v2ViewModel: GoluV2ViewModel,
    result: com.example.data.remote.dto.ScreenUnderstandResponseDto?,
) {
    val context = LocalContext.current
    val serviceActive = remember(result) { GoluAccessibilityVisionService.isServiceEnabled(context) }
    var screenText by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("EXPLAIN") }
    var question by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }

    GoluGlassCard(accentBorder = GoluNeonCyan.copy(alpha = 0.5f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(
                label = if (serviceActive) "ACCESSIBILITY READY" else "ACCESSIBILITY OFF",
                icon = Icons.Filled.ScreenSearchDesktop,
                tint = if (serviceActive) GoluEmerald else GoluAmber,
            )
            OutlinedButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
                modifier = Modifier.testTag("v2_open_accessibility_settings"),
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Accessibility")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Strictly passive: GOLU never reads screens in the background and skips password fields.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                val captured = v2ViewModel.captureScreenIfEnabled()
                if (captured.isNotBlank()) screenText = captured
            },
            modifier = Modifier.fillMaxWidth().testTag("v2_capture_screen_btn"),
        ) {
            Text("Capture Current Screen Text")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = screenText,
            onValueChange = { screenText = it },
            label = { Text("Screen Text to Analyze") },
            placeholder = { Text("Capture from active window or paste visible text here...") },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth().testTag("v2_screen_text_input"),
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("EXPLAIN", "SUMMARIZE", "EXTRACT", "QA").forEach { mode ->
                FilterChip(selected = action == mode, onClick = { action = mode }, label = { Text(mode) })
            }
        }
        if (action == "QA") {
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Your Question") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = consent,
                onCheckedChange = { consent = it },
                modifier = Modifier.testTag("v2_screen_consent_checkbox"),
            )
            Text("I consent to send this captured text to GOLU AI.", style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = { v2ViewModel.analyzeScreen(screenText, action, question, consent) },
            enabled = screenText.isNotBlank() && consent,
            modifier = Modifier.fillMaxWidth().testTag("v2_analyze_screen_btn"),
        ) {
            Text("Analyze Screen Text")
        }
    }

    result?.let { res ->
        Spacer(Modifier.height(10.dp))
        GoluGlassCard(accentBorder = GoluElectricBlue.copy(alpha = 0.6f)) {
            Text(
                text = "Screen Analysis (${res.action} • ${res.provider})",
                style = MaterialTheme.typography.titleMedium,
                color = GoluNeonCyan,
            )
            Spacer(Modifier.height(6.dp))
            Text(res.answer, style = MaterialTheme.typography.bodyMedium)
            if (res.extractedHighlights.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Highlights:", style = MaterialTheme.typography.labelLarge, color = GoluEmerald)
                res.extractedHighlights.forEach { hl ->
                    Text("• $hl", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CameraAiSection(
    v2ViewModel: GoluV2ViewModel,
    result: com.example.data.remote.dto.CameraAnalyzeResponseDto?,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasCameraPerm = granted
        }

    var mode by remember { mutableStateOf("SCENE") }
    var question by remember { mutableStateOf("What do you see and what text is written here?") }
    var consent by remember { mutableStateOf(false) }
    var isPreviewActive by remember { mutableStateOf(false) }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val previewView = remember { PreviewView(context) }

    DisposableEffect(hasCameraPerm, lifecycleOwner) {
        var providerRef: ProcessCameraProvider? = null
        if (hasCameraPerm) {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    runCatching {
                        val provider = future.get()
                        providerRef = provider
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture,
                        )
                        isPreviewActive = true
                    }.onFailure {
                        isPreviewActive = false
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        }
        onDispose {
            runCatching { providerRef?.unbindAll() }
            isPreviewActive = false
        }
    }

    GoluGlassCard(accentBorder = if (isPreviewActive) GoluNeonCyan else MaterialTheme.colorScheme.outline) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(
                label = if (isPreviewActive) "CAMERA ACTIVE" else if (hasCameraPerm) "INITIALIZING" else "CAMERA OFF",
                icon = Icons.Filled.CameraAlt,
                tint = if (isPreviewActive) GoluCoralRed else GoluAmber,
                modifier = Modifier.testTag("v2_camera_active_badge"),
            )
            if (!hasCameraPerm) {
                Button(
                    onClick = { permLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.testTag("v2_grant_camera_btn"),
                ) {
                    Text("Grant Camera")
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        if (hasCameraPerm) {
            Box(
                modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)),
            ) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            }
        } else {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().height(120.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Camera permission required for Live Camera AI. Camera stops automatically when leaving this tab.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("SCENE", "OCR", "OBJECT", "TRANSLATE").forEach { m ->
                FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m) })
            }
        }
        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text("Question for Camera AI") },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = consent, onCheckedChange = { consent = it })
            Text("I consent to analyze the captured frame on GOLU backend.", style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = {
                imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bytes = imageProxyToJpegBytes(image)
                            image.close()
                            if (bytes.isNotEmpty()) {
                                v2ViewModel.analyzeCameraBytes(bytes, question, mode, consent)
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            // Handled gracefully by leaving preview intact
                        }
                    },
                )
            },
            enabled = hasCameraPerm && isPreviewActive && consent && question.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("v2_camera_capture_btn"),
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Capture & Analyze Frame")
        }
    }

    result?.let { res ->
        Spacer(Modifier.height(10.dp))
        GoluGlassCard(accentBorder = GoluNeonCyan.copy(alpha = 0.6f)) {
            Text("Vision Result (${res.mode} • ${res.provider})", style = MaterialTheme.typography.titleMedium, color = GoluNeonCyan)
            Spacer(Modifier.height(6.dp))
            Text(res.description, style = MaterialTheme.typography.bodyMedium)
            if (res.detectedText.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("Detected Text: ${res.detectedText}", style = MaterialTheme.typography.bodySmall, color = GoluEmerald)
            }
            if (res.objects.isNotEmpty()) {
                Text("Objects: ${res.objects.joinToString(", ")}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun imageProxyToJpegBytes(image: ImageProxy): ByteArray {
    if (image.format == ImageFormat.JPEG && image.planes.isNotEmpty()) {
        val buf = image.planes[0].buffer
        val arr = ByteArray(buf.remaining())
        buf.get(arr)
        return arr
    }
    return runCatching {
        val bmp = image.toBitmap()
        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        baos.toByteArray()
    }.getOrDefault(ByteArray(0))
}

@Composable
private fun DocumentIntelligenceSection(
    v2ViewModel: GoluV2ViewModel,
    pickedDoc: com.example.domain.golu2.PickedDocumentPayload?,
    documents: List<com.example.data.remote.dto.DocumentResponseDto>,
    docAnswer: com.example.data.remote.dto.DocumentQueryResponseDto?,
) {
    var consent by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var askQuestion by remember { mutableStateOf("") }
    var activeDocId by remember { mutableStateOf<String?>(null) }

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) v2ViewModel.handlePickedDocument(uri)
        }

    GoluGlassCard(accentBorder = GoluElectricBlue.copy(alpha = 0.5f)) {
        Text("PDF, Text & Image Document Analyzer", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { picker.launch(arrayOf("application/pdf", "text/*", "image/*")) },
            modifier = Modifier.fillMaxWidth().testTag("v2_pick_document_btn"),
        ) {
            Icon(Icons.Filled.UploadFile, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Pick PDF / Text / Image Document")
        }
        pickedDoc?.let { doc ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Selected: ${doc.title} (${doc.sizeBytes / 1024} KB${if (doc.pdfPageCount > 0) " • ${doc.pdfPageCount} pages" else ""})",
                style = MaterialTheme.typography.labelLarge,
                color = GoluNeonCyan,
            )
            doc.firstPageBitmap?.let { bmp ->
                Spacer(Modifier.height(8.dp))
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "PDF first page preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = consent, onCheckedChange = { consent = it })
                Text("I consent to upload & index this document.", style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { v2ViewModel.uploadPickedDocument(consent) },
                enabled = consent,
                modifier = Modifier.fillMaxWidth().testTag("v2_upload_document_btn"),
            ) {
                Text("Summarize & Index Document")
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Indexed Documents") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = { v2ViewModel.loadDocuments(searchQuery.ifBlank { null }) }) {
            Text("Search")
        }
    }

    documents.forEach { doc ->
        Spacer(Modifier.height(8.dp))
        GoluGlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = GoluNeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(doc.title, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(doc.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    doc.keyPoints.take(3).forEach { kp ->
                        Text("• $kp", style = MaterialTheme.typography.bodySmall)
                    }
                }
                IconButton(
                    onClick = { v2ViewModel.deleteDocument(doc.id) },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete document", tint = GoluCoralRed)
                }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = { activeDocId = doc.id },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Ask Question About This Document")
            }
            if (activeDocId == doc.id) {
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = askQuestion,
                    onValueChange = { askQuestion = it },
                    label = { Text("Document Question") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { v2ViewModel.askDocument(doc.id, askQuestion) },
                    enabled = askQuestion.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Ask Document AI")
                }
            }
        }
    }

    docAnswer?.let { ans ->
        Spacer(Modifier.height(8.dp))
        GoluGlassCard(accentBorder = GoluEmerald) {
            Text("Document Q&A Answer", style = MaterialTheme.typography.titleMedium, color = GoluEmerald)
            Spacer(Modifier.height(4.dp))
            Text(ans.answer, style = MaterialTheme.typography.bodyMedium)
            ans.matchedExcerpts.forEach { ex ->
                Text("Excerpt: $ex", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NotificationIntelligenceSection(
    v2ViewModel: GoluV2ViewModel,
    summary: com.example.data.remote.dto.NotificationSummaryResponseDto?,
    bufferedCount: Int,
) {
    val context = LocalContext.current
    val accessGranted = remember(summary, bufferedCount) {
        GoluNotificationListenerService.isNotificationAccessGranted(context)
    }
    var consent by remember { mutableStateOf(false) }
    var autoReminder by remember { mutableStateOf(true) }

    GoluGlassCard(accentBorder = GoluAmber.copy(alpha = 0.6f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(
                label = if (accessGranted) "LISTENER ENABLED" else "ACCESS NOT GRANTED",
                icon = Icons.Filled.NotificationsActive,
                tint = if (accessGranted) GoluEmerald else GoluAmber,
            )
            OutlinedButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
            ) {
                Text("Settings")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Buffered local notifications ready for analysis: $bufferedCount",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = consent, onCheckedChange = { consent = it })
            Text("I consent to categorize & summarize buffered notifications.", style = MaterialTheme.typography.bodySmall)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Convert urgent alerts to reminders", style = MaterialTheme.typography.bodySmall)
            Switch(checked = autoReminder, onCheckedChange = { autoReminder = it })
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { v2ViewModel.analyzeCapturedNotifications(consent, autoReminder) },
            enabled = consent,
            modifier = Modifier.fillMaxWidth().testTag("v2_analyze_notifications_btn"),
        ) {
            Text("Analyze & Summarize Notifications")
        }
    }

    summary?.let { sum ->
        Spacer(Modifier.height(10.dp))
        GoluGlassCard {
            Text(
                text = "Summary (${sum.importantCount} important / ${sum.totalCount} total)",
                style = MaterialTheme.typography.titleMedium,
                color = GoluNeonCyan,
            )
            Spacer(Modifier.height(4.dp))
            Text(sum.summary, style = MaterialTheme.typography.bodyMedium)
            sum.items.take(8).forEach { item ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (item.isImportant) GoluCoralRed else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "[${item.category} • P${item.priorityScore}] ${item.title}",
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(item.aiSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(
                            onClick = { v2ViewModel.deleteNotificationInsight(item.id) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete notification insight", tint = GoluCoralRed)
                        }
                    }
                }
            }
        }
    }
}
