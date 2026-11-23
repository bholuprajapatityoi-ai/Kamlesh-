package com.example.core

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import org.json.JSONObject
import retrofit2.HttpException

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Error(
        val message: String,
        val causeCategory: String = "NETWORK_ERROR",
        val httpCode: Int? = null,
    ) : NetworkResult<Nothing>
}

object ApiErrorMapper {
    fun map(throwable: Throwable): NetworkResult.Error {
        return when (throwable) {
            is HttpException -> {
                val code = throwable.code()
                val rawBody = runCatching { throwable.response()?.errorBody()?.string() }.getOrNull()
                val parsed = parseBackendDetail(rawBody) ?: "Server returned HTTP $code"
                NetworkResult.Error(
                    message = parsed,
                    causeCategory = if (code == 401 || code == 403) "AUTH_ERROR" else "HTTP_$code",
                    httpCode = code,
                )
            }
            is SocketTimeoutException ->
                NetworkResult.Error(
                    message = "Backend request timed out. Check server load or network latency.",
                    causeCategory = "TIMEOUT",
                )
            is UnknownHostException ->
                NetworkResult.Error(
                    message = "Backend Unreachable: Cannot resolve server hostname. Verify DNS and deployed URL.",
                    causeCategory = "DNS_FAILURE",
                )
            is ConnectException ->
                NetworkResult.Error(
                    message = "Backend Unreachable: Connection refused by server. Verify FastAPI is running on the configured port.",
                    causeCategory = "CONNECTION_REFUSED",
                )
            is SSLException ->
                NetworkResult.Error(
                    message = "TLS/HTTPS error while connecting to backend. Verify SSL certificate.",
                    causeCategory = "SSL_ERROR",
                )
            is IOException ->
                NetworkResult.Error(
                    message = "Backend Unreachable: ${throwable.localizedMessage ?: "Network I/O error"}",
                    causeCategory = "IO_ERROR",
                )
            else ->
                NetworkResult.Error(
                    message = throwable.localizedMessage ?: "Unexpected error occurred.",
                    causeCategory = "UNKNOWN",
                )
        }
    }

    private fun parseBackendDetail(rawBody: String?): String? {
        if (rawBody.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(rawBody)
            when {
                json.has("detail") -> {
                    val detail = json.get("detail")
                    if (detail is String) detail else detail.toString()
                }
                json.has("message") -> json.getString("message")
                else -> null
            }
        }.getOrNull()
    }
}
