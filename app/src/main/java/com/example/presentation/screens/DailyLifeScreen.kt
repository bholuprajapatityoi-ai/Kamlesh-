package com.example.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.remote.dto.CalendarEventDto
import com.example.data.remote.dto.DailyRoutineDto
import com.example.data.remote.dto.NotificationPreferencesDto
import com.example.data.remote.dto.ReminderDto
import com.example.data.remote.dto.TaskDto
import com.example.data.remote.dto.TimetableDto
import com.example.domain.daily.ReminderScheduler
import com.example.domain.model.DeviceInfoSnapshot
import com.example.domain.model.Phase7To13UiState
import com.example.presentation.components.GoluGlassCard
import com.example.ui.theme.GoluAmber
import com.example.ui.theme.GoluCoralRed
import com.example.ui.theme.GoluEmerald
import com.example.ui.theme.GoluNeonCyan

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyLifeScreen(
    uiState: Phase7To13UiState,
    deviceInfo: DeviceInfoSnapshot,
    reminders: List<ReminderDto>,
    tasks: List<TaskDto>,
    timetable: List<TimetableDto>,
    events: List<CalendarEventDto>,
    routines: List<DailyRoutineDto>,
    notificationPrefs: NotificationPreferencesDto?,
    onRefresh: () -> Unit,
    onAddReminder: (title: String, notes: String, delayMinutes: Long, recurrence: String) -> Unit,
    onToggleReminder: (ReminderDto) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onAddTask: (title: String, priority: String) -> Unit,
    onToggleTask: (TaskDto) -> Unit,
    onDeleteTask: (String) -> Unit,
    onAddTimetable: (day: String, start: String, end: String, subject: String, location: String) -> Unit,
    onDeleteTimetable: (String) -> Unit,
    onAddCalendarEvent: (title: String, desc: String, delayMinutes: Long) -> Unit,
    onDeleteCalendarEvent: (String) -> Unit,
    onAddRoutine: (name: String, triggerTime: String, summaryPrompt: String) -> Unit,
    onUpdateNotificationPrefs: (morning: Boolean?, evening: Boolean?, reminders: Boolean?) -> Unit,
    onSpeakSummary: (isEvening: Boolean) -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    var remTitle by remember { mutableStateOf("") }
    var remDelayMin by remember { mutableLongStateOf(15L) }
    var remRecurring by remember { mutableStateOf(false) }

    var taskTitle by remember { mutableStateOf("") }
    var taskPriority by remember { mutableStateOf("MEDIUM") }

    var ttDay by remember { mutableStateOf("Monday") }
    var ttStart by remember { mutableStateOf("09:00") }
    var ttEnd by remember { mutableStateOf("10:00") }
    var ttSubject by remember { mutableStateOf("") }

    var evTitle by remember { mutableStateOf("") }
    var routineName by remember { mutableStateOf("") }
    var routineTime by remember { mutableStateOf("07:30") }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) { onRefresh() }

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
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("daily_back_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text("Daily Life Assistant", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "Timezone: ${deviceInfo.timezoneId} • WorkManager + PostgreSQL",
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
                    ) { Text(msg, color = GoluEmerald, modifier = Modifier.padding(12.dp)) }
                }

                // Morning Greeting, Evening Summary & Exact Alarm Truthfulness
                GoluGlassCard(accentBorder = GoluAmber.copy(alpha = 0.5f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.WbSunny, contentDescription = null, tint = GoluAmber)
                        Text("Daily Briefings & Permissions", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onSpeakSummary(false) }, modifier = Modifier.weight(1f).testTag("morning_greeting_btn")) {
                            Text("Morning Greeting")
                        }
                        OutlinedButton(onClick = { onSpeakSummary(true) }, modifier = Modifier.weight(1f).testTag("evening_summary_btn")) {
                            Text("Evening Summary")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val exactAllowed = ReminderScheduler.canScheduleExactAlarms(context)
                    Text(
                        text = if (exactAllowed) "Exact alarm scheduling: Permitted" else "Using Play-compliant WorkManager reminders (exact alarm permission optional).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Allow Reminder Notifications")
                        }
                    }
                    if (!exactAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        TextButton(onClick = onOpenExactAlarmSettings) {
                            Icon(Icons.Filled.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Open Alarms & Reminders Settings")
                        }
                    }
                    notificationPrefs?.let { prefs ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Morning Greeting Alert", style = MaterialTheme.typography.bodySmall)
                            Switch(checked = prefs.morningGreetingEnabled, onCheckedChange = { onUpdateNotificationPrefs(it, null, null) })
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Evening Summary Alert", style = MaterialTheme.typography.bodySmall)
                            Switch(checked = prefs.eveningSummaryEnabled, onCheckedChange = { onUpdateNotificationPrefs(null, it, null) })
                        }
                    }
                }

                // Reminders
                GoluGlassCard {
                    Text("Reminders (${reminders.size})", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = remTitle,
                        onValueChange = { remTitle = it },
                        label = { Text("Reminder title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reminder_title_input"),
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5L to "5 min", 15L to "15 min", 60L to "1 hr", 240L to "4 hr").forEach { (mins, label) ->
                            FilterChip(selected = remDelayMin == mins, onClick = { remDelayMin = mins }, label = { Text(label) })
                        }
                        FilterChip(selected = remRecurring, onClick = { remRecurring = !remRecurring }, label = { Text("Daily Repeat") })
                    }
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = {
                            if (remTitle.isNotBlank()) {
                                onAddReminder(remTitle.trim(), "", remDelayMin, if (remRecurring) "DAILY" else "NONE")
                                remTitle = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("reminder_add_button"),
                    ) { Text("Schedule Reminder") }
                    reminders.forEach { rem ->
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                IconButton(onClick = { onToggleReminder(rem) }) {
                                    Icon(
                                        if (rem.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                        contentDescription = "Toggle reminder",
                                        tint = if (rem.isCompleted) GoluEmerald else GoluNeonCyan,
                                    )
                                }
                                Column {
                                    Text(rem.title, style = MaterialTheme.typography.bodyMedium)
                                    Text("${rem.recurrence} • ${rem.timezoneName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                            IconButton(onClick = { onDeleteReminder(rem.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete reminder", tint = GoluCoralRed)
                            }
                        }
                    }
                }

                // Tasks
                GoluGlassCard {
                    Text("Tasks (${tasks.size})", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            label = { Text("New task") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("task_title_input"),
                        )
                        Button(onClick = {
                            if (taskTitle.isNotBlank()) {
                                onAddTask(taskTitle.trim(), taskPriority)
                                taskTitle = ""
                            }
                        }) { Text("Add") }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                            FilterChip(selected = taskPriority == p, onClick = { taskPriority = p }, label = { Text(p) })
                        }
                    }
                    tasks.forEach { task ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                IconButton(onClick = { onToggleTask(task) }) {
                                    Icon(
                                        if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                        contentDescription = "Toggle task",
                                        tint = if (task.isCompleted) GoluEmerald else GoluNeonCyan,
                                    )
                                }
                                Text("${task.title} [${task.priority}]", style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { onDeleteTask(task.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete task", tint = GoluCoralRed)
                            }
                        }
                    }
                }

                // Timetable & Calendar Events & Routines
                GoluGlassCard {
                    Text("Timetable, Calendar & Routines", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = ttDay, onValueChange = { ttDay = it }, label = { Text("Day") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = ttStart, onValueChange = { ttStart = it }, label = { Text("Start") }, singleLine = true, modifier = Modifier.weight(0.7f))
                        OutlinedTextField(value = ttEnd, onValueChange = { ttEnd = it }, label = { Text("End") }, singleLine = true, modifier = Modifier.weight(0.7f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = ttSubject, onValueChange = { ttSubject = it }, label = { Text("Subject / Activity") }, singleLine = true, modifier = Modifier.weight(1f))
                        Button(onClick = {
                            if (ttSubject.isNotBlank()) {
                                onAddTimetable(ttDay.trim(), ttStart.trim(), ttEnd.trim(), ttSubject.trim(), "")
                                ttSubject = ""
                            }
                        }) { Text("Add") }
                    }
                    timetable.forEach { tt ->
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("• ${tt.dayOfWeek} ${tt.startTime}-${tt.endTime}: ${tt.subjectOrActivity}", style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { onDeleteTimetable(tt.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete timetable", tint = GoluCoralRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = evTitle, onValueChange = { evTitle = it }, label = { Text("Calendar Event") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = {
                            if (evTitle.isNotBlank()) {
                                onAddCalendarEvent(evTitle.trim(), "", 60)
                                evTitle = ""
                            }
                        }) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                    events.forEach { ev ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("• Event: ${ev.title}", style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { onDeleteCalendarEvent(ev.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete event", tint = GoluCoralRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = routineName, onValueChange = { routineName = it }, label = { Text("Routine Name") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = routineTime, onValueChange = { routineTime = it }, label = { Text("Time") }, singleLine = true, modifier = Modifier.weight(0.6f))
                        OutlinedButton(onClick = {
                            if (routineName.isNotBlank()) {
                                onAddRoutine(routineName.trim(), routineTime.trim(), "")
                                routineName = ""
                            }
                        }) { Text("Save") }
                    }
                    routines.forEach { rt ->
                        Text("• Routine: ${rt.name} @ ${rt.triggerTime}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}
