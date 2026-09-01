package com.babakids.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tracks every word the child has successfully pronounced (spec: a new
 * per-child section, split by language, showing the word + a picture +
 * when it was learned + the child's own recording). A word is recorded
 * once — a later correct attempt of the same word (same id + language)
 * only refreshes lastPracticedAt, never adds a duplicate entry.
 */
class LearnedWordsRepository(private val context: Context) {

    private val learnedWordsKey = stringPreferencesKey("learned_words_v1")

    val learnedWordsFlow: Flow<List<LearnedWord>> =
        context.appDataStore.data.map { prefs -> decode(prefs[learnedWordsKey] ?: "") }

    /**
     * Records a correct pronunciation. If this word (by id + language) was
     * already learned, only its lastPracticedAt (and recording, if a new
     * one was captured this time) are updated — never a duplicate entry.
     */
    suspend fun recordCorrectPronunciation(
        wordId: String,
        word: String,
        emoji: String,
        imagePath: String?,
        language: String,
        recordingPath: String?
    ) {
        context.appDataStore.edit { prefs ->
            val existing = decode(prefs[learnedWordsKey] ?: "")
            val now = System.currentTimeMillis()
            val match = existing.firstOrNull { it.wordId == wordId && it.language == language }
            val updated = if (match != null) {
                // Same word practiced again — refresh the timestamp; drop
                // the old recording only if a fresh one was captured now.
                if (recordingPath != null && recordingPath != match.recordingPath) {
                    MediaStorage.deleteIfExists(match.recordingPath)
                }
                existing.map {
                    if (it.wordId == wordId && it.language == language) {
                        it.copy(lastPracticedAt = now, recordingPath = recordingPath ?: it.recordingPath)
                    } else {
                        it
                    }
                }
            } else {
                existing + LearnedWord(
                    wordId = wordId,
                    word = word,
                    emoji = emoji,
                    imagePath = imagePath,
                    language = language,
                    firstLearnedAt = now,
                    lastPracticedAt = now,
                    recordingPath = recordingPath
                )
            }
            prefs[learnedWordsKey] = encode(updated)
        }
    }

    /** Parent Mode: remove a word from the Learned Words list entirely (also cleans up its saved recording, if any). */
    suspend fun deleteWord(wordId: String, language: String) {
        context.appDataStore.edit { prefs ->
            val existing = decode(prefs[learnedWordsKey] ?: "")
            val match = existing.firstOrNull { it.wordId == wordId && it.language == language }
            match?.let { MediaStorage.deleteIfExists(it.recordingPath) }
            prefs[learnedWordsKey] = encode(existing.filterNot { it.wordId == wordId && it.language == language })
        }
    }

    /** Parent Mode: set (or clear, with null) a celebratory emoji reaction on a learned word. */
    suspend fun setReactionEmoji(wordId: String, language: String, emoji: String?) {
        context.appDataStore.edit { prefs ->
            val existing = decode(prefs[learnedWordsKey] ?: "")
            val updated = existing.map {
                if (it.wordId == wordId && it.language == language) it.copy(reactionEmoji = emoji) else it
            }
            prefs[learnedWordsKey] = encode(updated)
        }
    }

    private fun encode(items: List<LearnedWord>): String {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("wordId", item.wordId)
            obj.put("word", item.word)
            obj.put("emoji", item.emoji)
            obj.put("imagePath", item.imagePath ?: JSONObject.NULL)
            obj.put("language", item.language)
            obj.put("firstLearnedAt", item.firstLearnedAt)
            obj.put("lastPracticedAt", item.lastPracticedAt)
            obj.put("recordingPath", item.recordingPath ?: JSONObject.NULL)
            obj.put("reactionEmoji", item.reactionEmoji ?: JSONObject.NULL)
            array.put(obj)
        }
        return array.toString()
    }

    // See WordOverridesRepository.stringOrNull for why this guard (isNull
    // check before optString) matters: org.json's optString() returns the
    // literal string "null" — not real null — for a field stored as
    // JSONObject.NULL, which is exactly what silently corrupted words with
    // no recording (recordingPath) into displaying/speaking the text "null".
    private fun stringOrNull(obj: JSONObject, key: String): String? {
        if (!obj.has(key) || obj.isNull(key)) return null
        return obj.optString(key).ifBlank { null }
    }

    private fun decode(raw: String): List<LearnedWord> {
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val wordId = obj.optString("wordId")
                val word = obj.optString("word")
                if (wordId.isBlank() || word.isBlank()) return@mapNotNull null
                LearnedWord(
                    wordId = wordId,
                    word = word,
                    emoji = obj.optString("emoji").ifBlank { "🔤" },
                    imagePath = stringOrNull(obj, "imagePath"),
                    language = obj.optString("language").ifBlank { "ar" },
                    firstLearnedAt = obj.optLong("firstLearnedAt"),
                    lastPracticedAt = obj.optLong("lastPracticedAt"),
                    recordingPath = stringOrNull(obj, "recordingPath"),
                    reactionEmoji = stringOrNull(obj, "reactionEmoji")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
