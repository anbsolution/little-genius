package com.learning.mylittleschool

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.UUID

private const val MAX_ANNOUNCEMENT_DURATION_MS = 30_000L
private const val MAX_ANNOUNCEMENTS_PER_CHILD = 20
private const val RECORDING_DIRECTORY = "parent_voice_announcements"

private data class RecordedAnnouncementClip(val file: File, val durationMs: Long)

@Composable
fun VoiceAnnouncementsScreen(
    database: LearningDatabase,
    profileId: String,
    parentStore: ProgressStore,
    padding: PaddingValues,
    onOpenParentDashboard: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember(database) { database.parentAnnouncementDao() }
    val announcements by remember(dao, profileId) { dao.observeAnnouncements(profileId) }
        .collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val recorder = remember(context) { LocalParentAnnouncementRecorder(context) }
    val player = remember { LocalAnnouncementPlayer() }

    var parentUnlocked by remember(profileId) { mutableStateOf(false) }
    var pin by remember(profileId) { mutableStateOf("") }
    var pinError by remember(profileId) { mutableStateOf("") }
    var announcementTitle by remember(profileId) { mutableStateOf("") }
    var pendingTitle by remember(profileId) { mutableStateOf("") }
    var isRecording by remember(profileId) { mutableStateOf(false) }
    var recordingStartedAt by remember(profileId) { mutableLongStateOf(0L) }
    var elapsedSeconds by remember(profileId) { mutableIntStateOf(0) }
    var playingId by remember(profileId) { mutableStateOf<String?>(null) }
    var message by remember(profileId) { mutableStateOf("") }
    var itemToDelete by remember { mutableStateOf<ParentAnnouncementEntity?>(null) }

    fun finishRecording() {
        if (!isRecording) return
        isRecording = false
        val clip = recorder.stop()
        elapsedSeconds = 0
        if (clip == null) {
            message = "Recording was too short. Try speaking for at least one second."
            return
        }

        val savedTitle = pendingTitle
        scope.launch {
            val insertResult = runCatching {
                dao.insert(
                    ParentAnnouncementEntity(
                        profileId = profileId,
                        announcementId = UUID.randomUUID().toString(),
                        title = savedTitle,
                        audioFileName = clip.file.name,
                        createdAt = System.currentTimeMillis(),
                        durationMs = clip.durationMs
                    )
                )
            }
            if (insertResult.isFailure) {
                clip.file.delete()
                message = insertResult.exceptionOrNull()?.message ?: "Could not save this recording."
                return@launch
            }

            runCatching {
                val older = dao.getForProfile(profileId).drop(MAX_ANNOUNCEMENTS_PER_CHILD)
                older.forEach { old ->
                    val oldFile = runCatching {
                        announcementAudioFile(context, profileId, old.audioFileName)
                    }.getOrNull()
                    dao.delete(profileId, old.announcementId)
                    if (oldFile != null) {
                        withContext(kotlinx.coroutines.Dispatchers.IO) { oldFile.delete() }
                    }
                }
            }
            announcementTitle = ""
            message = "Voice note saved for this child. बच्चे के लिए संदेश सहेज दिया गया।"
        }
    }

    fun startRecording(title: String) {
        try {
            pendingTitle = title
            recorder.start(profileId) { finishRecording() }
            recordingStartedAt = SystemClock.elapsedRealtime()
            elapsedSeconds = 0
            isRecording = true
            message = "Recording · up to 30 seconds. Speak clearly, then tap Stop."
        } catch (failure: Exception) {
            recorder.cancel()
            isRecording = false
            message = failure.message ?: "Could not start the recording. Check microphone access."
        }
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording(pendingTitle)
        else message = "Microphone permission was not granted. No recording was made."
    }

    fun requestRecording() {
        if (!parentUnlocked) return
        val safeTitle = announcementTitle.trim().take(60)
        if (safeTitle.isBlank()) {
            message = "Add a short title for the voice note."
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startRecording(safeTitle)
        } else {
            pendingTitle = safeTitle
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun playAnnouncement(announcement: ParentAnnouncementEntity) {
        if (playingId == announcement.announcementId) {
            player.stop()
            playingId = null
            return
        }
        try {
            val file = announcementAudioFile(context, profileId, announcement.audioFileName)
            if (!file.isFile) {
                message = "This voice file is not on this device."
                return
            }
            player.play(file) { playingId = null }
            playingId = announcement.announcementId
            message = "Playing voice note."
        } catch (failure: Exception) {
            playingId = null
            message = failure.message ?: "Could not play this voice note."
        }
    }

    LaunchedEffect(isRecording, recordingStartedAt) {
        if (isRecording) {
            while (true) {
                val elapsed = SystemClock.elapsedRealtime() - recordingStartedAt
                elapsedSeconds = (elapsed / 1_000L).toInt()
                if (elapsed >= MAX_ANNOUNCEMENT_DURATION_MS) {
                    finishRecording()
                    break
                }
                delay(250)
            }
        }
    }

    LaunchedEffect(parentUnlocked, isRecording) {
        if (parentUnlocked && !isRecording) {
            delay(60_000)
            parentUnlocked = false
            pin = ""
            itemToDelete = null
        }
    }

    DisposableEffect(profileId, recorder, player) {
        onDispose {
            recorder.cancel()
            player.stop()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PageHeading(
                eyebrow = "FAMILY VOICE NOTES · परिवार के संदेश",
                title = "Parent announcements",
                subtitle = "Private, on-device voice messages for this child. इस बच्चे के लिए निजी संदेश।"
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF1FF))
            ) {
                Text(
                    "Only this child's profile shows these messages. Recordings stay in this app on this device; they are not uploaded or included in JSON backups.",
                    modifier = Modifier.padding(16.dp),
                    color = LittleGeniusColors.Ink,
                    fontSize = 13.sp
                )
            }
        }

        item { SectionHeading("Messages for this child", "${announcements.size} of $MAX_ANNOUNCEMENTS_PER_CHILD saved") }
        if (announcements.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        "No voice messages yet. A parent can record one below. अभी कोई voice message नहीं है।",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(announcements, key = { it.announcementId }) { announcement ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(announcement.title, color = LittleGeniusColors.Ink, fontWeight = FontWeight.Bold)
                            Text(
                                "${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(announcement.createdAt))} · ${(announcement.durationMs + 999L) / 1_000L}s",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        Button(onClick = { playAnnouncement(announcement) }) {
                            Text(if (playingId == announcement.announcementId) "Stop" else "Play")
                        }
                        if (parentUnlocked) {
                            TextButton(onClick = { itemToDelete = announcement }) { Text("Delete") }
                        }
                    }
                }
            }
        }

        item { SectionHeading("Parent recording controls", "PIN required · auto-locks after 1 minute · up to 30 seconds each") }
        item {
            if (!parentStore.hasParentPin()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Set a parent PIN before recording or deleting messages.", color = LittleGeniusColors.Ink)
                        Button(onClick = onOpenParentDashboard) { Text("Open Parent Dashboard") }
                    }
                }
            } else if (!parentUnlocked) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { value ->
                            if (value.length <= 6 && value.all(Char::isDigit)) {
                                pin = value
                                pinError = ""
                            }
                        },
                        label = { Text("Parent PIN / अभिभावक PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError.isNotBlank()) Text(pinError, color = Color(0xFFB3261E))
                    Button(
                        onClick = {
                            parentUnlocked = parentStore.verifyParentPin(pin)
                            pinError = if (parentUnlocked) "" else "Incorrect PIN / PIN सही नहीं है"
                            pin = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Unlock parent controls / अभिभावक नियंत्रण खोलें") }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = announcementTitle,
                        onValueChange = { announcementTitle = it.take(60); message = "" },
                        label = { Text("Message title / संदेश का शीर्षक") },
                        singleLine = true,
                        enabled = !isRecording,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isRecording) {
                        Text("Recording · ${elapsedSeconds}s / 30s", color = Color(0xFFB3261E), fontWeight = FontWeight.Bold)
                        Button(onClick = { finishRecording() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Stop and save / रोकें और सहेजें")
                        }
                    } else {
                        Button(onClick = { requestRecording() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Record voice note / संदेश रिकॉर्ड करें")
                        }
                        OutlinedButton(
                            onClick = { parentUnlocked = false; pin = "" },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Lock parent controls") }
                    }
                }
            }
        }
        if (message.isNotBlank()) {
            item { Text(message, color = LittleGeniusColors.Muted, fontSize = 13.sp) }
        }
        item {
            Text(
                "Recordings use the microphone only after you tap Record and approve Android's permission prompt.",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }

    itemToDelete?.let { selected ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete this voice note?") },
            text = { Text("This removes the recording from this child profile and this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            if (playingId == selected.announcementId) {
                                player.stop()
                                playingId = null
                            }
                            dao.delete(profileId, selected.announcementId)
                            withContext(kotlinx.coroutines.Dispatchers.IO) {
                                announcementAudioFile(context, profileId, selected.audioFileName).delete()
                            }
                            itemToDelete = null
                            message = "Voice note deleted."
                        }
                    }
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text("Cancel") } }
        )
    }
}

private class LocalParentAnnouncementRecorder(context: Context) {
    private val appContext = context.applicationContext
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt: Long = 0L

    fun start(profileId: String, onMaxDuration: () -> Unit) {
        check(recorder == null) { "A recording is already active." }
        val output = announcementAudioFile(appContext, profileId, "${UUID.randomUUID()}.m4a")
        val created = newMediaRecorder(appContext)
        try {
            created.setAudioSource(MediaRecorder.AudioSource.MIC)
            created.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            created.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            created.setAudioChannels(1)
            created.setAudioSamplingRate(16_000)
            created.setAudioEncodingBitRate(24_000)
            created.setMaxDuration(MAX_ANNOUNCEMENT_DURATION_MS.toInt())
            created.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    Handler(Looper.getMainLooper()).post { onMaxDuration() }
                }
            }
            created.setOutputFile(output.absolutePath)
            created.prepare()
            created.start()
            recorder = created
            outputFile = output
            startedAt = SystemClock.elapsedRealtime()
        } catch (failure: Exception) {
            runCatching { created.release() }
            output.delete()
            throw failure
        }
    }

    fun stop(): RecordedAnnouncementClip? {
        val activeRecorder = recorder ?: return null
        val file = outputFile
        recorder = null
        outputFile = null
        val duration = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L)
        runCatching { activeRecorder.stop() }
        runCatching { activeRecorder.reset() }
        runCatching { activeRecorder.release() }
        if (file == null || !file.isFile || file.length() < 512L) {
            file?.delete()
            return null
        }
        return RecordedAnnouncementClip(file, duration)
    }

    fun cancel() {
        val activeRecorder = recorder
        recorder = null
        val file = outputFile
        outputFile = null
        if (activeRecorder != null) {
            runCatching { activeRecorder.stop() }
            runCatching { activeRecorder.reset() }
            runCatching { activeRecorder.release() }
        }
        file?.delete()
    }
}

private class LocalAnnouncementPlayer {
    private var activePlayer: MediaPlayer? = null

    fun play(file: File, onCompletion: () -> Unit) {
        stop()
        val created = MediaPlayer()
        try {
            created.setDataSource(file.absolutePath)
            created.setOnCompletionListener {
                activePlayer = null
                runCatching { it.release() }
                onCompletion()
            }
            created.prepare()
            created.start()
            activePlayer = created
        } catch (failure: Exception) {
            runCatching { created.release() }
            throw failure
        }
    }

    fun stop() {
        val current = activePlayer ?: return
        activePlayer = null
        runCatching { current.stop() }
        runCatching { current.release() }
    }
}

private fun announcementAudioFile(context: Context, profileId: String, fileName: String): File {
    require(fileName.matches(Regex("[A-Fa-f0-9-]{36}\\.m4a"))) {
        "Invalid voice recording file name."
    }
    val directoryName = UUID.nameUUIDFromBytes(profileId.toByteArray(Charsets.UTF_8)).toString()
    val directory = File(File(context.filesDir, RECORDING_DIRECTORY), directoryName)
    check(directory.isDirectory || directory.mkdirs()) { "Could not create private recording storage." }
    return File(directory, fileName)
}

@Suppress("DEPRECATION")
private fun newMediaRecorder(context: Context): MediaRecorder =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
