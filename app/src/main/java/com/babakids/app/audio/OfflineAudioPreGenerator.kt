package com.babakids.app.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.babakids.app.data.ParentSettingsManager
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Automatically pre-generates offline audio for every phrase in
 * PhraseLibrary, using Android's own on-device TTS engine's
 * synthesizeToFile() API — genuinely on-device, no network, no external
 * tool, no manually dropping files into assets/. Triggered from Parent
 * Mode ("توليد كل الأصوات تلقائيًا"), this fills the local runtime cache
 * (see AudioCacheManager) so every one of these phrases plays instantly
 * afterward and works fully offline from then on — a real, working
 * answer to "do all this automatically" within what's actually possible
 * in this environment.
 *
 * HONEST LIMIT, stated plainly: this uses the exact same voice as the
 * existing device-TTS fallback tier — it cannot and does not produce a
 * more natural, distinctly Egyptian-accented voice than what the phone's
 * TTS engine already provides (same caveat as SmartVoiceManager's tier 4
 * everywhere else in this app). What this DOES genuinely solve: instant,
 * offline playback for every one of these phrases from the first
 * generation run onward, with zero manual steps. A genuinely different-
 * *sounding* (more natural/human) voice still requires real pre-recorded
 * audio — the "bundled audio" tier (see BundledAudioManifest) — which
 * still needs external preparation (a voice actor, or any TTS tool run
 * outside this app) since there is no such synthesis capability
 * available to generate it automatically anywhere in this environment.
 */
class OfflineAudioPreGenerator(private val context: Context) {
    private val cacheManager = AudioCacheManager(context)
    private val voice = "device_tts_fallback"

    data class Progress(val current: Int, val total: Int, val text: String)
    data class Summary(val generated: Int, val skippedAlreadyCached: Int, val failed: Int, val total: Int)

    suspend fun generateAll(onProgress: (Progress) -> Unit = {}): Summary {
        val entries = PhraseLibrary.allEntries()
        var generated = 0
        var skipped = 0
        var failed = 0

        val tts = createReadyEngine()
        if (tts == null) {
            return Summary(0, 0, entries.size, entries.size)
        }

        entries.forEachIndexed { index, entry ->
            onProgress(Progress(index + 1, entries.size, entry.text))

            val normalized = TextNormalizer.normalize(entry.text)
            val language = if (entry.english) "en" else "ar"
            val cacheKey = AudioCacheManager.computeCacheKey(normalized, language, entry.dialect, voice)

            if (cacheManager.get(cacheKey) != null) {
                skipped++
                return@forEachIndexed
            }

            val success = synthesizeOne(tts, entry, normalized, language)
            if (success) generated++ else failed++
        }

        runCatching { tts.shutdown() }
        return Summary(generated, skipped, failed, entries.size)
    }

    private suspend fun synthesizeOne(
        tts: TextToSpeech,
        entry: PhraseLibrary.Entry,
        normalized: String,
        language: String
    ): Boolean {
        val tempFile = File(context.cacheDir, "pregen_${UUID.randomUUID()}.wav")
        val locale = when {
            entry.english -> Locale.US
            entry.dialect == ParentSettingsManager.DIALECT_FUSHA -> Locale("ar")
            else -> Locale("ar", "EG")
        }
        runCatching { tts.language = locale }

        val utteranceId = UUID.randomUUID().toString()
        val success = suspendCancellableCoroutine<Boolean> { continuation ->
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}

                override fun onDone(id: String?) {
                    if (id == utteranceId && continuation.isActive) continuation.resume(true)
                }

                @Deprecated("Deprecated in Java, still required to override")
                override fun onError(id: String?) {
                    if (id == utteranceId && continuation.isActive) continuation.resume(false)
                }

                override fun onError(id: String?, errorCode: Int) {
                    if (id == utteranceId && continuation.isActive) continuation.resume(false)
                }
            })

            val queued = runCatching {
                tts.synthesizeToFile(entry.text, Bundle(), tempFile, utteranceId)
            }.getOrDefault(TextToSpeech.ERROR)

            if (queued != TextToSpeech.SUCCESS && continuation.isActive) {
                continuation.resume(false)
            }
        }

        if (success && tempFile.exists() && tempFile.length() > 0L) {
            val bytes = runCatching { tempFile.readBytes() }.getOrNull()
            runCatching { tempFile.delete() }
            if (bytes != null) {
                cacheManager.save(normalized, language, entry.dialect, voice, bytes, fileExtension = "wav")
                return true
            }
            return false
        }
        runCatching { tempFile.delete() }
        return false
    }

    private suspend fun createReadyEngine(): TextToSpeech? = suspendCancellableCoroutine { continuation ->
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (continuation.isActive) {
                continuation.resume(if (status == TextToSpeech.SUCCESS) engine else null)
            }
        }
    }
}
