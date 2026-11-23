package com.example.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.remote.dto.ContactAliasDto
import com.example.data.remote.dto.MessagingPreferencesDto
import com.example.data.remote.dto.MessagingTemplateDto
import com.example.data.remote.dto.PhonePreferencesDto
import com.example.domain.model.ContactLookupResult
import com.example.domain.model.ContactMatch
import com.example.domain.model.DeviceInfoSnapshot
import com.example.domain.model.Phase7To13UiState
import com.example.domain.model.WhatsAppDraft
import com.example.presentation.components.GoluGlassCard
import com.example.ui.theme.GoluCoralRed
import com.example.ui.theme.GoluElectricBlue
import com.example.ui.theme.GoluEmerald
import com.example.ui.theme.GoluNeonCyan

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhoneWhatsAppScreen(
    uiState: Phase7To13UiState,
    deviceInfo: DeviceInfoSnapshot,
    phonePrefs: PhonePreferencesDto?,
    aliases: List<ContactAliasDto>,
    lookupResult: ContactLookupResult?,
    pendingCallMatch: ContactMatch?,
    messagingPrefs: MessagingPreferencesDto?,
    templates: List<MessagingTemplateDto>,
    activeDraft: WhatsAppDraft?,
    onLoad: () -> Unit,
    onVolumeAdjust: (String) -> Unit,
    onMediaControl: (String) -> Unit,
    onOpenSettingsCategory: (String) -> Unit,
    onSaveAlias: (nickname: String, contactName: String, phoneHint: String) -> Unit,
    onDeleteAlias: (String) -> Unit,
    onLookupContact: (String) -> Unit,
    onSelectContact: (ContactMatch) -> Unit,
    onConfirmCall: (ContactMatch) -> Unit,
    onCancelCall: () -> Unit,
    onUpdatePhonePrefs: (confirmBeforeCall: Boolean?, allowVolume: Boolean?, allowMedia: Boolean?, allowContacts: Boolean?) -> Unit,
    onUpdateMessagingPrefs: (defaultLang: String?, alwaysConfirm: Boolean?) -> Unit,
    onPrepareWhatsAppDraft: (recipient: String, phone: String, body: String, lang: String) -> Unit,
    onConfirmLaunchWhatsApp: (WhatsAppDraft) -> Unit,
    onSaveTemplate: (title: String, lang: String, body: String) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    var contactQuery by remember { mutableStateOf("") }
    var aliasNickname by remember { mutableStateOf("Papa") }
    var aliasName by remember { mutableStateOf("") }
    var aliasPhone by remember { mutableStateOf("") }

    var waRecipient by remember { mutableStateOf(activeDraft?.recipientName ?: "Papa") }
    var waPhone by remember { mutableStateOf(activeDraft?.phoneNumber ?: "") }
    var waLanguage by remember { mutableStateOf(activeDraft?.language ?: (messagingPrefs?.defaultLanguage ?: "Hinglish")) }
    var waMessage by remember { mutableStateOf(activeDraft?.messageBody ?: "Namaste Papa, main pahunch gaya hoon.") }
    var templateTitle by remember { mutableStateOf("") }

    val contactsPermLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted && contactQuery.isNotBlank()) onLookupContact(contactQuery)
        }

    LaunchedEffect(Unit) { onLoad() }
    LaunchedEffect(activeDraft) {
        activeDraft?.let {
            waRecipient = it.recipientName
            waPhone = it.phoneNumber
            waLanguage = it.language
            waMessage = it.messageBody
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("phone_wa_back_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text("Phone Control & WhatsApp", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "${deviceInfo.manufacturer} ${deviceInfo.model} • Android ${deviceInfo.androidVersion}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoluNeonCyan,
                        )
                    }
                }

                uiState.statusMessage?.let { msg ->
                    Surface(
                        color = GoluEmerald.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoluEmerald),
                    ) {
                        Text(msg, color = GoluEmerald, modifier = Modifier.padding(12.dp))
                    }
                }
                uiState.errorMessage?.let { err ->
                    Surface(
                        color = GoluCoralRed.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoluCoralRed),
                    ) {
                        Text(err, color = GoluCoralRed, modifier = Modifier.padding(12.dp))
                    }
                }

                // Volume, Media & Quick OS Settings
                GoluGlassCard {
                    Text("Volume & Media Playback", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { onVolumeAdjust("up") }, modifier = Modifier.testTag("volume_up_btn")) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Volume Up", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Vol +")
                        }
                        OutlinedButton(onClick = { onVolumeAdjust("down") }, modifier = Modifier.testTag("volume_down_btn")) {
                            Icon(Icons.AutoMirrored.Filled.VolumeDown, contentDescription = "Volume Down", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Vol -")
                        }
                        OutlinedButton(onClick = { onVolumeAdjust("mute") }, modifier = Modifier.testTag("volume_mute_btn")) {
                            Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = "Mute", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Mute")
                        }
                        OutlinedButton(onClick = { onMediaControl("previous") }) {
                            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous track")
                        }
                        OutlinedButton(onClick = { onMediaControl("play_pause") }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play or Pause")
                            Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                        OutlinedButton(onClick = { onMediaControl("next") }) {
                            Icon(Icons.Filled.SkipNext, contentDescription = "Next track")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("System Settings Shortcuts", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("wifi" to "Wi-Fi", "bluetooth" to "Bluetooth", "display" to "Display", "sound" to "Sound", "app_info" to "App Info").forEach { (key, label) ->
                            AssistChip(
                                onClick = { onOpenSettingsCategory(key) },
                                label = { Text(label) },
                                leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            )
                        }
                    }
                    phonePrefs?.let { prefs ->
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Confirm before Dialer Call", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = prefs.confirmBeforeCall,
                                onCheckedChange = { onUpdatePhonePrefs(it, null, null, null) },
                            )
                        }
                    }
                }

                // Contacts Lookup & Nickname Aliases
                GoluGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Contacts, contentDescription = null, tint = GoluNeonCyan)
                        Text("Contacts & Nicknames (Papa, Mummy...)", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = contactQuery,
                            onValueChange = { contactQuery = it },
                            label = { Text("Search name or nickname") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("contact_lookup_input"),
                        )
                        Button(
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                                if (granted) onLookupContact(contactQuery)
                                else contactsPermLauncher.launch(Manifest.permission.READ_CONTACTS)
                            },
                            modifier = Modifier.testTag("contact_lookup_button"),
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = "Lookup Contact")
                        }
                    }

                    when (lookupResult) {
                        is ContactLookupResult.PermissionRequired ->
                            Text("Grant Contacts permission to search device contacts, or use nickname aliases below.", color = GoluCoralRed, style = MaterialTheme.typography.bodySmall)
                        is ContactLookupResult.NotFound ->
                            Text("No matching contact found for \"$contactQuery\".", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                        is ContactLookupResult.Multiple -> {
                            Spacer(Modifier.height(6.dp))
                            Text("Multiple contacts matched — pick one, Boss:", style = MaterialTheme.typography.labelMedium, color = GoluNeonCyan)
                            lookupResult.matches.forEach { m ->
                                OutlinedButton(
                                    onClick = {
                                        onSelectContact(m)
                                        waRecipient = m.displayName
                                        waPhone = m.phoneNumber
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                ) {
                                    Text("${m.displayName} (${m.phoneNumber})")
                                }
                            }
                        }
                        is ContactLookupResult.Single -> {
                            Spacer(Modifier.height(6.dp))
                            Text("Matched: ${lookupResult.match.displayName} • ${lookupResult.match.phoneNumber}", color = GoluEmerald)
                            OutlinedButton(
                                onClick = {
                                    waRecipient = lookupResult.match.displayName
                                    waPhone = lookupResult.match.phoneNumber
                                },
                                modifier = Modifier.padding(top = 4.dp),
                            ) { Text("Use for WhatsApp Draft") }
                        }
                        null -> Unit
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("Save Nickname Alias", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = aliasNickname,
                            onValueChange = { aliasNickname = it },
                            label = { Text("Nickname") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = aliasName,
                            onValueChange = { aliasName = it },
                            label = { Text("Contact Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = aliasPhone,
                            onValueChange = { aliasPhone = it },
                            label = { Text("Phone (optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = {
                            if (aliasNickname.isNotBlank() && aliasName.isNotBlank()) {
                                onSaveAlias(aliasNickname.trim(), aliasName.trim(), aliasPhone.trim())
                                aliasName = ""
                                aliasPhone = ""
                            }
                        }) { Text("Save") }
                    }

                    aliases.forEach { alias ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("• ${alias.nickname} → ${alias.contactName} (${alias.phoneHint})", style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { onDeleteAlias(alias.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete alias", tint = GoluCoralRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Phase 9 — WhatsApp Review & Explicit Confirmation Composer
                GoluGlassCard(accentBorder = GoluEmerald.copy(alpha = 0.6f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = GoluEmerald)
                        Text("WhatsApp Draft & Confirmation", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "GOLU drafts your message in Hindi, Hinglish, or English. Nothing is sent silently — review & confirm before opening WhatsApp.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Hinglish", "Hindi", "English").forEach { lang ->
                            FilterChip(
                                selected = waLanguage == lang,
                                onClick = {
                                    waLanguage = lang
                                    onUpdateMessagingPrefs(lang, null)
                                    waMessage =
                                        when (lang) {
                                            "Hindi" -> "नमस्ते $waRecipient, मैं सुरक्षित पहुँच गया हूँ।"
                                            "English" -> "Hello $waRecipient, I have reached safely. Speak soon!"
                                            else -> "Namaste $waRecipient, main pahunch gaya hoon, sab badhiya hai."
                                        }
                                },
                                label = { Text(lang) },
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = waRecipient,
                            onValueChange = { waRecipient = it },
                            label = { Text("Recipient") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = waPhone,
                            onValueChange = { waPhone = it },
                            label = { Text("Phone (+91...)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = waMessage,
                        onValueChange = { waMessage = it },
                        label = { Text("Editable Message Draft") },
                        modifier = Modifier.fillMaxWidth().testTag("whatsapp_draft_input"),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onPrepareWhatsAppDraft(waRecipient, waPhone, waMessage, waLanguage)
                                onConfirmLaunchWhatsApp(WhatsAppDraft(waRecipient, waPhone, waMessage, waLanguage))
                            },
                            enabled = waMessage.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = GoluEmerald),
                            modifier = Modifier.weight(1f).testTag("whatsapp_confirm_open_btn"),
                        ) {
                            Text("Confirm & Open WhatsApp")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = templateTitle,
                            onValueChange = { templateTitle = it },
                            label = { Text("Save as template title") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(onClick = {
                            if (templateTitle.isNotBlank() && waMessage.isNotBlank()) {
                                onSaveTemplate(templateTitle.trim(), waLanguage, waMessage.trim())
                                templateTitle = ""
                            }
                        }) { Text("Save") }
                    }
                    templates.forEach { tpl ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = { waMessage = tpl.body; waLanguage = tpl.language }) {
                                Text("${tpl.title} (${tpl.language})")
                            }
                            IconButton(onClick = { onDeleteTemplate(tpl.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete template", tint = GoluCoralRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    pendingCallMatch?.let { match ->
        AlertDialog(
            onDismissRequest = onCancelCall,
            title = { Text("Confirm Call with ${match.displayName}?") },
            text = { Text("GOLU will open the phone dialer with ${match.phoneNumber}. You press Call to place the call.") },
            confirmButton = {
                Button(onClick = { onConfirmCall(match) }) { Text("Open Dialer") }
            },
            dismissButton = {
                TextButton(onClick = onCancelCall) { Text("Cancel") }
            },
        )
    }
}
