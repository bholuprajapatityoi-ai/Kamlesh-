package com.example.domain.ai

import com.example.core.ApiErrorMapper
import com.example.core.BackendConfig
import com.example.core.BackendConfigStatus
import com.example.core.BackendEndpointInfo
import com.example.data.remote.GoluApiService
import com.example.data.remote.dto.AiChatRequestDto
import com.example.data.remote.dto.ClearContextRequestDto
import com.example.domain.model.GoluSettings
import com.example.domain.model.IntentType
import com.example.domain.model.RoutingTarget
import java.util.UUID

data class HybridAiResult(
    val reply: String,
    val routingTarget: RoutingTarget,
    val intentType: IntentType,
    val isError: Boolean = false,
    val requiresConfirmation: Boolean = false,
    val pendingMemoryContent: String? = null,
    val pendingMemoryCategory: String? = null,
    val pendingCallTarget: String? = null,
    val pendingWhatsAppRecipient: String? = null,
    val pendingWhatsAppDraft: String? = null,
)

/**
 * Phase 6–10 Hybrid AI Router:
 * 1. Classifies intent locally.
 * 2. Requires confirmation for sensitive actions, memory saving, calls, and WhatsApp drafts.
 * 3. Routes offline commands & native Android actions locally.
 * 4. Routes knowledge/explanations/daily life DB queries/chat to centralized FastAPI backend.
 */
class HybridAiRouter(
    private val api: GoluApiService,
    private val offlineCommandEngine: OfflineCommandEngine,
    private val endpointProvider: () -> BackendEndpointInfo = { BackendConfig.evaluateConfigured() },
) {
    private var temporarySessionId: String = UUID.randomUUID().toString()
    private val temporaryTurns = mutableListOf<Pair<String, String>>()

    suspend fun processInput(
        userText: String,
        settings: GoluSettings,
        confirmedSensitive: Boolean = false,
    ): HybridAiResult {
        val intent = IntentClassifier.classify(userText, hasPriorContext = temporaryTurns.isNotEmpty())

        if (intent == IntentType.SENSITIVE_ACTION && !confirmedSensitive) {
            return HybridAiResult(
                reply =
                    "Boss, yeh sensitive action lag raha hai (\"$userText\"). Kya aap confirm karte hain ki main aage badhun?",
                routingTarget = RoutingTarget.OFFLINE_ENGINE,
                intentType = intent,
                requiresConfirmation = true,
            )
        }

        // Try local offline command / Android native action first
        val offlineRes = offlineCommandEngine.tryExecute(userText, intent, settings, confirmedSensitive)
        if (offlineRes.handled) {
            recordTemporaryTurn(userText, offlineRes.reply, settings.keepSessionContext)
            return HybridAiResult(
                reply = offlineRes.reply,
                routingTarget = offlineRes.target,
                intentType = intent,
                requiresConfirmation = offlineRes.requiresConfirmation,
                pendingMemoryContent = offlineRes.pendingMemoryContent,
                pendingMemoryCategory = offlineRes.pendingMemoryCategory,
                pendingCallTarget = offlineRes.pendingCallTarget,
                pendingWhatsAppRecipient = offlineRes.pendingWhatsAppRecipient,
                pendingWhatsAppDraft = offlineRes.pendingWhatsAppDraft,
            )
        }

        // Route to Online AI Backend (FastAPI -> OpenAI / Daily Life DB)
        val endpoint = endpointProvider()
        if (endpoint.status != BackendConfigStatus.READY_HTTPS &&
            endpoint.status != BackendConfigStatus.READY_EMULATOR_DEV
        ) {
            return HybridAiResult(
                reply =
                    "Boss, yeh sawal Online AI ke liye hai, lekin backend abhi available nahi hai. Offline mein main time, date, battery, alarms, reminders, volume/settings aur apps open kar sakta hoon.",
                routingTarget = RoutingTarget.ONLINE_AI_BACKEND,
                intentType = intent,
                isError = true,
            )
        }

        return runCatching {
            val sessionForCall = if (settings.keepSessionContext) temporarySessionId else null
            val res =
                api.chat(
                    AiChatRequestDto(
                        message = userText,
                        language = settings.language.label,
                        responseStyle = settings.responseStyle.label,
                        sessionId = sessionForCall,
                    )
                )
            recordTemporaryTurn(userText, res.reply, settings.keepSessionContext)
            HybridAiResult(
                reply = res.reply,
                routingTarget = RoutingTarget.ONLINE_AI_BACKEND,
                intentType = intent,
                requiresConfirmation = res.requiresConfirmation,
            )
        }.getOrElse { throwable ->
            val mapped = ApiErrorMapper.map(throwable)
            val friendlyMsg =
                when (mapped.httpCode) {
                    401 ->
                        "Boss, Online AI use karne ke liye Profile tab mein sign in karna zaroori hai (401 Unauthorized)."
                    else ->
                        "Online AI unreachable: ${mapped.message}. Offline commands abhi bhi kaam kar rahe hain, Boss!"
                }
            HybridAiResult(
                reply = friendlyMsg,
                routingTarget = RoutingTarget.ONLINE_AI_BACKEND,
                intentType = intent,
                isError = true,
            )
        }
    }

    private fun recordTemporaryTurn(user: String, assistant: String, keepContext: Boolean) {
        if (!keepContext) return
        temporaryTurns.add(user to assistant)
        if (temporaryTurns.size > 12) {
            temporaryTurns.removeAt(0)
        }
    }

    suspend fun clearTemporaryContext() {
        temporaryTurns.clear()
        val oldSession = temporarySessionId
        temporarySessionId = UUID.randomUUID().toString()
        val endpoint = endpointProvider()
        if (endpoint.status == BackendConfigStatus.READY_HTTPS ||
            endpoint.status == BackendConfigStatus.READY_EMULATOR_DEV
        ) {
            runCatching { api.clearAiContext(ClearContextRequestDto(sessionId = oldSession)) }
        }
    }
}
