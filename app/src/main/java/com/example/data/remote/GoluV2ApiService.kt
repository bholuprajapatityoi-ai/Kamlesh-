package com.example.data.remote

import com.example.data.remote.dto.*
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GoluV2ApiService {

    // Phase 16: Vision & Perception
    @POST("api/v2/vision/analyze")
    suspend fun analyzeScreen(@Body request: ScreenUnderstandRequestDto): ScreenUnderstandResponseDto

    @POST("api/v2/camera/analyze")
    suspend fun analyzeCamera(@Body request: CameraAnalyzeRequestDto): CameraAnalyzeResponseDto

    @POST("api/v2/documents")
    suspend fun uploadDocument(@Body request: DocumentUploadRequestDto): DocumentResponseDto

    @GET("api/v2/documents")
    suspend fun listDocuments(@Query("q") query: String? = null): List<DocumentResponseDto>

    @POST("api/v2/documents/{id}/ask")
    suspend fun askDocument(
        @Path("id") id: String,
        @Body request: DocumentQueryRequestDto,
    ): DocumentQueryResponseDto

    @DELETE("api/v2/documents/{id}")
    suspend fun deleteDocument(@Path("id") id: String): MessageResponseDto

    @POST("api/v2/notifications/analyze")
    suspend fun analyzeNotification(@Body request: NotificationIngestRequestDto): NotificationInsightResponseDto

    @GET("api/v2/notifications/summary")
    suspend fun getNotificationSummary(): NotificationSummaryResponseDto

    @DELETE("api/v2/notifications/{id}")
    suspend fun deleteNotificationInsight(@Path("id") id: String): MessageResponseDto

    // Phase 17: Brain & Knowledge
    @GET("api/v2/knowledge")
    suspend fun listKnowledge(
        @Query("q") query: String? = null,
        @Query("category") category: String? = null,
    ): List<KnowledgeNoteResponseDto>

    @POST("api/v2/knowledge")
    suspend fun createKnowledge(@Body request: KnowledgeNoteRequestDto): KnowledgeNoteResponseDto

    @PUT("api/v2/knowledge/{id}")
    suspend fun updateKnowledge(
        @Path("id") id: String,
        @Body request: KnowledgeNoteRequestDto,
    ): KnowledgeNoteResponseDto

    @DELETE("api/v2/knowledge/{id}")
    suspend fun deleteKnowledge(@Path("id") id: String): MessageResponseDto

    @POST("api/v2/translator/translate")
    suspend fun translate(@Body request: TranslateRequestDto): TranslationResponseDto

    @GET("api/v2/translator/history")
    suspend fun listTranslations(): List<TranslationResponseDto>

    @DELETE("api/v2/translator/history/{id}")
    suspend fun deleteTranslation(@Path("id") id: String): MessageResponseDto

    @POST("api/v2/research/query")
    suspend fun runResearch(@Body request: ResearchRequestDto): ResearchReportResponseDto

    @GET("api/v2/research/history")
    suspend fun listResearchHistory(): List<ResearchReportResponseDto>

    @GET("api/v2/offline/manifest")
    suspend fun getOfflineManifest(): OfflineManifestResponseDto

    // Phase 18: Productivity & Creative
    @POST("api/v2/planner/suggest")
    suspend fun suggestPlan(@Body request: PlannerSuggestRequestDto): PlannerSuggestResponseDto

    @POST("api/v2/planner/approve")
    suspend fun approvePlan(@Body request: PlannerApproveRequestDto): MessageResponseDto

    @POST("api/v2/coding/assist")
    suspend fun assistCoding(@Body request: CodingAssistRequestDto): CodingAssistResponseDto

    @GET("api/v2/coding/history")
    suspend fun listCodingHistory(): List<CodingAssistResponseDto>

    @POST("api/v2/creative/generate")
    suspend fun generateCreative(@Body request: CreativeRequestDto): CreativeAssetResponseDto

    @GET("api/v2/creative/history")
    suspend fun listCreativeHistory(): List<CreativeAssetResponseDto>

    @DELETE("api/v2/creative/{id}")
    suspend fun deleteCreative(@Path("id") id: String): MessageResponseDto

    @POST("api/v2/recorder")
    suspend fun createRecording(@Body request: RecordingCreateRequestDto): RecordingResponseDto

    @GET("api/v2/recorder")
    suspend fun listRecordings(@Query("q") query: String? = null): List<RecordingResponseDto>

    @PUT("api/v2/recorder/{id}")
    suspend fun updateRecording(
        @Path("id") id: String,
        @Body request: RecordingUpdateRequestDto,
    ): RecordingResponseDto

    @DELETE("api/v2/recorder/{id}")
    suspend fun deleteRecording(@Path("id") id: String): MessageResponseDto

    // Phase 19: Smart Environment & Personalization
    @GET("api/v2/smart-home/config")
    suspend fun getSmartHomeConfig(): SmartHomeConfigResponseDto

    @PUT("api/v2/smart-home/config")
    suspend fun updateSmartHomeConfig(@Body request: SmartHomeConfigRequestDto): SmartHomeConfigResponseDto

    @GET("api/v2/smart-home/devices")
    suspend fun listSmartHomeDevices(): List<SmartHomeDeviceResponseDto>

    @POST("api/v2/smart-home/devices")
    suspend fun addSmartHomeDevice(@Body request: SmartHomeDeviceCreateRequestDto): SmartHomeDeviceResponseDto

    @POST("api/v2/smart-home/devices/{id}/command")
    suspend fun commandSmartHomeDevice(
        @Path("id") id: String,
        @Body request: SmartHomeDeviceCommandRequestDto,
    ): SmartHomeDeviceResponseDto

    @DELETE("api/v2/smart-home/devices/{id}")
    suspend fun deleteSmartHomeDevice(@Path("id") id: String): MessageResponseDto

    @GET("api/v2/expenses")
    suspend fun getExpenses(@Query("month_key") monthKey: String? = null): ExpenseSummaryResponseDto

    @POST("api/v2/expenses")
    suspend fun addExpense(@Body request: ExpenseCreateRequestDto): ExpenseRecordResponseDto

    @PUT("api/v2/expenses/budget")
    suspend fun setExpenseBudget(@Body request: BudgetUpdateRequestDto): ExpenseSummaryResponseDto

    @GET("api/v2/expenses/export")
    suspend fun exportExpensesCsv(): ResponseBody

    @DELETE("api/v2/expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: String): MessageResponseDto

    @GET("api/v2/personality")
    suspend fun getPersonality(): PersonalityResponseDto

    @PUT("api/v2/personality")
    suspend fun updatePersonality(@Body request: PersonalityUpdateRequestDto): PersonalityResponseDto

    @GET("api/v2/apps/shortcuts")
    suspend fun listAppShortcuts(): List<SmartAppPreferenceResponseDto>

    @POST("api/v2/apps/shortcuts")
    suspend fun upsertAppShortcut(@Body request: AppShortcutUpsertRequestDto): SmartAppPreferenceResponseDto

    @DELETE("api/v2/apps/shortcuts/{id}")
    suspend fun deleteAppShortcut(@Path("id") id: String): MessageResponseDto

    // Phase 20: Learning, Mobility & System
    @POST("api/v2/study/explain")
    suspend fun explainStudyTopic(@Body request: StudyAskRequestDto): StudySessionResponseDto

    @GET("api/v2/study/sessions")
    suspend fun listStudySessions(): List<StudySessionResponseDto>

    @PUT("api/v2/study/sessions/{id}/progress")
    suspend fun updateStudyProgress(
        @Path("id") id: String,
        @Body request: StudyProgressUpdateRequestDto,
    ): StudySessionResponseDto

    @POST("api/v2/travel/plan")
    suspend fun createTravelPlan(@Body request: TravelPlanRequestDto): TravelPlanResponseDto

    @GET("api/v2/travel/history")
    suspend fun listTravelPlans(): List<TravelPlanResponseDto>

    @GET("api/v2/devices/sync")
    suspend fun getMultiDeviceOverview(): MultiDeviceOverviewResponseDto

    @POST("api/v2/devices/sync")
    suspend fun pushDeviceSync(@Body request: SyncPushRequestDto): SyncCheckpointResponseDto

    @POST("api/v2/diagnostics/run")
    suspend fun runDiagnostics(@Body request: DiagnosticCheckInputDto): DiagnosticReportResponseDto
}
