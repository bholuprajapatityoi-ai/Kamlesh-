package com.example.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.data.repository.SystemRepository
import com.example.domain.model.BackendDiagnostics
import com.example.domain.model.ConnectionPhase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Automatic backend connectivity manager.
 * - Verifies /api/v1/health and /api/v1/health/database automatically on startup.
 * - Observes ConnectivityManager network restoration to reconnect without user URL input.
 * - Uses bounded exponential backoff (1s -> 2s -> 4s -> 8s -> 16s) when transient errors occur.
 * - Never fabricates a Connected state.
 */
class BackendConnectionManager(
    context: Context,
    private val systemRepository: SystemRepository,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val registerNetworkCallbackOnStart: Boolean = true,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val _diagnostics = MutableStateFlow(BackendDiagnostics())
    val diagnostics: StateFlow<BackendDiagnostics> = _diagnostics.asStateFlow()

    private var activeCheckJob: Job? = null
    private var networkCallbackRegistered = false
    private var connectivityManager: ConnectivityManager? = null

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val current = _diagnostics.value.connectionPhase
                if (current == ConnectionPhase.OFFLINE || current == ConnectionPhase.ERROR) {
                    retryNow()
                }
            }

            override fun onLost(network: Network) {
                if (!DeviceTelemetry.isNetworkAvailable(appContext)) {
                    val prev = _diagnostics.value
                    _diagnostics.value =
                        prev.copy(
                            connectionPhase = ConnectionPhase.OFFLINE,
                            isDeviceOnline = false,
                            isBackendReachable = false,
                            isDatabaseConnected = false,
                            statusSummary = "Offline",
                            rootCauseDetail =
                                "Network connection lost. Offline commands remain active; GOLU will reconnect automatically.",
                        )
                }
            }
        }

    fun start() {
        if (registerNetworkCallbackOnStart && !networkCallbackRegistered) {
            runCatching {
                val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                if (cm != null) {
                    val req =
                        NetworkRequest.Builder()
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            .build()
                    cm.registerNetworkCallback(req, networkCallback)
                    connectivityManager = cm
                    networkCallbackRegistered = true
                }
            }
        }
        verifyWithBackoff(maxAttempts = 4)
    }

    fun retryNow() {
        verifyWithBackoff(maxAttempts = 3)
    }

    suspend fun runSingleCheck(): BackendDiagnostics {
        val prev = _diagnostics.value
        _diagnostics.value =
            prev.copy(
                connectionPhase = ConnectionPhase.CONNECTING,
                statusSummary = "Connecting...",
            )
        val result =
            withTimeoutOrNull(12_000L) {
                systemRepository.checkDiagnostics(previousLastConnectedAt = prev.lastConnectedAt)
            }
                ?: prev.copy(
                    connectionPhase = ConnectionPhase.ERROR,
                    isBackendReachable = false,
                    isDatabaseConnected = false,
                    statusSummary = "Error (Timeout)",
                    rootCauseDetail = "Connection check timed out after 12 seconds.",
                )
        _diagnostics.value = result
        return result
    }

    private fun verifyWithBackoff(maxAttempts: Int) {
        activeCheckJob?.cancel()
        activeCheckJob =
            scope.launch {
                var backoffMs = 1_000L
                for (attempt in 1..maxAttempts) {
                    val prev = _diagnostics.value
                    _diagnostics.value =
                        prev.copy(
                            connectionPhase = ConnectionPhase.CONNECTING,
                            statusSummary =
                                if (attempt == 1) "Connecting automatically..."
                                else "Reconnecting (attempt $attempt/$maxAttempts)...",
                            retryAttempt = attempt,
                            nextRetrySeconds = 0,
                        )

                    val checked =
                        withTimeoutOrNull(12_000L) {
                            systemRepository.checkDiagnostics(previousLastConnectedAt = prev.lastConnectedAt)
                        }
                            ?: prev.copy(
                                connectionPhase = ConnectionPhase.ERROR,
                                isBackendReachable = false,
                                isDatabaseConnected = false,
                                statusSummary = "Error (Timeout)",
                                rootCauseDetail = "Backend health check timed out.",
                            )

                    val nextSec =
                        if (attempt < maxAttempts && checked.connectionPhase == ConnectionPhase.ERROR) {
                            (backoffMs / 1000L).toInt()
                        } else {
                            0
                        }
                    _diagnostics.value =
                        checked.copy(
                            retryAttempt = attempt,
                            nextRetrySeconds = nextSec,
                        )

                    if (checked.connectionPhase == ConnectionPhase.CONNECTED ||
                        checked.connectionPhase == ConnectionPhase.NOT_CONFIGURED ||
                        checked.connectionPhase == ConnectionPhase.OFFLINE
                    ) {
                        break
                    }

                    if (attempt < maxAttempts) {
                        delay(backoffMs)
                        backoffMs = (backoffMs * 2L).coerceAtMost(16_000L)
                    }
                }
            }
    }

    fun release() {
        activeCheckJob?.cancel()
        if (networkCallbackRegistered) {
            runCatching { connectivityManager?.unregisterNetworkCallback(networkCallback) }
            networkCallbackRegistered = false
        }
        scope.cancel()
    }
}
