package com.example.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.core.BatteryTelemetry
import com.example.domain.model.AuthState
import com.example.domain.model.BackendDiagnostics
import com.example.domain.model.GoluSettings
import com.example.domain.model.VoiceState
import com.example.domain.model.WakeWordState
import com.example.presentation.components.FatherFriendlyHomePanel
import com.example.presentation.components.GoluGlassCard
import com.example.presentation.components.GoluLogoBadge
import com.example.presentation.components.GoluRobotOrb
import com.example.presentation.components.GoluSmartModulesCard
import com.example.presentation.components.StatusPill
import com.example.ui.theme.GoluCoralRed
import com.example.ui.theme.GoluElectricBlue
import com.example.ui.theme.GoluEmerald
import com.example.ui.theme.GoluNeonCyan

private data class QuickActionSpec(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
)

@Composable
fun HomeScreen(
    settings: GoluSettings,
    authState: AuthState,
    diagnostics: BackendDiagnostics,
    voiceState: VoiceState,
    wakeWordState: WakeWordState,
    audioLevel: Float,
    partialTranscript: String,
    voiceError: String?,
    battery: BatteryTelemetry,
    currentTime: String,
    currentDate: String,
    onMicClick: () -> Unit,
    onStopVoice: () -> Unit,
    onPauseVoice: () -> Unit,
    onResumeVoice: () -> Unit,
    onQuickAction: (String) -> Unit,
    onOpenApps: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToConversation: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onRetryConnection: () -> Unit = {},
    fatherFriendlyMode: Boolean = false,
    onOpenModule: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val micPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) onMicClick()
        }

    fun handleMicTrigger() {
        val hasMic =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        if (hasMic) {
            onMicClick()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val quickActions =
        listOf(
            QuickActionSpec("Call", Icons.Filled.Call, GoluEmerald),
            QuickActionSpec("WhatsApp", Icons.AutoMirrored.Filled.Chat, Color(0xFF25D366)),
            QuickActionSpec("Open Apps", Icons.Filled.Apps, GoluElectricBlue),
            QuickActionSpec("Alarm", Icons.Filled.AccessAlarm, Color(0xFFF59E0B)),
            QuickActionSpec("Reminders", Icons.Filled.NotificationsActive, GoluNeonCyan),
            QuickActionSpec("Calendar", Icons.Filled.CalendarMonth, Color(0xFF818CF8)),
            QuickActionSpec(
                "Battery",
                if (battery.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd,
                GoluEmerald,
            ),
            QuickActionSpec("Settings", Icons.Filled.Settings, Color(0xFF94A3B8)),
        )

    Box(modifier = Modifier.fillMaxSize()) {
        // Subtle atmospheric HUD backdrop
        if (settings.isDarkTheme) {
            Image(
                painter = painterResource(id = R.drawable.img_golu_hud_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.16f),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(
                    modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Header: GOLU Logo + Date/Time + Telemetry Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GoluLogoBadge()
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = currentTime,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = currentDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Live Status Row: Online/Offline + Battery + Wake-Word
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val backendUp = diagnostics.isBackendReachable
                        StatusPill(
                            label = if (backendUp) "ONLINE" else "OFFLINE",
                            icon = if (backendUp) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                            tint = if (backendUp) GoluEmerald else Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f),
                        )
                        StatusPill(
                            label = "${battery.levelPercent}%${if (battery.isCharging) " ⚡" else ""}",
                            icon =
                                if (battery.isCharging) Icons.Filled.BatteryChargingFull
                                else Icons.Filled.BatteryStd,
                            tint = if (battery.levelPercent > 20) GoluNeonCyan else GoluCoralRed,
                        )
                        StatusPill(
                            label =
                                if (wakeWordState == WakeWordState.MONITORING) "HEY GOLU ON"
                                else "WAKE OFF",
                            icon = Icons.Filled.GraphicEq,
                            tint =
                                if (wakeWordState == WakeWordState.MONITORING) GoluElectricBlue
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Centerpiece Robot Card
                    GoluGlassCard(
                        accentBorder =
                            when (voiceState) {
                                VoiceState.LISTENING -> GoluNeonCyan.copy(alpha = 0.8f)
                                VoiceState.SPEAKING -> GoluElectricBlue.copy(alpha = 0.8f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                            }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val bossName =
                                (authState as? AuthState.Authenticated)?.user?.fullName?.split(" ")?.firstOrNull()
                            Text(
                                text =
                                    if (!bossName.isNullOrBlank()) "Boss $bossName • ${settings.language.label}"
                                    else "Boss Mode • ${settings.language.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            GoluRobotOrb(
                                voiceState = voiceState,
                                wakeWordState = wakeWordState,
                                audioLevel = audioLevel,
                                size = 188.dp,
                                onTap = ::handleMicTrigger,
                                modifier = Modifier.testTag("home_robot_orb"),
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = settings.greeting,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            val statusCaption =
                                when {
                                    voiceError != null -> voiceError
                                    voiceState == VoiceState.LISTENING ->
                                        partialTranscript.ifBlank { "Sun raha hoon Boss... Boliye!" }
                                    voiceState == VoiceState.PROCESSING -> "Soch raha hoon Boss..."
                                    voiceState == VoiceState.SPEAKING -> "GOLU bol raha hai..."
                                    voiceState == VoiceState.PAUSED -> "Speaking paused."
                                    else -> "Tap the mic or say \"Hey Golu\" (when enabled)"
                                }
                            Text(
                                text = statusCaption,
                                style = MaterialTheme.typography.bodyMedium,
                                color =
                                    if (voiceError != null) GoluCoralRed
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Large Microphone Button
                            Button(
                                onClick = ::handleMicTrigger,
                                modifier =
                                    Modifier
                                        .size(width = 210.dp, height = 56.dp)
                                        .testTag("home_mic_button"),
                                shape = CircleShape,
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            if (voiceState == VoiceState.LISTENING) GoluNeonCyan
                                            else GoluElectricBlue,
                                        contentColor =
                                            if (voiceState == VoiceState.LISTENING) Color(0xFF050B18)
                                            else Color.White,
                                    ),
                            ) {
                                Icon(
                                    imageVector =
                                        if (voiceState == VoiceState.LISTENING) Icons.Filled.Stop
                                        else Icons.Filled.Mic,
                                    contentDescription = "Microphone",
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text =
                                        when (voiceState) {
                                            VoiceState.LISTENING -> "Listening..."
                                            VoiceState.PROCESSING -> "Processing..."
                                            else -> "Talk to GOLU"
                                        },
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }

                            // Playback controls when GOLU is speaking or paused
                            AnimatedVisibility(
                                visible =
                                    voiceState == VoiceState.SPEAKING || voiceState == VoiceState.PAUSED
                            ) {
                                Row(
                                    modifier = Modifier.padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    if (voiceState == VoiceState.SPEAKING) onPauseVoice()
                                                    else onResumeVoice()
                                                },
                                                modifier = Modifier.minimumInteractiveComponentSize(),
                                            ) {
                                                Icon(
                                                    imageVector =
                                                        if (voiceState == VoiceState.SPEAKING) Icons.Filled.Pause
                                                        else Icons.Filled.PlayArrow,
                                                    contentDescription = "Pause or Resume Voice",
                                                    tint = GoluNeonCyan,
                                                )
                                            }
                                            IconButton(
                                                onClick = onStopVoice,
                                                modifier = Modifier.minimumInteractiveComponentSize(),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Stop,
                                                    contentDescription = "Stop Speaking",
                                                    tint = GoluCoralRed,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Backend Unreachable / Sign-in shortcut banner if needed
                    if (!diagnostics.isBackendReachable || authState is AuthState.Unauthenticated) {
                        Surface(
                            onClick = {
                                if (!diagnostics.isBackendReachable) onRetryConnection()
                                else onNavigateToProfile()
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth().testTag("home_connection_banner"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text =
                                            if (!diagnostics.isBackendReachable) diagnostics.statusSummary
                                            else "Sign in for Cloud AI & Sync",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text =
                                            if (!diagnostics.isBackendReachable)
                                                "Offline commands active. GOLU reconnects automatically."
                                            else "Tap to sign in or register your Boss profile.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = if (!diagnostics.isBackendReachable) "Retry" else "Account",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }

                    // Quick Actions Grid (4x2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Surface(
                            onClick = onNavigateToConversation,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = "Open Chat →",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }

                    quickActions.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            rowItems.forEach { spec ->
                                QuickActionTile(
                                    spec = spec,
                                    onClick = {
                                        when (spec.label) {
                                            "Open Apps" -> onOpenApps()
                                            "Settings" -> onNavigateToSettings()
                                            else -> onQuickAction(spec.label)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    if (fatherFriendlyMode) {
                        FatherFriendlyHomePanel(
                            onTalkToGolu = ::handleMicTrigger,
                            onQuickCall = { onQuickAction("Call") },
                            onOpenWhatsApp = { onOpenModule("phone_whatsapp") },
                            onOpenReminders = { onOpenModule("family") },
                        )
                    }

                    GoluSmartModulesCard(onOpenModule = onOpenModule)

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    spec: QuickActionSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier =
            modifier
                .height(68.dp)
                .testTag("quick_action_${spec.label.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(spec.tint.copy(alpha = 0.28f), spec.tint.copy(alpha = 0.10f))
                            )
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = spec.label,
                    tint = spec.tint,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = spec.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
