package com.babakids.app.audio

import com.babakids.app.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * NOT USED by default. This build runs fully offline by explicit
 * requirement — no online voice API is called at runtime, and no API
 * key is baked into the app (see app/build.gradle.kts, which now leaves
 * ELEVENLABS_API_KEY/ELEVENLABS_VOICE_ID blank unless you supply them
 * yourself via a GitHub Actions secret or a local secrets.properties
 * file). SmartVoiceManager's default pipeline never references this
 * class — it's kept only as a ready-made extension point if online
 * generation is ever wanted again later. With a blank key/voice ID,
 * synthesize() returns null immediately and does nothing.
 */
class ElevenLabsService : AITextToSpeechService {

    override suspend fun synthesize(text: String, language: String, dialect: String): ByteArray? {
        val apiKey = BuildConfig.ELEVENLABS_API_KEY
        val voiceId = BuildConfig.ELEVENLABS_VOICE_ID
        if (apiKey.isBlank() || voiceId.isBlank()) {
            VoiceDiagnostics.record("❌ ElevenLabs: API key or voice ID is blank in BuildConfig")
            return null
        }

        val result = runCatching {
            val url = URL("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("xi-api-key", apiKey)
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "audio/mpeg")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 30000
            }

            val requestBody = JSONObject().apply {
                put("text", text)
                // Multilingual v2 — ElevenLabs' model with the strongest
                // Arabic support, matching the spec's "طبيعي ودافئ" request.
                put("model_id", "eleven_multilingual_v2")
                put(
                    "voice_settings",
                    JSONObject().apply {
                        put("stability", 0.5)
                        put("similarity_boost", 0.75)
                    }
                )
            }

            connection.outputStream.use { output ->
                output.write(requestBody.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val bytes = connection.inputStream.use { it.readBytes() }
                VoiceDiagnostics.record("✅ ElevenLabs succeeded — ${bytes.size} bytes for \"$text\"")
                bytes
            } else {
                val errorBody = runCatching {
                    connection.errorStream?.use { it.readBytes() }?.let { String(it, Charsets.UTF_8) }
                }.getOrNull() ?: "(no error body)"
                VoiceDiagnostics.record("❌ ElevenLabs HTTP $responseCode for \"$text\": $errorBody")
                null
            }
        }

        return result.getOrElse { exception ->
            VoiceDiagnostics.record("❌ ElevenLabs exception (${exception::class.simpleName}): ${exception.message}")
            null
        }
    }
}
