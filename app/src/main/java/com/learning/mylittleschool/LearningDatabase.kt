package com.learning.mylittleschool

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

const val DEFAULT_PROFILE_ID = "default_child"

@Entity(tableName = "child_profiles")
data class ChildProfileEntity(
    @PrimaryKey val profileId: String,
    val displayName: String,
    val classId: String,
    val createdAt: Long,
    val isDefault: Boolean
)

@Entity(
    tableName = "favorite_lessons",
    primaryKeys = ["profileId", "lessonId"],
    indices = [Index(value = ["profileId", "savedAt"])]
)
data class FavoriteLessonEntity(
    val profileId: String,
    val lessonId: String,
    val classId: String,
    val subject: String,
    val title: String,
    val savedAt: Long
)

@Entity(
    tableName = "homework_tasks",
    indices = [Index(value = ["profileId", "dueAt"])]
)
data class HomeworkEntity(
    @PrimaryKey val taskId: String,
    val profileId: String,
    val title: String,
    val subject: String,
    val dueAt: Long,
    val reminderAt: Long?,
    val isCompleted: Boolean,
    val createdAt: Long
)

@Entity(
    tableName = "parent_announcements",
    primaryKeys = ["profileId", "announcementId"],
    indices = [Index(value = ["profileId", "createdAt"])]
)
data class ParentAnnouncementEntity(
    val profileId: String,
    val announcementId: String,
    val title: String,
    val audioFileName: String,
    val createdAt: Long,
    val durationMs: Long
)

@Dao
interface ChildProfileDao {
    @Query("SELECT * FROM child_profiles ORDER BY createdAt ASC")
    fun observeProfiles(): Flow<List<ChildProfileEntity>>

    @Query("SELECT * FROM child_profiles ORDER BY createdAt ASC")
    suspend fun getAllProfiles(): List<ChildProfileEntity>

    @Query("SELECT * FROM child_profiles WHERE profileId = :profileId LIMIT 1")
    suspend fun getProfile(profileId: String): ChildProfileEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(profile: ChildProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceAll(profiles: List<ChildProfileEntity>)

    @Query("UPDATE child_profiles SET classId = :classId WHERE profileId = :profileId")
    suspend fun updateClass(profileId: String, classId: String)

    @Query("DELETE FROM child_profiles")
    suspend fun deleteAll()

}

@Dao
interface FavoriteLessonDao {
    @Query("SELECT * FROM favorite_lessons WHERE profileId = :profileId ORDER BY savedAt DESC")
    fun observeFavorites(profileId: String): Flow<List<FavoriteLessonEntity>>

    @Query("SELECT * FROM favorite_lessons ORDER BY profileId ASC, savedAt DESC")
    suspend fun getAllFavorites(): List<FavoriteLessonEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_lessons WHERE profileId = :profileId AND lessonId = :lessonId)")
    fun observeIsFavorite(profileId: String, lessonId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(favorite: FavoriteLessonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceAll(favorites: List<FavoriteLessonEntity>)

    @Query("DELETE FROM favorite_lessons WHERE profileId = :profileId AND lessonId = :lessonId")
    suspend fun remove(profileId: String, lessonId: String)

    @Query("DELETE FROM favorite_lessons")
    suspend fun deleteAll()
}

@Dao
interface HomeworkDao {
    @Query("SELECT * FROM homework_tasks WHERE profileId = :profileId ORDER BY isCompleted ASC, dueAt ASC")
    fun observeTasks(profileId: String): Flow<List<HomeworkEntity>>

    @Query("SELECT * FROM homework_tasks ORDER BY profileId ASC, dueAt ASC")
    suspend fun getAllTasks(): List<HomeworkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(task: HomeworkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceAll(tasks: List<HomeworkEntity>)

    @Query("UPDATE homework_tasks SET isCompleted = :completed WHERE profileId = :profileId AND taskId = :taskId")
    suspend fun setCompleted(profileId: String, taskId: String, completed: Boolean)

    @Query("DELETE FROM homework_tasks WHERE profileId = :profileId AND taskId = :taskId")
    suspend fun delete(profileId: String, taskId: String)

    @Query("DELETE FROM homework_tasks")
    suspend fun deleteAll()
}

@Dao
interface ParentAnnouncementDao {
    @Query("SELECT * FROM parent_announcements WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun observeAnnouncements(profileId: String): Flow<List<ParentAnnouncementEntity>>

    @Query("SELECT * FROM parent_announcements WHERE profileId = :profileId ORDER BY createdAt DESC")
    suspend fun getForProfile(profileId: String): List<ParentAnnouncementEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(announcement: ParentAnnouncementEntity)

    @Query("DELETE FROM parent_announcements WHERE profileId = :profileId AND announcementId = :announcementId")
    suspend fun delete(profileId: String, announcementId: String)
}

@Database(
    entities = [ChildProfileEntity::class, FavoriteLessonEntity::class, HomeworkEntity::class, ParentAnnouncementEntity::class],
    version = 4,
    exportSchema = false
)
abstract class LearningDatabase : RoomDatabase() {
    abstract fun childProfileDao(): ChildProfileDao
    abstract fun favoriteLessonDao(): FavoriteLessonDao
    abstract fun homeworkDao(): HomeworkDao
    abstract fun parentAnnouncementDao(): ParentAnnouncementDao

    companion object {
        private const val DATABASE_NAME = "little_genius_learning.db"

        /** Adds child profiles while retaining all bookmarks from schema version 1. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS child_profiles (
                        profileId TEXT NOT NULL PRIMARY KEY,
                        displayName TEXT NOT NULL,
                        classId TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        isDefault INTEGER NOT NULL
                    )""".trimIndent()
                )
            }
        }

        /** Adds per-profile homework without changing existing profiles or favorites. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS homework_tasks (
                        taskId TEXT NOT NULL PRIMARY KEY,
                        profileId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        subject TEXT NOT NULL,
                        dueAt INTEGER NOT NULL,
                        reminderAt INTEGER,
                        isCompleted INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )""".trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_homework_tasks_profileId_dueAt " +
                        "ON homework_tasks(profileId, dueAt)"
                )
            }
        }

        /** Adds child-scoped voice announcement history without changing existing study data. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS parent_announcements (
                        profileId TEXT NOT NULL,
                        announcementId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        audioFileName TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        durationMs INTEGER NOT NULL,
                        PRIMARY KEY(profileId, announcementId)
                    )""".trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_parent_announcements_profileId_createdAt " +
                        "ON parent_announcements(profileId, createdAt)"
                )
            }
        }

        @Volatile
        private var instance: LearningDatabase? = null

        fun getInstance(context: Context): LearningDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                LearningDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                .also { instance = it }
        }
    }
}
