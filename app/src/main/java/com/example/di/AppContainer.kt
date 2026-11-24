package com.example.di

import android.content.Context
import com.example.core.BackendConfig
import com.example.core.BackendConnectionManager
import com.example.core.KeystoreCrypto
import com.example.data.local.SecurityPinVault
import com.example.data.local.SettingsRepository
import com.example.data.local.TokenVault
import com.example.data.remote.GoluApiService
import com.example.data.remote.GoluV2ApiService
import com.example.data.remote.dto.RefreshRequestDto
import com.example.data.repository.AuthRepository
import com.example.data.repository.GoluFeaturesRepository
import com.example.data.repository.GoluV2Repository
import com.example.data.repository.SystemRepository
import com.example.domain.ai.AndroidActionExecutor
import com.example.domain.ai.HybridAiRouter
import com.example.domain.ai.OfflineCommandEngine
import com.example.domain.automation.AutomationEvaluator
import com.example.domain.golu2.AudioRecorderHelper
import com.example.domain.model.UserProfile
import com.example.domain.phone.ContactLookupHelper
import com.example.domain.voice.VoiceEngine
import com.example.domain.wakeword.WakeWordEngine
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val crypto = KeystoreCrypto()
    val tokenVault = TokenVault(appContext, crypto)
    val securityPinVault = SecurityPinVault(appContext, crypto)
    val settingsRepository = SettingsRepository(appContext)

    private val centralBaseUrlInterceptor = BackendConfig.createBaseUrlInterceptor()

    private val authHeaderInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = tokenVault.getAccessTokenBlocking()
        val builder = original.newBuilder().header("Accept", "application/json")
        if (!token.isNullOrBlank() && original.header("Authorization") == null) {
            builder.header("Authorization", "Bearer $token")
        }
        chain.proceed(builder.build())
    }

    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
            redactHeader("Cookie")
        }

    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // Separate lightweight Retrofit client for synchronous token refresh inside Authenticator
    private val refreshClient: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(centralBaseUrlInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

    private val refreshApi: GoluApiService =
        Retrofit.Builder()
            .baseUrl(BackendConfig.getRetrofitBaseUrl())
            .client(refreshClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GoluApiService::class.java)

    private val tokenAuthenticator =
        object : Authenticator {
            private val lock = Any()

            override fun authenticate(route: Route?, response: Response): Request? {
                val path = response.request.url.encodedPath
                if (path.contains("/auth/login") ||
                    path.contains("/auth/register") ||
                    path.contains("/auth/refresh")
                ) {
                    return null
                }
                if (response.request.header("X-Token-Refreshed") != null) {
                    return null
                }

                synchronized(lock) {
                    val currentAccess = tokenVault.getAccessTokenBlocking()
                    val requestAuth = response.request.header("Authorization")?.removePrefix("Bearer ")?.trim()
                    if (!currentAccess.isNullOrBlank() && currentAccess != requestAuth) {
                        return response.request
                            .newBuilder()
                            .header("Authorization", "Bearer $currentAccess")
                            .header("X-Token-Refreshed", "1")
                            .build()
                    }

                    val refreshToken = runBlocking { tokenVault.getRefreshToken() } ?: return null
                    val refreshRes =
                        runCatching {
                            refreshApi.refreshTokenSync(RefreshRequestDto(refreshToken)).execute()
                        }.getOrNull()

                    val body = if (refreshRes != null && refreshRes.isSuccessful) refreshRes.body() else null
                    if (body == null) {
                        runBlocking { tokenVault.clearSession() }
                        return null
                    }

                    val profile =
                        UserProfile(
                            id = body.user.id,
                            fullName = body.user.fullName,
                            email = body.user.email,
                            preferredTitle = body.user.preferredTitle,
                            isVerified = body.user.isVerified,
                            createdAt = body.user.createdAt,
                        )
                    runBlocking {
                        tokenVault.saveSession(body.accessToken, body.refreshToken, profile)
                    }

                    return response.request
                        .newBuilder()
                        .header("Authorization", "Bearer ${body.accessToken}")
                        .header("X-Token-Refreshed", "1")
                        .build()
                }
            }
        }

    private val okHttpClient: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(centralBaseUrlInterceptor)
            .addInterceptor(authHeaderInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .build()

    private val retrofit: Retrofit =
        Retrofit.Builder()
            .baseUrl(BackendConfig.getRetrofitBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    val apiService: GoluApiService = retrofit.create(GoluApiService::class.java)
    val apiV2Service: GoluV2ApiService = retrofit.create(GoluV2ApiService::class.java)

    val authRepository = AuthRepository(apiService, tokenVault)
    val systemRepository = SystemRepository(appContext, apiService)
    val backendConnectionManager = BackendConnectionManager(appContext, systemRepository)
    val featuresRepository = GoluFeaturesRepository(apiService)
    val goluV2Repository = GoluV2Repository(apiV2Service)

    val voiceEngine = VoiceEngine(appContext)
    val wakeWordEngine = WakeWordEngine(appContext)
    val actionExecutor = AndroidActionExecutor(appContext)
    val contactLookupHelper = ContactLookupHelper(appContext)
    val offlineCommandEngine = OfflineCommandEngine(appContext, actionExecutor)
    val hybridAiRouter = HybridAiRouter(apiService, offlineCommandEngine)
    val automationEvaluator = AutomationEvaluator(appContext, actionExecutor, voiceEngine)
    val audioRecorderHelper = AudioRecorderHelper(appContext)
}
