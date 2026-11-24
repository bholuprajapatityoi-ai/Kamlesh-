package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.core.BackendConfig
import com.example.core.BackendConfigStatus
import com.example.core.BackendConnectionManager
import com.example.core.BackendEndpointInfo
import com.example.core.KeystoreCrypto
import com.example.core.NetworkResult
import com.example.data.local.TokenVault
import com.example.data.remote.GoluApiService
import com.example.data.remote.dto.*
import com.example.data.repository.AuthRepository
import com.example.data.repository.SystemRepository
import com.example.domain.model.BackendDiagnostics
import com.example.domain.model.ConnectionPhase
import com.example.domain.model.GoluSettings
import com.example.presentation.screens.SettingsScreen
import com.example.ui.theme.GoluTheme
import java.io.IOException
import java.lang.reflect.Proxy
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response as OkHttpResponse
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AutomaticConnectionRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `settings screen shows automatic backend connection and zero manual url controls`() {
        var retryClicks = 0
        val diagnostics =
            BackendDiagnostics(
                connectionPhase = ConnectionPhase.OFFLINE,
                configStatus = BackendConfigStatus.READY_HTTPS,
                isDeviceOnline = false,
                isBackendReachable = false,
                isDatabaseConnected = false,
                statusSummary = "Offline",
                rootCauseDetail = "No active internet connection.",
                lastConnectedAt = "",
            )

        composeRule.setContent {
            GoluTheme(darkTheme = true) {
                SettingsScreen(
                    settings = GoluSettings(),
                    diagnostics = diagnostics,
                    onDarkThemeChange = {},
                    onVoiceGenderChange = {},
                    onSpeechSpeedChange = {},
                    onLanguageChange = {},
                    onGreetingChange = {},
                    onResponseStyleChange = {},
                    onWakeWordChange = {},
                    onKeepContextChange = {},
                    onRedactLogsChange = {},
                    onRetryConnection = { retryClicks += 1 },
                    onOpenBatteryOptimization = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Automatic Backend Connection").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("API Health").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Database Health").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Last Connected").performScrollTo().assertIsDisplayed()
        composeRule
            .onNodeWithTag("retry_connection_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertEquals(1, retryClicks)

        // Manual URL fields/buttons must be completely absent
        composeRule.onNodeWithTag("backend_url_input").assertDoesNotExist()
        composeRule.onNodeWithTag("save_backend_url_button").assertDoesNotExist()
        composeRule.onNodeWithText("Deployed HTTPS Backend URL").assertDoesNotExist()
        composeRule.onNodeWithText("Save & Test /api/v1/health").assertDoesNotExist()
    }

    @Test
    fun `backend connection manager transitions honestly across not configured, offline, error, and connected`() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val dispatcher = StandardTestDispatcher(testScheduler)

            // 1. Unconfigured endpoint must report NOT_CONFIGURED, never CONNECTED
            val unconfiguredRepo =
                SystemRepository(
                    context = context,
                    api = fakeApiService(),
                    endpointProvider = { BackendConfig.evaluate("UNCONFIGURED") },
                    networkOnlineProvider = { true },
                )
            val unconfiguredManager =
                BackendConnectionManager(
                    context = context,
                    systemRepository = unconfiguredRepo,
                    ioDispatcher = dispatcher,
                    registerNetworkCallbackOnStart = false,
                )
            val unconfiguredRes = unconfiguredManager.runSingleCheck()
            assertEquals(ConnectionPhase.NOT_CONFIGURED, unconfiguredRes.connectionPhase)
            assertFalse(unconfiguredRes.isBackendReachable)
            unconfiguredManager.release()

            // 2. Device offline must report OFFLINE
            val readyEndpoint = BackendEndpointInfo(
                baseUrl = "https://api.example.com/",
                status = BackendConfigStatus.READY_HTTPS,
                diagnosticMessage = "Automatic HTTPS endpoint ready.",
            )
            val offlineRepo =
                SystemRepository(
                    context = context,
                    api = fakeApiService(),
                    endpointProvider = { readyEndpoint },
                    networkOnlineProvider = { false },
                )
            val offlineManager =
                BackendConnectionManager(
                    context = context,
                    systemRepository = offlineRepo,
                    ioDispatcher = dispatcher,
                    registerNetworkCallbackOnStart = false,
                )
            val offlineRes = offlineManager.runSingleCheck()
            assertEquals(ConnectionPhase.OFFLINE, offlineRes.connectionPhase)
            assertFalse(offlineRes.isBackendReachable)
            offlineManager.release()

            // 3. Backend network failure must report ERROR, never simulated CONNECTED
            val failingRepo =
                SystemRepository(
                    context = context,
                    api = fakeApiService(throwNetworkError = true),
                    endpointProvider = { readyEndpoint },
                    networkOnlineProvider = { true },
                )
            val failingManager =
                BackendConnectionManager(
                    context = context,
                    systemRepository = failingRepo,
                    ioDispatcher = dispatcher,
                    registerNetworkCallbackOnStart = false,
                )
            val errRes = failingManager.runSingleCheck()
            assertEquals(ConnectionPhase.ERROR, errRes.connectionPhase)
            assertFalse(errRes.isBackendReachable)
            assertFalse(errRes.isDatabaseConnected)
            failingManager.release()

            // 4. Real healthy API + Database response reports CONNECTED and records lastConnectedAt
            val healthyRepo =
                SystemRepository(
                    context = context,
                    api = fakeApiService(dbOk = true),
                    endpointProvider = { readyEndpoint },
                    networkOnlineProvider = { true },
                )
            val healthyManager =
                BackendConnectionManager(
                    context = context,
                    systemRepository = healthyRepo,
                    ioDispatcher = dispatcher,
                    registerNetworkCallbackOnStart = false,
                )
            val okRes = healthyManager.runSingleCheck()
            assertEquals(ConnectionPhase.CONNECTED, okRes.connectionPhase)
            assertTrue(okRes.isBackendReachable)
            assertTrue(okRes.isDatabaseConnected)
            assertTrue(okRes.lastConnectedAt.isNotBlank())
            healthyManager.release()
        }

    @Test
    fun `backend reports 503 degraded database honestly as ERROR`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val readyEndpoint =
            BackendEndpointInfo(
                baseUrl = "https://api.example.com/",
                status = BackendConfigStatus.READY_HTTPS,
                diagnosticMessage = "Automatic HTTPS endpoint ready.",
            )
        val degradedRepo =
            SystemRepository(
                context = context,
                api = fakeApiService(dbOk = false),
                endpointProvider = { readyEndpoint },
                networkOnlineProvider = { true },
            )
        val manager =
            BackendConnectionManager(
                context = context,
                systemRepository = degradedRepo,
                ioDispatcher = dispatcher,
                registerNetworkCallbackOnStart = false,
            )
        val res = manager.runSingleCheck()
        assertEquals(ConnectionPhase.ERROR, res.connectionPhase)
        assertTrue(res.isBackendReachable)
        assertFalse(res.isDatabaseConnected)
        manager.release()
    }

    @Test
    fun `auth repository blocks unconfigured endpoint cleanly without faking login`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val aesKey = javax.crypto.KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val vault = TokenVault(context, KeystoreCrypto("golu_test_auth_key", testOnlySecretKey = aesKey))
        val repo =
            AuthRepository(
                api = fakeApiService(),
                tokenVault = vault,
                endpointProvider = { BackendConfig.evaluate("UNCONFIGURED") },
            )
        val res = repo.login("boss@example.com", "StrongPassword!123")
        assertTrue(res is NetworkResult.Error)
        assertEquals("UNCONFIGURED", (res as NetworkResult.Error).causeCategory)
    }

    @Test
    fun `backend config validates security rules, thread safety, and fail-closed interceptor`() {
        // Rejects embedded credentials, query/fragment, and disallowed hosts
        assertEquals(
            BackendConfigStatus.UNCONFIGURED,
            BackendConfig.evaluate("https://user:pass@api.example.com").status,
        )
        assertEquals(
            BackendConfigStatus.UNCONFIGURED,
            BackendConfig.evaluate("https://api.example.com?token=123").status,
        )
        assertEquals(
            BackendConfigStatus.INVALID_LOCALHOST,
            BackendConfig.evaluate("https://0.0.0.0:8000").status,
        )
        assertEquals(
            BackendConfig.UNCONFIGURED_SENTINEL_URL,
            BackendConfig.getRetrofitBaseUrl("UNCONFIGURED"),
        )
        assertEquals(
            "https://api.example.com/v1/",
            BackendConfig.getRetrofitBaseUrl("https://api.example.com/v1"),
        )

        // Fail-closed OkHttp interceptor throws IOException when unconfigured
        val unconfiguredClient =
            OkHttpClient.Builder()
                .addInterceptor(
                    BackendConfig.createBaseUrlInterceptor { BackendConfig.evaluate("UNCONFIGURED") }
                )
                .build()
        var thrown: IOException? = null
        try {
            unconfiguredClient
                .newCall(Request.Builder().url("https://unconfigured.invalid/api/v1/health").build())
                .execute()
        } catch (e: IOException) {
            thrown = e
        }
        assertTrue(thrown != null)

        // Thread-safe concurrent OkHttp request rewriting with base-path preservation
        val readyClient =
            OkHttpClient.Builder()
                .addInterceptor(
                    BackendConfig.createBaseUrlInterceptor {
                        BackendConfig.evaluate("https://golu.example.com/prod")
                    }
                )
                .addInterceptor(
                    Interceptor { chain ->
                        val req = chain.request()
                        OkHttpResponse.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(req.url.toString().toResponseBody("text/plain".toMediaType()))
                            .build()
                    }
                )
                .build()

        val pool = Executors.newFixedThreadPool(8)
        try {
            val tasks =
                (1..16).map { idx ->
                    Callable {
                        val req =
                            Request.Builder()
                                .url("https://unconfigured.invalid/api/v1/health?req=$idx")
                                .build()
                        readyClient.newCall(req).execute().use { resp ->
                            assertEquals(200, resp.code)
                            resp.body?.string().orEmpty()
                        }
                    }
                }
            val results = pool.invokeAll(tasks).map { it.get() }
            results.forEachIndexed { i, url ->
                assertEquals("https://golu.example.com/prod/api/v1/health?req=${i + 1}", url)
            }
        } finally {
            pool.shutdownNow()
        }
    }

    private fun fakeApiService(
        dbOk: Boolean = true,
        throwNetworkError: Boolean = false,
    ): GoluApiService {
        return Proxy.newProxyInstance(
            GoluApiService::class.java.classLoader,
            arrayOf(GoluApiService::class.java),
        ) { _, method, _ ->
            if (throwNetworkError) {
                throw IOException("Simulated unreachable host")
            }
            when (method.name) {
                "getHealth" -> {
                    if (dbOk) {
                        Response.success(
                            HealthResponseDto(
                                status = "ok",
                                backendOk = true,
                                databaseOk = true,
                                redisOk = true,
                                timestamp = "2026-11-23T14:00:00Z",
                            )
                        )
                    } else {
                        val json =
                            """{"status":"degraded","backend_ok":true,"database_ok":false,"database_error":"ConnectionRefused","timestamp":"2026-11-23T14:00:00Z"}"""
                        Response.error<HealthResponseDto>(
                            503,
                            json.toResponseBody("application/json".toMediaType()),
                        )
                    }
                }
                "getDatabaseHealth" -> {
                    if (dbOk) {
                        Response.success(
                            DatabaseHealthResponseDto(
                                status = "connected",
                                databaseOk = true,
                                databaseEngine = "postgresql",
                                latencyMs = 2.5,
                                redisOk = true,
                                timestamp = "2026-11-23T14:00:00Z",
                            )
                        )
                    } else {
                        val json =
                            """{"status":"unavailable","database_ok":false,"database_engine":"postgresql","latency_ms":0.0,"redis_ok":false,"database_error":"ConnectionRefused","timestamp":"2026-11-23T14:00:00Z"}"""
                        Response.error<DatabaseHealthResponseDto>(
                            503,
                            json.toResponseBody("application/json".toMediaType()),
                        )
                    }
                }
                "getStatus" ->
                    SystemStatusResponseDto(
                        app = "GOLU AI Backend",
                        version = "1.1.0",
                        environment = "production",
                        databaseStatus = if (dbOk) "connected" else "unreachable",
                        redisStatus = "connected",
                        openaiConfigured = true,
                        emailDeliveryConfigured = false,
                    )
                else -> throw UnsupportedOperationException("Unstubbed method ${method.name}")
            }
        } as GoluApiService
    }
}
