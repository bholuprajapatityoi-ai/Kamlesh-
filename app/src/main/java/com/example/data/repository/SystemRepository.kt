package com.example.data.repository

import android.content.Context
import com.example.core.ApiErrorMapper
import com.example.core.BackendConfig
import com.example.core.BackendConfigStatus
import com.example.core.BackendEndpointInfo
import com.example.core.DeviceTelemetry
import com.example.data.remote.GoluApiService
import com.example.data.remote.dto.DatabaseHealthResponseDto
import com.example.data.remote.dto.HealthResponseDto
import com.example.domain.model.BackendDiagnostics
import com.example.domain.model.ConnectionPhase
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class SystemRepository(
    private val context: Context,
    private val api: GoluApiService,
    private val endpointProvider: () -> BackendEndpointInfo = { BackendConfig.evaluateConfigured() },
    private val networkOnlineProvider: () -> Boolean = { DeviceTelemetry.isNetworkAvailable(context) },
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val healthAdapter = moshi.adapter(HealthResponseDto::class.java)
    private val dbHealthAdapter = moshi.adapter(DatabaseHealthResponseDto::class.java)

    suspend fun checkDiagnostics(previousLastConnectedAt: String = ""): BackendDiagnostics {
        val endpoint = endpointProvider()
        val deviceOnline = networkOnlineProvider()
        val checkedAt = DeviceTelemetry.formattedTime()

        if (endpoint.status != BackendConfigStatus.READY_HTTPS &&
            endpoint.status != BackendConfigStatus.READY_EMULATOR_DEV
        ) {
            val summary =
                when (endpoint.status) {
                    BackendConfigStatus.INVALID_LOCALHOST,
                    BackendConfigStatus.INSECURE_HTTP -> "Backend Endpoint Invalid"
                    else -> "Offline Mode (Backend Not Provisioned)"
                }
            return BackendDiagnostics(
                configStatus = endpoint.status,
                connectionPhase = ConnectionPhase.NOT_CONFIGURED,
                baseUrl = endpoint.baseUrl,
                isDeviceOnline = deviceOnline,
                isBackendReachable = false,
                isDatabaseConnected = false,
                aiConfigured = false,
                emailDeliveryConfigured = false,
                statusSummary = summary,
                rootCauseDetail = endpoint.diagnosticMessage,
                lastCheckedAt = checkedAt,
                lastConnectedAt = previousLastConnectedAt,
            )
        }

        if (!deviceOnline) {
            return BackendDiagnostics(
                configStatus = endpoint.status,
                connectionPhase = ConnectionPhase.OFFLINE,
                baseUrl = endpoint.baseUrl,
                isDeviceOnline = false,
                isBackendReachable = false,
                isDatabaseConnected = false,
                statusSummary = "Offline",
                rootCauseDetail = "No active internet connection. Offline commands active; GOLU will reconnect automatically.",
                lastCheckedAt = checkedAt,
                lastConnectedAt = previousLastConnectedAt,
            )
        }

        return runCatching {
            val healthResponse = api.getHealth()
            val health =
                healthResponse.body()
                    ?: healthResponse.errorBody()?.string()?.let { raw ->
                        runCatching { healthAdapter.fromJson(raw) }.getOrNull()
                    }

            if (health == null) {
                return@runCatching BackendDiagnostics(
                    configStatus = endpoint.status,
                    connectionPhase = ConnectionPhase.ERROR,
                    baseUrl = endpoint.baseUrl,
                    isDeviceOnline = true,
                    isBackendReachable = false,
                    isDatabaseConnected = false,
                    statusSummary = "Error (HTTP ${healthResponse.code()})",
                    rootCauseDetail = "Server returned HTTP ${healthResponse.code()} without a valid health payload.",
                    lastCheckedAt = checkedAt,
                    lastConnectedAt = previousLastConnectedAt,
                )
            }

            val dbHealthResponse = runCatching { api.getDatabaseHealth() }.getOrNull()
            val dbHealth =
                dbHealthResponse?.body()
                    ?: dbHealthResponse?.errorBody()?.string()?.let { raw ->
                        runCatching { dbHealthAdapter.fromJson(raw) }.getOrNull()
                    }
            val status = runCatching { api.getStatus() }.getOrNull()

            val dbOk = dbHealth?.databaseOk ?: health.databaseOk
            val dbErr = dbHealth?.databaseError ?: health.databaseError
            val aiOk = status?.openaiConfigured ?: false
            val emailOk = status?.emailDeliveryConfigured ?: false

            val phase =
                if (health.backendOk && dbOk) ConnectionPhase.CONNECTED
                else ConnectionPhase.ERROR

            val summary =
                when {
                    health.backendOk && dbOk -> "Connected"
                    health.backendOk && !dbOk -> "API Up • Database Unavailable"
                    else -> "Error"
                }
            val missingNote =
                if (health.missingConfig.isNotEmpty()) {
                    " Missing config: ${health.missingConfig.joinToString(", ")}."
                } else ""
            val rootCause =
                when {
                    !dbOk -> "Database unavailable: ${dbErr ?: "PostgreSQL unreachable"}"
                    else -> "Automatic HTTPS connection active.$missingNote"
                }

            val connectedTimestamp =
                if (phase == ConnectionPhase.CONNECTED) checkedAt else previousLastConnectedAt

            BackendDiagnostics(
                configStatus = endpoint.status,
                connectionPhase = phase,
                baseUrl = endpoint.baseUrl,
                isDeviceOnline = true,
                isBackendReachable = health.backendOk,
                isDatabaseConnected = dbOk,
                aiConfigured = aiOk,
                emailDeliveryConfigured = emailOk,
                statusSummary = summary,
                rootCauseDetail = rootCause,
                lastCheckedAt = checkedAt,
                lastConnectedAt = connectedTimestamp,
            )
        }.getOrElse { throwable ->
            val mapped = ApiErrorMapper.map(throwable)
            BackendDiagnostics(
                configStatus = endpoint.status,
                connectionPhase = ConnectionPhase.ERROR,
                baseUrl = endpoint.baseUrl,
                isDeviceOnline = true,
                isBackendReachable = false,
                isDatabaseConnected = false,
                statusSummary = "Error",
                rootCauseDetail = mapped.message,
                lastCheckedAt = checkedAt,
                lastConnectedAt = previousLastConnectedAt,
            )
        }
    }
}
