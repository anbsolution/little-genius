package com.learning.mylittleschool

import android.content.Context

/** Small offline progress store. Uses Android SharedPreferences; no account or network required. */
class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("little_genius_progress", Context.MODE_PRIVATE)

    @Synchronized fun recordLesson(lessonId: String, correct: Boolean) {
        val completed = prefs.getStringSet(KEY_COMPLETED, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (correct) completed.add(lessonId)
        val attempts = prefs.getInt(KEY_ATTEMPTS, 0) + 1
        val correctCount = prefs.getInt(KEY_CORRECT, 0) + if (correct) 1 else 0
        prefs.edit()
            .putStringSet(KEY_COMPLETED, completed)
            .putInt(KEY_ATTEMPTS, attempts)
            .putInt(KEY_CORRECT, correctCount)
            .apply()
    }

    fun completedCount(): Int = prefs.getStringSet(KEY_COMPLETED, emptySet())?.size ?: 0
    fun attempts(): Int = prefs.getInt(KEY_ATTEMPTS, 0)
    fun correctAnswers(): Int = prefs.getInt(KEY_CORRECT, 0)

    companion object {
        private const val KEY_COMPLETED = "completed_lessons"
        private const val KEY_ATTEMPTS = "quiz_attempts"
        private const val KEY_CORRECT = "correct_answers"
    }
}
