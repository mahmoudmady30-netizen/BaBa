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
 * Real-world rewards a parent sets up ("ice cream at 20 stars!", "trip to
 * the park at 50!") — this is what the child now earns instead of a
 * generic sticker, with its own celebration popup the moment they hit the
 * star count. JSON storage (see CustomWordsRepository for why this is
 * used instead of a hand-rolled delimiter format).
 */
class CustomRewardsRepository(private val context: Context) {

    private val rewardsKey = stringPreferencesKey("custom_rewards_v1")

    val rewardsFlow: Flow<List<CustomReward>> =
        context.appDataStore.data.map { prefs -> decode(prefs[rewardsKey] ?: "") }

    suspend fun addReward(title: String, starsRequired: Int) {
        context.appDataStore.edit { prefs ->
            val existing = decode(prefs[rewardsKey] ?: "")
            val newReward = CustomReward(
                id = "reward_${UUID.randomUUID()}",
                title = title.trim(),
                starsRequired = starsRequired
            )
            prefs[rewardsKey] = encode((existing + newReward).sortedBy { it.starsRequired })
        }
    }

    suspend fun removeReward(id: String) {
        context.appDataStore.edit { prefs ->
            val existing = decode(prefs[rewardsKey] ?: "")
            prefs[rewardsKey] = encode(existing.filterNot { it.id == id })
        }
    }

    /** Records the moment this reward was actually earned — called once, when its popup fires. */
    suspend fun markEarned(id: String, timestamp: Long = System.currentTimeMillis()) {
        context.appDataStore.edit { prefs ->
            val existing = decode(prefs[rewardsKey] ?: "")
            val updated = existing.map { reward ->
                if (reward.id == id && reward.earnedAt == null) reward.copy(earnedAt = timestamp) else reward
            }
            prefs[rewardsKey] = encode(updated)
        }
    }

    private fun encode(items: List<CustomReward>): String {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("starsRequired", item.starsRequired)
            obj.put("earnedAt", item.earnedAt ?: JSONObject.NULL)
            array.put(obj)
        }
        return array.toString()
    }

    private fun decode(raw: String): List<CustomReward> {
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val id = obj.optString("id")
                val title = obj.optString("title")
                if (id.isBlank() || title.isBlank()) return@mapNotNull null
                val earnedAt = if (obj.has("earnedAt") && !obj.isNull("earnedAt")) obj.optLong("earnedAt") else null
                CustomReward(
                    id = id,
                    title = title,
                    starsRequired = obj.optInt("starsRequired", 10),
                    earnedAt = earnedAt
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
