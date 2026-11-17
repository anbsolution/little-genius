package com.learning.mylittleschool

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@Composable
fun HomeworkPlannerScreen(
    database: LearningDatabase,
    profileId: String,
    padding: PaddingValues
) {
    val context = LocalContext.current
    val dao = remember(database) { database.homeworkDao() }
    val tasks by remember(dao, profileId) { dao.observeTasks(profileId) }
        .collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var title by remember(profileId) { mutableStateOf("") }
    var subject by remember(profileId) { mutableStateOf("") }
    var dueDate by remember(profileId) { mutableStateOf(LocalDate.now().plusDays(1)) }
    var dueTime by remember(profileId) { mutableStateOf(LocalTime.of(16, 0)) }
    var reminderEnabled by remember(profileId) { mutableStateOf(false) }
    var pendingReminderTask by remember { mutableStateOf<HomeworkEntity?>(null) }
    var message by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    suspend fun saveHomework(task: HomeworkEntity, allowReminder: Boolean) {
        saving = true
        val savedTask = if (allowReminder) task else task.copy(reminderAt = null)
        val result = runCatching {
            dao.save(savedTask)
            if (savedTask.reminderAt != null) {
                HomeworkReminderWorker.schedule(context, savedTask)
            } else {
                HomeworkReminderWorker.cancel(context, profileId, task.taskId)
            }
        }
        if (result.isSuccess) {
            title = ""
            subject = ""
            reminderEnabled = false
            message = when {
                task.reminderAt != null && allowReminder -> "Homework saved. A reminder is scheduled."
                task.reminderAt != null -> "Homework saved. Notifications are off, so no reminder was scheduled."
                else -> "Homework saved."
            }
        } else {
            message = result.exceptionOrNull()?.message ?: "Could not save this homework."
        }
        saving = false
    }

    val reminderPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val task = pendingReminderTask
        pendingReminderTask = null
        if (task != null) scope.launch { saveHomework(task, granted) }
    }

    fun addHomework() {
        val cleanTitle = title.trim()
        val cleanSubject = subject.trim()
        if (cleanTitle.isEmpty() || cleanSubject.isEmpty()) {
            message = "Add a homework title and subject first."
            return
        }
        val dueAt = dueDate.atTime(dueTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        if (dueAt <= now) {
            message = "Choose a future due date and time."
            return
        }
        val task = HomeworkEntity(
            taskId = UUID.randomUUID().toString(),
            profileId = profileId,
            title = cleanTitle,
            subject = cleanSubject,
            dueAt = dueAt,
            reminderAt = if (reminderEnabled) (dueAt - REMINDER_LEAD_MILLIS).coerceAtLeast(now) else null,
            isCompleted = false,
            createdAt = now
        )
        if (!reminderEnabled) {
            scope.launch { saveHomework(task, allowReminder = false) }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            pendingReminderTask = task
            reminderPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            scope.launch {
                saveHomework(task, allowReminder = NotificationManagerCompat.from(context).areNotificationsEnabled())
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PageHeading(
                eyebrow = "HOMEWORK · OFFLINE PLANNER",
                title = "Homework planner",
                subtitle = "Set due dates and optional reminders for this child."
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Add homework", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(160) },
                        label = { Text("Homework / काम") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it.take(80) },
                        label = { Text("Subject / विषय") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day -> dueDate = LocalDate.of(year, month + 1, day) },
                                    dueDate.year,
                                    dueDate.monthValue - 1,
                                    dueDate.dayOfMonth
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(dueDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())))
                        }
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute -> dueTime = LocalTime.of(hour, minute) },
                                    dueTime.hour,
                                    dueTime.minute,
                                    false
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(dueTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                        Column {
                            Text("Remind me / याद दिलाएँ", fontWeight = FontWeight.SemiBold)
                            Text("About 30 minutes before the due time", fontSize = 12.sp, color = LittleGeniusColors.Muted)
                        }
                    }
                    Button(
                        onClick = ::addHomework,
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (saving) "Saving…" else "Save homework") }
                    if (message.isNotBlank()) Text(message, color = LittleGeniusColors.Muted, fontSize = 13.sp)
                }
            }
        }
        item {
            Text("Your homework · ${tasks.count { !it.isCompleted }} open", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
        if (tasks.isEmpty()) {
            item { Text("No homework added yet. / अभी कोई गृहकार्य नहीं है।", color = LittleGeniusColors.Muted) }
        }
        items(tasks, key = { it.taskId }) { task ->
            HomeworkTaskCard(
                task = task,
                onCompletedChange = { completed ->
                    scope.launch {
                        dao.setCompleted(profileId, task.taskId, completed)
                        if (completed) {
                            HomeworkReminderWorker.cancel(context, profileId, task.taskId)
                        } else {
                            HomeworkReminderWorker.schedule(context, task.copy(isCompleted = false))
                        }
                    }
                },
                onDelete = {
                    scope.launch {
                        dao.delete(profileId, task.taskId)
                        HomeworkReminderWorker.cancel(context, profileId, task.taskId)
                    }
                }
            )
        }
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                "Reminders run on this device without internet and may be delayed by Android battery settings.",
                color = LittleGeniusColors.Muted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun HomeworkTaskCard(
    task: HomeworkEntity,
    onCompletedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val dueText = remember(task.dueAt) {
        Instant.ofEpochMilli(task.dueAt).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a", Locale.getDefault()))
    }
    val overdue = !task.isCompleted && task.dueAt < System.currentTimeMillis()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) androidx.compose.ui.graphics.Color(0xFFEAF8EF)
            else androidx.compose.ui.graphics.Color.White
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.isCompleted, onCheckedChange = onCompletedChange)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(task.title, fontWeight = FontWeight.Bold)
                Text(task.subject, color = LittleGeniusColors.Blue, fontSize = 13.sp)
                Text(
                    "${if (overdue) "Overdue · " else "Due · "}$dueText",
                    color = if (overdue) androidx.compose.ui.graphics.Color(0xFFB3261E) else LittleGeniusColors.Muted,
                    fontSize = 12.sp
                )
                if (!task.isCompleted && task.reminderAt != null) {
                    Text("Reminder set / अनुस्मारक चालू", color = LittleGeniusColors.Muted, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.width(2.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete ${task.title}", tint = LittleGeniusColors.Muted)
            }
        }
    }
}

private const val REMINDER_LEAD_MILLIS = 30L * 60L * 1000L
