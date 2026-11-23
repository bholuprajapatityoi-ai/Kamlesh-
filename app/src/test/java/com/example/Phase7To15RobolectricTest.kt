package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.KeystoreCrypto
import com.example.data.local.SecurityPinVault
import com.example.data.remote.dto.AutomationActionDto
import com.example.data.remote.dto.AutomationConditionDto
import com.example.data.remote.dto.AutomationDto
import com.example.data.remote.dto.ContactAliasDto
import com.example.domain.ai.AndroidActionExecutor
import com.example.domain.ai.IntentClassifier
import com.example.domain.ai.OfflineCommandEngine
import com.example.domain.automation.AutomationEvaluator
import com.example.domain.daily.ReminderScheduler
import com.example.domain.model.ContactLookupResult
import com.example.domain.model.GoluLanguage
import com.example.domain.model.GoluSettings
import com.example.domain.model.IntentType
import com.example.domain.phone.ContactLookupHelper
import com.example.domain.voice.VoiceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase7To15RobolectricTest {

    @Test
    fun `explicit memory activation requires confirmation and never silently saves`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = OfflineCommandEngine(context, AndroidActionExecutor(context))
        val settings = GoluSettings(language = GoluLanguage.HINGLISH)

        val intent = IntentClassifier.classify("GOLU, ye yaad rakhna meri favourite app Spotify hai")
        assertEquals(IntentType.MEMORY_SAVE, intent)

        val result = engine.tryExecute(
            text = "GOLU, ye yaad rakhna meri favourite app Spotify hai",
            intentType = intent,
            settings = settings,
        )
        assertTrue(result.handled)
        assertTrue(result.requiresConfirmation)
        assertEquals("Favourite Apps", result.pendingMemoryCategory)
        assertNotNull(result.pendingMemoryContent)
        assertTrue(result.pendingMemoryContent!!.contains("Spotify"))
    }

    @Test
    fun `ambiguous memory request asks clarification without setting pending save`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = OfflineCommandEngine(context, AndroidActionExecutor(context))
        val result = engine.tryExecute("GOLU, ye yaad rakhna", IntentType.MEMORY_SAVE, GoluSettings())
        assertTrue(result.handled)
        assertFalse(result.requiresConfirmation)
        assertEquals(null, result.pendingMemoryContent)
    }

    @Test
    fun `whatsapp message prepares editable draft and requires explicit confirmation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = OfflineCommandEngine(context, AndroidActionExecutor(context))
        val intent = IntentClassifier.classify("Papa ko WhatsApp message bhejo")
        assertEquals(IntentType.WHATSAPP_ACTION, intent)

        val res = engine.tryExecute("Papa ko WhatsApp message bhejo", intent, GoluSettings())
        assertTrue(res.handled)
        assertTrue(res.requiresConfirmation)
        assertEquals("Papa", res.pendingWhatsAppRecipient)
        assertFalse(res.pendingWhatsAppDraft.isNullOrBlank())
    }

    @Test
    fun `security pin vault hashes, verifies, rejects wrong pin, and supports father mode flag`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val aesKey = javax.crypto.KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val vault = SecurityPinVault(context, KeystoreCrypto("golu_test_pin_key", testOnlySecretKey = aesKey))
        vault.clearPin()
        assertFalse(vault.isPinLockEnabled())

        assertFalse(vault.setPin("12"))
        assertTrue(vault.setPin("2580"))
        assertTrue(vault.isPinLockEnabled())
        assertTrue(vault.verifyPin("2580"))
        assertFalse(vault.verifyPin("9999"))

        vault.setFatherModeEnabled(true)
        assertTrue(vault.isFatherModeEnabled())
        vault.clearPin()
        assertFalse(vault.isPinLockEnabled())
    }

    @Test
    fun `contact lookup resolves alias phone hint cleanly even before runtime permission`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = ContactLookupHelper(context)
        val aliases = listOf(ContactAliasDto(id = "1", nickname = "Papa", contactName = "Dadaji", phoneHint = "+919876543210"))

        val single = helper.lookupByQuery("call Papa", aliases)
        assertTrue(single is ContactLookupResult.Single)
        assertEquals("+919876543210", (single as ContactLookupResult.Single).match.phoneNumber)

        val missingPerm = helper.lookupByQuery("call UnknownPerson", aliases)
        assertTrue(missingPerm is ContactLookupResult.PermissionRequired)
    }

    @Test
    fun `automation evaluator enforces enabled state and sensitive confirmation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val evaluator = AutomationEvaluator(context, AndroidActionExecutor(context), VoiceEngine(context))

        val disabledRule = AutomationDto(
            id = "r1",
            name = "Disabled Rule",
            triggerType = "TIME",
            triggerValue = "07:00",
            isEnabled = false,
            requiresConfirmation = false,
        )
        assertEquals("SKIPPED", evaluator.runRuleSafely(disabledRule).status)

        val sensitiveRule = AutomationDto(
            id = "r2",
            name = "Open App Rule",
            triggerType = "COMMAND",
            triggerValue = "morning",
            isEnabled = true,
            requiresConfirmation = true,
            conditions = listOf(AutomationConditionDto("DEVICE_STATE", "NORMAL")),
            actions = listOf(AutomationActionDto("OPEN_APP", "Settings")),
        )
        val unconfirmed = evaluator.runRuleSafely(sensitiveRule, confirmedSensitive = false)
        assertFalse(unconfirmed.executed)
        assertTrue(unconfirmed.requiresConfirmation)
        assertEquals("CONFIRMATION_REQUIRED", unconfirmed.status)
    }

    @Test
    fun `device info snapshot and daily reminder scheduler checks operate cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val snapshot = AndroidActionExecutor(context).getDeviceInfoSnapshot()
        assertFalse(snapshot.timezoneId.isBlank())
        assertTrue(snapshot.sdkInt > 0)
        // Ensure permission check API doesn't crash on Robolectric
        ReminderScheduler.canScheduleExactAlarms(context)
    }
}
