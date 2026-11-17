package com.learning.mylittleschool

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDate

private data class PendingLearningBackup(
    val text: String,
    val snapshot: LearningBackupSnapshot
)

@Composable
fun BackupRestoreScreen(
    database: LearningDatabase,
    settingsStore: AppSettingsStore,
    padding: PaddingValues,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context, database, settingsStore) {
        LearningBackupRepository(context, database, settingsStore)
    }
    val scope = rememberCoroutineScope()
    var pendingBackup by remember { mutableStateOf<PendingLearningBackup?>(null) }
    var working by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                working = true
                message = "Preparing backup…"
                val result = runCatching {
                    val backup = repository.createBackup()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            output.write(backup.toByteArray(Charsets.UTF_8))
                        } ?: error("Could not open the selected destination.")
                    }
                }
                message = if (result.isSuccess) {
                    "Backup saved. Keep the file somewhere safe."
                } else {
                    result.exceptionOrNull()?.message ?: "Could not create the backup."
                }
                working = false
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                working = true
                message = "Checking backup file…"
                val result = runCatching {
                    val backupText = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            String(input.readBounded(LearningBackupCodec.MAX_BACKUP_BYTES), Charsets.UTF_8)
                        } ?: error("Could not open the selected backup.")
                    }
                    val snapshot = LearningBackupCodec.decode(backupText)
                    pendingBackup = PendingLearningBackup(backupText, snapshot)
                    "Backup checked. Review the restore details below."
                }
                message = result.getOrElse {
                    it.message ?: "This backup file could not be read."
                }
                working = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Back to Parent Dashboard") }
        PageHeading(
            eyebrow = "LOCAL DATA TOOLS",
            title = "Backup & restore",
            subtitle = "Save a portable copy of this device's Little Genius learning data."
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Included in the backup", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Child profiles, favorites, homework and deadlines, class selection, saved lesson history, quiz/game results, study planner progress and English/Hindi preference.")
                Text("The parent PIN is never exported. Downloaded PDFs and imported document links are not included; download or select those files again on another device.")
            }
        }
        Button(
            enabled = !working,
            onClick = {
                message = "Choose where to save the backup file."
                createBackupLauncher.launch("little-genius-backup-${LocalDate.now()}.json")
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Create backup / बैकअप बनाएँ") }
        OutlinedButton(
            enabled = !working,
            onClick = {
                message = "Select a Little Genius backup file."
                restoreBackupLauncher.launch(arrayOf("application/json", "text/plain"))
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Restore from backup / बैकअप से लौटाएँ") }
        if (message.isNotBlank()) {
            Text(message, color = LittleGeniusColors.Muted)
        }
        if (working) Text("Please keep the app open while this finishes.", color = LittleGeniusColors.Muted)
    }

    pendingBackup?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingBackup = null },
            title = { Text("Restore this backup?") },
            text = {
                Text(
                    "This backup contains ${pending.snapshot.profiles.size} child profiles, ${pending.snapshot.favorites.size} favorites and ${pending.snapshot.homework.size} homework tasks. Restoring it replaces those records and learning progress on this device. Backup date: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(pending.snapshot.createdAt))}. The parent PIN stays unchanged."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !working,
                    onClick = {
                        scope.launch {
                            working = true
                            val existingHomework = database.homeworkDao().getAllTasks()
                            existingHomework.forEach { task ->
                                HomeworkReminderWorker.cancel(context, task.profileId, task.taskId)
                            }
                            val result = runCatching { repository.restoreBackup(pending.text) }
                            if (result.isSuccess) {
                                pending.snapshot.homework.filter { it.reminderAt != null && it.reminderAt > System.currentTimeMillis() }
                                    .forEach { HomeworkReminderWorker.schedule(context, it) }
                            } else {
                                existingHomework.filter { it.reminderAt != null && it.reminderAt > System.currentTimeMillis() }
                                    .forEach { HomeworkReminderWorker.schedule(context, it) }
                            }
                            message = if (result.isSuccess) {
                                "Backup restored."
                            } else {
                                result.exceptionOrNull()?.message ?: "Could not restore the backup."
                            }
                            pendingBackup = null
                            working = false
                        }
                    }
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { pendingBackup = null }) { Text("Cancel") }
            }
        )
    }
}

private fun InputStream.readBounded(maxBytes: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        require(total <= maxBytes) { "Backup file is too large." }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}
