package com.learning.mylittleschool

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CertificateMilestoneTest {
    @Test
    fun certificateUnlocksAfterFiveUniqueCompletedLessons() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val profileId = "certificate-${UUID.randomUUID()}"
        val preferencesName = "little_genius_progress_$profileId"
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        preferences.edit().clear().commit()

        try {
            val store = ProgressStore(context, profileId)
            repeat(CertificateMilestone.LESSON_TARGET - 1) { index ->
                store.recordLesson("lesson-$index", correct = true)
            }
            assertFalse(CertificateMilestone.isUnlocked(store))

            store.recordLesson("lesson-final", correct = true)
            assertTrue(CertificateMilestone.isUnlocked(store))
        } finally {
            context.deleteSharedPreferences(preferencesName)
        }
    }
}
