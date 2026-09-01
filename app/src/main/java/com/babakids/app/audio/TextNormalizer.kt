package com.babakids.app.audio

import java.text.Normalizer

/**
 * Cleans text before it's used as a cache/manifest lookup key, so trivial
 * differences ("قربت خالص!" vs "قربت خالص !" vs "قربت خالص !!") don't
 * cause cache misses or duplicate bundled-audio entries. Never changes
 * the *meaning* of the text — only whitespace, punctuation variants, and
 * Unicode form.
 */
object TextNormalizer {
    fun normalize(text: String): String {
        var result = text.trim()

        // Unicode NFC normalization — combines composed/decomposed forms
        // of the same character so visually-identical text always
        // produces the same key.
        result = Normalizer.normalize(result, Normalizer.Form.NFC)

        // Collapse repeated whitespace (including Arabic tatweel-adjacent
        // spacing quirks) down to single spaces.
        result = result.replace(Regex("\\s+"), " ")

        // Normalize question/exclamation mark variants (Arabic ؟ vs
        // Western ?, repeated !! vs single !) without touching the words.
        result = result.replace("؟", "?").replace(Regex("\\?+"), "?")
        result = result.replace(Regex("!+"), "!")
        result = result.replace("،", ",")

        // Strip Arabic diacritics (tashkeel) for lookup purposes only —
        // EgyptianSpokenForms still supplies the fully-diacritized version
        // for actual speech; this normalization is just for matching.
        result = result.replace(Regex("[\u064B-\u065F\u0670]"), "")

        return result.trim()
    }
}
