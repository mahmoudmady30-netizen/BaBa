package com.babakids.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** A parent-made replacement for one category's picture and/or displayed name, keyed by Category.id. */
data class CategoryOverride(
    val categoryId: String,
    val imagePath: String? = null,
    val titleAr: String? = null,
    val titleEn: String? = null
)

/**
 * Backs the same "✏️ Word Edit Mode" setting, extended to sections
 * themselves: lets a parent rename a category and/or replace its icon
 * with a real photo. Mirrors WordOverridesRepository exactly, including
 * the isNull()-before-optString() guard — see stringOrNull below and
 * WordOverridesRepository's own note — an unset field decodes to the
 * literal text "null" via org.json's optString() otherwise, which is
 * exactly the bug that once made an edited word display/speak "null".
 */
class CategoryOverridesRepository(private val context: Context) {

    private val overridesKey = stringPreferencesKey("category_overrides_v1")

    val overridesFlow: Flow<Map<String, CategoryOverride>> =
        context.appDataStore.data.map { prefs -> decode(prefs[overridesKey] ?: "") }

    suspend fun setImage(categoryId: String, imagePath: String?) {
        update(categoryId) { it.copy(imagePath = imagePath) }
    }

    /** Renames the category in whichever language is currently active (english=true edits the English title). */
    suspend fun setText(categoryId: String, english: Boolean, text: String?) {
        update(categoryId) { current ->
            if (english) current.copy(titleEn = text) else current.copy(titleAr = text)
        }
    }

    suspend fun clear(categoryId: String) {
        context.appDataStore.edit { prefs ->
            val existing = decode(prefs[overridesKey] ?: "").toMutableMap()
            existing.remove(categoryId)?.let { MediaStorage.deleteIfExists(it.imagePath) }
            prefs[overridesKey] = encode(existing.values.toList())
        }
    }

    private suspend fun update(categoryId: String, transform: (CategoryOverride) -> CategoryOverride) {
        context.appDataStore.edit { prefs ->
            val existing = decode(prefs[overridesKey] ?: "").toMutableMap()
            val current = existing[categoryId] ?: CategoryOverride(categoryId)
            existing[categoryId] = transform(current)
            prefs[overridesKey] = encode(existing.values.toList())
        }
    }

    private fun encode(items: List<CategoryOverride>): String {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("categoryId", item.categoryId)
            obj.put("imagePath", item.imagePath ?: JSONObject.NULL)
            obj.put("titleAr", item.titleAr ?: JSONObject.NULL)
            obj.put("titleEn", item.titleEn ?: JSONObject.NULL)
            array.put(obj)
        }
        return array.toString()
    }

    private fun stringOrNull(obj: JSONObject, key: String): String? {
        if (!obj.has(key) || obj.isNull(key)) return null
        return obj.optString(key).ifBlank { null }
    }

    private fun decode(raw: String): Map<String, CategoryOverride> {
        if (raw.isBlank()) return emptyMap()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val categoryId = obj.optString("categoryId")
                if (categoryId.isBlank()) return@mapNotNull null
                categoryId to CategoryOverride(
                    categoryId = categoryId,
                    imagePath = stringOrNull(obj, "imagePath"),
                    titleAr = stringOrNull(obj, "titleAr"),
                    titleEn = stringOrNull(obj, "titleEn")
                )
            }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

/** Applies parent-made overrides (picture/name) on top of the real category list. Safe to call with an empty map. */
fun List<Category>.withCategoryOverrides(overrides: Map<String, CategoryOverride>): List<Category> {
    if (overrides.isEmpty()) return this
    return map { category ->
        val override = overrides[category.id] ?: return@map category
        category.copy(
            imagePath = override.imagePath ?: category.imagePath,
            title = override.titleAr ?: category.title,
            titleEn = override.titleEn ?: category.titleEn
        )
    }
}
