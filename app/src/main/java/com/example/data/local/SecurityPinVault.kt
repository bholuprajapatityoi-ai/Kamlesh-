package com.example.data.local

import android.content.Context
import com.example.core.KeystoreCrypto
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

class SecurityPinVault(
    context: Context,
    private val crypto: KeystoreCrypto,
) {
    private val prefs = context.getSharedPreferences("golu_pin_vault", Context.MODE_PRIVATE)

    fun isPinConfigured(): Boolean = !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()

    fun isPinLockEnabled(): Boolean = prefs.getBoolean(KEY_PIN_ENABLED, false) && isPinConfigured()

    fun isFatherModeEnabled(): Boolean = prefs.getBoolean(KEY_FATHER_MODE, false)

    fun setFatherModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FATHER_MODE, enabled).apply()
    }

    fun setPin(pin: String): Boolean {
        val cleaned = pin.trim()
        if (cleaned.length !in 4..6 || !cleaned.all { it.isDigit() }) return false
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val digest = hashPin(cleaned, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, crypto.encrypt(saltB64))
            .putString(KEY_PIN_HASH, crypto.encrypt(digest))
            .putBoolean(KEY_PIN_ENABLED, true)
            .apply()
        return true
    }

    fun setPinLockEnabled(enabled: Boolean) {
        if (enabled && !isPinConfigured()) return
        prefs.edit().putBoolean(KEY_PIN_ENABLED, enabled).apply()
    }

    fun verifyPin(candidate: String): Boolean {
        val encSalt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val encHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val saltB64 = crypto.decrypt(encSalt) ?: return false
        val expectedHash = crypto.decrypt(encHash) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val actualHash = hashPin(candidate.trim(), salt)
        return MessageDigest.isEqual(actualHash.toByteArray(), expectedHash.toByteArray())
    }

    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_PIN_ENABLED, false)
            .apply()
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        md.update(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(md.digest(), Base64.NO_WRAP)
    }

    private companion object {
        const val KEY_PIN_SALT = "enc_pin_salt"
        const val KEY_PIN_HASH = "enc_pin_hash"
        const val KEY_PIN_ENABLED = "pin_lock_enabled"
        const val KEY_FATHER_MODE = "father_friendly_mode"
    }
}
