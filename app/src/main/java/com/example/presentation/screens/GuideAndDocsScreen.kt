package com.example.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.presentation.components.GoluGlassCard
import com.example.ui.theme.GoluNeonCyan

@Composable
fun GuideAndDocsScreen(
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val sections = listOf(
        "1. Wake Phrase & Voice Commands" to
            "Say \"Hey Golu\" (when Wake-Word monitoring is enabled in Assistant tab) or tap the glowing GOLU orb. GOLU supports Hindi, Hinglish, and English.",
        "2. Smart Personal Memory (Phase 7)" to
            "Say \"GOLU, ye yaad rakhna...\" and confirm the card to save. GOLU never saves chats silently, and passwords/OTPs/API keys are automatically blocked.",
        "3. Smart Phone & WhatsApp (Phases 8–9)" to
            "Control volume, media playback, Wi-Fi/Bluetooth/Display settings, alarms, and dialer calls. For WhatsApp, GOLU prepares an editable draft in Hindi/Hinglish/English and waits for your confirmation before opening WhatsApp.",
        "4. Daily Life & WorkManager Reminders (Phase 10)" to
            "Manage Reminders, Tasks, Timetable, Calendar Events, and Morning/Evening summaries. Notifications include Done and Snooze (10m) actions.",
        "5. Family Mode & Papa Interface (Phase 11)" to
            "Create a family group and invite members with separate logins. Private memories and private chats are never visible to other members or admins. Enable Papa-Friendly Mode for extra-large simplified controls.",
        "6. Complete Security & Automatic Cloud Sync (Phase 12)" to
            "Set a Keystore-encrypted 4–6 digit PIN in Security Dashboard, revoke active sessions, or wipe personal data. GOLU connects automatically to its HTTPS backend on launch and reconnects whenever internet returns; check Settings > Automatic Backend Connection anytime to retry.",
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("guide_back_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text("GOLU User Guide & Release Info", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "App ID: ${BuildConfig.APPLICATION_ID} • v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoluNeonCyan,
                        )
                    }
                }

                sections.forEach { (title, body) ->
                    GoluGlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = GoluNeonCyan)
                            Text(title, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
