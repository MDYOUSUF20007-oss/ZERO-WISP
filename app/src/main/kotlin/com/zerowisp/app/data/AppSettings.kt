package com.zerowisp.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zerowisp_settings")

data class ZeroWispSettings(
    val alwaysOutputEnglish: Boolean = true,
    val cleanFillerWords: Boolean = true,
    val autoChunk: Boolean = true,
    val language: String = "auto",
    val activeModelId: String? = null,
    val activeModelPath: String? = null
)

class AppSettings(private val context: Context) {

    companion object {
        val KEY_ALWAYS_OUTPUT_ENGLISH = booleanPreferencesKey("always_output_english")
        val KEY_CLEAN_FILLER_WORDS = booleanPreferencesKey("clean_filler_words")
        val KEY_AUTO_CHUNK = booleanPreferencesKey("auto_chunk")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_ACTIVE_MODEL_ID = stringPreferencesKey("active_model_id")
        val KEY_ACTIVE_MODEL_PATH = stringPreferencesKey("active_model_path")
    }

    val settingsFlow: Flow<ZeroWispSettings> = context.dataStore.data.map { prefs ->
        ZeroWispSettings(
            alwaysOutputEnglish = prefs[KEY_ALWAYS_OUTPUT_ENGLISH] != false, // default true
            cleanFillerWords = prefs[KEY_CLEAN_FILLER_WORDS] != false,        // default true
            autoChunk = prefs[KEY_AUTO_CHUNK] != false,                        // default true
            language = prefs[KEY_LANGUAGE] ?: "auto",
            activeModelId = prefs[KEY_ACTIVE_MODEL_ID],
            activeModelPath = prefs[KEY_ACTIVE_MODEL_PATH]
        )
    }

    suspend fun setAlwaysOutputEnglish(value: Boolean) {
        context.dataStore.edit { it[KEY_ALWAYS_OUTPUT_ENGLISH] = value }
    }

    suspend fun setCleanFillerWords(value: Boolean) {
        context.dataStore.edit { it[KEY_CLEAN_FILLER_WORDS] = value }
    }

    suspend fun setAutoChunk(value: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_CHUNK] = value }
    }

    suspend fun setActiveModel(modelId: String, modelPath: String) {
        context.dataStore.edit {
            it[KEY_ACTIVE_MODEL_ID] = modelId
            it[KEY_ACTIVE_MODEL_PATH] = modelPath
        }
    }

    suspend fun clearActiveModel() {
        context.dataStore.edit {
            it.remove(KEY_ACTIVE_MODEL_ID)
            it.remove(KEY_ACTIVE_MODEL_PATH)
        }
    }
}
