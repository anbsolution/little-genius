package com.learning.mylittleschool

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.littleGeniusSettings: DataStore<Preferences> by preferencesDataStore(
    name = "little_genius_settings"
)

class AppSettingsStore(context: Context) {
    private val dataStore = context.applicationContext.littleGeniusSettings
    private val languageKey = stringPreferencesKey("preferred_language")

    val preferredLanguage: Flow<String> = dataStore.data.map { preferences ->
        preferences[languageKey]?.takeIf { it == "Hindi" || it == "English" } ?: "English"
    }

    suspend fun setPreferredLanguage(language: String) {
        require(language == "Hindi" || language == "English")
        dataStore.edit { preferences -> preferences[languageKey] = language }
    }
}
