package com.zerowisp.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.historyStore: DataStore<Preferences> by preferencesDataStore(name = "zerowisp_history")

data class TranscriptEntry(
    val id: Long,
    val text: String,
    val timestamp: Long
)

class HistoryRepository(private val context: Context) {

    companion object {
        private val KEY_ENTRIES = stringPreferencesKey("entries")
        private const val MAX_ENTRIES = 20
    }

    val entriesFlow: Flow<List<TranscriptEntry>> = context.historyStore.data.map { prefs ->
        val json = prefs[KEY_ENTRIES] ?: return@map emptyList()
        parseEntries(json)
    }

    suspend fun addEntry(text: String) {
        context.historyStore.edit { prefs ->
            val current = parseEntries(prefs[KEY_ENTRIES] ?: "[]").toMutableList()
            current.add(0, TranscriptEntry(System.currentTimeMillis(), text, System.currentTimeMillis()))
            if (current.size > MAX_ENTRIES) {
                current.subList(MAX_ENTRIES, current.size).clear()
            }
            prefs[KEY_ENTRIES] = serializeEntries(current)
        }
    }

    suspend fun clearAll() {
        context.historyStore.edit { it.remove(KEY_ENTRIES) }
    }

    private fun parseEntries(json: String): List<TranscriptEntry> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                TranscriptEntry(
                    id = obj.getLong("id"),
                    text = obj.getString("text"),
                    timestamp = obj.getLong("timestamp")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeEntries(entries: List<TranscriptEntry>): String {
        val arr = JSONArray()
        entries.forEach { e ->
            val obj = org.json.JSONObject()
            obj.put("id", e.id)
            obj.put("text", e.text)
            obj.put("timestamp", e.timestamp)
            arr.put(obj)
        }
        return arr.toString()
    }
}
