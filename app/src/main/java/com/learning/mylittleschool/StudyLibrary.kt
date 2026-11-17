package com.learning.mylittleschool

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

private val LibraryBlue = LittleGeniusColors.Blue
private val LibraryInk = LittleGeniusColors.Ink
private val LibrarySoftBlue = LittleGeniusColors.SoftBlue

private data class StudyMaterialSource(
    val title: String,
    val detail: String,
    val publisher: String,
    val url: String,
    val canDownloadHere: Boolean = false
)

private data class StudyDownload(
    val id: Long,
    val title: String,
    val url: String,
    val status: Int,
    val reason: Int,
    val downloadedBytes: Long,
    val totalBytes: Long
) {
    val isActive: Boolean
        get() = status == DownloadManager.STATUS_PENDING ||
            status == DownloadManager.STATUS_RUNNING ||
            status == DownloadManager.STATUS_PAUSED

    val isComplete: Boolean
        get() = status == DownloadManager.STATUS_SUCCESSFUL

    fun statusText(): String = when (status) {
        DownloadManager.STATUS_PENDING -> "Waiting to download"
        DownloadManager.STATUS_RUNNING -> {
            if (totalBytes > 0) "Downloading · ${downloadedBytes * 100 / totalBytes}%"
            else "Downloading"
        }
        DownloadManager.STATUS_PAUSED -> "Paused · waiting for a connection"
        DownloadManager.STATUS_SUCCESSFUL -> "Saved on this device · available offline"
        else -> "Download failed${if (reason != 0) " · code $reason" else ""}"
    }
}

private data class ImportedStudyMaterial(
    val key: String,
    val title: String,
    val uri: String
)

private class StudyDownloadStore(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(DownloadManager::class.java)
    private val preferences = appContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun enqueue(source: StudyMaterialSource): Long {
        require(source.canDownloadHere && Uri.parse(source.url).scheme == "https") {
            "Only the listed official HTTPS PDFs can be downloaded here."
        }

        val safeName = source.title.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(48)
            .ifBlank { "study-material" }
        val request = DownloadManager.Request(Uri.parse(source.url))
            .setTitle(source.title)
            .setDescription("Little Genius · official learning material")
            .setMimeType("application/pdf")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                appContext,
                Environment.DIRECTORY_DOWNLOADS,
                "$safeName-${System.currentTimeMillis()}.pdf"
            )

        val id = manager.enqueue(request)
        preferences.edit()
            .putString(titleKey(id), source.title)
            .putString(urlKey(id), source.url)
            .apply()
        return id
    }

    fun list(): List<StudyDownload> = preferences.all.keys
        .mapNotNull { key ->
            if (!key.endsWith(TITLE_SUFFIX) || !key.startsWith(DOWNLOAD_PREFIX)) {
                return@mapNotNull null
            }
            key.removePrefix(DOWNLOAD_PREFIX)
                .removeSuffix(TITLE_SUFFIX)
                .toLongOrNull()
        }
        .distinct()
        .mapNotNull(::readDownload)
        .sortedByDescending { it.id }

    fun uriFor(id: Long): Uri? = manager.getUriForDownloadedFile(id)

    fun saveImported(uri: Uri, title: String) {
        val key = importKey(uri)
        preferences.edit()
            .putString(importedTitleKey(key), title)
            .putString(importedUriKey(key), uri.toString())
            .apply()
    }

    fun imported(): List<ImportedStudyMaterial> = preferences.all.keys
        .filter { it.startsWith(IMPORTED_PREFIX) && it.endsWith(IMPORTED_URI_SUFFIX) }
        .mapNotNull { uriKey ->
            val key = uriKey.removePrefix(IMPORTED_PREFIX).removeSuffix(IMPORTED_URI_SUFFIX)
            val uri = preferences.getString(uriKey, null) ?: return@mapNotNull null
            val title = preferences.getString(importedTitleKey(key), null) ?: return@mapNotNull null
            ImportedStudyMaterial(key, title, uri)
        }
        .sortedBy { it.title.lowercase() }

    fun forgetImported(item: ImportedStudyMaterial) {
        runCatching {
            appContext.contentResolver.releasePersistableUriPermission(
                Uri.parse(item.uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        preferences.edit()
            .remove(importedTitleKey(item.key))
            .remove(importedUriKey(item.key))
            .apply()
    }

    fun remove(id: Long) {
        manager.remove(id)
        preferences.edit()
            .remove(titleKey(id))
            .remove(urlKey(id))
            .apply()
    }

    private fun readDownload(id: Long): StudyDownload? {
        val title = preferences.getString(titleKey(id), null) ?: return null
        val url = preferences.getString(urlKey(id), null) ?: return null
        val cursor = manager.query(DownloadManager.Query().setFilterById(id)) ?: return null
        cursor.use {
            if (!it.moveToFirst()) return null
            return StudyDownload(
                id = id,
                title = title,
                url = url,
                status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)),
                downloadedBytes = it.getLong(
                    it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                ),
                totalBytes = it.getLong(
                    it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                )
            )
        }
    }

    private fun titleKey(id: Long) = "$DOWNLOAD_PREFIX$id$TITLE_SUFFIX"
    private fun urlKey(id: Long) = "$DOWNLOAD_PREFIX$id$URL_SUFFIX"
    private fun importedTitleKey(key: String) = "$IMPORTED_PREFIX$key$IMPORTED_TITLE_SUFFIX"
    private fun importedUriKey(key: String) = "$IMPORTED_PREFIX$key$IMPORTED_URI_SUFFIX"

    private fun importKey(uri: Uri): String = MessageDigest.getInstance("SHA-256")
        .digest(uri.toString().toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    companion object {
        private const val PREFERENCES_NAME = "little_genius_study_downloads"
        private const val DOWNLOAD_PREFIX = "download_"
        private const val TITLE_SUFFIX = "_title"
        private const val URL_SUFFIX = "_url"
        private const val IMPORTED_PREFIX = "imported_"
        private const val IMPORTED_TITLE_SUFFIX = "_title"
        private const val IMPORTED_URI_SUFFIX = "_uri"
    }
}

private val studyMaterialSources = listOf(
    StudyMaterialSource(
        title = "Learning Outcomes at the Foundational Stage",
        detail = "NCERT reference for foundational learning goals and classroom activities.",
        publisher = "NCERT · PDF",
        url = "https://www.ncert.nic.in/pdf/publication/otherpublications/Learning_Outcome_for_the_Foundational_Stage.pdf",
        canDownloadHere = true
    ),
    StudyMaterialSource(
        title = "Pre-School Curriculum",
        detail = "NCERT curriculum reference for pre-school learning.",
        publisher = "NCERT · PDF",
        url = "https://www.ncert.nic.in/pdf_files/preschool_curriculum.pdf",
        canDownloadHere = true
    ),
    StudyMaterialSource(
        title = "National Curriculum Framework for Foundational Stage",
        detail = "Foundational-stage framework and illustrative learning outcomes.",
        publisher = "CBSE Academic · PDF",
        url = "https://cbseacademic.nic.in/hpc/pdf/NCF_for_Foundational_Stage_20_October_2022.pdf",
        canDownloadHere = true
    ),
    StudyMaterialSource(
        title = "NCERT Textbooks",
        detail = "Choose class, subject and language, then download a book or chapter from the official portal.",
        publisher = "ePathshala · official textbook portal",
        url = "https://epathshala.nic.in/wp-content/doc/book/btextbook/textbook.htm"
    ),
    StudyMaterialSource(
        title = "NCERT e-Resources",
        detail = "Find textbook-linked audio, video, interactives, images, maps and question banks.",
        publisher = "ePathshala · official learning resources",
        url = "https://epathshala.nic.in/topics.php"
    )
)

@Composable
fun StudyLibraryScreen(
    schoolClass: SchoolClass,
    padding: PaddingValues,
    onOpenSubject: (String) -> Unit
) {
    val context = LocalContext.current
    val store = remember(context) { StudyDownloadStore(context) }
    val scope = rememberCoroutineScope()
    var refreshToken by remember { mutableIntStateOf(0) }
    var downloads by remember { mutableStateOf<List<StudyDownload>>(emptyList()) }
    var importedFiles by remember { mutableStateOf<List<ImportedStudyMaterial>>(emptyList()) }
    var pendingDownloadUrls by remember { mutableStateOf(emptySet<String>()) }
    var pendingRemoval by remember { mutableStateOf<StudyDownload?>(null) }
    var pendingImportedRemoval by remember { mutableStateOf<ImportedStudyMaterial?>(null) }

    LaunchedEffect(refreshToken) {
        while (true) {
            val latest = withContext(Dispatchers.IO) { store.list() }
            downloads = latest
            importedFiles = withContext(Dispatchers.IO) { store.imported() }
            if (latest.none { it.isActive }) break
            delay(1_000)
        }
    }

    val importPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        val title = withContext(Dispatchers.IO) {
                            val mimeType = context.contentResolver.getType(uri)
                            val displayName = context.contentResolver.query(
                                uri,
                                arrayOf(OpenableColumns.DISPLAY_NAME),
                                null,
                                null,
                                null
                            )?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                                } else null
                            }
                            val looksLikePdf = displayName?.endsWith(".pdf", ignoreCase = true) == true
                            require(mimeType == "application/pdf" || looksLikePdf) {
                                "Choose a downloaded PDF file."
                            }
                            displayName?.takeIf { it.isNotBlank() } ?: "Study material.pdf"
                        }
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        withContext(Dispatchers.IO) { store.saveImported(uri, title) }
                        Toast.makeText(context, "Added to your offline library.", Toast.LENGTH_LONG).show()
                        refreshToken++
                    } catch (error: Exception) {
                        Toast.makeText(
                            context,
                            error.message ?: "Could not add this PDF. Choose a file saved on this device.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    )

    val offlineSubjects = remember(schoolClass.id) {
        listOf("English", "Mathematics", "Hindi", "EVS / Science", "Computer", "Social Studies")
            .mapNotNull { subject ->
                val count = lessonsFor(schoolClass.id, subject)
                    .count { !it.title.startsWith("Getting Started:") }
                if (count == 0) null else subject to count
            }
    }

    fun openSource(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No browser is available to open this source.", Toast.LENGTH_LONG).show()
        }
    }

    fun startDownload(source: StudyMaterialSource) {
        if (source.url in pendingDownloadUrls) {
            Toast.makeText(context, "This download is already starting.", Toast.LENGTH_SHORT).show()
            return
        }
        val existing = downloads.firstOrNull {
            it.url == source.url && (it.isComplete || it.isActive)
        }
        if (existing != null) {
            Toast.makeText(context, "Already downloaded or in progress.", Toast.LENGTH_SHORT).show()
            return
        }
        pendingDownloadUrls = pendingDownloadUrls + source.url
        scope.launch {
            try {
                withContext(Dispatchers.IO) { store.enqueue(source) }
                Toast.makeText(context, "Download started. It will be available offline when complete.", Toast.LENGTH_LONG).show()
                refreshToken++
            } catch (error: Exception) {
                Toast.makeText(
                    context,
                    error.message ?: "Could not start the download.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                pendingDownloadUrls = pendingDownloadUrls - source.url
            }
        }
    }

    fun openDownload(download: StudyDownload) {
        val uri = store.uriFor(download.id)
        if (uri == null) {
            Toast.makeText(context, "This file is not ready yet.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open study material"))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Install a PDF reader to open this material.", Toast.LENGTH_LONG).show()
        }
    }

    fun openImported(item: ImportedStudyMaterial) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(item.uri), "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open study material"))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Install a PDF reader to open this material.", Toast.LENGTH_LONG).show()
        } catch (_: SecurityException) {
            Toast.makeText(context, "This file is no longer available. Add it again from Downloads.", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        Modifier.fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PremiumHeroCard(
            eyebrow = "YOUR STUDY LIBRARY",
            title = "Learn anywhere, even offline",
            subtitle = "Keep starter lessons here and download trusted materials while connected. Starter content is not a full syllabus.",
            decoration = "📚"
        ) {
            AccentPill("${schoolClass.title} · OFFLINE READY", Color.White.copy(alpha = 0.92f))
        }

        SectionHeading("Offline lessons", schoolClass.title)
        Text("These starter activities and your progress stay on this phone.", color = Color.Gray, fontSize = 12.sp)
        if (offlineSubjects.isEmpty()) {
            Text("No starter lessons are bundled for this class yet.", color = Color.Gray)
        } else {
            offlineSubjects.forEach { (subject, count) ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenSubject(subject) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(subject, color = LibraryInk, fontWeight = FontWeight.Bold)
                            Text("$count starter lesson${if (count == 1) "" else "s"} · available offline", color = Color.Gray, fontSize = 12.sp)
                        }
                        Text("Open", color = LibraryBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        SectionHeading("Download study material", "Official sources for your next study session")
        Text(
            "Use official NCERT and CBSE sources. Files you download here are stored on this device and can be opened offline.",
            color = Color.Gray,
            fontSize = 13.sp
        )
        studyMaterialSources.forEach { source ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(source.title, color = LibraryInk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(source.detail, color = Color.DarkGray, fontSize = 13.sp)
                    AccentPill(source.publisher)
                    if (source.canDownloadHere) {
                        Button(onClick = { startDownload(source) }) {
                            Text("Download PDF for offline use")
                        }
                        TextButton(onClick = { openSource(source.url) }) {
                            Text("View official PDF")
                        }
                    } else {
                        Button(onClick = { openSource(source.url) }) {
                            Text("Browse and download")
                        }
                    }
                }
            }
        }

        SectionHeading("Saved on this device", "Your downloaded and imported PDFs")
        Text("App downloads are kept in app storage. Imported files stay in their original folder; keep those files on this device.", color = Color.Gray, fontSize = 12.sp)
        OutlinedButton(onClick = { importPdfLauncher.launch(arrayOf("application/pdf")) }) {
            Text("Add a downloaded PDF")
        }
        Text(
            "If you downloaded a textbook from the official portal in your browser, add it here to keep it in this library.",
            color = Color.Gray,
            fontSize = 12.sp
        )
        if (downloads.isEmpty() && importedFiles.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Text(
                    "Your downloads will appear here. Choose an official PDF above while connected to the internet.",
                    Modifier.padding(16.dp),
                    color = Color.Gray
                )
            }
        } else {
            downloads.forEach { download ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(download.title, color = LibraryInk, fontWeight = FontWeight.Bold)
                        Text(download.statusText(), color = if (download.isComplete) Color(0xFF16803C) else Color.Gray, fontSize = 12.sp)
                        if (download.isActive && download.totalBytes > 0) {
                            ProgressTrack(
                                progress = download.downloadedBytes.toFloat() / download.totalBytes.toFloat(),
                                trackColor = LibrarySoftBlue,
                                fillColor = LibraryBlue
                            )
                        }
                        if (download.isComplete) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { openDownload(download) }) { Text("Open offline") }
                                OutlinedButton(onClick = { pendingRemoval = download }) { Text("Remove") }
                            }
                        } else if (!download.isActive) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    val source = studyMaterialSources.firstOrNull { it.url == download.url }
                                    if (source != null) startDownload(source)
                                }) { Text("Try again") }
                                OutlinedButton(onClick = { pendingRemoval = download }) { Text("Remove") }
                            }
                        } else {
                            TextButton(onClick = { pendingRemoval = download }) { Text("Cancel download") }
                        }
                    }
                }
            }
            importedFiles.forEach { item ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(item.title, color = LibraryInk, fontWeight = FontWeight.Bold)
                        Text("Added from this device · opens offline", color = Color(0xFF16803C), fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { openImported(item) }) { Text("Open offline") }
                            OutlinedButton(onClick = { pendingImportedRemoval = item }) { Text("Remove") }
                        }
                    }
                }
            }
        }

        Text(
            "Official source files remain the publishers' content. Little Genius does not bundle or republish NCERT/CBSE books.",
            color = Color.Gray,
            fontSize = 11.sp
        )
    }

    pendingRemoval?.let { selected ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(if (selected.isActive) "Cancel this download?" else "Remove this material?") },
            text = {
                Text(
                    if (selected.isActive) "The download will be cancelled."
                    else "The downloaded file will be removed from this device."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingRemoval = null
                    scope.launch {
                        withContext(Dispatchers.IO) { store.remove(selected.id) }
                        refreshToken++
                    }
                }) { Text(if (selected.isActive) "Cancel download" else "Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text("Keep") }
            }
        )
    }

    pendingImportedRemoval?.let { selected ->
        AlertDialog(
            onDismissRequest = { pendingImportedRemoval = null },
            title = { Text("Remove from this library?") },
            text = { Text("This removes the saved link from Little Genius. The original PDF in Downloads will not be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportedRemoval = null
                    scope.launch {
                        withContext(Dispatchers.IO) { store.forgetImported(selected) }
                        refreshToken++
                    }
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportedRemoval = null }) { Text("Keep") }
            }
        )
    }
}
