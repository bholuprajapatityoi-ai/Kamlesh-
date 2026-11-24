package com.example.presentation.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Base64
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.DeviceTelemetry
import com.example.core.NetworkResult
import com.example.data.remote.dto.*
import com.example.data.repository.GoluV2Repository
import com.example.di.AppContainer
import com.example.domain.golu2.AudioRecorderHelper
import com.example.domain.golu2.GoluAccessibilityVisionService
import com.example.domain.golu2.GoluNotificationListenerService
import com.example.domain.golu2.LocationAndAppHelper
import com.example.domain.golu2.OfflineBrainV2
import com.example.domain.golu2.PdfDocumentHelper
import com.example.domain.golu2.PickedDocumentPayload
import com.example.domain.model.InstalledAppInfo
import com.example.domain.voice.VoiceEngine
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GoluV2UiState(
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

class GoluV2ViewModel(
    private val appContext: Context,
    private val repo: GoluV2Repository,
    private val recorderHelper: AudioRecorderHelper,
    private val voiceEngine: VoiceEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoluV2UiState())
    val uiState: StateFlow<GoluV2UiState> = _uiState.asStateFlow()

    // Phase 16: Vision, Camera, Documents, Notifications
    private val _screenResult = MutableStateFlow<ScreenUnderstandResponseDto?>(null)
    val screenResult: StateFlow<ScreenUnderstandResponseDto?> = _screenResult.asStateFlow()

    private val _cameraResult = MutableStateFlow<CameraAnalyzeResponseDto?>(null)
    val cameraResult: StateFlow<CameraAnalyzeResponseDto?> = _cameraResult.asStateFlow()

    private val _pickedDoc = MutableStateFlow<PickedDocumentPayload?>(null)
    val pickedDoc: StateFlow<PickedDocumentPayload?> = _pickedDoc.asStateFlow()

    private val _documents = MutableStateFlow<List<DocumentResponseDto>>(emptyList())
    val documents: StateFlow<List<DocumentResponseDto>> = _documents.asStateFlow()

    private val _docAnswer = MutableStateFlow<DocumentQueryResponseDto?>(null)
    val docAnswer: StateFlow<DocumentQueryResponseDto?> = _docAnswer.asStateFlow()

    private val _notificationSummary = MutableStateFlow<NotificationSummaryResponseDto?>(null)
    val notificationSummary: StateFlow<NotificationSummaryResponseDto?> = _notificationSummary.asStateFlow()

    // Phase 17: Knowledge, Translator, Research, Offline Brain
    private val _knowledgeNotes = MutableStateFlow<List<KnowledgeNoteResponseDto>>(emptyList())
    val knowledgeNotes: StateFlow<List<KnowledgeNoteResponseDto>> = _knowledgeNotes.asStateFlow()

    private val _translations = MutableStateFlow<List<TranslationResponseDto>>(emptyList())
    val translations: StateFlow<List<TranslationResponseDto>> = _translations.asStateFlow()

    private val _latestTranslation = MutableStateFlow<TranslationResponseDto?>(null)
    val latestTranslation: StateFlow<TranslationResponseDto?> = _latestTranslation.asStateFlow()

    private val _researchReports = MutableStateFlow<List<ResearchReportResponseDto>>(emptyList())
    val researchReports: StateFlow<List<ResearchReportResponseDto>> = _researchReports.asStateFlow()

    private val _offlineManifest = MutableStateFlow<OfflineManifestResponseDto?>(null)
    val offlineManifest: StateFlow<OfflineManifestResponseDto?> = _offlineManifest.asStateFlow()

    private val _offlineTestOutput = MutableStateFlow<String?>(null)
    val offlineTestOutput: StateFlow<String?> = _offlineTestOutput.asStateFlow()

    // Phase 18: Planner, Coding, Creative, Voice Recorder
    private val _plannerProposal = MutableStateFlow<PlannerSuggestResponseDto?>(null)
    val plannerProposal: StateFlow<PlannerSuggestResponseDto?> = _plannerProposal.asStateFlow()

    private val _codingHistory = MutableStateFlow<List<CodingAssistResponseDto>>(emptyList())
    val codingHistory: StateFlow<List<CodingAssistResponseDto>> = _codingHistory.asStateFlow()

    private val _creativeAssets = MutableStateFlow<List<CreativeAssetResponseDto>>(emptyList())
    val creativeAssets: StateFlow<List<CreativeAssetResponseDto>> = _creativeAssets.asStateFlow()

    private val _recordings = MutableStateFlow<List<RecordingResponseDto>>(emptyList())
    val recordings: StateFlow<List<RecordingResponseDto>> = _recordings.asStateFlow()

    private val _isRecordingAudio = MutableStateFlow(false)
    val isRecordingAudio: StateFlow<Boolean> = _isRecordingAudio.asStateFlow()

    private val _lastRecordedPath = MutableStateFlow("")
    val lastRecordedPath: StateFlow<String> = _lastRecordedPath.asStateFlow()

    // Phase 19: Smart Home, Expenses, Personality, Smart Apps
    private val _smartHomeConfig = MutableStateFlow<SmartHomeConfigResponseDto?>(null)
    val smartHomeConfig: StateFlow<SmartHomeConfigResponseDto?> = _smartHomeConfig.asStateFlow()

    private val _smartHomeDevices = MutableStateFlow<List<SmartHomeDeviceResponseDto>>(emptyList())
    val smartHomeDevices: StateFlow<List<SmartHomeDeviceResponseDto>> = _smartHomeDevices.asStateFlow()

    private val _expenseSummary = MutableStateFlow<ExpenseSummaryResponseDto?>(null)
    val expenseSummary: StateFlow<ExpenseSummaryResponseDto?> = _expenseSummary.asStateFlow()

    private val _personality = MutableStateFlow<PersonalityResponseDto?>(null)
    val personality: StateFlow<PersonalityResponseDto?> = _personality.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _appShortcuts = MutableStateFlow<List<SmartAppPreferenceResponseDto>>(emptyList())
    val appShortcuts: StateFlow<List<SmartAppPreferenceResponseDto>> = _appShortcuts.asStateFlow()

    // Phase 20: Study Tutor, Travel, Multi-Device Sync, Self-Diagnostics
    private val _studySessions = MutableStateFlow<List<StudySessionResponseDto>>(emptyList())
    val studySessions: StateFlow<List<StudySessionResponseDto>> = _studySessions.asStateFlow()

    private val _travelPlans = MutableStateFlow<List<TravelPlanResponseDto>>(emptyList())
    val travelPlans: StateFlow<List<TravelPlanResponseDto>> = _travelPlans.asStateFlow()

    private val _multiDeviceOverview = MutableStateFlow<MultiDeviceOverviewResponseDto?>(null)
    val multiDeviceOverview: StateFlow<MultiDeviceOverviewResponseDto?> = _multiDeviceOverview.asStateFlow()

    private val _diagnosticReport = MutableStateFlow<DiagnosticReportResponseDto?>(null)
    val diagnosticReport: StateFlow<DiagnosticReportResponseDto?> = _diagnosticReport.asStateFlow()

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(statusMessage = null, errorMessage = null)
    }

    private fun startLoading() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
    }

    private fun fail(err: NetworkResult.Error) {
        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = err.message)
    }

    private fun reportLoadError(err: NetworkResult.Error) {
        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = err.message)
    }

    private fun ok(msg: String) {
        _uiState.value = _uiState.value.copy(isLoading = false, statusMessage = msg, errorMessage = null)
    }

    // Feature 1: Screen Understanding
    fun captureScreenIfEnabled(): String {
        val captured = GoluAccessibilityVisionService.captureCurrentScreenIfAuthorized()
        return if (captured == null) {
            _uiState.value =
                _uiState.value.copy(
                    errorMessage = "Screen Accessibility helper is not enabled. Enable it in Settings or paste text explicitly below."
                )
            ""
        } else {
            ok("Captured visible text from ${captured.first.ifBlank { "current window" }}.")
            captured.second
        }
    }

    fun analyzeScreen(text: String, action: String, question: String, consent: Boolean) = viewModelScope.launch {
        if (!consent) {
            _uiState.value = _uiState.value.copy(errorMessage = "Consent checkbox is required before analyzing screen text.")
            return@launch
        }
        startLoading()
        when (val res = repo.analyzeScreen(ScreenUnderstandRequestDto(text, action, question, "Android", "Hinglish", true))) {
            is NetworkResult.Success -> {
                _screenResult.value = res.data
                ok("Screen analysis ready (${res.data.provider}).")
            }
            is NetworkResult.Error -> {
                val lines = text.lines().map { it.trim() }.filter { it.length > 8 }.take(4)
                _screenResult.value =
                    ScreenUnderstandResponseDto(
                        action = action,
                        answer = "Offline preview (${res.message}): Captured ${text.length} chars across ${lines.size} line(s).",
                        extractedHighlights = lines,
                        provider = "local-offline-fallback",
                        aiBacked = false,
                    )
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
            }
        }
    }

    // Feature 2: Camera AI
    fun analyzeCameraBytes(jpegBytes: ByteArray, question: String, mode: String, consent: Boolean) = viewModelScope.launch {
        if (!consent) {
            _uiState.value = _uiState.value.copy(errorMessage = "Explicit consent required before sending camera frame.")
            return@launch
        }
        startLoading()
        val b64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        when (val res = repo.analyzeCamera(CameraAnalyzeRequestDto(b64, question, mode, "Hinglish", true))) {
            is NetworkResult.Success -> {
                _cameraResult.value = res.data
                ok("Camera AI analysis complete (${res.data.provider}).")
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    // Feature 3: Document Intelligence
    fun handlePickedDocument(uri: Uri) {
        PdfDocumentHelper.readPickedDocument(appContext, uri)
            .onSuccess { payload ->
                _pickedDoc.value = payload
                ok("Selected '${payload.title}' (${payload.sizeBytes / 1024} KB). Grant consent to analyze/upload.")
            }
            .onFailure { err ->
                _uiState.value = _uiState.value.copy(errorMessage = err.message ?: "Failed to read document.")
            }
    }

    fun uploadPickedDocument(consent: Boolean) = viewModelScope.launch {
        val doc = _pickedDoc.value ?: return@launch
        if (!consent) {
            _uiState.value = _uiState.value.copy(errorMessage = "Explicit consent is required before uploading documents.")
            return@launch
        }
        startLoading()
        val req =
            DocumentUploadRequestDto(
                title = doc.title,
                mimeType = doc.mimeType,
                textContent = doc.textPreview,
                fileBase64 = doc.fileBase64,
                consentGranted = true,
            )
        when (val res = repo.uploadDocument(req)) {
            is NetworkResult.Success -> {
                ok("Document '${res.data.title}' analyzed & indexed!")
                loadDocuments()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    fun loadDocuments(query: String? = null) = viewModelScope.launch {
        when (val res = repo.listDocuments(query)) {
            is NetworkResult.Success -> _documents.value = res.data
            is NetworkResult.Error -> reportLoadError(res)
        }
    }

    fun askDocument(docId: String, question: String) = viewModelScope.launch {
        startLoading()
        when (val res = repo.askDocument(docId, DocumentQueryRequestDto(question))) {
            is NetworkResult.Success -> {
                _docAnswer.value = res.data
                ok("Document answer ready.")
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    fun deleteDocument(docId: String) = viewModelScope.launch {
        when (val res = repo.deleteDocument(docId)) {
            is NetworkResult.Success -> {
                ok(res.data.message)
                loadDocuments()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    // Feature 4: Smart Notification Intelligence
    fun loadNotificationSummary() = viewModelScope.launch {
        when (val res = repo.getNotificationSummary()) {
            is NetworkResult.Success -> _notificationSummary.value = res.data
            is NetworkResult.Error -> reportLoadError(res)
        }
    }

    fun analyzeCapturedNotifications(consent: Boolean, createReminder: Boolean) = viewModelScope.launch {
        if (!consent) {
            _uiState.value = _uiState.value.copy(errorMessage = "Explicit notification consent is required.")
            return@launch
        }
        val items = GoluNotificationListenerService.recentNotifications.value.take(5)
        if (items.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "No buffered device notifications yet. Ensure Notification Access is enabled.")
            return@launch
        }
        startLoading()
        var analyzed = 0
        var lastError: NetworkResult.Error? = null
        for (item in items) {
            when (
                val res =
                    repo.analyzeNotification(
                        NotificationIngestRequestDto(
                            sourceApp = item.packageName,
                            title = item.title,
                            bodyPreview = item.body,
                            consentGranted = true,
                            createReminderIfImportant = createReminder,
                        )
                    )
            ) {
                is NetworkResult.Success -> analyzed++
                is NetworkResult.Error -> lastError = res
            }
        }
        if (analyzed > 0) {
            ok("Analyzed $analyzed notification(s).")
            loadNotificationSummary()
        } else if (lastError != null) {
            fail(lastError)
        }
    }

    fun deleteNotificationInsight(id: String) = viewModelScope.launch {
        when (val res = repo.deleteNotificationInsight(id)) {
            is NetworkResult.Success -> {
                ok(res.data.message)
                loadNotificationSummary()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    // Feature 5: Personal Knowledge Brain
    fun loadKnowledge(q: String? = null) = viewModelScope.launch {
        when (val res = repo.listKnowledge(q)) {
            is NetworkResult.Success -> _knowledgeNotes.value = res.data
            is NetworkResult.Error -> reportLoadError(res)
        }
    }

    fun saveKnowledgeNote(title: String, category: String, content: String, tagsCsv: String) = viewModelScope.launch {
        startLoading()
        val tags = tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        when (val res = repo.createKnowledge(KnowledgeNoteRequestDto(title, category, content, tags))) {
            is NetworkResult.Success -> {
                ok("Note saved to Unified Knowledge Brain & Memory!")
                loadKnowledge()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    fun updateKnowledgeNote(id: String, title: String, category: String, content: String, tagsCsv: String) = viewModelScope.launch {
        startLoading()
        val tags = tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        when (val res = repo.updateKnowledge(id, KnowledgeNoteRequestDto(title, category, content, tags))) {
            is NetworkResult.Success -> {
                ok("Note updated in Knowledge Brain.")
                loadKnowledge()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    fun deleteKnowledgeNote(id: String) = viewModelScope.launch {
        when (val res = repo.deleteKnowledge(id)) {
            is NetworkResult.Success -> {
                ok(res.data.message)
                loadKnowledge()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    // Feature 6: Live Language Translator
    fun translateText(text: String, source: String, target: String, speakOutput: Boolean) = viewModelScope.launch {
        startLoading()
        when (val res = repo.translate(TranslateRequestDto(text, source, target, "TEXT"))) {
            is NetworkResult.Success -> {
                _latestTranslation.value = res.data
                if (speakOutput) voiceEngine.speak(res.data.translatedText)
                ok("Translated to $target.")
                loadTranslations()
            }
            is NetworkResult.Error -> {
                val localFallback = OfflineBrainV2.evaluateLocalQuery(text)
                if (localFallback.handled) {
                    _uiState.value = _uiState.value.copy(isLoading = false, statusMessage = localFallback.reply)
                } else {
                    fail(res)
                }
            }
        }
    }

    fun loadTranslations() = viewModelScope.launch {
        when (val res = repo.listTranslations()) {
            is NetworkResult.Success -> _translations.value = res.data
            is NetworkResult.Error -> reportLoadError(res)
        }
    }

    fun deleteTranslation(id: String) = viewModelScope.launch {
        when (val res = repo.deleteTranslation(id)) {
            is NetworkResult.Success -> {
                ok(res.data.message)
                loadTranslations()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    // Feature 7: Deep Research
    fun runResearch(query: String) = viewModelScope.launch {
        startLoading()
        when (val res = repo.runResearch(ResearchRequestDto(query))) {
            is NetworkResult.Success -> {
                ok("Research report ready (${res.data.providerUsed}).")
                loadResearchHistory()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    fun loadResearchHistory() = viewModelScope.launch {
        when (val res = repo.listResearchHistory()) {
            is NetworkResult.Success -> _researchReports.value = res.data
            is NetworkResult.Error -> reportLoadError(res)
        }
    }

    // Feature 8: Offline Brain
    fun loadOfflineManifest() = viewModelScope.launch {
        when (val res = repo.getOfflineManifest()) {
            is NetworkResult.Success -> _offlineManifest.value = res.data
            is NetworkResult.Error -> reportLoadError(res)
        }
    }

    fun testOfflineBrain(query: String) {
        val result = OfflineBrainV2.evaluateLocalQuery(query)
        _offlineTestOutput.value = "${result.reply}\n(${result.capabilityNote})"
    }

    // Feature 9: Life Planner AI
    fun suggestPlan(focus: String, hours: Int) = viewModelScope.launch {
        startLoading()
        when (val res = repo.suggestPlan(PlannerSuggestRequestDto(focus, hours))) {
            is NetworkResult.Success -> {
                _plannerProposal.value = res.data
                ok("Plan generated. Review and approve to save.")
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    fun approveCurrentPlan() = viewModelScope.launch {
        val prop = _plannerProposal.value ?: return@launch
        startLoading()
        when (val res = repo.approvePlan(PlannerApproveRequestDto(prop.proposals))) {
            is NetworkResult.Success -> ok(res.data.message)
            is NetworkResult.Error -> fail(res)
        }
    }

    // Feature 10: Coding Assistant
    fun assistCoding(prompt: String, language: String, taskType: String) = viewModelScope.launch {
        startLoading()
        when (val res = repo.assistCoding(CodingAssistRequestDto(prompt, language, taskType))) {
            is NetworkResult.Success -> {
                ok("Code assistance generated.")
                loadCodingHistory()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    fun loadCodingHistory() = viewModelScope.launch {
        when (val res = repo.listCodingHistory()) {
            is NetworkResult.Success -> _codingHistory.value = res.data
            is NetworkResult.Error -> reportLoadError(res)
        }
    }

    // Feature 11: Creative Studio
    fun generateCreative(assetType: String, prompt: String, style: String) = viewModelScope.launch {
        startLoading()
        when (val res = repo.generateCreative(CreativeRequestDto(assetType, prompt, style))) {
            is NetworkResult.Success -> {
                ok("Creative asset ready!")
                loadCreativeHistory()
            }
            is NetworkResult.Error -> {
                fail(res)
                loadCreativeHistory()
            }
        }
    }

    fun loadCreativeHistory() = viewModelScope.launch {
        when (val res = repo.listCreativeHistory()) {
            is NetworkResult.Success -> _creativeAssets.value = res.data
            is NetworkResult.Error -> reportLoadError(res)
        }
    }

    fun deleteCreative(id: String) = viewModelScope.launch {
        when (val res = repo.deleteCreative(id)) {
            is NetworkResult.Success -> {
                ok(res.data.message)
                loadCreativeHistory()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    // Feature 12: Voice Recorder Intelligence
    fun startAudioRecording() {
        recorderHelper.startRecording()
            .onSuccess { file ->
                _isRecordingAudio.value = true
                _lastRecordedPath.value = file.absolutePath
                ok("Recording audio visibly on device...")
            }
            .onFailure { err ->
                _uiState.value = _uiState.value.copy(errorMessage = err.message ?: "Failed to start audio recording.")
            }
    }

    fun stopAndSaveRecording(title: String, transcript: String) = viewModelScope.launch {
        val (file, dur) = recorderHelper.stopRecording()
        _isRecordingAudio.value = false
        val path = file?.absolutePath ?: _lastRecordedPath.value
        _lastRecordedPath.value = path
        startLoading()
        val req =
            RecordingCreateRequestDto(
                title = title.ifBlank { "GOLU Recording" },
                durationSeconds = dur,
                transcript = transcript.ifBlank { "Audio recorded locally (${dur}s). Add or edit transcript notes." },
                localFileHint = path,
                generateSummary = true,
            )
        when (val res = repo.createRecording(req)) {
            is NetworkResult.Success -> {
                ok("Recording saved & summarized (${dur}s).")
                loadRecordings()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    fun playLastRecording() {
        val path = _lastRecordedPath.value
        playRecordingPath(path)
    }

    fun playRecordingPath(path: String) {
        if (!recorderHelper.playFile(path)) {
            _uiState.value = _uiState.value.copy(errorMessage = "No local recording file ready to play.")
        } else {
            ok("Playing local recording...")
        }
    }

    fun updateRecordingTranscript(id: String, title: String, transcript: String) = viewModelScope.launch {
        startLoading()
        when (val res = repo.updateRecording(id, RecordingUpdateRequestDto(title, transcript, regenerateSummary = true))) {
            is NetworkResult.Success -> {
                ok("Recording transcript & summary updated.")
                loadRecordings()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    fun loadRecordings(q: String? = null) = viewModelScope.launch {
        when (val res = repo.listRecordings(q)) {
            is NetworkResult.Success -> _recordings.value = res.data
            is NetworkResult.Error -> reportLoadError(res)
        }
    }

    fun deleteRecording(rec: RecordingResponseDto) = viewModelScope.launch {
        recorderHelper.deleteLocalFile(rec.localFileHint)
        when (val res = repo.deleteRecording(rec.id)) {
            is NetworkResult.Success -> {
                ok(res.data.message)
                loadRecordings()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    // Feature 13: Smart Home
    fun loadSmartHome() = viewModelScope.launch {
        when (val c = repo.getSmartHomeConfig()) {
            is NetworkResult.Success -> _smartHomeConfig.value = c.data
            is NetworkResult.Error -> reportLoadError(c)
        }
        when (val d = repo.listSmartHomeDevices()) {
            is NetworkResult.Success -> _smartHomeDevices.value = d.data
            is NetworkResult.Error -> reportLoadError(d)
        }
    }

    fun authorizeSmartHome(hubUrl: String, token: String) = viewModelScope.launch {
        startLoading()
        when (val res = repo.updateSmartHomeConfig(SmartHomeConfigRequestDto("HOME_ASSISTANT", hubUrl, token, true))) {
            is NetworkResult.Success -> {
                _smartHomeConfig.value = res.data
                ok("Smart Home hub configured.")
                loadSmartHome()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    fun addSmartHomeDevice(name: String, room: String, entityId: String, type: String) = viewModelScope.launch {
        startLoading()
        when (val res = repo.addSmartHomeDevice(SmartHomeDeviceCreateRequestDto(entityId, name, room, type))) {
            is NetworkResult.Success -> {
                ok("Added device '${res.data.name}'.")
                loadSmartHome()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    fun toggleSmartHomeDevice(dev: SmartHomeDeviceResponseDto, confirmedSensitive: Boolean) = viewModelScope.launch {
        startLoading()
        when (val res = repo.commandSmartHomeDevice(dev.id, SmartHomeDeviceCommandRequestDto(!dev.isOn, dev.brightnessPct, confirmedSensitive))) {
            is NetworkResult.Success -> {
                ok("Device '${res.data.name}' updated via verified hub.")
                loadSmartHome()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    fun deleteSmartHomeDevice(id: String) = viewModelScope.launch {
        when (val res = repo.deleteSmartHomeDevice(id)) {
            is NetworkResult.Success -> {
                ok(res.data.message)
                loadSmartHome()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    // Feature 14: Expenses
    fun loadExpenses(monthKey: String? = null) = viewModelScope.launch {
        when (val res = repo.getExpenses(monthKey)) {
            is NetworkResult.Success -> _expenseSummary.value = res.data
            is NetworkResult.Error -> reportLoadError(res)
        }
    }

    fun addExpense(title: String, category: String, amount: Double, notes: String) = viewModelScope.launch {
        startLoading()
        when (val res = repo.addExpense(ExpenseCreateRequestDto(title, category, amount, "INR", notes))) {
            is NetworkResult.Success -> {
                ok("Added expense ₹${res.data.amount} (${res.data.category}).")
                loadExpenses()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    fun setMonthlyBudget(limit: Double) = viewModelScope.launch {
        val month = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        startLoading()
        when (val res = repo.setExpenseBudget(BudgetUpdateRequestDto(month, limit, "INR"))) {
            is NetworkResult.Success -> {
                _expenseSummary.value = res.data
                ok("Monthly budget updated to ₹$limit.")
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    fun exportExpensesCsv(onReady: (String) -> Unit) = viewModelScope.launch {
        startLoading()
        when (val res = repo.exportExpensesCsv()) {
            is NetworkResult.Success -> {
                ok("Expenses CSV exported from backend.")
                onReady(res.data)
            }
            is NetworkResult.Error -> {
                val cached = _expenseSummary.value
                if (cached != null) {
                    val csv = buildString {
                        appendLine("id,date,title,category,amount,currency,notes")
                        cached.items.forEach { e ->
                            appendLine(
                                "${e.id},${e.spentAt},\"${e.title.replace("\"", "\"\"")}\",${e.category},${e.amount},${e.currency},\"${e.notes.replace("\"", "\"\"")}\""
                            )
                        }
                    }
                    ok("Offline CSV prepared (${res.message}).")
                    onReady(csv)
                } else {
                    fail(res)
                }
            }
        }
    }

    fun deleteExpense(id: String) = viewModelScope.launch {
        when (val res = repo.deleteExpense(id)) {
            is NetworkResult.Success -> {
                ok(res.data.message)
                loadExpenses()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    // Feature 15: Custom Personality
    fun loadPersonality() = viewModelScope.launch {
        when (val res = repo.getPersonality()) {
            is NetworkResult.Success -> _personality.value = res.data
            is NetworkResult.Error -> reportLoadError(res)
        }
    }

    fun updatePersonality(
        style: String,
        humor: Int,
        length: String,
        greeting: String,
        avatar: String,
        notes: String,
    ) = viewModelScope.launch {
        startLoading()
        val req =
            PersonalityUpdateRequestDto(
                responseStyle = style,
                humorLevel = humor.coerceIn(0, 100),
                responseLength = length,
                greetingMessage = greeting.ifBlank { "Hello! Boss, GOLU ready hai!" },
                avatarStyle = avatar,
                customSystemNotes = notes,
            )
        when (val res = repo.updatePersonality(req)) {
            is NetworkResult.Success -> {
                _personality.value = res.data
                ok("Personality settings saved!")
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    // Feature 16: Smart App Intelligence
    fun refreshSmartApps() = viewModelScope.launch {
        _installedApps.value = LocationAndAppHelper.queryLauncherApps(appContext)
        when (val res = repo.listAppShortcuts()) {
            is NetworkResult.Success -> _appShortcuts.value = res.data
            is NetworkResult.Error -> reportLoadError(res)
        }
    }

    fun launchAndTrackApp(app: InstalledAppInfo) = viewModelScope.launch {
        val launched = LocationAndAppHelper.launchPackage(appContext, app.packageName)
        if (!launched) {
            _uiState.value = _uiState.value.copy(errorMessage = "Unable to launch ${app.appName}.")
            return@launch
        }
        val res =
            repo.upsertAppShortcut(
                AppShortcutUpsertRequestDto(
                    aliasName = app.appName,
                    packageName = app.packageName,
                    isFavourite = true,
                    incrementLaunch = true,
                )
            )
        when (res) {
            is NetworkResult.Success -> {
                ok("Launching ${app.appName} (launch count synced).")
                refreshSmartApps()
            }
            is NetworkResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Launched ${app.appName} locally.",
                    errorMessage = res.message,
                )
            }
        }
    }

    fun deleteAppShortcut(id: String) = viewModelScope.launch {
        when (val res = repo.deleteAppShortcut(id)) {
            is NetworkResult.Success -> {
                ok(res.data.message)
                refreshSmartApps()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    // Feature 17: Study Tutor
    fun explainStudyTopic(subject: String, topic: String, language: String, difficulty: String) = viewModelScope.launch {
        startLoading()
        when (val res = repo.explainStudyTopic(StudyAskRequestDto(subject, topic, language, difficulty))) {
            is NetworkResult.Success -> {
                ok("Study lesson ready for ${res.data.topic}!")
                loadStudySessions()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    fun loadStudySessions() = viewModelScope.launch {
        when (val res = repo.listStudySessions()) {
            is NetworkResult.Success -> _studySessions.value = res.data
            is NetworkResult.Error -> reportLoadError(res)
        }
    }

    fun bumpStudyProgress(session: StudySessionResponseDto) = viewModelScope.launch {
        val next = (session.masteryPct + 20).coerceAtMost(100)
        when (val res = repo.updateStudyProgress(session.id, next)) {
            is NetworkResult.Success -> {
                ok("Mastery updated to $next%!")
                loadStudySessions()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    // Feature 18: Travel Intelligence
    fun planTravel(origin: String, destination: String, interests: String) = viewModelScope.launch {
        startLoading()
        val coords = LocationAndAppHelper.getLastKnownCoordinates(appContext)
        val req =
            TravelPlanRequestDto(
                origin = origin,
                destination = destination,
                latitude = coords?.latitude,
                longitude = coords?.longitude,
                interests = interests,
            )
        when (val res = repo.createTravelPlan(req)) {
            is NetworkResult.Success -> {
                ok("Travel plan & place matches ready.")
                loadTravelPlans()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    fun loadTravelPlans() = viewModelScope.launch {
        when (val res = repo.listTravelPlans()) {
            is NetworkResult.Success -> _travelPlans.value = res.data
            is NetworkResult.Error -> reportLoadError(res)
        }
    }

    // Feature 19: Multi-Device Sync
    fun loadMultiDeviceOverview() = viewModelScope.launch {
        when (val res = repo.getMultiDeviceOverview()) {
            is NetworkResult.Success -> _multiDeviceOverview.value = res.data
            is NetworkResult.Error -> reportLoadError(res)
        }
    }

    fun triggerDeviceSync(policy: String = "SERVER_WINS") = viewModelScope.launch {
        startLoading()
        val deviceId = "${android.os.Build.MANUFACTURER}-${android.os.Build.MODEL}".take(60)
        when (val res = repo.pushDeviceSync(SyncPushRequestDto(deviceId, "ALL", policy))) {
            is NetworkResult.Success -> {
                ok("Synced ${res.data.syncedItemsCount} item(s) across account.")
                loadMultiDeviceOverview()
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    // Feature 20: Self-Diagnostics
    fun runSelfDiagnostics() = viewModelScope.launch {
        startLoading()
        val micOk =
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        val camOk =
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        val locOk = LocationAndAppHelper.hasLocationPermission(appContext)
        val notifOk = GoluNotificationListenerService.isNotificationAccessGranted(appContext)
        val netOk = DeviceTelemetry.isNetworkAvailable(appContext)

        when (val res = repo.runDiagnostics(DiagnosticCheckInputDto(micOk, camOk, locOk, notifOk, netOk))) {
            is NetworkResult.Success -> {
                _diagnosticReport.value = res.data
                ok("Self-diagnostics complete (${res.data.overallStatus}).")
            }
            is NetworkResult.Error -> fail(res)
        }
    }

    override fun onCleared() {
        recorderHelper.release()
        super.onCleared()
    }

    class Factory(
        private val appContext: Context,
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GoluV2ViewModel(
                appContext = appContext,
                repo = container.goluV2Repository,
                recorderHelper = container.audioRecorderHelper,
                voiceEngine = container.voiceEngine,
            ) as T
        }
    }
}
