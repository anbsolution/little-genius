package com.learning.mylittleschool

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BackupPreference(
    val key: String,
    val type: String,
    val scalarValue: String = "",
    val stringValues: List<String> = emptyList()
) {
    companion object {
        const val TYPE_STRING = "string"
        const val TYPE_INT = "int"
        const val TYPE_LONG = "long"
        const val TYPE_FLOAT = "float"
        const val TYPE_BOOLEAN = "boolean"
        const val TYPE_STRING_SET = "string_set"
    }
}

data class LearningBackupSnapshot(
    val createdAt: Long,
    val activeProfileId: String,
    val preferredLanguage: String,
    val profiles: List<ChildProfileEntity>,
    val favorites: List<FavoriteLessonEntity>,
    val progressByProfile: Map<String, List<BackupPreference>>,
    val homework: List<HomeworkEntity> = emptyList()
)

object LearningBackupCodec {
    private const val FORMAT = "little-genius-backup"
    private const val VERSION = 2
    const val MAX_BACKUP_BYTES = 8 * 1024 * 1024

    fun encode(snapshot: LearningBackupSnapshot): String {
        require(snapshot.createdAt > 0L)
        require(snapshot.profiles.size in 1..MAX_PROFILES)
        require(snapshot.profiles.any { it.profileId == DEFAULT_PROFILE_ID })
        require(snapshot.profiles.count { it.isDefault } == 1)
        require(snapshot.profiles.single { it.isDefault }.profileId == DEFAULT_PROFILE_ID)
        require(snapshot.activeProfileId in snapshot.profiles.map { it.profileId })
        require(snapshot.preferredLanguage == "English" || snapshot.preferredLanguage == "Hindi")
        val profileIds = snapshot.profiles.map { it.profileId }.toSet()
        require(profileIds.size == snapshot.profiles.size)
        snapshot.profiles.forEach { profile ->
            requireSafeId(profile.profileId)
            requireSafeId(profile.classId)
            require(profile.displayName.length in 1..40)
            require(profile.createdAt > 0L)
        }
        require(snapshot.favorites.all { it.profileId in profileIds })
        require(snapshot.favorites.size <= MAX_FAVORITES)
        require(snapshot.favorites.map { it.profileId to it.lessonId }.toSet().size == snapshot.favorites.size)
        snapshot.favorites.forEach { favorite ->
            requireSafeId(favorite.lessonId)
            requireSafeId(favorite.classId)
            require(favorite.subject.length in 1..80)
            require(favorite.title.length in 1..160)
            require(favorite.savedAt >= 0L)
        }
        require(snapshot.homework.size <= MAX_HOMEWORK_TASKS)
        require(snapshot.homework.map { it.taskId }.toSet().size == snapshot.homework.size)
        snapshot.homework.forEach { task ->
            require(task.profileId in profileIds)
            requireSafeId(task.taskId)
            require(task.title.length in 1..160)
            require(task.subject.length in 1..80)
            require(task.dueAt > 0L)
            require(task.reminderAt == null || task.reminderAt > 0L)
            require(task.reminderAt == null || task.reminderAt <= task.dueAt)
            require(task.createdAt > 0L)
        }
        require(snapshot.progressByProfile.values.all { it.size <= MAX_PREFERENCES })
        require(snapshot.progressByProfile.keys == profileIds)
        snapshot.progressByProfile.values.forEach { ProgressStore.validateBackupPreferences(it) }

        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("createdAt", snapshot.createdAt)
            .put("activeProfileId", snapshot.activeProfileId)
            .put("preferredLanguage", snapshot.preferredLanguage)
            .put("profiles", JSONArray().apply {
                snapshot.profiles.forEach { profile ->
                    put(
                        JSONObject()
                            .put("profileId", profile.profileId)
                            .put("displayName", profile.displayName)
                            .put("classId", profile.classId)
                            .put("createdAt", profile.createdAt)
                            .put("isDefault", profile.isDefault)
                    )
                }
            })
            .put("favorites", JSONArray().apply {
                snapshot.favorites.forEach { favorite ->
                    put(
                        JSONObject()
                            .put("profileId", favorite.profileId)
                            .put("lessonId", favorite.lessonId)
                            .put("classId", favorite.classId)
                            .put("subject", favorite.subject)
                            .put("title", favorite.title)
                            .put("savedAt", favorite.savedAt)
                    )
                }
            })
            .put("homework", JSONArray().apply {
                snapshot.homework.forEach { task ->
                    put(
                        JSONObject()
                            .put("taskId", task.taskId)
                            .put("profileId", task.profileId)
                            .put("title", task.title)
                            .put("subject", task.subject)
                            .put("dueAt", task.dueAt)
                            .put("reminderAt", task.reminderAt ?: JSONObject.NULL)
                            .put("isCompleted", task.isCompleted)
                            .put("createdAt", task.createdAt)
                    )
                }
            })
            .put("progress", JSONArray().apply {
                snapshot.progressByProfile.toSortedMap().forEach { (profileId, values) ->
                    put(
                        JSONObject()
                            .put("profileId", profileId)
                            .put("preferences", JSONArray().apply {
                                values.forEach { preference ->
                                    val item = JSONObject()
                                        .put("key", preference.key)
                                        .put("type", preference.type)
                                    if (preference.type == BackupPreference.TYPE_STRING_SET) {
                                        item.put("values", JSONArray(preference.stringValues))
                                    } else {
                                        item.put("value", preference.scalarValue)
                                    }
                                    put(item)
                                }
                            })
                    )
                }
            })
        return root.toString().also { require(it.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_BYTES) }
    }

    fun decode(json: String): LearningBackupSnapshot {
        require(json.isNotEmpty() && json.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_BYTES) {
            "Backup file is empty or too large."
        }
        val root = JSONObject(json)
        require(root.getString("format") == FORMAT) { "This is not a Little Genius backup file." }
        val version = root.getInt("version")
        require(version in 1..VERSION) { "This backup version is not supported." }

        val profilesArray = root.getJSONArray("profiles")
        require(profilesArray.length() in 1..MAX_PROFILES) { "Backup has an invalid profile count." }
        val profiles = (0 until profilesArray.length()).map { index ->
            val item = profilesArray.getJSONObject(index)
            ChildProfileEntity(
                profileId = item.getString("profileId").also(::requireSafeId),
                displayName = item.getString("displayName").also { require(it.length in 1..40) },
                classId = item.getString("classId").also(::requireSafeId),
                createdAt = item.getLong("createdAt").also { require(it > 0L) },
                isDefault = item.getBoolean("isDefault")
            )
        }
        val profileIds = profiles.map { it.profileId }.toSet()
        require(profileIds.size == profiles.size) { "Backup contains duplicate profiles." }
        require(DEFAULT_PROFILE_ID in profileIds) { "Backup is missing the default student profile." }
        require(profiles.count { it.isDefault } == 1 && profiles.single { it.isDefault }.profileId == DEFAULT_PROFILE_ID) {
            "Backup has an invalid default profile."
        }

        val favoritesArray = root.getJSONArray("favorites")
        require(favoritesArray.length() <= MAX_FAVORITES) { "Backup contains too many favorite lessons." }
        val favorites = (0 until favoritesArray.length()).map { index ->
            val item = favoritesArray.getJSONObject(index)
            FavoriteLessonEntity(
                profileId = item.getString("profileId").also { require(it in profileIds) },
                lessonId = item.getString("lessonId").also(::requireSafeId),
                classId = item.getString("classId").also(::requireSafeId),
                subject = item.getString("subject").also { require(it.length in 1..80) },
                title = item.getString("title").also { require(it.length in 1..160) },
                savedAt = item.getLong("savedAt").also { require(it >= 0L) }
            )
        }
        require(favorites.map { it.profileId to it.lessonId }.toSet().size == favorites.size) {
            "Backup contains duplicate favorite lessons."
        }

        val homework = if (version >= 2) {
            val homeworkArray = root.getJSONArray("homework")
            require(homeworkArray.length() <= MAX_HOMEWORK_TASKS) { "Backup contains too many homework tasks." }
            (0 until homeworkArray.length()).map { index ->
                val item = homeworkArray.getJSONObject(index)
                HomeworkEntity(
                    taskId = item.getString("taskId").also(::requireSafeId),
                    profileId = item.getString("profileId").also { require(it in profileIds) },
                    title = item.getString("title").also { require(it.length in 1..160) },
                    subject = item.getString("subject").also { require(it.length in 1..80) },
                    dueAt = item.getLong("dueAt").also { require(it > 0L) },
                    reminderAt = if (item.isNull("reminderAt")) null else item.getLong("reminderAt").also {
                        require(it > 0L && it <= item.getLong("dueAt"))
                    },
                    isCompleted = item.getBoolean("isCompleted"),
                    createdAt = item.getLong("createdAt").also { require(it > 0L) }
                )
            }.also { tasks ->
                require(tasks.map { it.taskId }.toSet().size == tasks.size) {
                    "Backup contains duplicate homework tasks."
                }
            }
        } else {
            emptyList()
        }

        val progressArray = root.getJSONArray("progress")
        val progress = buildMap<String, List<BackupPreference>> {
            for (index in 0 until progressArray.length()) {
                val item = progressArray.getJSONObject(index)
                val profileId = item.getString("profileId").also { require(it in profileIds) }
                require(!containsKey(profileId)) { "Backup contains duplicate progress profiles." }
                val preferences = item.getJSONArray("preferences")
                require(preferences.length() <= MAX_PREFERENCES) { "Backup has too many progress values." }
                val values = (0 until preferences.length()).map { preferenceIndex ->
                    val preference = preferences.getJSONObject(preferenceIndex)
                    val type = preference.getString("type")
                    if (type == BackupPreference.TYPE_STRING_SET) {
                        val strings = preference.getJSONArray("values")
                        BackupPreference(
                            key = preference.getString("key"),
                            type = type,
                            stringValues = (0 until strings.length()).map(strings::getString).also { values ->
                                require(values.distinct().size == values.size) {
                                    "Backup contains duplicate progress values."
                                }
                            }
                        )
                    } else {
                        BackupPreference(
                            key = preference.getString("key"),
                            type = type,
                            scalarValue = preference.getString("value")
                        )
                    }
                }
                ProgressStore.validateBackupPreferences(values)
                put(profileId, values)
            }
        }
        require(progress.keys == profileIds) { "Backup is missing progress for one or more profiles." }

        val activeProfileId = root.getString("activeProfileId").also { require(it in profileIds) }
        val language = root.getString("preferredLanguage")
        require(language == "English" || language == "Hindi") { "Backup language is invalid." }
        val createdAt = root.getLong("createdAt").also { require(it > 0L) }

        return LearningBackupSnapshot(
            createdAt = createdAt,
            activeProfileId = activeProfileId,
            preferredLanguage = language,
            profiles = profiles,
            favorites = favorites,
            progressByProfile = progress,
            homework = homework
        )
    }

    private fun requireSafeId(value: String) {
        require(value.matches(Regex("[A-Za-z0-9_-]{1,100}"))) { "Backup contains an invalid identifier." }
    }

    private const val MAX_PROFILES = 100
    private const val MAX_FAVORITES = 10_000
    private const val MAX_PREFERENCES = 20_000
    private const val MAX_HOMEWORK_TASKS = 10_000
}

class LearningBackupRepository(
    context: Context,
    private val database: LearningDatabase,
    private val settingsStore: AppSettingsStore
) {
    private val appContext = context.applicationContext

    suspend fun createBackup(): String = withContext(Dispatchers.IO) {
        val profiles = database.childProfileDao().getAllProfiles()
        require(profiles.any { it.profileId == DEFAULT_PROFILE_ID }) {
            "Choose a class before creating the first backup."
        }
        val progressByProfile = profiles.associate { profile ->
            profile.profileId to ProgressStore(appContext, profile.profileId).exportLearningData()
        }
        LearningBackupCodec.encode(
            LearningBackupSnapshot(
                createdAt = System.currentTimeMillis(),
                activeProfileId = settingsStore.activeProfileId.first(),
                preferredLanguage = settingsStore.preferredLanguage.first(),
                profiles = profiles,
                favorites = database.favoriteLessonDao().getAllFavorites(),
                progressByProfile = progressByProfile,
                homework = database.homeworkDao().getAllTasks()
            )
        )
    }

    suspend fun restoreBackup(json: String) = withContext(Dispatchers.IO) {
        val snapshot = LearningBackupCodec.decode(json)
        val previousProfiles = database.childProfileDao().getAllProfiles()
        val previousFavorites = database.favoriteLessonDao().getAllFavorites()
        val previousHomework = database.homeworkDao().getAllTasks()
        val profileIdsToRestore = (previousProfiles.map { it.profileId } + snapshot.profiles.map { it.profileId }).distinct()
        val previousProgress = profileIdsToRestore.associateWith { profileId ->
            ProgressStore(appContext, profileId).exportLearningData()
        }
        val previousLanguage = settingsStore.preferredLanguage.first()
        val previousActiveProfileId = settingsStore.activeProfileId.first()

        try {
            profileIdsToRestore.forEach { profileId ->
                ProgressStore(appContext, profileId).restoreLearningData(
                    snapshot.progressByProfile[profileId].orEmpty()
                )
            }
            replaceDatabase(snapshot.profiles, snapshot.favorites, snapshot.homework)
            settingsStore.setPreferredLanguage(snapshot.preferredLanguage)
            settingsStore.setActiveProfileId(snapshot.activeProfileId)
        } catch (failure: Throwable) {
            withContext(NonCancellable) {
                try {
                    profileIdsToRestore.forEach { profileId ->
                        ProgressStore(appContext, profileId).restoreLearningData(previousProgress[profileId].orEmpty())
                    }
                    replaceDatabase(previousProfiles, previousFavorites, previousHomework)
                    settingsStore.setPreferredLanguage(previousLanguage)
                    settingsStore.setActiveProfileId(previousActiveProfileId)
                } catch (rollbackFailure: Throwable) {
                    failure.addSuppressed(rollbackFailure)
                }
            }
            throw failure
        }
    }

    private suspend fun replaceDatabase(
        profiles: List<ChildProfileEntity>,
        favorites: List<FavoriteLessonEntity>,
        homework: List<HomeworkEntity>
    ) {
        database.withTransaction {
            database.homeworkDao().deleteAll()
            database.favoriteLessonDao().deleteAll()
            database.childProfileDao().deleteAll()
            database.childProfileDao().replaceAll(profiles)
            database.favoriteLessonDao().replaceAll(favorites)
            database.homeworkDao().replaceAll(homework)
        }
    }
}
