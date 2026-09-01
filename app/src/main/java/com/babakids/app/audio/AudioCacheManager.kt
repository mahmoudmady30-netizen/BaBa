package com.babakids.app.audio

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Local cache for AI-generated audio clips. Cache key = SHA-256 of
 * (text + language + dialect + voice), so the same sentence never
 * generates twice. Metadata (text, key, path, language, dialect, voice,
 * created/last-used timestamps) lives in a small JSON index file next to
 * the audio files themselves — a lightweight local "database", using the
 * same org.json approach already proven elsewhere in this project rather
 * than pulling in a new database dependency for what's a fairly small
 * amount of structured data.
 */
class AudioCacheManager(private val context: Context) {

    companion object {
        private const val MAX_CACHE_BYTES = 20L * 1024 * 1024 // 20MB cap, then LRU eviction kicks in

        /** SHA-256(text + language + dialect + voice) — a stable, collision-free cache key. */
        fun computeCacheKey(text: String, language: String, dialect: String, voice: String): String {
            val raw = "$text|$language|$dialect|$voice"
            val digestBytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
            return digestBytes.joinToString("") { byte -> "%02x".format(byte) }
        }
    }

    private val cacheDir: File by lazy {
        File(context.filesDir, "audio_cache").apply { mkdirs() }
    }
    private val indexFile: File by lazy { File(cacheDir, "index.json") }

    private fun readIndex(): MutableList<AudioCacheEntry> {
        if (!indexFile.exists()) return mutableListOf()
        return try {
            val array = JSONArray(indexFile.readText())
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                AudioCacheEntry(
                    cacheKey = obj.optString("cacheKey"),
                    text = obj.optString("text"),
                    filePath = obj.optString("filePath"),
                    language = obj.optString("language"),
                    dialect = obj.optString("dialect"),
                    voice = obj.optString("voice"),
                    createdAt = obj.optLong("createdAt"),
                    lastUsedAt = obj.optLong("lastUsedAt"),
                    fileSizeBytes = obj.optLong("fileSizeBytes")
                )
            }.toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun writeIndex(entries: List<AudioCacheEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject()
            obj.put("cacheKey", entry.cacheKey)
            obj.put("text", entry.text)
            obj.put("filePath", entry.filePath)
            obj.put("language", entry.language)
            obj.put("dialect", entry.dialect)
            obj.put("voice", entry.voice)
            obj.put("createdAt", entry.createdAt)
            obj.put("lastUsedAt", entry.lastUsedAt)
            obj.put("fileSizeBytes", entry.fileSizeBytes)
            array.put(obj)
        }
        runCatching { indexFile.writeText(array.toString()) }
    }

    /**
     * Looks up a cached clip by key. Also self-heals: if the index
     * references a file that's gone missing (e.g. deleted by the OS
     * under storage pressure), the stale entry is quietly dropped instead
     * of ever surfacing an error to the caller.
     */
    fun get(cacheKey: String): AudioCacheEntry? {
        val entries = readIndex()
        val match = entries.firstOrNull { it.cacheKey == cacheKey } ?: return null
        if (!File(match.filePath).exists()) {
            writeIndex(entries.filterNot { it.cacheKey == cacheKey })
            return null
        }
        val touched = match.copy(lastUsedAt = System.currentTimeMillis())
        writeIndex(entries.map { if (it.cacheKey == cacheKey) touched else it })
        return touched
    }

    /** Saves newly-generated audio bytes and records its metadata; evicts old entries if now over budget. */
    fun save(
        text: String,
        language: String,
        dialect: String,
        voice: String,
        audioBytes: ByteArray,
        fileExtension: String = "mp3"
    ): AudioCacheEntry {
        val cacheKey = computeCacheKey(text, language, dialect, voice)
        val file = File(cacheDir, "$cacheKey.$fileExtension")
        runCatching { file.writeBytes(audioBytes) }
        val now = System.currentTimeMillis()
        val entry = AudioCacheEntry(
            cacheKey = cacheKey,
            text = text,
            filePath = file.absolutePath,
            language = language,
            dialect = dialect,
            voice = voice,
            createdAt = now,
            lastUsedAt = now,
            fileSizeBytes = audioBytes.size.toLong()
        )
        val entries = readIndex().filterNot { it.cacheKey == cacheKey } + entry
        writeIndex(entries)
        evictIfNeeded()
        return entry
    }

    /** LRU eviction — deletes the least-recently-used clips first once the cache exceeds its size budget. */
    fun evictIfNeeded() {
        val entries = readIndex()
        var totalSize = entries.sumOf { it.fileSizeBytes }
        if (totalSize <= MAX_CACHE_BYTES) return

        val sortedByOldest = entries.sortedBy { it.lastUsedAt }
        val kept = entries.toMutableList()
        for (entry in sortedByOldest) {
            if (totalSize <= MAX_CACHE_BYTES) break
            runCatching { File(entry.filePath).delete() }
            kept.remove(entry)
            totalSize -= entry.fileSizeBytes
        }
        writeIndex(kept)
    }

    /** Deletes every cached clip — backs Parent Mode's "clear cached voices" option. */
    fun clearAll() {
        readIndex().forEach { runCatching { File(it.filePath).delete() } }
        writeIndex(emptyList())
    }

    fun cacheSizeBytes(): Long = readIndex().sumOf { it.fileSizeBytes }
}
