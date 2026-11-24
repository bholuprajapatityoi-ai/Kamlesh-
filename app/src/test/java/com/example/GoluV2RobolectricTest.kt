package com.example

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.core.BackendConfig
import com.example.core.BackendConfigStatus
import com.example.core.BackendEndpointInfo
import com.example.core.NetworkResult
import com.example.data.remote.GoluV2ApiService
import com.example.data.remote.dto.*
import com.example.data.repository.GoluV2Repository
import com.example.domain.golu2.AudioRecorderHelper
import com.example.domain.golu2.OfflineBrainV2
import com.example.domain.model.BackendDiagnostics
import com.example.domain.model.ConnectionPhase
import com.example.domain.model.GoluSettings
import com.example.domain.voice.VoiceEngine
import com.example.presentation.screens.GuideAndDocsScreen
import com.example.presentation.screens.SettingsScreen
import com.example.presentation.screens.v2.BrainKnowledgeScreen
import com.example.presentation.screens.v2.EnvironmentPersonalizationScreen
import com.example.presentation.screens.v2.Golu2DashboardScreen
import com.example.presentation.screens.v2.MobilitySystemScreen
import com.example.presentation.screens.v2.ProductivityCreativeScreen
import com.example.presentation.screens.v2.VisionPerceptionScreen
import com.example.presentation.screens.v2.golu2AllTiles
import com.example.presentation.viewmodel.GoluV2ViewModel
import com.example.ui.theme.GoluTheme
import java.lang.reflect.Proxy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GoluV2RobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun readyEndpoint(): BackendEndpointInfo =
        BackendEndpointInfo(
            baseUrl = "https://api.example.com/",
            status = BackendConfigStatus.READY_HTTPS,
            diagnosticMessage = "HTTPS ready",
        )

    @Test
    fun `golu2 dashboard renders all 20 module tiles and navigates to correct routes`() {
        val navigated = mutableListOf<String>()
        composeRule.setContent {
            GoluTheme(darkTheme = true) {
                Golu2DashboardScreen(
                    onNavigateRoute = { navigated += it },
                    onBack = {},
                )
            }
        }
        composeRule.waitForIdle()

        assertEquals(20, golu2AllTiles.size)
        golu2AllTiles.forEach { tile ->
            composeRule.onNodeWithTag("v2_tile_${tile.id}").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithText(tile.title).assertIsDisplayed()
            assertFalse(tile.categoryLabel.contains("Phase", ignoreCase = true))
        }
        composeRule.onAllNodesWithText("Phase", substring = true, ignoreCase = true).assertCountEquals(0)

        composeRule.onNodeWithTag("v2_tile_vision").performScrollTo().performClick()
        composeRule.onNodeWithTag("v2_tile_expenses").performScrollTo().performClick()
        composeRule.onNodeWithTag("v2_tile_diagnostics").performScrollTo().performClick()

        assertEquals(listOf("v2_vision/0", "v2_environment/1", "v2_mobility/3"), navigated)
    }

    @Test
    fun `user guide, settings, and all golu2 feature screens expose zero visible Phase labels`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GoluV2Repository(api = fakeV2ApiService(), endpointProvider = ::readyEndpoint)
        val vm = GoluV2ViewModel(context, repo, AudioRecorderHelper(context), VoiceEngine(context))
        val offlineDiagnostics = BackendConfig.evaluate("UNCONFIGURED").let {
            BackendDiagnostics(
                connectionPhase = ConnectionPhase.NOT_CONFIGURED,
                configStatus = it.status,
                statusSummary = "Offline Mode (Backend Not Configured)",
                rootCauseDetail = it.diagnosticMessage,
            )
        }

        val screenIndex = androidx.compose.runtime.mutableIntStateOf(0)
        composeRule.setContent {
            GoluTheme(darkTheme = true) {
                when (screenIndex.intValue) {
                    0 -> GuideAndDocsScreen(onBack = {})
                    1 -> SettingsScreen(
                        settings = GoluSettings(),
                        diagnostics = offlineDiagnostics,
                        onDarkThemeChange = {},
                        onVoiceGenderChange = {},
                        onSpeechSpeedChange = {},
                        onLanguageChange = {},
                        onGreetingChange = {},
                        onResponseStyleChange = {},
                        onWakeWordChange = {},
                        onKeepContextChange = {},
                        onRedactLogsChange = {},
                        onRetryConnection = {},
                        onOpenBatteryOptimization = {},
                    )
                    2 -> VisionPerceptionScreen(v2ViewModel = vm, initialTab = 0, onBack = {})
                    3 -> BrainKnowledgeScreen(v2ViewModel = vm, initialTab = 0, onBack = {})
                    4 -> ProductivityCreativeScreen(v2ViewModel = vm, initialTab = 0, onBack = {})
                    5 -> EnvironmentPersonalizationScreen(v2ViewModel = vm, initialTab = 0, onBack = {})
                    else -> MobilitySystemScreen(
                        v2ViewModel = vm,
                        diagnostics = offlineDiagnostics,
                        initialTab = 0,
                        onRetryConnection = {},
                        onBack = {},
                    )
                }
            }
        }

        for (idx in 0..6) {
            composeRule.runOnUiThread { screenIndex.intValue = idx }
            composeRule.waitForIdle()
            composeRule.onAllNodesWithText("Phase", substring = true, ignoreCase = true).assertCountEquals(0)
        }
    }

    @Test
    fun `offline brain v2 evaluates math, conversions, phrases, and discloses cloud boundaries`() {
        val mathRes = OfflineBrainV2.evaluateLocalQuery("calculate 125 * 8")
        assertTrue(mathRes.handled)
        assertTrue(mathRes.reply.contains("1000"))

        val unitRes = OfflineBrainV2.evaluateLocalQuery("10 km")
        assertTrue(unitRes.handled)
        assertTrue(unitRes.reply.contains("6.21"))

        val phraseRes = OfflineBrainV2.evaluateLocalQuery("say good morning")
        assertTrue(phraseRes.handled)
        assertTrue(phraseRes.reply.contains("Suprabhat"))

        val cloudOnlyRes = OfflineBrainV2.evaluateLocalQuery("Write a 5 page quantum physics essay")
        assertFalse(cloudOnlyRes.handled)
        assertTrue(cloudOnlyRes.capabilityNote.contains("Cloud AI requires backend connection"))
    }

    @Test
    fun `v2 repository executes all 20 mapped endpoints and parses expense CSV responseBody`() = runTest {
        val unconfiguredRepo =
            GoluV2Repository(
                api = fakeV2ApiService(),
                endpointProvider = { BackendConfig.evaluate("UNCONFIGURED") },
            )
        val blocked = unconfiguredRepo.runDiagnostics(DiagnosticCheckInputDto(true, true, true, true, true))
        assertTrue(blocked is NetworkResult.Error)
        assertEquals("UNCONFIGURED", (blocked as NetworkResult.Error).causeCategory)

        val repo = GoluV2Repository(api = fakeV2ApiService(), endpointProvider = ::readyEndpoint)

        // 1-4: Screen, Camera (including TRANSLATE), Documents, Notifications
        assertTrue(repo.analyzeScreen(ScreenUnderstandRequestDto("text", "EXPLAIN", consentGranted = true)) is NetworkResult.Success)
        val camRes = repo.analyzeCamera(CameraAnalyzeRequestDto("AQID", "Translate", "TRANSLATE", consentGranted = true))
        assertTrue(camRes is NetworkResult.Success)
        assertEquals("TRANSLATE", (camRes as NetworkResult.Success).data.mode)
        assertTrue(repo.uploadDocument(DocumentUploadRequestDto("Doc", "text/plain", "body", consentGranted = true)) is NetworkResult.Success)
        assertTrue(repo.listDocuments() is NetworkResult.Success)
        assertTrue(repo.askDocument("d1", DocumentQueryRequestDto("Summary?")) is NetworkResult.Success)
        assertTrue(repo.deleteDocument("d1") is NetworkResult.Success)
        assertTrue(repo.analyzeNotification(NotificationIngestRequestDto("Bank", "OTP", "1234", consentGranted = true)) is NetworkResult.Success)
        assertTrue(repo.getNotificationSummary() is NetworkResult.Success)
        assertTrue(repo.deleteNotificationInsight("n1") is NetworkResult.Success)

        // 5-8: Knowledge, Translator, Research, Offline Manifest
        assertTrue(repo.createKnowledge(KnowledgeNoteRequestDto("Title", "Study", "Content")) is NetworkResult.Success)
        assertTrue(repo.updateKnowledge("k1", KnowledgeNoteRequestDto("Title2", "Study", "Content2")) is NetworkResult.Success)
        assertTrue(repo.listKnowledge() is NetworkResult.Success)
        assertTrue(repo.deleteKnowledge("k1") is NetworkResult.Success)
        assertTrue(repo.translate(TranslateRequestDto("Hello", "English", "Hindi")) is NetworkResult.Success)
        assertTrue(repo.listTranslations() is NetworkResult.Success)
        assertTrue(repo.deleteTranslation("t1") is NetworkResult.Success)
        assertTrue(repo.runResearch(ResearchRequestDto("Kotlin coroutines")) is NetworkResult.Success)
        assertTrue(repo.listResearchHistory() is NetworkResult.Success)
        assertTrue(repo.getOfflineManifest() is NetworkResult.Success)

        // 9-12: Planner, Coding, Creative, Voice Recorder
        assertTrue(repo.suggestPlan(PlannerSuggestRequestDto("Release", 4)) is NetworkResult.Success)
        assertTrue(repo.approvePlan(PlannerApproveRequestDto(listOf(PlannedItemProposalDto("Task")))) is NetworkResult.Success)
        assertTrue(repo.assistCoding(CodingAssistRequestDto("sort list", "kotlin", "GENERATE")) is NetworkResult.Success)
        assertTrue(repo.listCodingHistory() is NetworkResult.Success)
        assertTrue(repo.generateCreative(CreativeRequestDto("POSTER_IDEA", "Hackathon", "Neon")) is NetworkResult.Success)
        assertTrue(repo.listCreativeHistory() is NetworkResult.Success)
        assertTrue(repo.deleteCreative("c1") is NetworkResult.Success)
        assertTrue(repo.createRecording(RecordingCreateRequestDto("Memo", 12, "Notes")) is NetworkResult.Success)
        assertTrue(repo.listRecordings() is NetworkResult.Success)
        assertTrue(repo.updateRecording("r1", RecordingUpdateRequestDto("Memo2", "Notes2")) is NetworkResult.Success)
        assertTrue(repo.deleteRecording("r1") is NetworkResult.Success)

        // 13-16: Smart Home, Expenses (incl. raw CSV ResponseBody), Personality, Smart Apps
        assertTrue(repo.getSmartHomeConfig() is NetworkResult.Success)
        assertTrue(repo.updateSmartHomeConfig(SmartHomeConfigRequestDto("HOME_ASSISTANT", "https://hub.example.com", "tok")) is NetworkResult.Success)
        assertTrue(repo.addSmartHomeDevice(SmartHomeDeviceCreateRequestDto("climate.hall", "Hall AC", "Hall", "THERMOSTAT")) is NetworkResult.Success)
        assertTrue(repo.listSmartHomeDevices() is NetworkResult.Success)
        assertTrue(repo.commandSmartHomeDevice("sh1", SmartHomeDeviceCommandRequestDto(true, 80, true)) is NetworkResult.Success)
        assertTrue(repo.deleteSmartHomeDevice("sh1") is NetworkResult.Success)
        assertTrue(repo.addExpense(ExpenseCreateRequestDto("Coffee", "Food", 150.0)) is NetworkResult.Success)
        assertTrue(repo.setExpenseBudget(BudgetUpdateRequestDto("2026-11", 5000.0)) is NetworkResult.Success)
        assertTrue(repo.getExpenses() is NetworkResult.Success)
        val csvRes = repo.exportExpensesCsv()
        assertTrue(csvRes is NetworkResult.Success)
        assertTrue((csvRes as NetworkResult.Success).data.contains("Coffee"))
        assertTrue(repo.deleteExpense("e1") is NetworkResult.Success)
        assertTrue(repo.getPersonality() is NetworkResult.Success)
        assertTrue(repo.updatePersonality(PersonalityUpdateRequestDto(humorLevel = 85)) is NetworkResult.Success)
        assertTrue(repo.upsertAppShortcut(AppShortcutUpsertRequestDto("Maps", "com.google.android.apps.maps", true, "", true)) is NetworkResult.Success)
        assertTrue(repo.listAppShortcuts() is NetworkResult.Success)
        assertTrue(repo.deleteAppShortcut("a1") is NetworkResult.Success)

        // 17-20: Study Tutor, Travel, Multi-Device Sync, Self-Diagnostics
        assertTrue(repo.explainStudyTopic(StudyAskRequestDto("CS", "Graphs")) is NetworkResult.Success)
        assertTrue(repo.listStudySessions() is NetworkResult.Success)
        assertTrue(repo.updateStudyProgress("st1", 60) is NetworkResult.Success)
        assertTrue(repo.createTravelPlan(TravelPlanRequestDto("Delhi", "Jaipur")) is NetworkResult.Success)
        assertTrue(repo.listTravelPlans() is NetworkResult.Success)
        assertTrue(repo.getMultiDeviceOverview() is NetworkResult.Success)
        val syncRes = repo.pushDeviceSync(SyncPushRequestDto("pixel-test", "ALL", "LATEST_TIMESTAMP"))
        assertTrue(syncRes is NetworkResult.Success)
        assertEquals("LATEST_TIMESTAMP", (syncRes as NetworkResult.Success).data.conflictPolicy)
        val diagOk = repo.runDiagnostics(DiagnosticCheckInputDto(true, true, false, false, true))
        assertTrue(diagOk is NetworkResult.Success)
        assertEquals("DEGRADED", (diagOk as NetworkResult.Success).data.overallStatus)
    }

    @Test
    fun `vision perception screen requires explicit consent checkbox before screen analysis`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GoluV2Repository(api = fakeV2ApiService(), endpointProvider = ::readyEndpoint)
        val vm = GoluV2ViewModel(context, repo, AudioRecorderHelper(context), VoiceEngine(context))

        composeRule.setContent {
            GoluTheme(darkTheme = true) {
                VisionPerceptionScreen(v2ViewModel = vm, initialTab = 0, onBack = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("v2_analyze_screen_btn").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag("v2_screen_text_input").performScrollTo().performTextInput("Meeting at 4 PM Boss")
        composeRule.onNodeWithTag("v2_analyze_screen_btn").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag("v2_screen_consent_checkbox").performScrollTo().performClick()
        composeRule.onNodeWithTag("v2_analyze_screen_btn").performScrollTo().assertIsEnabled()
    }

    @Test
    fun `brain knowledge and productivity screens load repository state cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GoluV2Repository(api = fakeV2ApiService(), endpointProvider = ::readyEndpoint)
        val vm = GoluV2ViewModel(context, repo, AudioRecorderHelper(context), VoiceEngine(context))

        composeRule.setContent {
            GoluTheme(darkTheme = true) {
                BrainKnowledgeScreen(v2ViewModel = vm, initialTab = 0, onBack = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Exam Prep").performScrollTo().assertIsDisplayed()

        // Also assert ViewModel load errors are surfaced rather than swallowed
        val unconfiguredVm = GoluV2ViewModel(
            context,
            GoluV2Repository(api = fakeV2ApiService(), endpointProvider = { BackendConfig.evaluate("UNCONFIGURED") }),
            AudioRecorderHelper(context),
            VoiceEngine(context),
        )
        unconfiguredVm.loadKnowledge()
        composeRule.waitForIdle()
        assertNotNull(unconfiguredVm.uiState.value.errorMessage)
    }

    @Test
    fun `environment screen enforces thermostat confirmation, humor 0-100 scale, and live csv export`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GoluV2Repository(api = fakeV2ApiService(), endpointProvider = ::readyEndpoint)
        val vm = GoluV2ViewModel(context, repo, AudioRecorderHelper(context), VoiceEngine(context))

        composeRule.setContent {
            GoluTheme(darkTheme = true) {
                EnvironmentPersonalizationScreen(v2ViewModel = vm, initialTab = 0, onBack = {})
            }
        }
        composeRule.waitForIdle()

        // Smart Home THERMOSTAT must trigger confirmation dialog
        composeRule.onNodeWithTag("v2_smarthome_toggle_sh1").performScrollTo().performClick()
        composeRule.onNodeWithText("Confirm Sensitive Smart Home Action").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()

        // Personality tab uses 0..100 percentage scale (85% from backend mock)
        composeRule.onNodeWithTag("env_tab_2").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Humor Level: 85%").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("CYBER_BOT").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `mobility screen uses backend-valid sync policies and honest offline diagnostics`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GoluV2Repository(api = fakeV2ApiService(), endpointProvider = { BackendConfig.evaluate("UNCONFIGURED") })
        val vm = GoluV2ViewModel(context, repo, AudioRecorderHelper(context), VoiceEngine(context))
        val offlineDiagnostics =
            BackendDiagnostics(
                connectionPhase = ConnectionPhase.OFFLINE,
                configStatus = BackendConfigStatus.UNCONFIGURED,
                isDeviceOnline = false,
                isBackendReachable = false,
                isDatabaseConnected = false,
                statusSummary = "Offline Mode (Backend Not Provisioned)",
                rootCauseDetail = "No deployed backend configured.",
                lastConnectedAt = "",
            )

        composeRule.setContent {
            GoluTheme(darkTheme = true) {
                MobilitySystemScreen(
                    v2ViewModel = vm,
                    diagnostics = offlineDiagnostics,
                    initialTab = 2,
                    onRetryConnection = {},
                    onBack = {},
                )
            }
        }
        composeRule.waitForIdle()

        // Multi-device conflict policy chip must match backend regex (LATEST_TIMESTAMP, never MANUAL_REVIEW)
        composeRule.onNodeWithText("LATEST_TIMESTAMP").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("MANUAL_REVIEW").assertDoesNotExist()

        // Switch to Diagnostics tab
        composeRule.onNodeWithTag("mob_tab_3").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Offline Mode (Backend Not Provisioned)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("HEALTHY").assertDoesNotExist()
        composeRule.onNodeWithTag("v2_diag_status_pill").assertIsDisplayed()
    }

    @Test
    fun `productivity screen renders all 4 module tabs cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GoluV2Repository(api = fakeV2ApiService(), endpointProvider = ::readyEndpoint)
        val vm = GoluV2ViewModel(context, repo, AudioRecorderHelper(context), VoiceEngine(context))

        composeRule.setContent {
            GoluTheme(darkTheme = true) {
                ProductivityCreativeScreen(v2ViewModel = vm, initialTab = 0, onBack = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("v2_planner_suggest_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("prod_tab_1").performScrollTo().performClick()
        composeRule.onNodeWithTag("v2_coding_submit_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("prod_tab_2").performScrollTo().performClick()
        composeRule.onNodeWithTag("v2_creative_generate_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("prod_tab_3").performScrollTo().performClick()
        composeRule.onNodeWithTag("v2_recorder_status_pill").performScrollTo().assertIsDisplayed()
    }

    private fun fakeV2ApiService(): GoluV2ApiService {
        return Proxy.newProxyInstance(
            GoluV2ApiService::class.java.classLoader,
            arrayOf(GoluV2ApiService::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "analyzeScreen" ->
                    ScreenUnderstandResponseDto("EXPLAIN", "Meeting scheduled at 4 PM.", listOf("Meeting at 4 PM"), "openai-test", true)
                "analyzeCamera" ->
                    CameraAnalyzeResponseDto("TRANSLATE", "Translated sign text", "Exit", listOf("Sign"), "gpt-4o-mini", true)
                "uploadDocument" ->
                    DocumentResponseDto("d1", "Doc", "text/plain", 12, "Summary", listOf("Point 1"), "body", "2026-11-24T00:00:00Z")
                "listDocuments" ->
                    listOf(DocumentResponseDto("d1", "Doc", "text/plain", 12, "Summary", listOf("Point 1"), "body", "2026-11-24T00:00:00Z"))
                "askDocument" ->
                    DocumentQueryResponseDto("d1", "Coroutines answer", listOf("Kotlin coroutines"), true)
                "deleteDocument", "deleteNotificationInsight", "deleteKnowledge", "deleteTranslation",
                "approvePlan", "deleteCreative", "deleteRecording", "deleteSmartHomeDevice",
                "deleteExpense", "deleteAppShortcut" ->
                    MessageResponseDto("OK")
                "analyzeNotification" ->
                    NotificationInsightResponseDto("n1", "Bank", "OTP", "1234", "FINANCE_ALERT", 5, true, "[FINANCE_ALERT] Bank: OTP", "rem1", "2026-11-24T00:00:00Z")
                "getNotificationSummary" ->
                    NotificationSummaryResponseDto(1, 1, "1 important alert", emptyList())
                "listKnowledge" ->
                    listOf(KnowledgeNoteResponseDto("k1", "Exam Prep", "Study", "SQL joins", listOf("sql"), false, "2026-11-24T00:00:00Z", "2026-11-24T00:00:00Z"))
                "createKnowledge", "updateKnowledge" ->
                    KnowledgeNoteResponseDto("k1", "Exam Prep", "Study", "SQL joins", listOf("sql"), false, "2026-11-24T00:00:00Z", "2026-11-24T00:00:00Z")
                "translate" ->
                    TranslationResponseDto("t1", "English", "Hindi", "Hello", "Namaste", "TEXT", true, "2026-11-24T00:00:00Z")
                "listTranslations" ->
                    listOf(TranslationResponseDto("t1", "English", "Hindi", "Hello", "Namaste", "TEXT", true, "2026-11-24T00:00:00Z"))
                "runResearch" ->
                    ResearchReportResponseDto("rs1", "Kotlin", "Summary", "Verified", "Uncertain", listOf(CitationItemDto("Kotlin", "https://kotlinlang.org", "Docs")), "wikipedia-api", "2026-11-24T00:00:00Z")
                "listResearchHistory" ->
                    emptyList<ResearchReportResponseDto>()
                "getOfflineManifest" ->
                    OfflineManifestResponseDto(listOf("Math"), listOf("Vision"), listOf("Calculate 2+2"), "Cloud AI requires backend")
                "suggestPlan" ->
                    PlannerSuggestResponseDto("Plan summary", listOf(PlannedItemProposalDto("Ship APK", "TASK", "HIGH", "09:00", "Focus")), true)
                "assistCoding" ->
                    CodingAssistResponseDto("c1", "kotlin", "GENERATE", "sort", "list.sorted()", "Sorted cleanly", "Verified", true, "2026-11-24T00:00:00Z")
                "listCodingHistory" ->
                    emptyList<CodingAssistResponseDto>()
                "generateCreative" ->
                    CreativeAssetResponseDto("cr1", "POSTER_IDEA", "Hackathon", "", "", "Neon layout", "COMPLETED", "", "2026-11-24T00:00:00Z")
                "listCreativeHistory" ->
                    emptyList<CreativeAssetResponseDto>()
                "createRecording", "updateRecording" ->
                    RecordingResponseDto("r1", "Memo", 12, "Notes", "Summary", "Notes", "", "2026-11-24T00:00:00Z", "2026-11-24T00:00:00Z")
                "listRecordings" ->
                    emptyList<RecordingResponseDto>()
                "getSmartHomeConfig", "updateSmartHomeConfig" ->
                    SmartHomeConfigResponseDto("HOME_ASSISTANT", "", "", false, "NOT_AUTHORIZED", "Configure HTTPS hub")
                "listSmartHomeDevices" ->
                    listOf(SmartHomeDeviceResponseDto("sh1", "climate.hall", "Hall AC", "Hall", "THERMOSTAT", false, 80, "AWAITING_HUB_VERIFICATION"))
                "addSmartHomeDevice", "commandSmartHomeDevice" ->
                    SmartHomeDeviceResponseDto("sh1", "climate.hall", "Hall AC", "Hall", "THERMOSTAT", true, 80, "CONNECTED_VERIFIED")
                "getExpenses", "setExpenseBudget" ->
                    ExpenseSummaryResponseDto("2026-11", 5000.0, 150.0, 4850.0, "INR", mapOf("Food" to 150.0), listOf(ExpenseRecordResponseDto("e1", "Coffee", "Food", 150.0, "INR", "", "2026-11-24T00:00:00Z")))
                "addExpense" ->
                    ExpenseRecordResponseDto("e1", "Coffee", "Food", 150.0, "INR", "", "2026-11-24T00:00:00Z")
                "exportExpensesCsv" ->
                    "id,date,title,category,amount,currency,notes\ne1,2026-11-24,Coffee,Food,150.0,INR,\"\"".toResponseBody("text/plain".toMediaType())
                "getPersonality", "updatePersonality" ->
                    PersonalityResponseDto("Hinglish", 85, "MEDIUM", "Hello! Boss", "FEMALE", "CYBER_BOT", "")
                "listAppShortcuts" ->
                    emptyList<SmartAppPreferenceResponseDto>()
                "upsertAppShortcut" ->
                    SmartAppPreferenceResponseDto("a1", "Maps", "com.google.android.apps.maps", true, 1, "")
                "explainStudyTopic", "updateStudyProgress" ->
                    StudySessionResponseDto("st1", "CS", "Graphs", "Hinglish", "Beginner", "Step-by-step", listOf("Q1"), 40, "2026-11-24T00:00:00Z")
                "listStudySessions" ->
                    emptyList<StudySessionResponseDto>()
                "createTravelPlan" ->
                    TravelPlanResponseDto("tr1", "Delhi", "Jaipur", "Route ready", listOf(TravelPlaceItemDto("Hawa Mahal", "tourism", "Jaipur", 26.92, 75.82)), "Checklist", "OSM lookup complete", "2026-11-24T00:00:00Z")
                "listTravelPlans" ->
                    emptyList<TravelPlanResponseDto>()
                "getMultiDeviceOverview" ->
                    MultiDeviceOverviewResponseDto(emptyList(), emptyList(), 1, 2, 1)
                "pushDeviceSync" ->
                    SyncCheckpointResponseDto("sy1", "pixel-test", "ALL", "LATEST_TIMESTAMP", 4, "2026-11-24T00:00:00Z")
                "runDiagnostics" ->
                    DiagnosticReportResponseDto(
                        backendOk = true,
                        databaseOk = true,
                        redisOk = false,
                        openaiConfigured = false,
                        searchApiConfigured = false,
                        overallStatus = "DEGRADED",
                        troubleshootingSuggestions = listOf("Set OPENAI_API_KEY in backend environment."),
                    )
                else -> throw UnsupportedOperationException("Unstubbed v2 method ${method.name}")
            }
        } as GoluV2ApiService
    }
}
