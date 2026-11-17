package com.learning.mylittleschool

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LearningDatabaseTest {
    @Test
    fun migrationPreservesFavoritesAndKeepsProfilesSeparate() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "learning-migration-${UUID.randomUUID()}.db"
        val legacyDatabase = VersionOneDatabase(context, databaseName)
        legacyDatabase.writableDatabase.use { legacy ->
            legacy.execSQL(
                """INSERT INTO favorite_lessons
                    (profileId, lessonId, classId, subject, title, savedAt)
                    VALUES ('child-a', 'lesson-one', 'class_1', 'Mathematics', 'Addition', 10)"""
            )
        }
        legacyDatabase.close()

        val database = Room.databaseBuilder(
            context,
            LearningDatabase::class.java,
            databaseName
        )
            .addMigrations(LearningDatabase.MIGRATION_1_2)
            .addMigrations(LearningDatabase.MIGRATION_2_3)
            .build()

        try {
            val migratedFavorites = database.favoriteLessonDao()
                .observeFavorites("child-a")
                .first()
            assertEquals(listOf("lesson-one"), migratedFavorites.map { it.lessonId })

            database.childProfileDao().insert(
                ChildProfileEntity("child-a", "Asha", "class_1", 1L, false)
            )
            database.childProfileDao().insert(
                ChildProfileEntity("child-b", "Kabir", "class_2", 2L, false)
            )
            database.favoriteLessonDao().save(
                FavoriteLessonEntity("child-b", "lesson-two", "class_2", "English", "Nouns", 20L)
            )
            database.homeworkDao().save(
                HomeworkEntity("task-one", "child-a", "Finish page 4", "Mathematics", 100L, 50L, false, 30L)
            )

            assertNotNull(database.childProfileDao().getProfile("child-a"))
            assertEquals(
                listOf("lesson-one"),
                database.favoriteLessonDao().observeFavorites("child-a").first().map { it.lessonId }
            )
            assertEquals(
                listOf("lesson-two"),
                database.favoriteLessonDao().observeFavorites("child-b").first().map { it.lessonId }
            )
            assertEquals(
                listOf("task-one"),
                database.homeworkDao().observeTasks("child-a").first().map { it.taskId }
            )
            assertEquals(emptyList<String>(), database.homeworkDao().observeTasks("child-b").first().map { it.taskId })
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    private class VersionOneDatabase(
        context: Context,
        name: String
    ) : SQLiteOpenHelper(context, name, null, 1) {
        override fun onCreate(database: SQLiteDatabase) {
            database.execSQL(
                """CREATE TABLE favorite_lessons (
                    profileId TEXT NOT NULL,
                    lessonId TEXT NOT NULL,
                    classId TEXT NOT NULL,
                    subject TEXT NOT NULL,
                    title TEXT NOT NULL,
                    savedAt INTEGER NOT NULL,
                    PRIMARY KEY(profileId, lessonId)
                )"""
            )
            database.execSQL(
                "CREATE INDEX index_favorite_lessons_profileId_savedAt " +
                    "ON favorite_lessons(profileId, savedAt)"
            )
        }

        override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }
}
