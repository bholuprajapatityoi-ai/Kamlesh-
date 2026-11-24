package com.example.core

import com.example.BuildConfig
import java.io.IOException
import java.net.URI
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor

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

/**
 * Thread-safe, immutable central provider for the GOLU backend API base URL.
 *
 * Security & Immutability guarantees:
 * - Reads exclusively from compile-time `BuildConfig.GOLU_BACKEND_URL` (injected via Secrets/env).
 * - Exposes no setters, preferences, or runtime mutation hooks so UI screens cannot alter it.
 * - Thread-safe synchronized lazy initialization ensures safe concurrent access across OkHttp/Retrofit threads.
 * - Fails closed on unconfigured, cleartext HTTP (except emulator `10.0.2.2`), localhost, embedded credentials,
 *   or malformed base URLs.
 */
object BackendConfig {

    const val UNCONFIGURED_SENTINEL_URL = "https://unconfigured.invalid/"

    private val disallowedHosts = setOf("localhost", "127.0.0.1", "::1", "[::1]", "0.0.0.0")

    private val cachedEndpoint: BackendEndpointInfo by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        evaluate(BuildConfig.GOLU_BACKEND_URL)
    }

    private val cachedHttpUrl: HttpUrl? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val info = cachedEndpoint
        if (info.isReady()) info.baseUrl.toHttpUrlOrNull() else null
    }

    fun evaluateConfigured(): BackendEndpointInfo = cachedEndpoint

    fun isConfiguredAndReady(): Boolean = cachedEndpoint.isReady()

    fun getValidatedHttpUrl(): HttpUrl? = cachedHttpUrl

    /**
     * Returns a Retrofit-safe normalized trailing-slash base URL for `Retrofit.Builder().baseUrl(...)`.
     * When `BuildConfig.GOLU_BACKEND_URL` is unconfigured or invalid, returns [UNCONFIGURED_SENTINEL_URL]
     * so Retrofit construction succeeds cleanly while [createBaseUrlInterceptor] blocks outbound calls.
     */
    fun getRetrofitBaseUrl(): String =
        if (cachedEndpoint.isReady()) cachedEndpoint.baseUrl else UNCONFIGURED_SENTINEL_URL

    fun getRetrofitBaseUrl(candidateUrl: String?): String {
        val evaluated = evaluate(candidateUrl)
        return if (evaluated.isReady()) evaluated.baseUrl else UNCONFIGURED_SENTINEL_URL
    }

    /**
     * Thread-safe OkHttp interceptor enforcing the centralized [BuildConfig.GOLU_BACKEND_URL].
     * Fails closed with [IOException] if no valid configured endpoint exists.
     */
    fun createBaseUrlInterceptor(
        endpointProvider: () -> BackendEndpointInfo = ::evaluateConfigured,
    ): Interceptor = Interceptor { chain ->
        val original = chain.request()
        val evaluated = endpointProvider()
        val targetBase =
            if (evaluated.isReady()) {
                evaluated.baseUrl.toHttpUrlOrNull()
            } else {
                null
            }
                ?: throw IOException(
                    evaluated.diagnosticMessage.ifBlank {
                        "Central backend URL is not configured."
                    }
                )

        val basePath = targetBase.encodedPath.trimEnd('/')
        val relPath = original.url.encodedPath
        val combinedPath =
            if (basePath.isNotEmpty() && !relPath.startsWith(basePath)) {
                basePath + relPath
            } else {
                relPath
            }
        val newUrl =
            original.url
                .newBuilder()
                .scheme(targetBase.scheme)
                .host(targetBase.host)
                .port(targetBase.port)
                .encodedPath(combinedPath)
                .build()

        chain.proceed(original.newBuilder().url(newUrl).build())
    }

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
        val httpUrl = normalized.toHttpUrlOrNull()
        val scheme = uri?.scheme?.lowercase().orEmpty()
        val host = uri?.host?.lowercase().orEmpty()

        if (uri == null || httpUrl == null || host.isBlank() || (scheme != "https" && scheme != "http")) {
            return BackendEndpointInfo(
                baseUrl = "",
                status = BackendConfigStatus.UNCONFIGURED,
                diagnosticMessage = "Configured backend endpoint format is invalid.",
            )
        }

        if (!uri.userInfo.isNullOrEmpty() || httpUrl.username.isNotEmpty() || httpUrl.password.isNotEmpty()) {
            return BackendEndpointInfo(
                baseUrl = "",
                status = BackendConfigStatus.UNCONFIGURED,
                diagnosticMessage = "Embedded credentials are not allowed in the backend URL.",
            )
        }

        if (httpUrl.query != null || httpUrl.fragment != null) {
            return BackendEndpointInfo(
                baseUrl = "",
                status = BackendConfigStatus.UNCONFIGURED,
                diagnosticMessage = "Query parameters and fragments are not allowed in the base backend URL.",
            )
        }

        if (host in disallowedHosts) {
            return BackendEndpointInfo(
                baseUrl = "",
                status = BackendConfigStatus.INVALID_LOCALHOST,
                diagnosticMessage =
                    "localhost / 127.0.0.1 is disallowed on Android devices. Configure a deployed HTTPS endpoint in build settings.",
            )
        }

        if (scheme == "http" && host == "10.0.2.2") {
            return BackendEndpointInfo(
                baseUrl = httpUrl.toString(),
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
            baseUrl = httpUrl.toString(),
            status = BackendConfigStatus.READY_HTTPS,
            diagnosticMessage = "Automatic HTTPS endpoint ready.",
        )
    }

    private fun BackendEndpointInfo.isReady(): Boolean =
        status == BackendConfigStatus.READY_HTTPS || status == BackendConfigStatus.READY_EMULATOR_DEV
}

