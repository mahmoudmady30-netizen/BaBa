package com.babakids.app.audio

/**
 * Metadata for one cached, AI-generated audio clip — mirrors the fields
 * requested in the spec (text, cache key, file path, language, dialect,
 * voice, created/last-used timestamps), stored in a small local JSON
 * index alongside the actual audio files.
 */
data class AudioCacheEntry(
    val cacheKey: String,
    val text: String,
    val filePath: String,
    val language: String,
    val dialect: String,
    val voice: String,
    val createdAt: Long,
    val lastUsedAt: Long,
    val fileSizeBytes: Long
)
