package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.common.GoluConstants
import com.example.domain.model.GoluLanguage
import com.example.domain.model.GoluSettings
import com.example.domain.model.ResponseStyle
import com.example.domain.model.VoiceGender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "golu_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val VOICE_GENDER = stringPreferencesKey("voice_gender")
        val SPEECH_SPEED = floatPreferencesKey("speech_speed")
        val LANGUAGE = stringPreferencesKey("language")
        val GREETING = stringPreferencesKey("greeting")
        val RESPONSE_STYLE = stringPreferencesKey("response_style")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val KEEP_SESSION_CONTEXT = booleanPreferencesKey("keep_session_context")
        val PRIVACY_REDACT_LOGS = booleanPreferencesKey("privacy_redact_logs")
    }

    val settingsFlow: Flow<GoluSettings> =
        context.settingsDataStore.data.map { prefs ->
            GoluSettings(
                isDarkTheme = prefs[Keys.DARK_THEME] ?: true,
                voiceGender =
                    VoiceGender.entries.firstOrNull { it.name == prefs[Keys.VOICE_GENDER] }
                        ?: VoiceGender.MALE,
                speechSpeed = (prefs[Keys.SPEECH_SPEED] ?: 1.0f).coerceIn(0.6f, 1.6f),
                language =
                    GoluLanguage.entries.firstOrNull { it.name == prefs[Keys.LANGUAGE] }
                        ?: GoluLanguage.HINGLISH,
                greeting = prefs[Keys.GREETING]?.takeIf { it.isNotBlank() }
                    ?: GoluConstants.DEFAULT_GREETING,
                responseStyle =
                    ResponseStyle.entries.firstOrNull { it.name == prefs[Keys.RESPONSE_STYLE] }
                        ?: ResponseStyle.PLAYFUL_BOSS,
                wakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: false,
                keepSessionContext = prefs[Keys.KEEP_SESSION_CONTEXT] ?: true,
                privacyRedactLogs = prefs[Keys.PRIVACY_REDACT_LOGS] ?: true,
            )
        }

    suspend fun updateDarkTheme(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun updateVoiceGender(gender: VoiceGender) {
        context.settingsDataStore.edit { it[Keys.VOICE_GENDER] = gender.name }
    }

    suspend fun updateSpeechSpeed(speed: Float) {
        context.settingsDataStore.edit { it[Keys.SPEECH_SPEED] = speed.coerceIn(0.6f, 1.6f) }
    }

    suspend fun updateLanguage(language: GoluLanguage) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun updateGreeting(greeting: String) {
        context.settingsDataStore.edit {
            it[Keys.GREETING] = greeting.ifBlank { GoluConstants.DEFAULT_GREETING }
        }
    }

    suspend fun updateResponseStyle(style: ResponseStyle) {
        context.settingsDataStore.edit { it[Keys.RESPONSE_STYLE] = style.name }
    }

    suspend fun updateWakeWordEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun updateKeepSessionContext(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.KEEP_SESSION_CONTEXT] = enabled }
    }

    suspend fun updatePrivacyRedactLogs(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.PRIVACY_REDACT_LOGS] = enabled }
    }
}
