package com.babakids.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Spec §13 ("إضافة كلمات جديدة، إضافة صور جديدة") + §14 (تسجيل صوت الوالد)
 * + §17 (every card = image + word + audio + category + difficulty).
 *
 * Storage: JSON via org.json (built into Android, no extra dependency
 * needed). This replaced an earlier hand-rolled delimiter scheme
 * (`id~word~emoji~...` joined by a unit separator) that turned out to be
 * the actual cause of custom words losing their word text/image — proper
 * JSON serialization removes that whole class of encode/decode bug
 * instead of chasing it field by field.
 */
class CustomWordsRepository(private val context: Context) {

    private val customWordsKey = stringPreferencesKey("custom_words_v3")

    val customWordsFlow: Flow<List<WordItem>> =
        context.appDataStore.data.map { prefs ->
            decode(prefs[customWordsKey] ?: "")
        }

    suspend fun addWord(
        word: String,
        emoji: String,
        category: String,
        difficulty: Int,
        imagePath: String? = null,
        audioPath: String? = null,
        wordLanguage: String = "ar",
        starsRequired: Int = 0
    ) {
        context.appDataStore.edit { prefs ->
            val existing = decode(prefs[customWordsKey] ?: "")
            val newItem = WordItem(
                id = "custom_${UUID.randomUUID()}",
                word = word.trim(),
                emoji = emoji.trim().ifBlank { "🔤" },
                category = category,
                difficulty = difficulty,
                imagePath = imagePath,
                parentRecordingPath = audioPath,
                wordLanguage = wordLanguage,
                starsRequired = starsRequired
            )
            prefs[customWordsKey] = encode(existing + newItem)
        }
    }

    suspend fun removeWord(id: String) {
        context.appDataStore.edit { prefs ->
            val existing = decode(prefs[customWordsKey] ?: "")
            val removed = existing.filter { it.id == id }
            removed.forEach {
                MediaStorage.deleteIfExists(it.imagePath)
                MediaStorage.deleteIfExists(it.parentRecordingPath)
            }
            prefs[customWordsKey] = encode(existing.filterNot { it.id == id })
        }
    }

    private fun encode(items: List<WordItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("word", item.word)
            obj.put("emoji", item.emoji)
            obj.put("category", item.category)
            obj.put("difficulty", item.difficulty)
            obj.put("imagePath", item.imagePath ?: JSONObject.NULL)
            obj.put("audioPath", item.parentRecordingPath ?: JSONObject.NULL)
            obj.put("wordLanguage", item.wordLanguage)
            obj.put("starsRequired", item.starsRequired)
            array.put(obj)
        }
        return array.toString()
    }

    // Same org.json optString() trap as WordOverridesRepository.stringOrNull
    // — a field stored as JSONObject.NULL decodes to the literal string
    // "null" via optString(), not real null, unless isNull() is checked first.
    private fun stringOrNull(obj: JSONObject, key: String): String? {
        if (!obj.has(key) || obj.isNull(key)) return null
        return obj.optString(key).ifBlank { null }
    }

    private fun decode(raw: String): List<WordItem> {
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val id = obj.optString("id")
                val word = obj.optString("word")
                if (id.isBlank() || word.isBlank()) return@mapNotNull null
                WordItem(
                    id = id,
                    word = word,
                    emoji = obj.optString("emoji").ifBlank { "🔤" },
                    category = obj.optString("category"),
                    difficulty = obj.optInt("difficulty", 1),
                    imagePath = stringOrNull(obj, "imagePath"),
                    parentRecordingPath = stringOrNull(obj, "audioPath"),
                    wordLanguage = obj.optString("wordLanguage").ifBlank { "ar" },
                    starsRequired = obj.optInt("starsRequired", 0)
                )
            }
        } catch (e: Exception) {
            // A corrupted/old-format value should never crash the app —
            // just show no custom words rather than looping forever.
            emptyList()
        }
    }
}
