package com.babakids.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * A parent-made replacement for one word's picture, spoken audio, and/or
 * displayed name, keyed by WordItem.id. textAr/textEn are separate because
 * a word can be renamed independently in Arabic mode vs English mode.
 */
data class WordOverride(
    val wordId: String,
    val imagePath: String? = null,
    val audioPath: String? = null,
    val textAr: String? = null,
    val textEn: String? = null
)

/**
 * Backs the "✏️ Word Edit Mode" parent setting: lets a parent replace the
 * picture, spoken pronunciation, and/or displayed name for *any* word —
 * built-in words included, not just the ones added from scratch in Parent
 * Mode.
 *
 * Deliberately separate from CustomWordsRepository (which creates whole new
 * WordItems) — this only stores a small patch keyed by an existing word's
 * id. withOverrides() below merges these on top of the real WordItem
 * wherever words are loaded, so every screen (category grid, word detail,
 * mini-game) picks the replacement up automatically through the existing
 * WordItem fields, with no special-casing needed at render time.
 */
class WordOverridesRepository(private val context: Context) {

    private val overridesKey = stringPreferencesKey("word_overrides_v1")

    val overridesFlow: Flow<Map<String, WordOverride>> =
        context.appDataStore.data.map { prefs -> decode(prefs[overridesKey] ?: "") }

    suspend fun setImage(wordId: String, imagePath: String?) {
        update(wordId) { it.copy(imagePath = imagePath) }
    }

    suspend fun setAudio(wordId: String, audioPath: String?) {
        update(wordId) { it.copy(audioPath = audioPath) }
    }

    /** Renames the word in whichever language is currently active (english=true edits the English mirror). */
    suspend fun setText(wordId: String, english: Boolean, text: String?) {
        update(wordId) { current ->
            if (english) current.copy(textEn = text) else current.copy(textAr = text)
        }
    }

    suspend fun clear(wordId: String) {
        context.appDataStore.edit { prefs ->
            val existing = decode(prefs[overridesKey] ?: "").toMutableMap()
            existing.remove(wordId)?.let {
                MediaStorage.deleteIfExists(it.imagePath)
                MediaStorage.deleteIfExists(it.audioPath)
            }
            prefs[overridesKey] = encode(existing.values.toList())
        }
    }

    private suspend fun update(wordId: String, transform: (WordOverride) -> WordOverride) {
        context.appDataStore.edit { prefs ->
            val existing = decode(prefs[overridesKey] ?: "").toMutableMap()
            val current = existing[wordId] ?: WordOverride(wordId)
            existing[wordId] = transform(current)
            prefs[overridesKey] = encode(existing.values.toList())
        }
    }

    private fun encode(items: List<WordOverride>): String {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("wordId", item.wordId)
            obj.put("imagePath", item.imagePath ?: JSONObject.NULL)
            obj.put("audioPath", item.audioPath ?: JSONObject.NULL)
            obj.put("textAr", item.textAr ?: JSONObject.NULL)
            obj.put("textEn", item.textEn ?: JSONObject.NULL)
            array.put(obj)
        }
        return array.toString()
    }

    /**
     * org.json's JSONObject.optString(key) has a well-known trap: if the
     * stored value is JSONObject.NULL (which is what we write for an unset
     * field — see encode() above), optString returns the literal four-letter
     * string "null" instead of an actual null. That's exactly what caused
     * words to visibly show/speak the text "null" after only their picture
     * was edited and their name/audio were left untouched: those untouched
     * fields decoded as the STRING "null", which is not equal to Kotlin
     * null, so every `?:` fallback elsewhere in this file silently failed
     * to kick in. Checking isNull() first is the actual fix.
     */
    private fun stringOrNull(obj: JSONObject, key: String): String? {
        if (!obj.has(key) || obj.isNull(key)) return null
        return obj.optString(key).ifBlank { null }
    }

    private fun decode(raw: String): Map<String, WordOverride> {
        if (raw.isBlank()) return emptyMap()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val wordId = obj.optString("wordId")
                if (wordId.isBlank()) return@mapNotNull null
                wordId to WordOverride(
                    wordId = wordId,
                    imagePath = stringOrNull(obj, "imagePath"),
                    audioPath = stringOrNull(obj, "audioPath"),
                    textAr = stringOrNull(obj, "textAr"),
                    textEn = stringOrNull(obj, "textEn")
                )
            }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

/** Applies parent-made overrides (picture/pronunciation/name) on top of the real word list. Safe to call with an empty map. */
fun List<WordItem>.withOverrides(overrides: Map<String, WordOverride>): List<WordItem> {
    if (overrides.isEmpty()) return this
    return map { word ->
        val override = overrides[word.id] ?: return@map word
        word.copy(
            imagePath = override.imagePath ?: word.imagePath,
            parentRecordingPath = override.audioPath ?: word.parentRecordingPath,
            word = override.textAr ?: word.word,
            wordEn = override.textEn ?: word.wordEn,
            // A renamed word should always be read the way it's now
            // spelled, not by an old hand-tuned pronunciation meant for
            // the original wording (see WordItem.spokenWord()).
            bypassDialectSpokenForm = (override.textAr != null || override.textEn != null) || word.bypassDialectSpokenForm
        )
    }
}
