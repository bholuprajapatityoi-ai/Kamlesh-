package com.example.presentation.screens.v2

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.components.GoluGlassCard
import com.example.presentation.viewmodel.GoluV2ViewModel
import com.example.ui.theme.GoluAmber
import com.example.ui.theme.GoluCoralRed
import com.example.ui.theme.GoluElectricBlue
import com.example.ui.theme.GoluEmerald
import com.example.ui.theme.GoluNeonCyan

@Composable
fun BrainKnowledgeScreen(
    v2ViewModel: GoluV2ViewModel,
    initialTab: Int = 0,
    onBack: () -> Unit,
) {
    val uiState by v2ViewModel.uiState.collectAsStateWithLifecycle()
    val knowledgeNotes by v2ViewModel.knowledgeNotes.collectAsStateWithLifecycle()
    val latestTranslation by v2ViewModel.latestTranslation.collectAsStateWithLifecycle()
    val translations by v2ViewModel.translations.collectAsStateWithLifecycle()
    val researchReports by v2ViewModel.researchReports.collectAsStateWithLifecycle()
    val offlineManifest by v2ViewModel.offlineManifest.collectAsStateWithLifecycle()
    val offlineTestOutput by v2ViewModel.offlineTestOutput.collectAsStateWithLifecycle()

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0 -> v2ViewModel.loadKnowledge()
            1 -> v2ViewModel.loadTranslations()
            2 -> v2ViewModel.loadResearchHistory()
            3 -> v2ViewModel.loadOfflineManifest()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 700.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Golu2HeaderBar(
                    title = "Advanced Brain & Knowledge",
                    subtitle = "Unified Memory, Translator, Deep Research & Offline Brain",
                    onBack = onBack,
                )
                Golu2SegmentedTabs(
                    tabs = listOf("Knowledge", "Translator", "Research", "Offline Brain"),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                    prefixTag = "brain",
                )
                Golu2StatusBanners(uiState = uiState, onDismiss = v2ViewModel::clearFeedback)

                when (selectedTab) {
                    0 -> KnowledgeBrainSection(v2ViewModel = v2ViewModel, notes = knowledgeNotes)
                    1 -> TranslatorSection(v2ViewModel = v2ViewModel, latest = latestTranslation, history = translations)
                    2 -> DeepResearchSection(v2ViewModel = v2ViewModel, reports = researchReports)
                    3 -> OfflineBrainSection(
                        v2ViewModel = v2ViewModel,
                        manifest = offlineManifest,
                        testOutput = offlineTestOutput,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun KnowledgeBrainSection(
    v2ViewModel: GoluV2ViewModel,
    notes: List<com.example.data.remote.dto.KnowledgeNoteResponseDto>,
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Study Notes") }
    var tags by remember { mutableStateOf("important, boss") }
    var content by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<com.example.data.remote.dto.KnowledgeNoteResponseDto?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("") }
    var editTags by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }

    GoluGlassCard(accentBorder = GoluNeonCyan.copy(alpha = 0.5f)) {
        Text("Unified Knowledge Brain & Smart Memory", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Note Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("v2_knowledge_title"),
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = tags,
            onValueChange = { tags = it },
            label = { Text("Tags (comma separated)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Knowledge Content") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth().testTag("v2_knowledge_content"),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                v2ViewModel.saveKnowledgeNote(title, category, content, tags)
                title = ""
                content = ""
            },
            enabled = title.isNotBlank() && content.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("v2_knowledge_save_btn"),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Save to Knowledge Brain")
        }
    }

    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search Notes & Tags") },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("v2_knowledge_search"),
        )
        OutlinedButton(onClick = { v2ViewModel.loadKnowledge(query.ifBlank { null }) }) {
            Icon(Icons.Filled.Search, contentDescription = "Search knowledge")
        }
    }

    notes.forEach { note ->
        Spacer(Modifier.height(8.dp))
        GoluGlassCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(note.title, style = MaterialTheme.typography.titleMedium, color = GoluNeonCyan)
                    Text(
                        text = "${note.category} ${if (note.tags.isNotEmpty()) "• ${note.tags.joinToString(", ")}" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoluEmerald,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(note.content, style = MaterialTheme.typography.bodyMedium)
                }
                Row {
                    IconButton(
                        onClick = {
                            editingNote = note
                            editTitle = note.title
                            editCategory = note.category
                            editTags = note.tags.joinToString(", ")
                            editContent = note.content
                        },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit note")
                    }
                    IconButton(
                        onClick = { v2ViewModel.deleteKnowledgeNote(note.id) },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete note", tint = GoluCoralRed)
                    }
                }
            }
        }
    }

    editingNote?.let { note ->
        AlertDialog(
            onDismissRequest = { editingNote = null },
            title = { Text("Edit Knowledge Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Title") })
                    OutlinedTextField(value = editCategory, onValueChange = { editCategory = it }, label = { Text("Category") })
                    OutlinedTextField(value = editTags, onValueChange = { editTags = it }, label = { Text("Tags") })
                    OutlinedTextField(value = editContent, onValueChange = { editContent = it }, label = { Text("Content") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        v2ViewModel.updateKnowledgeNote(note.id, editTitle, editCategory, editContent, editTags)
                        editingNote = null
                    },
                    enabled = editTitle.isNotBlank() && editContent.isNotBlank(),
                ) { Text("Update") }
            },
            dismissButton = { TextButton(onClick = { editingNote = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TranslatorSection(
    v2ViewModel: GoluV2ViewModel,
    latest: com.example.data.remote.dto.TranslationResponseDto?,
    history: List<com.example.data.remote.dto.TranslationResponseDto>,
) {
    var text by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("auto") }
    var target by remember { mutableStateOf("Hindi") }
    var speakOut by remember { mutableStateOf(true) }

    GoluGlassCard(accentBorder = GoluEmerald.copy(alpha = 0.5f)) {
        Text("Live Language Translator", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Enter text to translate") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth().testTag("v2_translator_input"),
        )
        Spacer(Modifier.height(6.dp))
        Text("Source:", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("auto", "English", "Hindi", "Tamil", "Spanish").forEach { lang ->
                FilterChip(selected = source == lang, onClick = { source = lang }, label = { Text(lang) })
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Target:", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Hindi", "English", "Spanish", "French", "German", "Japanese").forEach { lang ->
                FilterChip(selected = target == lang, onClick = { target = lang }, label = { Text(lang) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = speakOut, onCheckedChange = { speakOut = it })
            Text("Speak translation aloud using GOLU TTS", style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = { v2ViewModel.translateText(text, source, target, speakOut) },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("v2_translate_btn"),
        ) {
            Icon(Icons.Filled.Translate, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Translate to $target")
        }
    }

    latest?.let { tr ->
        Spacer(Modifier.height(8.dp))
        GoluGlassCard(accentBorder = GoluNeonCyan) {
            Text("${tr.sourceLanguage} → ${tr.targetLanguage}", style = MaterialTheme.typography.labelSmall, color = GoluNeonCyan)
            Spacer(Modifier.height(4.dp))
            Text(tr.translatedText, style = MaterialTheme.typography.titleMedium)
        }
    }

    history.take(5).forEach { tr ->
        Spacer(Modifier.height(6.dp))
        GoluGlassCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${tr.sourceLanguage} → ${tr.targetLanguage}", style = MaterialTheme.typography.labelSmall, color = GoluEmerald)
                    Text(tr.sourceText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(tr.translatedText, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(
                    onClick = { v2ViewModel.deleteTranslation(tr.id) },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete translation", tint = GoluCoralRed)
                }
            }
        }
    }
}

@Composable
private fun DeepResearchSection(
    v2ViewModel: GoluV2ViewModel,
    reports: List<com.example.data.remote.dto.ResearchReportResponseDto>,
) {
    var query by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    GoluGlassCard(accentBorder = GoluElectricBlue.copy(alpha = 0.6f)) {
        Text("Deep Research Mode", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Uses verified search providers (Tavily / Wikipedia) + distinguishes verified facts from uncertain points.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Research Topic or Question") },
            modifier = Modifier.fillMaxWidth().testTag("v2_research_input"),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { v2ViewModel.runResearch(query) },
            enabled = query.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("v2_research_btn"),
        ) {
            Text("Run Deep Research")
        }
    }

    reports.forEach { report ->
        Spacer(Modifier.height(10.dp))
        GoluGlassCard {
            Text(report.query, style = MaterialTheme.typography.titleMedium, color = GoluNeonCyan)
            Text("Provider: ${report.providerUsed}", style = MaterialTheme.typography.labelSmall, color = GoluElectricBlue)
            Spacer(Modifier.height(6.dp))
            Text(report.summary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = GoluEmerald.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, GoluEmerald.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Verified Findings", style = MaterialTheme.typography.labelLarge, color = GoluEmerald)
                    Text(report.verifiedFindings, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = GoluAmber.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, GoluAmber.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Uncertain / Gaps", style = MaterialTheme.typography.labelLarge, color = GoluAmber)
                    Text(report.uncertainPoints, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (report.citations.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Citations:", style = MaterialTheme.typography.labelLarge)
                report.citations.forEach { cit ->
                    OutlinedButton(
                        onClick = { runCatching { uriHandler.openUri(cit.url) } },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(cit.title, style = MaterialTheme.typography.labelMedium)
                            Text(cit.url, style = MaterialTheme.typography.bodySmall, color = GoluNeonCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineBrainSection(
    v2ViewModel: GoluV2ViewModel,
    manifest: com.example.data.remote.dto.OfflineManifestResponseDto?,
    testOutput: String?,
) {
    var query by remember { mutableStateOf("calculate 125 * 8") }

    GoluGlassCard(accentBorder = GoluAmber.copy(alpha = 0.6f)) {
        Text("Advanced Offline Brain Tester", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Runs 100% locally for math, unit conversions, device commands, and Hindi/English phrasebook — with honest cloud boundaries.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Try: '125 * 8', '10 km', 'good morning', or general prompt") },
            modifier = Modifier.fillMaxWidth().testTag("v2_offline_input"),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { v2ViewModel.testOfflineBrain(query) },
            enabled = query.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("v2_offline_test_btn"),
        ) {
            Text("Evaluate Locally")
        }
        testOutput?.let { out ->
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, GoluNeonCyan.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().testTag("v2_offline_output"),
            ) {
                Text(out, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    manifest?.let { m ->
        Spacer(Modifier.height(8.dp))
        GoluGlassCard {
            Text("Offline-Capable Modules:", style = MaterialTheme.typography.labelLarge, color = GoluEmerald)
            m.offlineCapableFeatures.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(6.dp))
            Text("Requires Cloud Connection:", style = MaterialTheme.typography.labelLarge, color = GoluAmber)
            m.onlineRequiredFeatures.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            if (m.cloudAiNote.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(m.cloudAiNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
