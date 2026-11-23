package com.example.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.model.GoluSettings
import com.example.domain.model.VoiceState
import com.example.domain.model.WakeWordState
import com.example.presentation.components.GoluGlassCard
import com.example.presentation.components.GoluRobotOrb
import com.example.presentation.components.GoluSmartModulesCard
import com.example.presentation.components.StatusPill
import com.example.ui.theme.GoluAmber
import com.example.ui.theme.GoluCoralRed
import com.example.ui.theme.GoluElectricBlue
import com.example.ui.theme.GoluEmerald
import com.example.ui.theme.GoluNeonCyan

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssistantScreen(
    settings: GoluSettings,
    voiceState: VoiceState,
    wakeWordState: WakeWordState,
    wakeWordNote: String,
    audioLevel: Float,
    partialTranscript: String,
    voiceError: String?,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onPauseSpeaking: () -> Unit,
    onResumeSpeaking: () -> Unit,
    onStopSpeaking: () -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onOpenBatteryOptimization: () -> Unit,
    onRunSamplePrompt: (String) -> Unit,
    onOpenModule: (String) -> Unit = {},
) {
    val context = LocalContext.current

    val notifPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            onToggleWakeWord(true)
        }

    val wakeMicLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                ) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onToggleWakeWord(true)
                }
            }
        }

    val voiceMicLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) onStartListening()
        }

    fun requestWakeWordToggle(enable: Boolean) {
        if (!enable) {
            onToggleWakeWord(false)
            return
        }
        val hasMic =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        if (!hasMic) {
            wakeMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        onToggleWakeWord(true)
    }

    val sampleCommands =
        listOf(
            "Time kya hua?",
            "Battery kitni hai?",
            "WhatsApp kholo",
            "Aaj date kya hai?",
            "Photosynthesis samjhao",
            "Alarm lagao 07:00",
        )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Voice & Wake-Word Cockpit",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                // Phase 4 Voice Engine Card
                GoluGlassCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StatusPill(
                                label = "VOICE: ${voiceState.name}",
                                icon = Icons.Filled.Mic,
                                tint =
                                    when (voiceState) {
                                        VoiceState.LISTENING -> GoluNeonCyan
                                        VoiceState.SPEAKING -> GoluElectricBlue
                                        VoiceState.ERROR -> GoluCoralRed
                                        VoiceState.PROCESSING -> GoluAmber
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                            Text(
                                text = "${settings.voiceGender.label} • ${String.format("%.1fx", settings.speechSpeed)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }

                        GoluRobotOrb(
                            voiceState = voiceState,
                            wakeWordState = wakeWordState,
                            audioLevel = audioLevel,
                            size = 156.dp,
                            onTap = {
                                val hasMic =
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO,
                                    ) == PackageManager.PERMISSION_GRANTED
                                if (hasMic) onStartListening()
                                else voiceMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            modifier = Modifier.testTag("assistant_robot_orb"),
                        )

                        if (partialTranscript.isNotBlank()) {
                            Text(
                                text = "\"$partialTranscript\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = GoluNeonCyan,
                            )
                        }
                        if (!voiceError.isNullOrBlank()) {
                            Text(
                                text = voiceError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = GoluCoralRed,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = {
                                    if (voiceState == VoiceState.LISTENING) {
                                        onStopListening()
                                    } else {
                                        val hasMic =
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO,
                                            ) == PackageManager.PERMISSION_GRANTED
                                        if (hasMic) onStartListening()
                                        else voiceMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("assistant_listen_button"),
                                shape = CircleShape,
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            if (voiceState == VoiceState.LISTENING) GoluCoralRed
                                            else GoluElectricBlue
                                    ),
                            ) {
                                Icon(
                                    imageVector =
                                        if (voiceState == VoiceState.LISTENING) Icons.Filled.Stop
                                        else Icons.Filled.Mic,
                                    contentDescription = "Listen",
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (voiceState == VoiceState.LISTENING) "Stop Mic" else "Speak Now")
                            }

                            if (voiceState == VoiceState.SPEAKING || voiceState == VoiceState.PAUSED) {
                                OutlinedButton(
                                    onClick = {
                                        if (voiceState == VoiceState.SPEAKING) onPauseSpeaking()
                                        else onResumeSpeaking()
                                    },
                                    shape = CircleShape,
                                ) {
                                    Icon(
                                        imageVector =
                                            if (voiceState == VoiceState.SPEAKING) Icons.Filled.Pause
                                            else Icons.Filled.PlayArrow,
                                        contentDescription = "Pause/Resume",
                                    )
                                }
                                OutlinedButton(
                                    onClick = onStopSpeaking,
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, GoluCoralRed),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Stop,
                                        contentDescription = "Stop TTS",
                                        tint = GoluCoralRed,
                                    )
                                }
                            }
                        }
                    }
                }

                // Phase 5 Hey Golu Wake-Word State Machine Card
                GoluGlassCard(
                    accentBorder =
                        if (wakeWordState == WakeWordState.MONITORING) GoluEmerald.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "\"Hey Golu\" Wake-Word",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "State: ${wakeWordState.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color =
                                    if (wakeWordState == WakeWordState.MONITORING) GoluEmerald
                                    else MaterialTheme.colorScheme.secondary,
                            )
                        }
                        Switch(
                            checked = settings.wakeWordEnabled,
                            onCheckedChange = ::requestWakeWordToggle,
                            modifier = Modifier.testTag("wake_word_switch"),
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = wakeWordNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Privacy",
                            tint = GoluNeonCyan,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text =
                                "On-device local wake processing only. No continuous cloud audio uploads. Lock screen is never bypassed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenBatteryOptimization,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.BatterySaver, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Check Battery Optimization Restrictions")
                    }
                }

                // Sample Hybrid Commands
                GoluGlassCard {
                    Text(
                        text = "Try Hybrid Commands (Offline + Online)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        sampleCommands.forEach { cmd ->
                            AssistChip(
                                onClick = { onRunSamplePrompt(cmd) },
                                label = { Text(cmd) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.GraphicEq,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFF38BDF8),
                                    )
                                },
                            )
                        }
                    }
                }

                GoluSmartModulesCard(onOpenModule = onOpenModule)
            }
        }
    }
}
