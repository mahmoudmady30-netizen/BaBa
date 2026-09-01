package com.babakids.app.data

/**
 * Spec: child's name used dynamically in every encouragement message, with
 * gender-appropriate Arabic grammar ("ممتاز" for a boy vs "ممتازة" for a
 * girl), an Egyptian-colloquial vs Modern Standard Arabic (Fusha) choice,
 * and an English set for when the app language is English.
 *
 * `dialect` only matters when `english` is false — see
 * ParentSettingsManager.DIALECT_EGYPTIAN / DIALECT_FUSHA.
 *
 * The Egyptian banks below use real everyday colloquial words a kid would
 * actually hear at home ("جامد", "معلم", "تحفة", "خلاص"...), not just
 * Fusha with the locale tag changed — vocabulary is the one part of
 * "sounding Egyptian" fully within this app's control. The TTS *voice's*
 * accent depends on what voice packs the phone's TTS engine has installed,
 * which the app can select from (see SpeechHelper) but can't fabricate if
 * the device has none.
 */
object Phrases {

    private const val EGYPTIAN = ParentSettingsManager.DIALECT_EGYPTIAN
    private const val FEMALE = ParentSettingsManager.GENDER_FEMALE

    fun fallbackAddress(gender: String, english: Boolean): String = when {
        english -> "Champ"
        gender == FEMALE -> "بطلة"
        else -> "بطل"
    }

    fun displayName(childName: String, gender: String, english: Boolean): String =
        childName.ifBlank { fallbackAddress(gender, english) }

    fun successPhrases(gender: String, english: Boolean, dialect: String = EGYPTIAN): List<String> = when {
        english -> listOf("🌟 Great job, %s!", "👏 Well done, %s!", "⭐ Amazing, %s!", "🎉 You did it, %s!")
        dialect != EGYPTIAN -> // Fusha / Modern Standard Arabic
            if (gender == FEMALE)
                listOf("🌟 ممتازة يا %s!", "👏 أحسنتِ يا %s!", "⭐ رائعة يا %s!", "😊 عمل جميل يا %s!")
            else
                listOf("🌟 ممتاز يا %s!", "👏 أحسنت يا %s!", "⭐ رائع يا %s!", "😊 عمل جميل يا %s!")
        gender == FEMALE ->
            listOf(
                "🌟 شاطرة يا %s!", "👏 برافو يا %s!", "🔥 جامدة يا %s!",
                "⭐ تحفة يا %s!", "😍 الله عليكي يا %s!", "👍 معلمة يا %s!"
            )
        else ->
            listOf(
                "🌟 شاطر يا %s!", "👏 برافو يا %s!", "🔥 جامد يا %s!",
                "⭐ تحفة يا %s!", "😍 الله عليك يا %s!", "👍 معلم يا %s!"
            )
    }

    fun tryAgainPhrases(gender: String, english: Boolean, dialect: String = EGYPTIAN): List<String> = when {
        english -> listOf("😊 Try again, %s", "❤️ So close, %s!", "Let's try again, %s ❤️", "You're getting there, %s!")
        dialect != EGYPTIAN ->
            if (gender == FEMALE)
                listOf("😊 حاولي مرة أخرى يا %s", "❤️ اقتربتِ كثيرًا يا %s!", "لنحاول مرة أخرى يا %s ❤️")
            else
                listOf("😊 حاول مرة أخرى يا %s", "❤️ اقتربت كثيرًا يا %s!", "لنحاول مرة أخرى يا %s ❤️")
        gender == FEMALE ->
            listOf(
                "😊 حاولي تاني يا %s", "❤️ قريبة أوي يا %s!", "يلا كمان مرة يا %s ❤️",
                "معلش يا %s، جربي تاني", "كمّلي يا %s، هتعرفيها!"
            )
        else ->
            listOf(
                "😊 حاول تاني يا %s", "❤️ قريب أوي يا %s!", "يلا كمان مرة يا %s ❤️",
                "معلش يا %s، جرب تاني", "كمّل يا %s، هتعرفها!"
            )
    }

    fun findPrompts(english: Boolean, dialect: String = EGYPTIAN): List<String> = when {
        english -> listOf("Find the %s", "Where is the %s?", "Tap the %s", "Show me the %s")
        dialect != EGYPTIAN -> listOf("أحضر ال%s", "أين ال%s؟", "المس ال%s")
        else -> listOf("هات ال%s", "فين ال%s؟", "دوس على ال%s", "وريني ال%s")
    }

    fun celebrationTitle(displayName: String, english: Boolean, dialect: String = EGYPTIAN): String = when {
        english -> "Bravo $displayName! 🎉"
        dialect != EGYPTIAN -> "أحسنت يا $displayName! 🎉"
        else -> "ألف مبروك يا $displayName! 🎉"
    }

    fun celebrationSubtitle(milestone: Int, english: Boolean): String =
        if (english) "You collected $milestone stars! ⭐" else "جمعت $milestone نجمة! ⭐"

    /** "I want <word>" / "I am <feeling>" sentence templates for the sentence-builder screen. */
    fun wantSentence(word: String, isFeeling: Boolean, english: Boolean, dialect: String = EGYPTIAN): String = when {
        english -> if (isFeeling) "I am $word" else "I want $word"
        dialect != EGYPTIAN -> if (isFeeling) "أنا $word" else "أنا أريد $word"
        else -> if (isFeeling) "أنا $word" else "أنا عايز $word"
    }
}
