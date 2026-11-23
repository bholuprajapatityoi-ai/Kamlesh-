package com.example.data.repository

import android.util.Patterns
import com.example.core.ApiErrorMapper
import com.example.core.BackendConfig
import com.example.core.BackendConfigStatus
import com.example.core.BackendEndpointInfo
import com.example.core.NetworkResult
import com.example.data.local.TokenVault
import com.example.data.remote.GoluApiService
import com.example.data.remote.dto.ForgotPasswordRequestDto
import com.example.data.remote.dto.LoginRequestDto
import com.example.data.remote.dto.MessageResponseDto
import com.example.data.remote.dto.RefreshRequestDto
import com.example.data.remote.dto.RegisterRequestDto
import com.example.data.remote.dto.ResetPasswordRequestDto
import com.example.data.remote.dto.UserDto
import com.example.domain.model.UserProfile

class AuthRepository(
    private val api: GoluApiService,
    private val tokenVault: TokenVault,
    private val endpointProvider: () -> BackendEndpointInfo = { BackendConfig.evaluateConfigured() },
) {
    private fun ensureBackendConfigured(): NetworkResult.Error? {
        val info = endpointProvider()
        return when (info.status) {
            BackendConfigStatus.READY_HTTPS,
            BackendConfigStatus.READY_EMULATOR_DEV -> null
            else ->
                NetworkResult.Error(
                    message = "Backend Unreachable: ${info.diagnosticMessage}",
                    causeCategory = info.status.name,
                )
        }
    }

    suspend fun register(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): NetworkResult<UserProfile> {
        if (fullName.trim().length < 2) {
            return NetworkResult.Error("Please enter your full name (at least 2 characters).", "VALIDATION")
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return NetworkResult.Error("Please enter a valid email address.", "VALIDATION")
        }
        if (password.length < 8) {
            return NetworkResult.Error("Password must be at least 8 characters.", "VALIDATION")
        }
        if (password != confirmPassword) {
            return NetworkResult.Error("Password and confirm password do not match.", "VALIDATION")
        }
        ensureBackendConfigured()?.let { return it }

        return runCatching {
            val res =
                api.register(
                    RegisterRequestDto(
                        fullName = fullName.trim(),
                        email = email.trim().lowercase(),
                        password = password,
                        confirmPassword = confirmPassword,
                    )
                )
            val profile = res.user.toDomain()
            tokenVault.saveSession(res.accessToken, res.refreshToken, profile)
            NetworkResult.Success(profile)
        }.getOrElse { ApiErrorMapper.map(it) }
    }

    suspend fun login(email: String, password: String): NetworkResult<UserProfile> {
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return NetworkResult.Error("Please enter a valid email address.", "VALIDATION")
        }
        if (password.isBlank()) {
            return NetworkResult.Error("Password cannot be empty.", "VALIDATION")
        }
        ensureBackendConfigured()?.let { return it }

        return runCatching {
            val res = api.login(LoginRequestDto(email = email.trim().lowercase(), password = password))
            val profile = res.user.toDomain()
            tokenVault.saveSession(res.accessToken, res.refreshToken, profile)
            NetworkResult.Success(profile)
        }.getOrElse { ApiErrorMapper.map(it) }
    }

    suspend fun restoreSession(): NetworkResult<UserProfile?> {
        val access = tokenVault.getAccessToken()
        val refresh = tokenVault.getRefreshToken()
        if (access.isNullOrBlank() && refresh.isNullOrBlank()) {
            return NetworkResult.Success(null)
        }
        ensureBackendConfigured()?.let { return it }

        val meResult = runCatching { api.getCurrentUser().toDomain() }
        if (meResult.isSuccess) {
            return NetworkResult.Success(meResult.getOrThrow())
        }

        // Attempt refresh token rotation if access token expired
        if (!refresh.isNullOrBlank()) {
            val refreshResult = runCatching {
                val res = api.refreshToken(RefreshRequestDto(refresh))
                val profile = res.user.toDomain()
                tokenVault.saveSession(res.accessToken, res.refreshToken, profile)
                profile
            }
            if (refreshResult.isSuccess) {
                return NetworkResult.Success(refreshResult.getOrThrow())
            }
        }

        val err = meResult.exceptionOrNull()?.let { ApiErrorMapper.map(it) }
            ?: NetworkResult.Error("Session expired. Please sign in again.", "AUTH_ERROR")
        if (err.httpCode == 401 || err.httpCode == 403) {
            tokenVault.clearSession()
        }
        return err
    }

    suspend fun logout(): NetworkResult<String> {
        val configError = ensureBackendConfigured()
        val serverMsg =
            if (configError == null && !tokenVault.getAccessToken().isNullOrBlank()) {
                runCatching { api.logout().message }.getOrElse { "Signed out locally (server unreachable)." }
            } else {
                "Signed out locally."
            }
        tokenVault.clearSession()
        return NetworkResult.Success(serverMsg)
    }

    suspend fun forgotPassword(email: String): NetworkResult<MessageResponseDto> {
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return NetworkResult.Error("Please enter a valid email address.", "VALIDATION")
        }
        ensureBackendConfigured()?.let { return it }

        return runCatching {
            val res = api.forgotPassword(ForgotPasswordRequestDto(email = email.trim().lowercase()))
            NetworkResult.Success(res)
        }.getOrElse { ApiErrorMapper.map(it) }
    }

    suspend fun resetPassword(
        token: String,
        newPassword: String,
        confirmPassword: String,
    ): NetworkResult<MessageResponseDto> {
        if (token.isBlank()) {
            return NetworkResult.Error("Reset token is required.", "VALIDATION")
        }
        if (newPassword.length < 8 || newPassword != confirmPassword) {
            return NetworkResult.Error("Passwords must match and be at least 8 characters.", "VALIDATION")
        }
        ensureBackendConfigured()?.let { return it }

        return runCatching {
            val res =
                api.resetPassword(
                    ResetPasswordRequestDto(
                        token = token.trim(),
                        newPassword = newPassword,
                        confirmPassword = confirmPassword,
                    )
                )
            NetworkResult.Success(res)
        }.getOrElse { ApiErrorMapper.map(it) }
    }

    suspend fun getCachedUser(): UserProfile? = tokenVault.getCachedUser()

    private fun UserDto.toDomain() =
        UserProfile(
            id = id,
            fullName = fullName,
            email = email,
            preferredTitle = preferredTitle,
            isVerified = isVerified,
            createdAt = createdAt,
        )
}
