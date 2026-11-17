package com.learning.mylittleschool

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object CertificateMilestone {
    const val LESSON_TARGET = 5

    fun isUnlocked(store: ProgressStore): Boolean = store.completedCount() >= LESSON_TARGET
}

@Composable
fun LearningCertificateCard(
    studentName: String,
    className: String,
    store: ProgressStore
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val unlocked = CertificateMilestone.isUnlocked(store)
    var message by remember { mutableStateOf("") }

    val certificateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            LearningCertificatePdf.write(
                                output = output,
                                studentName = studentName,
                                className = className,
                                completedLessons = store.completedCount()
                            )
                        } ?: error("Could not open the selected destination.")
                    }
                }
                message = if (result.isSuccess) "Certificate saved as a PDF."
                else result.exceptionOrNull()?.message ?: "Could not save the certificate."
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Learning milestone certificate", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                if (unlocked) {
                    "$studentName has completed ${store.completedCount()} lessons in $className."
                } else {
                    "Complete ${CertificateMilestone.LESSON_TARGET} lessons to unlock a certificate for $studentName."
                },
                color = LittleGeniusColors.Muted
            )
            Text(
                "A local progress milestone, not an official CBSE certificate.",
                fontSize = 12.sp,
                color = LittleGeniusColors.Muted
            )
            Button(
                enabled = unlocked,
                onClick = {
                    val safeName = studentName.replace(Regex("[^A-Za-z0-9_-]"), "-").take(32)
                    certificateLauncher.launch("Little-Genius-Certificate-$safeName.pdf")
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save certificate PDF / प्रमाणपत्र सेव करें") }
            if (message.isNotBlank()) Text(message, color = LittleGeniusColors.Muted, fontSize = 13.sp)
        }
    }
}

private object LearningCertificatePdf {
    fun write(output: OutputStream, studentName: String, className: String, completedLessons: Int) {
        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create()
            val page = document.startPage(pageInfo)
            drawCertificate(page.canvas, studentName, className, completedLessons)
            document.finishPage(page)
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    private fun drawCertificate(canvas: Canvas, studentName: String, className: String, completedLessons: Int) {
        val width = 842f
        val height = 595f
        canvas.drawColor(android.graphics.Color.WHITE)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(36, 87, 214)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(RectF(22f, 22f, width - 22f, height - 22f), 18f, 18f, borderPaint)
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(126, 87, 194)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(RectF(36f, 36f, width - 36f, height - 36f), 14f, 14f, accentPaint)

        val brand = textPaint(18f, android.graphics.Color.rgb(36, 87, 214), Typeface.create("sans-serif", Typeface.BOLD))
        val title = textPaint(38f, android.graphics.Color.rgb(23, 35, 60), Typeface.create("serif", Typeface.BOLD))
        val body = textPaint(18f, android.graphics.Color.rgb(80, 91, 110), Typeface.create("sans-serif", Typeface.NORMAL))
        val name = textPaint(34f, android.graphics.Color.rgb(36, 87, 214), Typeface.create("serif", Typeface.BOLD))
        val detail = textPaint(16f, android.graphics.Color.rgb(80, 91, 110), Typeface.create("sans-serif", Typeface.NORMAL))

        drawCentered(canvas, "LITTLE GENIUS", 112f, brand, width)
        drawCentered(canvas, "Learning Milestone Certificate", 180f, title, width)
        drawCentered(canvas, "This certificate is proudly presented to", 230f, body, width)
        drawCentered(canvas, studentName.take(48), 292f, name, width)
        canvas.drawLine(175f, 310f, width - 175f, 310f, accentPaint)
        drawCentered(canvas, "for completing $completedLessons lessons in $className", 355f, body, width)
        drawCentered(canvas, "Keep learning, practising and exploring.", 398f, detail, width)
        val date = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH))
        drawCentered(canvas, "Awarded on $date", 478f, detail, width)
        drawCentered(canvas, "A personal learning milestone · Not an official school or CBSE certificate", 525f, detail, width)
    }

    private fun textPaint(size: Float, color: Int, typeface: Typeface) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        this.typeface = typeface
        textAlign = Paint.Align.LEFT
    }

    private fun drawCentered(canvas: Canvas, text: String, y: Float, paint: Paint, pageWidth: Float) {
        canvas.drawText(text, (pageWidth - paint.measureText(text)) / 2f, y, paint)
    }
}
