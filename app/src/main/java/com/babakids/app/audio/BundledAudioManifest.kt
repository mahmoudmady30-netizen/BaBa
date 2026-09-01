package com.babakids.app.audio

import android.content.Context
import org.json.JSONObject

/**
 * Loads the developer-bundled pre-generated audio manifest from
 * assets/audio/audio_manifest.json — a text→file mapping for real,
 * pre-recorded audio clips shipped with the app itself (no generation,
 * no network, works from the very first launch, and is the *only* tier
 * that can honestly claim a specific accent, since it's whatever voice
 * actually recorded the clips).
 *
 * HONEST STATUS: no audio files are bundled yet in this build — an empty
 * manifest ships by default, so this tier is a safe no-op until real
 * clips are added. Supplying them (recording or generating real Egyptian
 * Arabic .wav files matching this manifest's filenames) is a step outside
 * what's possible to do inside this environment — there's no audio
 * synthesis or voice-recording capability available here. See
 * BaBaKids-all-phrases.txt (the full phrase list) and
 * tools/generate_audio_manifest.py (which turns that list into the exact
 * manifest + filenames this class expects) for how to prepare them.
 */
class BundledAudioManifest(private val context: Context) {
    // normalized text -> "words/xxx.wav" (relative to assets/audio/)
    private val entries: Map<String, String> by lazy { loadManifest() }

    private fun loadManifest(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val raw = runCatching {
            context.assets.open("audio/audio_manifest.json").use { it.readBytes() }
        }.getOrNull() ?: return emptyMap()

        return try {
            val root = JSONObject(String(raw, Charsets.UTF_8))
            listOf("words", "phrases", "names").forEach { section ->
                val obj = root.optJSONObject(section) ?: return@forEach
                obj.keys().forEach { key ->
                    val relativePath = obj.optString(key)
                    if (relativePath.isNotBlank()) {
                        result[TextNormalizer.normalize(key)] = relativePath
                    }
                }
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /** Returns the asset-relative path (e.g. "words/word_001.wav") for this text, or null if not bundled. */
    fun lookup(text: String): String? = entries[TextNormalizer.normalize(text)]

    /** Full assets/ path for use with AssetManager/AudioManager.playAsset(). */
    fun assetPathFor(relativePath: String): String = "audio/$relativePath"

    fun entryCount(): Int = entries.size
}
