package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.KeystoreCrypto
import com.example.domain.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.tokenDataStore by preferencesDataStore(name = "golu_secure_vault")

/**
 * Stores access/refresh tokens encrypted with Android Keystore AES-256-GCM inside DataStore.
 */
class TokenVault(
    private val context: Context,
    private val crypto: KeystoreCrypto,
) {
    private val keyAccessToken = stringPreferencesKey("enc_access_token")
    private val keyRefreshToken = stringPreferencesKey("enc_refresh_token")
    private val keyUserId = stringPreferencesKey("user_id")
    private val keyUserName = stringPreferencesKey("user_name")
    private val keyUserEmail = stringPreferencesKey("user_email")

    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var cachedRefreshToken: String? = null

    suspend fun saveSession(accessToken: String, refreshToken: String, user: UserProfile) {
        cachedAccessToken = accessToken
        cachedRefreshToken = refreshToken
        val encAccess = crypto.encrypt(accessToken)
        val encRefresh = crypto.encrypt(refreshToken)
        context.tokenDataStore.edit { prefs ->
            prefs[keyAccessToken] = encAccess
            prefs[keyRefreshToken] = encRefresh
            prefs[keyUserId] = user.id
            prefs[keyUserName] = user.fullName
            prefs[keyUserEmail] = user.email
        }
    }

    suspend fun getAccessToken(): String? {
        cachedAccessToken?.let { return it }
        val enc = context.tokenDataStore.data.first()[keyAccessToken] ?: return null
        return crypto.decrypt(enc).also { cachedAccessToken = it }
    }

    fun getAccessTokenBlocking(): String? {
        cachedAccessToken?.let { return it }
        return runBlocking { getAccessToken() }
    }

    suspend fun getRefreshToken(): String? {
        cachedRefreshToken?.let { return it }
        val enc = context.tokenDataStore.data.first()[keyRefreshToken] ?: return null
        return crypto.decrypt(enc).also { cachedRefreshToken = it }
    }

    suspend fun getCachedUser(): UserProfile? {
        val prefs = context.tokenDataStore.data.first()
        val id = prefs[keyUserId] ?: return null
        val name = prefs[keyUserName] ?: return null
        val email = prefs[keyUserEmail] ?: return null
        return UserProfile(id = id, fullName = name, email = email)
    }

    suspend fun clearSession() {
        cachedAccessToken = null
        cachedRefreshToken = null
        context.tokenDataStore.edit { it.clear() }
    }
}
