package com.example.data.repository

import com.example.core.ApiErrorMapper
import com.example.core.BackendConfig
import com.example.core.BackendConfigStatus
import com.example.core.BackendEndpointInfo
import com.example.core.NetworkResult
import com.example.data.remote.GoluV2ApiService
import com.example.data.remote.dto.*

class GoluV2Repository(
    private val api: GoluV2ApiService,
    private val endpointProvider: () -> BackendEndpointInfo = { BackendConfig.evaluateConfigured() },
) {
    private suspend fun <T> callApi(block: suspend () -> T): NetworkResult<T> {
        val endpoint = endpointProvider()
        if (endpoint.status != BackendConfigStatus.READY_HTTPS &&
            endpoint.status != BackendConfigStatus.READY_EMULATOR_DEV
        ) {
            return NetworkResult.Error(
                message = endpoint.diagnosticMessage,
                causeCategory = "UNCONFIGURED",
            )
        }
        return runCatching { block() }.fold(
            onSuccess = { NetworkResult.Success(it) },
            onFailure = { err ->
                val mapped = ApiErrorMapper.map(err)
                NetworkResult.Error(mapped.message, mapped.causeCategory, mapped.httpCode)
            },
        )
    }

    // Phase 16
    suspend fun analyzeScreen(req: ScreenUnderstandRequestDto) = callApi { api.analyzeScreen(req) }
    suspend fun analyzeCamera(req: CameraAnalyzeRequestDto) = callApi { api.analyzeCamera(req) }
    suspend fun uploadDocument(req: DocumentUploadRequestDto) = callApi { api.uploadDocument(req) }
    suspend fun listDocuments(query: String? = null) = callApi { api.listDocuments(query) }
    suspend fun askDocument(id: String, req: DocumentQueryRequestDto) = callApi { api.askDocument(id, req) }
    suspend fun deleteDocument(id: String) = callApi { api.deleteDocument(id) }
    suspend fun analyzeNotification(req: NotificationIngestRequestDto) = callApi { api.analyzeNotification(req) }
    suspend fun getNotificationSummary() = callApi { api.getNotificationSummary() }
    suspend fun deleteNotificationInsight(id: String) = callApi { api.deleteNotificationInsight(id) }

    // Phase 17
    suspend fun listKnowledge(q: String? = null, cat: String? = null) = callApi { api.listKnowledge(q, cat) }
    suspend fun createKnowledge(req: KnowledgeNoteRequestDto) = callApi { api.createKnowledge(req) }
    suspend fun updateKnowledge(id: String, req: KnowledgeNoteRequestDto) = callApi { api.updateKnowledge(id, req) }
    suspend fun deleteKnowledge(id: String) = callApi { api.deleteKnowledge(id) }
    suspend fun translate(req: TranslateRequestDto) = callApi { api.translate(req) }
    suspend fun listTranslations() = callApi { api.listTranslations() }
    suspend fun deleteTranslation(id: String) = callApi { api.deleteTranslation(id) }
    suspend fun runResearch(req: ResearchRequestDto) = callApi { api.runResearch(req) }
    suspend fun listResearchHistory() = callApi { api.listResearchHistory() }
    suspend fun getOfflineManifest() = callApi { api.getOfflineManifest() }

    // Phase 18
    suspend fun suggestPlan(req: PlannerSuggestRequestDto) = callApi { api.suggestPlan(req) }
    suspend fun approvePlan(req: PlannerApproveRequestDto) = callApi { api.approvePlan(req) }
    suspend fun assistCoding(req: CodingAssistRequestDto) = callApi { api.assistCoding(req) }
    suspend fun listCodingHistory() = callApi { api.listCodingHistory() }
    suspend fun generateCreative(req: CreativeRequestDto) = callApi { api.generateCreative(req) }
    suspend fun listCreativeHistory() = callApi { api.listCreativeHistory() }
    suspend fun deleteCreative(id: String) = callApi { api.deleteCreative(id) }
    suspend fun createRecording(req: RecordingCreateRequestDto) = callApi { api.createRecording(req) }
    suspend fun listRecordings(q: String? = null) = callApi { api.listRecordings(q) }
    suspend fun updateRecording(id: String, req: RecordingUpdateRequestDto) = callApi { api.updateRecording(id, req) }
    suspend fun deleteRecording(id: String) = callApi { api.deleteRecording(id) }

    // Phase 19
    suspend fun getSmartHomeConfig() = callApi { api.getSmartHomeConfig() }
    suspend fun updateSmartHomeConfig(req: SmartHomeConfigRequestDto) = callApi { api.updateSmartHomeConfig(req) }
    suspend fun listSmartHomeDevices() = callApi { api.listSmartHomeDevices() }
    suspend fun addSmartHomeDevice(req: SmartHomeDeviceCreateRequestDto) = callApi { api.addSmartHomeDevice(req) }
    suspend fun commandSmartHomeDevice(id: String, req: SmartHomeDeviceCommandRequestDto) =
        callApi { api.commandSmartHomeDevice(id, req) }
    suspend fun deleteSmartHomeDevice(id: String) = callApi { api.deleteSmartHomeDevice(id) }
    suspend fun getExpenses(monthKey: String? = null) = callApi { api.getExpenses(monthKey) }
    suspend fun addExpense(req: ExpenseCreateRequestDto) = callApi { api.addExpense(req) }
    suspend fun setExpenseBudget(req: BudgetUpdateRequestDto) = callApi { api.setExpenseBudget(req) }
    suspend fun exportExpensesCsv(): NetworkResult<String> = callApi {
        api.exportExpensesCsv().use { it.string() }
    }
    suspend fun deleteExpense(id: String) = callApi { api.deleteExpense(id) }
    suspend fun getPersonality() = callApi { api.getPersonality() }
    suspend fun updatePersonality(req: PersonalityUpdateRequestDto) = callApi { api.updatePersonality(req) }
    suspend fun listAppShortcuts() = callApi { api.listAppShortcuts() }
    suspend fun upsertAppShortcut(req: AppShortcutUpsertRequestDto) = callApi { api.upsertAppShortcut(req) }
    suspend fun deleteAppShortcut(id: String) = callApi { api.deleteAppShortcut(id) }

    // Phase 20
    suspend fun explainStudyTopic(req: StudyAskRequestDto) = callApi { api.explainStudyTopic(req) }
    suspend fun listStudySessions() = callApi { api.listStudySessions() }
    suspend fun updateStudyProgress(id: String, pct: Int) =
        callApi { api.updateStudyProgress(id, StudyProgressUpdateRequestDto(pct)) }
    suspend fun createTravelPlan(req: TravelPlanRequestDto) = callApi { api.createTravelPlan(req) }
    suspend fun listTravelPlans() = callApi { api.listTravelPlans() }
    suspend fun getMultiDeviceOverview() = callApi { api.getMultiDeviceOverview() }
    suspend fun pushDeviceSync(req: SyncPushRequestDto) = callApi { api.pushDeviceSync(req) }
    suspend fun runDiagnostics(req: DiagnosticCheckInputDto) = callApi { api.runDiagnostics(req) }
}
