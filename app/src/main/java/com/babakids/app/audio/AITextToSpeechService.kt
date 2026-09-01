package com.babakids.app.audio

/**
 * The online "AI voice generation" tier described in the hybrid voice
 * spec — sends text to a cloud text-to-speech provider and returns the
 * generated audio bytes (MP3/WAV/OGG), or null on any failure.
 */
interface AITextToSpeechService {
    suspend fun synthesize(text: String, language: String, dialect: String): ByteArray?
}

/**
 * HONEST STATUS: this is NOT connected to a real provider. There is no
 * API key available in this project, and one can't be fabricated or
 * guessed — a working key has to come from an actual account with a
 * text-to-speech provider that supports Egyptian Arabic
 * (e.g. ElevenLabs, Google Cloud Text-to-Speech, Azure AI Speech,
 * Murf.ai...). Until one is wired in, this always returns null, and
 * SmartVoiceManager gracefully falls through to the offline fallback
 * tier (the app's existing Android TTS) — nothing breaks, nothing shows
 * an error, the child never sees this gap.
 *
 * To connect a real provider once you have an API key:
 * 1. Replace the body of synthesize() below with an HTTP call to that
 *    provider's TTS endpoint (see the commented example shape).
 * 2. Store the key somewhere it isn't checked into source control
 *    (e.g. a local.properties value read into BuildConfig, or better,
 *    a small backend/proxy you control — never a hardcoded string here,
 *    per the spec's own security requirement about not exposing keys).
 * 3. That's it — no other code in the app needs to change. SmartVoice-
 *    Manager, the cache, the fallback, and every screen that calls
 *    playSmartVoice() will automatically start using real AI audio.
 */
class UnconfiguredAITextToSpeechService : AITextToSpeechService {
    override suspend fun synthesize(text: String, language: String, dialect: String): ByteArray? {
        // Example shape for a real provider call (illustrative only —
        // intentionally not wired up, since there's no key to use):
        //
        // val connection = URL("https://api.<provider>.com/v1/text-to-speech")
        //     .openConnection() as HttpURLConnection
        // connection.requestMethod = "POST"
        // connection.setRequestProperty("Authorization", "Bearer $API_KEY")
        // connection.setRequestProperty("Content-Type", "application/json")
        // connection.doOutput = true
        // val body = """{"text":"$text","voice":"child_friendly_ar_eg"}"""
        // connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        // return if (connection.responseCode == 200) connection.inputStream.readBytes() else null
        return null
    }
}
