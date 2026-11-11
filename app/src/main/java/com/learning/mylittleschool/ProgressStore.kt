package com.learning.mylittleschool

import android.content.Context
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class DailyProgress(
    val day: String,
    val attempts: Int,
    val correct: Int
)

data class DailyStudyProgress(
    val day: String,
    val completed: Int
)

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences(
        "little_genius_progress",
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun recordLesson(lessonId: String, correct: Boolean) {
        val completed = prefs.getStringSet(KEY_COMPLETED, emptySet())
            ?.toMutableSet() ?: mutableSetOf()

        if (correct) completed.add(lessonId)

        val history = prefs.getStringSet(KEY_HISTORY, emptySet())
            ?.toMutableSet() ?: mutableSetOf()

        history.add("${LocalDate.now()}|${if (correct) 1 else 0}")

        prefs.edit()
            .putStringSet(KEY_COMPLETED, completed)
            .putStringSet(KEY_HISTORY, history)
            .putInt(KEY_ATTEMPTS, attempts() + 1)
            .putInt(KEY_CORRECT, correctAnswers() + if (correct) 1 else 0)
            .apply()
    }

    fun completedCount(): Int =
        prefs.getStringSet(KEY_COMPLETED, emptySet())?.size ?: 0

    @Synchronized
    fun isStudyTaskDone(taskId: String): Boolean {
        val key = "study_tasks_${LocalDate.now()}"
        return prefs.getStringSet(key, emptySet())?.contains(taskId) == true
    }

    @Synchronized
    fun setStudyTaskDone(taskId: String, done: Boolean) {
        val key = "study_tasks_${LocalDate.now()}"
        val tasks = prefs.getStringSet(key, emptySet())
            ?.toMutableSet() ?: mutableSetOf()
        if (done) tasks.add(taskId) else tasks.remove(taskId)
        prefs.edit().putStringSet(key, tasks).apply()
    }

    @Synchronized
    fun studyTasksDoneCount(): Int {
        val key = "study_tasks_${LocalDate.now()}"
        return prefs.getStringSet(key, emptySet())?.size ?: 0
    }

    fun studyStreakDays(): Int {
        var date = LocalDate.now()
        if (prefs.getStringSet("study_tasks_$date", emptySet()).isNullOrEmpty()) {
            date = date.minusDays(1)
        }

        var streak = 0
        while (!prefs.getStringSet("study_tasks_$date", emptySet()).isNullOrEmpty()) {
            streak++
            date = date.minusDays(1)
        }
        return streak
    }

    fun studyDaysLast7Days(): Int {
        val today = LocalDate.now()
        return (0..6).count { offset ->
            val date = today.minusDays(offset.toLong())
            !prefs.getStringSet("study_tasks_$date", emptySet()).isNullOrEmpty()
        }
    }

    fun weeklyStudyProgress(): List<DailyStudyProgress> {
        val today = LocalDate.now()

        return (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val key = "study_tasks_$date"
            val completed = prefs.getStringSet(key, emptySet())?.size ?: 0

            DailyStudyProgress(
                day = date.dayOfWeek.getDisplayName(
                    TextStyle.SHORT,
                    Locale.ENGLISH
                ),
                completed = completed
            )
        }
    }

    fun attempts(): Int = prefs.getInt(KEY_ATTEMPTS, 0)

    fun correctAnswers(): Int = prefs.getInt(KEY_CORRECT, 0)

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hasParentPin(): Boolean =
        !prefs.getString(KEY_PARENT_PIN_HASH, null).isNullOrBlank()

    fun setParentPin(pin: String) {
        require(pin.length in 4..6 && pin.all { it.isDigit() }) {
            "PIN must contain 4 to 6 digits"
        }
        prefs.edit()
            .putString(KEY_PARENT_PIN_HASH, hashPin(pin))
            .apply()
    }

    fun verifyParentPin(pin: String): Boolean {
        val saved = prefs.getString(KEY_PARENT_PIN_HASH, null) ?: return false
        return MessageDigest.isEqual(
            saved.toByteArray(Charsets.UTF_8),
            hashPin(pin).toByteArray(Charsets.UTF_8)
        )
    }

    fun weeklyProgress(): List<DailyProgress> {
        val history = prefs.getStringSet(KEY_HISTORY, emptySet())
            ?.toList() ?: emptyList()

        val today = LocalDate.now()

        return (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())

            val results = history.mapNotNull { entry ->
                val parts = entry.split('|')
                if (parts.size == 2 && parts[0] == date.toString()) {
                    parts[1] == "1"
                } else {
                    null
                }
            }

            DailyProgress(
                day = date.dayOfWeek.getDisplayName(
                    TextStyle.SHORT,
                    Locale.ENGLISH
                ),
                attempts = results.size,
                correct = results.count { it }
            )
        }
    }

    companion object {
        private const val KEY_COMPLETED = "completed_lessons"
        private const val KEY_ATTEMPTS = "quiz_attempts"
        private const val KEY_CORRECT = "correct_answers"
        private const val KEY_HISTORY = "daily_learning_history"
        private const val KEY_PARENT_PIN_HASH = "parent_pin_hash"
    }
}
