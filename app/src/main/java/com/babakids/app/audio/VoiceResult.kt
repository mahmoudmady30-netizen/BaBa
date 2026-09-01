package com.babakids.app.audio

/** Outcome of a single playSmartVoice() request — surfaced to the debug screen and logs. */
enum class VoiceResult {
    /** Played from the developer-bundled pre-generated audio library. */
    BUNDLED_HIT,
    /** Played from the local runtime cache (a previous ERROR/GENERATED result saved a file). */
    CACHE_HIT,
    /** No bundled or cached audio existed; a new clip was generated at runtime. */
    GENERATED,
    /** Nothing available — used the fallback (device TTS, not confirmed Egyptian-accented). */
    NOT_AVAILABLE,
    /** Something failed unexpectedly (still falls back safely; this is for diagnostics only). */
    ERROR
}
