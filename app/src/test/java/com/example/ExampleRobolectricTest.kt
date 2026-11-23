package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.BackendConfig
import com.example.core.BackendConfigStatus
import com.example.domain.ai.AndroidActionExecutor
import com.example.domain.ai.IntentClassifier
import com.example.domain.ai.OfflineCommandEngine
import com.example.domain.model.GoluSettings
import com.example.domain.model.IntentType
import com.example.domain.model.RoutingTarget
import com.example.domain.model.WakeWordState
import com.example.domain.wakeword.WakeWordEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GOLU", appName)
    }

    @Test
    fun `backend config rejects localhost and insecure http`() {
        assertEquals(BackendConfigStatus.UNCONFIGURED, BackendConfig.evaluate("").status)
        assertEquals(
            BackendConfigStatus.INVALID_LOCALHOST,
            BackendConfig.evaluate("https://127.0.0.1:8000").status,
        )
        assertEquals(
            BackendConfigStatus.INVALID_LOCALHOST,
            BackendConfig.evaluate("http://localhost:8000").status,
        )
        assertEquals(
            BackendConfigStatus.INSECURE_HTTP,
            BackendConfig.evaluate("http://api.example.com").status,
        )
        assertEquals(
            BackendConfigStatus.READY_HTTPS,
            BackendConfig.evaluate("https://api.example.com").status,
        )
        assertEquals(
            BackendConfigStatus.READY_EMULATOR_DEV,
            BackendConfig.evaluate("http://10.0.2.2:8000").status,
        )
    }

    @Test
    fun `intent classifier and offline engine handle Hinglish commands`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = OfflineCommandEngine(context, AndroidActionExecutor(context))
        val settings = GoluSettings()

        assertEquals(IntentType.TIME_QUERY, IntentClassifier.classify("Time kya hua?"))
        assertEquals(IntentType.BATTERY_QUERY, IntentClassifier.classify("Battery kitni hai?"))
        assertEquals(IntentType.WHATSAPP_ACTION, IntentClassifier.classify("WhatsApp kholo."))
        assertEquals(IntentType.EXPLANATION, IntentClassifier.classify("Photosynthesis samjhao."))
        assertEquals(IntentType.SENSITIVE_ACTION, IntentClassifier.classify("delete my files"))

        val timeResult = engine.tryExecute("Time kya hua?", IntentType.TIME_QUERY, settings)
        assertTrue(timeResult.handled)
        assertEquals(RoutingTarget.OFFLINE_ENGINE, timeResult.target)
        assertTrue(timeResult.reply.contains("Boss"))

        val onlineResult = engine.tryExecute("Photosynthesis samjhao.", IntentType.EXPLANATION, settings)
        assertFalse(onlineResult.handled)
    }

    @Test
    fun `wake word engine starts disabled by default for privacy`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val wakeEngine = WakeWordEngine(context)
        assertEquals(WakeWordState.DISABLED, wakeEngine.state.value)
    }
}
