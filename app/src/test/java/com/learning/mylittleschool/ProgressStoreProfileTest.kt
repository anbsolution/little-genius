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
