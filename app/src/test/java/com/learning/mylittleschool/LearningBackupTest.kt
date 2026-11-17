package com.learning.mylittleschool

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LearningBackupTest {
    @Test
    fun codecRoundTripsLearningDataAndRejectsUnsafeProgressFields() {
        val defaultProfile = ChildProfileEntity(DEFAULT_PROFILE_ID, "Student", "class_1", 1L, true)
        val child = ChildProfileEntity("child_a", "Asha", "class_2", 2L, false)
        val snapshot = LearningBackupSnapshot(
            createdAt = 3L,
            activeProfileId = child.profileId,
            preferredLanguage = "Hindi",
            profiles = listOf(defaultProfile, child),
            favorites = listOf(
                FavoriteLessonEntity(child.profileId, "lesson_2", "class_2", "Hindi", "शब्द", 4L)
            ),
            progressByProfile = mapOf(
                defaultProfile.profileId to listOf(
                    BackupPreference("quiz_attempts", BackupPreference.TYPE_INT, scalarValue = "3")
                ),
                child.profileId to listOf(
                    BackupPreference(
                        "completed_lessons",
                        BackupPreference.TYPE_STRING_SET,
                        stringValues = listOf("lesson_2")
                    )
                )
            )
        )

        val encoded = LearningBackupCodec.encode(snapshot)
        assertEquals(snapshot, LearningBackupCodec.decode(encoded))
        assertFalse(encoded.contains("parent_pin_hash"))
        assertThrows(IllegalArgumentException::class.java) {
            LearningBackupCodec.decode(encoded.replace("quiz_attempts", "parent_pin_hash"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            LearningBackupCodec.decode(encoded.replace("\"version\":1", "\"version\":9"))
        }
    }

    @Test
    fun repositoryRestoresProfilesFavoritesProgressSettingsAndKeepsParentPin() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val defaultPreferences = context.getSharedPreferences("little_genius_progress", Context.MODE_PRIVATE)
        val originalDefaultPreferences = defaultPreferences.all
        val childId = "child-${UUID.randomUUID()}"
        val settings = AppSettingsStore(context)
        val originalLanguage = settings.preferredLanguage.first()
        val originalActiveProfile = settings.activeProfileId.first()
        val database = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        defaultPreferences.edit().clear().commit()
        context.deleteSharedPreferences("little_genius_progress_$childId")
        try {
            val defaultProfile = ChildProfileEntity(DEFAULT_PROFILE_ID, "Student", "class_1", 1L, true)
            val childProfile = ChildProfileEntity(childId, "Asha", "class_2", 2L, false)
            database.childProfileDao().insert(defaultProfile)
            database.childProfileDao().insert(childProfile)
            database.favoriteLessonDao().save(
                FavoriteLessonEntity(childId, "fractions", "class_2", "Mathematics", "Fractions", 5L)
            )

            val defaultStore = ProgressStore(context)
            val childStore = ProgressStore(context, childId)
            defaultStore.setParentPin("5731")
            defaultStore.recordActivityAnswer("default-practice", correct = true)
            childStore.recordLesson("fractions", correct = true)
            settings.setPreferredLanguage("Hindi")
            settings.setActiveProfileId(childId)

            val repository = LearningBackupRepository(context, database, settings)
            val backup = repository.createBackup()
            assertFalse(backup.contains("5731"))
            assertFalse(backup.contains("parent_pin_hash"))

            defaultStore.recordActivityAnswer("new-default-practice", correct = false)
            childStore.recordActivityAnswer("new-child-practice", correct = false)
            database.favoriteLessonDao().deleteAll()
            database.childProfileDao().deleteAll()
            database.childProfileDao().insert(
                ChildProfileEntity(DEFAULT_PROFILE_ID, "Changed", "class_3", 8L, true)
            )
            settings.setPreferredLanguage("English")
            settings.setActiveProfileId(DEFAULT_PROFILE_ID)

            repository.restoreBackup(backup)

            assertEquals(listOf(DEFAULT_PROFILE_ID, childId), database.childProfileDao().getAllProfiles().map { it.profileId })
            assertEquals("Student", database.childProfileDao().getProfile(DEFAULT_PROFILE_ID)?.displayName)
            assertEquals(
                listOf("fractions"),
                database.favoriteLessonDao().observeFavorites(childId).first().map { it.lessonId }
            )
            assertEquals(1, defaultStore.attempts())
            assertEquals(1, childStore.attempts())
            assertEquals("Hindi", settings.preferredLanguage.first())
            assertEquals(childId, settings.activeProfileId.first())
            assertTrue(defaultStore.verifyParentPin("5731"))
            assertNotNull(LearningBackupCodec.decode(backup))
        } finally {
            database.close()
            context.deleteSharedPreferences("little_genius_progress_$childId")
            restorePreferences(defaultPreferences, originalDefaultPreferences)
            settings.setPreferredLanguage(originalLanguage)
            settings.setActiveProfileId(originalActiveProfile)
        }
    }

    private fun restorePreferences(
        preferences: SharedPreferences,
        values: Map<String, *>
    ) {
        val editor = preferences.edit().clear()
        values.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        assertTrue(editor.commit())
    }
}
