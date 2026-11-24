package com.example.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GoluCoralRed
import com.example.ui.theme.GoluElectricBlue
import com.example.ui.theme.GoluEmerald
import com.example.ui.theme.GoluNeonCyan

data class SmartModuleNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val goluSmartModuleItems = listOf(
    SmartModuleNavItem("memory", "Memory", Icons.Filled.Psychology),
    SmartModuleNavItem("phone_whatsapp", "Phone & WA", Icons.Filled.PhoneAndroid),
    SmartModuleNavItem("daily_life", "Daily Life", Icons.Filled.CalendarMonth),
    SmartModuleNavItem("family", "Family Mode", Icons.Filled.FamilyRestroom),
    SmartModuleNavItem("security", "Security", Icons.Filled.Security),
    SmartModuleNavItem("automation", "Automation", Icons.Filled.AutoFixHigh),
    SmartModuleNavItem("guide", "User Guide", Icons.Filled.MenuBook),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoluSmartModulesCard(
    onOpenModule: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    GoluGlassCard(modifier = modifier) {
        Text(
            text = "GOLU Smart Modules",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onOpenModule("v2_hub") },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_golu2_hub_button"),
            shape = RoundedCornerShape(14.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = GoluElectricBlue,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                ),
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("GOLU 2.0 Hub (20 AI Modules)")
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            goluSmartModuleItems.forEach { item ->
                AssistChip(
                    onClick = { onOpenModule(item.route) },
                    label = { Text(item.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = GoluNeonCyan,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    modifier = Modifier.testTag("module_chip_${item.route}"),
                )
            }
        }
    }
}

@Composable
fun FatherFriendlyHomePanel(
    onTalkToGolu: () -> Unit,
    onQuickCall: () -> Unit,
    onOpenWhatsApp: () -> Unit,
    onOpenReminders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GoluGlassCard(accentBorder = GoluEmerald.copy(alpha = 0.7f), modifier = modifier) {
        Text(
            text = "Papa-Friendly Quick Panel",
            style = MaterialTheme.typography.titleLarge,
            color = GoluNeonCyan,
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onTalkToGolu,
            modifier = Modifier.fillMaxWidth().height(58.dp).testTag("home_father_talk"),
            colors = ButtonDefaults.buttonColors(containerColor = GoluElectricBlue),
        ) {
            Icon(Icons.Filled.Mic, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("GOLU से बात करें", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onQuickCall,
                modifier = Modifier.weight(1f).height(56.dp).testTag("home_father_call"),
                colors = ButtonDefaults.buttonColors(containerColor = GoluEmerald),
            ) {
                Icon(Icons.Filled.Call, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Call")
            }
            Button(
                onClick = onOpenWhatsApp,
                modifier = Modifier.weight(1f).height(56.dp).testTag("home_father_wa"),
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("WhatsApp")
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onOpenReminders,
            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("home_father_reminders"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Icon(Icons.Filled.NotificationsActive, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Shared Family Reminders")
        }
    }
}

@Composable
fun PinUnlockOverlay(
    errorMessage: String?,
    onUnlock: (String) -> Boolean,
) {
    var pinInput by remember { mutableStateOf("") }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.98f))
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, GoluNeonCyan.copy(alpha = 0.6f)),
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Filled.Lock, contentDescription = "App Locked", tint = GoluNeonCyan, modifier = Modifier.size(44.dp))
                Text("GOLU Security Lock", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Enter your 4–6 digit Boss PIN to unlock personal assistant controls.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pinInput = it },
                    label = { Text("Enter PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().testTag("pin_unlock_input"),
                )
                errorMessage?.let { err ->
                    Text(err, color = GoluCoralRed, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = { if (onUnlock(pinInput)) pinInput = "" },
                    enabled = pinInput.length in 4..6,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("pin_unlock_button"),
                ) {
                    Text("Unlock GOLU")
                }
            }
        }
    }
}
