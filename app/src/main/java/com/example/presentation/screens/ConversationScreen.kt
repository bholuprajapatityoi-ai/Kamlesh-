package com.example.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.model.ChatMessage
import com.example.domain.model.RoutingTarget
import com.example.domain.model.VoiceState
import com.example.ui.theme.GoluAmber
import com.example.ui.theme.GoluCoralRed
import com.example.ui.theme.GoluElectricBlue
import com.example.ui.theme.GoluEmerald
import com.example.ui.theme.GoluNeonCyan

@Composable
fun ConversationScreen(
    messages: List<ChatMessage>,
    isThinking: Boolean,
    voiceState: VoiceState,
    onSendPrompt: (String) -> Unit,
    onConfirmSensitive: (String) -> Unit,
    onConfirmSaveMemory: (category: String, content: String) -> Unit = { _, _ -> },
    onOpenWhatsAppDraft: (recipient: String, draft: String) -> Unit = { _, _ -> },
    onConfirmCallTarget: (String) -> Unit = {},
    onVoiceInput: () -> Unit,
    onStopVoice: () -> Unit,
    onRetryLast: () -> Unit,
    onClearContext: () -> Unit,
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val micLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) onVoiceInput()
        }

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top Bar
        Row(
            modifier =
                Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "GOLU Conversation",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Temporary multi-turn session context",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onRetryLast,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("chat_retry_button"),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Retry last message")
                }
                IconButton(
                    onClick = onClearContext,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("chat_clear_button"),
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear temporary context")
                }
            }
        }

        // Chat Bubbles
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).widthIn(max = 680.dp).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(
                    message = msg,
                    onConfirmSensitive = onConfirmSensitive,
                    onConfirmSaveMemory = onConfirmSaveMemory,
                    onOpenWhatsAppDraft = onOpenWhatsAppDraft,
                    onConfirmCallTarget = onConfirmCallTarget,
                )
            }

            if (isThinking) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, GoluAmber.copy(alpha = 0.5f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = GoluNeonCyan,
                            )
                            Text(
                                text = "GOLU soch raha hai, Boss...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }

        // Stop Speaking pill if TTS is playing
        if (voiceState == VoiceState.SPEAKING || voiceState == VoiceState.LISTENING) {
            OutlinedButton(
                onClick = onStopVoice,
                shape = CircleShape,
                border = BorderStroke(1.dp, GoluCoralRed),
                modifier = Modifier.padding(bottom = 6.dp).testTag("chat_stop_voice_button"),
            ) {
                Icon(Icons.Filled.Stop, contentDescription = "Stop Voice", tint = GoluCoralRed)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (voiceState == VoiceState.SPEAKING) "Stop Speaking" else "Stop Listening",
                    color = GoluCoralRed,
                )
            }
        }

        // Input Composer
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier =
                    Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = {
                        val hasMic =
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO,
                            ) == PackageManager.PERMISSION_GRANTED
                        if (hasMic) onVoiceInput()
                        else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("chat_mic_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Voice Input",
                        tint = if (voiceState == VoiceState.LISTENING) GoluNeonCyan else GoluElectricBlue,
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask GOLU in Hindi, Hinglish, or English...") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f).testTag("chat_text_input"),
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendPrompt(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("chat_send_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = GoluNeonCyan,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onConfirmSensitive: (String) -> Unit,
    onConfirmSaveMemory: (String, String) -> Unit,
    onOpenWhatsAppDraft: (String, String) -> Unit,
    onConfirmCallTarget: (String) -> Unit,
) {
    val alignment = if (message.isFromUser) Alignment.End else Alignment.Start
    val bubbleColor =
        when {
            message.isFromUser -> GoluElectricBlue
            message.isError -> GoluCoralRed.copy(alpha = 0.14f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    val textColor =
        if (message.isFromUser) Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        Surface(
            shape =
                RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (message.isFromUser) 18.dp else 4.dp,
                    bottomEnd = if (message.isFromUser) 4.dp else 18.dp,
                ),
            color = bubbleColor,
            border =
                if (!message.isFromUser) {
                    BorderStroke(
                        1.dp,
                        if (message.isError) GoluCoralRed.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    )
                } else null,
            modifier = Modifier.widthIn(max = 520.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (!message.isFromUser && message.routingTarget != null) {
                    val badgeLabel =
                        when (message.routingTarget) {
                            RoutingTarget.OFFLINE_ENGINE -> "OFFLINE ENGINE"
                            RoutingTarget.ANDROID_ACTION -> "ANDROID ACTION"
                            RoutingTarget.ONLINE_AI_BACKEND -> "ONLINE AI"
                        }
                    val badgeColor =
                        when (message.routingTarget) {
                            RoutingTarget.OFFLINE_ENGINE -> GoluNeonCyan
                            RoutingTarget.ANDROID_ACTION -> GoluEmerald
                            RoutingTarget.ONLINE_AI_BACKEND -> GoluElectricBlue
                        }
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                )
                if (message.pendingConfirmationCommand != null) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onConfirmSensitive(message.pendingConfirmationCommand) },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = GoluAmber, contentColor = Color.Black),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Confirm Action")
                    }
                }
                if (message.pendingMemoryContent != null) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onConfirmSaveMemory(
                                message.pendingMemoryCategory ?: "Custom Notes",
                                message.pendingMemoryContent,
                            )
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = GoluEmerald),
                        modifier = Modifier.testTag("chat_confirm_memory_btn"),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Confirm Save Memory")
                    }
                }
                if (message.pendingWhatsAppDraft != null) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onOpenWhatsAppDraft(
                                message.pendingWhatsAppRecipient ?: "Contact",
                                message.pendingWhatsAppDraft,
                            )
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = GoluEmerald),
                        modifier = Modifier.testTag("chat_review_whatsapp_btn"),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Review & Open WhatsApp")
                    }
                }
                if (message.pendingCallTarget != null) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onConfirmCallTarget(message.pendingCallTarget) },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = GoluNeonCyan, contentColor = Color.Black),
                        modifier = Modifier.testTag("chat_confirm_call_btn"),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Confirm Call")
                    }
                }
            }
        }
    }
}
