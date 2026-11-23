package com.example.core

import com.example.BuildConfig
import java.net.URI

enum class BackendConfigStatus {
    UNCONFIGURED,
    INVALID_LOCALHOST,
    INSECURE_HTTP,
    READY_HTTPS,
    READY_EMULATOR_DEV,
}

data class BackendEndpointInfo(
    val baseUrl: String,
    val status: BackendConfigStatus,
    val diagnosticMessage: String,
)

object BackendConfig {
    fun evaluateConfigured(): BackendEndpointInfo = evaluate(BuildConfig.GOLU_BACKEND_URL)

    fun evaluate(candidateUrl: String?): BackendEndpointInfo {
        val raw = candidateUrl?.trim().orEmpty()
        if (raw.isBlank() || raw.equals("UNCONFIGURED", ignoreCase = true)) {
            return BackendEndpointInfo(
                baseUrl = "",
                status = BackendConfigStatus.UNCONFIGURED,
                diagnosticMessage =
                    "Central HTTPS backend endpoint is not configured in build configuration. Offline commands remain active.",
            )
        }

        val normalized = if (raw.endsWith("/")) raw else "$raw/"
        val uri = runCatching { URI(normalized) }.getOrNull()
        val scheme = uri?.scheme?.lowercase().orEmpty()
        val host = uri?.host?.lowercase().orEmpty()

        if (uri == null || host.isBlank() || (scheme != "https" && scheme != "http")) {
            return BackendEndpointInfo(
                baseUrl = "",
                status = BackendConfigStatus.UNCONFIGURED,
                diagnosticMessage = "Configured backend endpoint format is invalid.",
            )
        }

        if (host == "localhost" || host == "127.0.0.1" || host == "::1") {
            return BackendEndpointInfo(
                baseUrl = "",
                status = BackendConfigStatus.INVALID_LOCALHOST,
                diagnosticMessage =
                    "localhost / 127.0.0.1 is disallowed on Android devices. Configure a deployed HTTPS endpoint in build settings.",
            )
        }

        if (scheme == "http" && host == "10.0.2.2") {
            return BackendEndpointInfo(
                baseUrl = normalized,
                status = BackendConfigStatus.READY_EMULATOR_DEV,
                diagnosticMessage = "Emulator development backend active (10.0.2.2).",
            )
        }

        if (scheme != "https") {
            return BackendEndpointInfo(
                baseUrl = "",
                status = BackendConfigStatus.INSECURE_HTTP,
                diagnosticMessage = "Cleartext HTTP is blocked for security. HTTPS is required.",
            )
        }

        return BackendEndpointInfo(
            baseUrl = normalized,
            status = BackendConfigStatus.READY_HTTPS,
            diagnosticMessage = "Automatic HTTPS endpoint ready.",
        )
    }
}
