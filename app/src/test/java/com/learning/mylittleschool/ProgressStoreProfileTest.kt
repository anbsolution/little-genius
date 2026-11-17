package com.learning.mylittleschool

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProgressStoreProfileTest {
    @Test
    fun lessonAccuracyIsProfileScopedAndIncludedInBackupData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val suffix = UUID.randomUUID().toString()
        val firstId = "accuracy-$suffix-a"
        val secondId = "accuracy-$suffix-b"
        val firstPreferences = context.getSharedPreferences(
            "little_genius_progress_$firstId",
            Context.MODE_PRIVATE
        )
        val secondPreferences = context.getSharedPreferences(
            "little_genius_progress_$secondId",
            Context.MODE_PRIVATE
        )
        firstPreferences.edit().clear().commit()
        secondPreferences.edit().clear().commit()

        try {
            val firstStore = ProgressStore(context, firstId)
            val secondStore = ProgressStore(context, secondId)
            firstStore.recordLesson("c1_add", correct = false)
            firstStore.recordLesson("c1_add", correct = false)
            firstStore.recordLesson("c1_add", correct = true)
            firstStore.recordLesson("c1_nouns", correct = true)
            secondStore.recordLesson("c1_add", correct = true)

            val addition = firstStore.lessonPerformance().single { it.lessonId == "c1_add" }
            assertEquals(3, addition.attempts)
            assertEquals(1, addition.correct)
            assertEquals(33, addition.accuracyPercent)
            assertEquals(100, secondStore.lessonPerformance().single().accuracyPercent)

            val backupData = firstStore.exportLearningData()
            assertTrue(backupData.any { it.key == "lesson_answer_history" })
            ProgressStore.validateBackupPreferences(backupData)
            firstStore.restoreLearningData(emptyList())
            assertTrue(firstStore.lessonPerformance().isEmpty())
            firstStore.restoreLearningData(backupData)
            assertEquals(3, firstStore.lessonPerformance().single { it.lessonId == "c1_add" }.attempts)
        } finally {
            firstPreferences.edit().clear().commit()
            secondPreferences.edit().clear().commit()
        }
    }

    @Test
    fun legacyProgressRemainsAndNewChildProgressIsIsolated() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val legacyName = "little_genius_progress"
        val legacyPreferences = context.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
        val childId = "child-${UUID.randomUUID()}"
        val childPreferences = context.getSharedPreferences("${legacyName}_$childId", Context.MODE_PRIVATE)

        legacyPreferences.edit().clear().putInt("quiz_attempts", 7).commit()
        childPreferences.edit().clear().commit()

        try {
            val legacyStore = ProgressStore(context)
            val childStore = ProgressStore(context, childId)

            assertEquals(7, legacyStore.attempts())
            assertEquals(0, childStore.attempts())
            childStore.recordActivityAnswer("child-practice", correct = true)
            assertEquals(1, childStore.attempts())
            assertEquals(1, childStore.correctAnswers())
            assertEquals(7, legacyStore.attempts())
            assertFalse(legacyStore.isActivityCompleted("child-practice"))

            legacyStore.setParentPin("2468")
            assertTrue(childStore.hasParentPin())
            assertTrue(childStore.verifyParentPin("2468"))
        } finally {
            legacyPreferences.edit().clear().commit()
            childPreferences.edit().clear().commit()
        }
    }
}
