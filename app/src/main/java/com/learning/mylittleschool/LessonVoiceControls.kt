package com.learning.mylittleschool

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * On-device lesson narration using Android TextToSpeech.
 * No microphone permission, account, network request, or audio recording.
 */
@Composable
fun LessonVoiceControls(text: String, language: String) {
    val context = LocalContext.current
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Preparing voice…") }

    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            message = if (ready) "Voice ready" else "Text-to-speech is unavailable"
        }
        engine = tts
        onDispose {
            tts.stop()
            tts.shutdown()
            engine = null
            ready = false
        }
    }

    val locale = if (language == "Hindi") Locale.forLanguageTag("hi-IN")
                 else Locale.forLanguageTag("en-IN")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = {
                val tts = engine ?: return@Button
                val result = tts.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    message = "Install the ${if (language == "Hindi") "Hindi" else "English"} voice in phone settings"
                } else {
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "little_genius_lesson")
                    message = "Speaking"
                }
            },
            enabled = ready,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.VolumeUp, contentDescription = null)
            Text("  Listen")
        }

        OutlinedButton(
            onClick = {
                engine?.stop()
                message = "Stopped"
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Pause, contentDescription = null)
            Text("  Stop")
        }
    }
    Text(message)
}
