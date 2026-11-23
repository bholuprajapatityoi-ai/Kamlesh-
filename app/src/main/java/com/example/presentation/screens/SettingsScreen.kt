package com.example.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.model.BackendDiagnostics
import com.example.domain.model.GoluLanguage
import com.example.domain.model.GoluSettings
import com.example.domain.model.ResponseStyle
import com.example.domain.model.VoiceGender
import com.example.presentation.components.AutomaticBackendConnectionCard
import com.example.presentation.components.GoluGlassCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: GoluSettings,
    diagnostics: BackendDiagnostics,
    onDarkThemeChange: (Boolean) -> Unit,
    onVoiceGenderChange: (VoiceGender) -> Unit,
    onSpeechSpeedChange: (Float) -> Unit,
    onLanguageChange: (GoluLanguage) -> Unit,
    onGreetingChange: (String) -> Unit,
    onResponseStyleChange: (ResponseStyle) -> Unit,
    onWakeWordChange: (Boolean) -> Unit,
    onKeepContextChange: (Boolean) -> Unit,
    onRedactLogsChange: (Boolean) -> Unit,
    onRetryConnection: () -> Unit,
    onOpenBatteryOptimization: () -> Unit,
) {
    val context = LocalContext.current
    var greetingInput by remember(settings.greeting) { mutableStateOf(settings.greeting) }

    val notifLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            onWakeWordChange(true)
        }
    val micLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                ) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onWakeWordChange(true)
                }
            }
        }

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
                    text = "Settings & Diagnostics",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                // Appearance
                GoluGlassCard {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    SettingSwitchRow(
                        title = "Dark Navy Theme",
                        subtitle = "Signature dark cyber-navy UI with electric cyan accents",
                        checked = settings.isDarkTheme,
                        onCheckedChange = onDarkThemeChange,
                        testTag = "setting_dark_theme",
                    )
                }

                // Voice & Speech
                GoluGlassCard {
                    Text(
                        text = "Voice & Language (Phase 4)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(10.dp))

                    Text("Voice Personality", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        VoiceGender.entries.forEach { gender ->
                            FilterChip(
                                selected = settings.voiceGender == gender,
                                onClick = { onVoiceGenderChange(gender) },
                                label = { Text(gender.label) },
                                modifier = Modifier.testTag("voice_gender_${gender.name.lowercase()}"),
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Speech Speed: ${String.format("%.2fx", settings.speechSpeed)}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Slider(
                        value = settings.speechSpeed,
                        onValueChange = onSpeechSpeedChange,
                        valueRange = 0.6f..1.6f,
                        modifier = Modifier.testTag("speech_speed_slider"),
                    )

                    Spacer(Modifier.height(6.dp))
                    Text("Primary Language", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        GoluLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = settings.language == lang,
                                onClick = { onLanguageChange(lang) },
                                label = { Text(lang.label) },
                                modifier = Modifier.testTag("language_${lang.name.lowercase()}"),
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("Response Style", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        ResponseStyle.entries.forEach { style ->
                            FilterChip(
                                selected = settings.responseStyle == style,
                                onClick = { onResponseStyleChange(style) },
                                label = { Text(style.label) },
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = greetingInput,
                        onValueChange = { greetingInput = it },
                        label = { Text("Activation Greeting") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("greeting_input"),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onGreetingChange(greetingInput) },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Save Greeting")
                    }
                }

                // Wake-Word, Memory & Privacy
                GoluGlassCard {
                    Text(
                        text = "Wake-Word, Memory & Privacy",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    SettingSwitchRow(
                        title = "\"Hey Golu\" Wake-Word Monitoring",
                        subtitle = "Off by default. Uses foreground microphone service when enabled.",
                        checked = settings.wakeWordEnabled,
                        onCheckedChange = { enable ->
                            if (!enable) {
                                onWakeWordChange(false)
                            } else {
                                val hasMic =
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO,
                                    ) == PackageManager.PERMISSION_GRANTED
                                if (hasMic) onWakeWordChange(true)
                                else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        testTag = "setting_wake_word",
                    )

                    Spacer(Modifier.height(8.dp))
                    SettingSwitchRow(
                        title = "Temporary Session Context",
                        subtitle = "Multi-turn context during active session (permanent memory reserved for Phase 7).",
                        checked = settings.keepSessionContext,
                        onCheckedChange = onKeepContextChange,
                        testTag = "setting_temp_context",
                    )

                    Spacer(Modifier.height(8.dp))
                    SettingSwitchRow(
                        title = "Redact Sensitive Activity Logs",
                        subtitle = "Store intent categories instead of full prompt text in session history.",
                        checked = settings.privacyRedactLogs,
                        onCheckedChange = onRedactLogsChange,
                        testTag = "setting_privacy_redact",
                    )

                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onOpenBatteryOptimization,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Open Battery Optimization Settings")
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text =
                            "Android Limitations: Background wake-word accuracy depends on OEM battery savers and device SpeechRecognizer offline pack support. Screen-off activation is subject to OS microphone & lock-screen policies.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Automatic Backend Connection Status (Zero manual URL controls)
                AutomaticBackendConnectionCard(
                    diagnostics = diagnostics,
                    onRetryConnection = onRetryConnection,
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
        )
    }
}
