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

data class LastOpenedLesson(
    val classId: String,
    val subject: String,
    val lessonId: String
)

data class LessonHistoryEntry(
    val classId: String,
    val subject: String,
    val lessonId: String,
    val openedAt: Long,
    val completed: Boolean
)

data class LessonPerformance(
    val lessonId: String,
    val attempts: Int,
    val correct: Int,
    val lastAttemptAt: Long
) {
    val accuracyPercent: Int
        get() = if (attempts == 0) 0 else correct * 100 / attempts
}

class ProgressStore(context: Context, profileId: String = DEFAULT_PROFILE_ID) {
    private val prefs = context.getSharedPreferences(
        if (profileId == DEFAULT_PROFILE_ID) PREFERENCES_NAME else "${PREFERENCES_NAME}_$profileId",
        Context.MODE_PRIVATE
    )
    private val parentPrefs = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun saveLastOpenedLesson(classId: String, subject: String, lessonId: String) {
        val now = System.currentTimeMillis()
        val history = prefs.getStringSet(KEY_LESSON_HISTORY, emptySet())
            ?.toMutableSet() ?: mutableSetOf()
        val prefix = "$classId|$subject|$lessonId|"
        history.removeAll { it.startsWith(prefix) }
        history.add("$classId|$subject|$lessonId|$now|0")
        val trimmed = history.sortedByDescending {
            it.split("|").getOrNull(3)?.toLongOrNull() ?: 0L
        }.take(10).toSet()

        prefs.edit()
            .putString(KEY_LAST_LESSON_CLASS, classId)
            .putString(KEY_LAST_LESSON_SUBJECT, subject)
            .putString(KEY_LAST_LESSON_ID, lessonId)
            .putStringSet(KEY_LESSON_HISTORY, trimmed)
            .apply()
    }

    fun lessonHistory(): List<LessonHistoryEntry> =
        prefs.getStringSet(KEY_LESSON_HISTORY, emptySet())
            ?.mapNotNull { value ->
                val parts = value.split("|")
                if (parts.size != 5) return@mapNotNull null
                val openedAt = parts[3].toLongOrNull() ?: return@mapNotNull null
                LessonHistoryEntry(
                    parts[0], parts[1], parts[2], openedAt, parts[4] == "1"
                )
            }
            ?.sortedByDescending { it.openedAt }
            ?: emptyList()

    fun lastOpenedLesson(): LastOpenedLesson? {
        val classId = prefs.getString(KEY_LAST_LESSON_CLASS, null) ?: return null
        val subject = prefs.getString(KEY_LAST_LESSON_SUBJECT, null) ?: return null
        val lessonId = prefs.getString(KEY_LAST_LESSON_ID, null) ?: return null
        return LastOpenedLesson(classId, subject, lessonId)
    }

    @Synchronized
    fun recordLesson(lessonId: String, correct: Boolean) {
        val completed = prefs.getStringSet(KEY_COMPLETED, emptySet())
            ?.toMutableSet() ?: mutableSetOf()

        if (correct) completed.add(lessonId)

        if (correct) {
            val lessonHistory = prefs.getStringSet(KEY_LESSON_HISTORY, emptySet())
                ?.toMutableSet() ?: mutableSetOf()
            val updated = lessonHistory.map { entry ->
                val parts = entry.split("|")
                if (parts.size == 5 && parts[2] == lessonId) {
                    "${parts[0]}|${parts[1]}|${parts[2]}|${parts[3]}|1"
                } else entry
            }.toSet()
            prefs.edit().putStringSet(KEY_LESSON_HISTORY, updated).apply()
        }

        val history = prefs.getStringSet(KEY_HISTORY, emptySet())
            ?.toMutableSet() ?: mutableSetOf()

        val lessonAnswers = prefs.getStringSet(KEY_LESSON_ANSWERS, emptySet())
            ?.toMutableSet() ?: mutableSetOf()
        val attemptTime = System.currentTimeMillis()
        lessonAnswers.add(
            "$lessonId|${if (correct) 1 else 0}|$attemptTime|${System.nanoTime()}"
        )
        if (lessonAnswers.size > MAX_LESSON_ANSWER_HISTORY) {
            val latest = lessonAnswers.sortedByDescending { entry ->
                entry.split('|').getOrNull(2)?.toLongOrNull() ?: 0L
            }.take(MAX_LESSON_ANSWER_HISTORY)
            lessonAnswers.clear()
            lessonAnswers.addAll(latest)
        }

        appendAttemptHistory(history, correct)

        prefs.edit()
            .putStringSet(KEY_COMPLETED, completed)
            .putStringSet(KEY_HISTORY, history)
            .putStringSet(KEY_LESSON_ANSWERS, lessonAnswers)
            .putInt(KEY_ATTEMPTS, attempts() + 1)
            .putInt(KEY_CORRECT, correctAnswers() + if (correct) 1 else 0)
            .apply()
    }

    @Synchronized
    fun recordActivityAnswer(activityId: String, correct: Boolean) {
        val completed = prefs.getStringSet(KEY_ACTIVITY_COMPLETED, emptySet())
            ?.toMutableSet() ?: mutableSetOf()
        if (correct) completed.add(activityId)

        val history = prefs.getStringSet(KEY_HISTORY, emptySet())
            ?.toMutableSet() ?: mutableSetOf()
        appendAttemptHistory(history, correct)

        prefs.edit()
            .putStringSet(KEY_ACTIVITY_COMPLETED, completed)
            .putStringSet(KEY_HISTORY, history)
            .putInt(KEY_ATTEMPTS, attempts() + 1)
            .putInt(KEY_CORRECT, correctAnswers() + if (correct) 1 else 0)
            .apply()
    }

    fun isActivityCompleted(activityId: String): Boolean =
        prefs.getStringSet(KEY_ACTIVITY_COMPLETED, emptySet())?.contains(activityId) == true

    @Synchronized
    fun isPracticalGuideDone(guideId: String): Boolean =
        prefs.getStringSet(KEY_PRACTICAL_GUIDES, emptySet())?.contains(guideId) == true

    @Synchronized
    fun setPracticalGuideDone(guideId: String, done: Boolean) {
        val completed = prefs.getStringSet(KEY_PRACTICAL_GUIDES, emptySet())
            ?.toMutableSet() ?: mutableSetOf()
        if (done) completed.add(guideId) else completed.remove(guideId)
        prefs.edit().putStringSet(KEY_PRACTICAL_GUIDES, completed).apply()
    }

    fun practicalGuidesDoneCount(): Int =
        prefs.getStringSet(KEY_PRACTICAL_GUIDES, emptySet())?.size ?: 0

    private fun appendAttemptHistory(history: MutableSet<String>, correct: Boolean) {
        history.add(
            "${LocalDate.now()}|${if (correct) 1 else 0}|${System.currentTimeMillis()}|${System.nanoTime()}"
        )
        if (history.size > MAX_ATTEMPT_HISTORY) {
            val latest = history.sortedByDescending { entry ->
                entry.split('|').getOrNull(2)?.toLongOrNull() ?: 0L
            }.take(MAX_ATTEMPT_HISTORY)
            history.clear()
            history.addAll(latest)
        }
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

    fun totalStudyDays(): Int {
        return prefs.all.keys.count { key ->
            key.startsWith("study_tasks_") &&
                !prefs.getStringSet(key, emptySet()).isNullOrEmpty()
        }
    }

    fun longestStudyStreak(): Int {
        val dates = prefs.all.keys.mapNotNull { key ->
            if (!key.startsWith("study_tasks_")) return@mapNotNull null
            val date = runCatching {
                LocalDate.parse(key.removePrefix("study_tasks_"))
            }.getOrNull() ?: return@mapNotNull null
            if (prefs.getStringSet(key, emptySet()).isNullOrEmpty()) null else date
        }.toSet().sorted()

        var longest = 0
        var current = 0
        var previous: LocalDate? = null

        dates.forEach { date ->
            current = if (previous != null && date == previous!!.plusDays(1)) current + 1 else 1
            longest = maxOf(longest, current)
            previous = date
        }
        return longest
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

    fun lessonPerformance(): List<LessonPerformance> {
        val answers = prefs.getStringSet(KEY_LESSON_ANSWERS, emptySet())
            ?.mapNotNull { entry ->
                val parts = entry.split('|')
                if (parts.size != 4 || parts[0].isBlank()) return@mapNotNull null
                val timestamp = parts[2].toLongOrNull() ?: return@mapNotNull null
                if (parts[1] != "0" && parts[1] != "1") return@mapNotNull null
                Triple(parts[0], parts[1] == "1", timestamp)
            }.orEmpty()

        return answers.groupBy { it.first }
            .map { (lessonId, attempts) ->
                LessonPerformance(
                    lessonId = lessonId,
                    attempts = attempts.size,
                    correct = attempts.count { it.second },
                    lastAttemptAt = attempts.maxOf { it.third }
                )
            }
            .sortedWith(compareBy<LessonPerformance> { it.accuracyPercent }
                .thenByDescending { it.attempts }
                .thenByDescending { it.lastAttemptAt })
    }

    fun exportLearningData(): List<BackupPreference> = prefs.all.entries.mapNotNull { (key, value) ->
        val expectedType = backupTypeForKey(key) ?: return@mapNotNull null
        when (value) {
            is String -> BackupPreference(key, expectedType, scalarValue = value)
            is Int -> BackupPreference(key, expectedType, scalarValue = value.toString())
            is Long -> BackupPreference(key, expectedType, scalarValue = value.toString())
            is Float -> BackupPreference(key, expectedType, scalarValue = value.toString())
            is Boolean -> BackupPreference(key, expectedType, scalarValue = value.toString())
            is Set<*> -> {
                val values = value.mapNotNull { it as? String }.sorted()
                if (values.size == value.size) {
                    BackupPreference(key, expectedType, stringValues = values)
                } else {
                    null
                }
            }
            else -> null
        }
    }.sortedBy { it.key }

    fun restoreLearningData(values: List<BackupPreference>) {
        validateBackupPreferences(values)
        val editor = prefs.edit()
        prefs.all.keys.filter { backupTypeForKey(it) != null }.forEach(editor::remove)
        values.forEach { value ->
            when (value.type) {
                BackupPreference.TYPE_STRING -> editor.putString(value.key, value.scalarValue)
                BackupPreference.TYPE_INT -> editor.putInt(value.key, value.scalarValue.toInt())
                BackupPreference.TYPE_LONG -> editor.putLong(value.key, value.scalarValue.toLong())
                BackupPreference.TYPE_FLOAT -> editor.putFloat(value.key, value.scalarValue.toFloat())
                BackupPreference.TYPE_BOOLEAN -> editor.putBoolean(value.key, value.scalarValue.toBooleanStrict())
                BackupPreference.TYPE_STRING_SET -> editor.putStringSet(value.key, value.stringValues.toSet())
            }
        }
        check(editor.commit()) { "Could not save restored learning progress." }
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hasParentPin(): Boolean =
        !parentPrefs.getString(KEY_PARENT_PIN_HASH, null).isNullOrBlank()

    fun setParentPin(pin: String) {
        require(pin.length in 4..6 && pin.all { it.isDigit() }) {
            "PIN must contain 4 to 6 digits"
        }
        parentPrefs.edit()
            .putString(KEY_PARENT_PIN_HASH, hashPin(pin))
            .apply()
    }

    fun verifyParentPin(pin: String): Boolean {
        val saved = parentPrefs.getString(KEY_PARENT_PIN_HASH, null) ?: return false
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
                if (parts.size >= 2 && parts[0] == date.toString()) {
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
        private const val PREFERENCES_NAME = "little_genius_progress"
        private const val KEY_COMPLETED = "completed_lessons"
        private const val KEY_ATTEMPTS = "quiz_attempts"
        private const val KEY_CORRECT = "correct_answers"
        private const val KEY_HISTORY = "daily_learning_history"
        private const val KEY_PARENT_PIN_HASH = "parent_pin_hash"
        private const val KEY_LAST_LESSON_CLASS = "last_lesson_class"
        private const val KEY_LAST_LESSON_SUBJECT = "last_lesson_subject"
        private const val KEY_LAST_LESSON_ID = "last_lesson_id"
        private const val KEY_LESSON_HISTORY = "lesson_history"
        private const val KEY_LESSON_ANSWERS = "lesson_answer_history"
        private const val KEY_ACTIVITY_COMPLETED = "completed_practice_activities"
        private const val KEY_PRACTICAL_GUIDES = "completed_practical_guides"
        private const val MAX_ATTEMPT_HISTORY = 5_000
        private const val MAX_LESSON_ANSWER_HISTORY = 5_000

        fun validateBackupPreferences(values: List<BackupPreference>) {
            require(values.map { it.key }.distinct().size == values.size) {
                "Backup contains duplicate progress fields."
            }
            values.forEach { value ->
                require(backupTypeForKey(value.key) == value.type) {
                    "Backup contains an unsupported progress field."
                }
                when (value.type) {
                    BackupPreference.TYPE_STRING -> require(value.stringValues.isEmpty())
                    BackupPreference.TYPE_INT -> value.scalarValue.toIntOrNull()
                        ?: throw IllegalArgumentException("Invalid integer in backup.")
                    BackupPreference.TYPE_LONG -> value.scalarValue.toLongOrNull()
                        ?: throw IllegalArgumentException("Invalid long value in backup.")
                    BackupPreference.TYPE_FLOAT -> value.scalarValue.toFloatOrNull()
                        ?: throw IllegalArgumentException("Invalid decimal value in backup.")
                    BackupPreference.TYPE_BOOLEAN -> require(
                        value.scalarValue == "true" || value.scalarValue == "false"
                    )
                    BackupPreference.TYPE_STRING_SET -> require(value.scalarValue.isEmpty())
                    else -> throw IllegalArgumentException("Unsupported backup field type.")
                }
            }
        }

        private fun backupTypeForKey(key: String): String? = when (key) {
            KEY_COMPLETED,
            KEY_HISTORY,
            KEY_LESSON_HISTORY,
            KEY_LESSON_ANSWERS,
            KEY_ACTIVITY_COMPLETED,
            KEY_PRACTICAL_GUIDES -> BackupPreference.TYPE_STRING_SET
            KEY_ATTEMPTS, KEY_CORRECT -> BackupPreference.TYPE_INT
            KEY_LAST_LESSON_CLASS,
            KEY_LAST_LESSON_SUBJECT,
            KEY_LAST_LESSON_ID -> BackupPreference.TYPE_STRING
            else -> if (key.startsWith("study_tasks_")) {
                val date = key.removePrefix("study_tasks_")
                if (runCatching { LocalDate.parse(date) }.isSuccess) {
                    BackupPreference.TYPE_STRING_SET
                } else {
                    null
                }
            } else {
                null
            }
        }
    }
}
