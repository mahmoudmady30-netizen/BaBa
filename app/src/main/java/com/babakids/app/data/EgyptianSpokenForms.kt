package com.babakids.app.data

/**
 * Fully-diacritized (tashkeel) spoken forms of the built-in words, used
 * only when *speaking* via TTS in Egyptian-dialect mode — on-screen text
 * keeps using the plain WordItem.word as before, unchanged.
 *
 * Honest note on what this technique can and can't do: Arabic TTS engines
 * default to Modern Standard Arabic vowelling rules when text has no
 * diacritics, which is a real, common cause of words sounding stiff/formal
 * even when the word choice itself is already colloquial Egyptian
 * ("عربية" not "سيارة", "عايز" not "أريد", etc. — that part was already
 * handled in Phrases.kt). Adding full tashkeel removes that guesswork and
 * steers the engine toward the intended vowels — this is a standard,
 * legitimate technique. It genuinely cannot fabricate an Egyptian *accent*
 * that the installed TTS voice doesn't have; that depends entirely on the
 * voice data the device's TTS engine ships (SpeechHelper separately
 * searches for an actual Egyptian voice when one exists on the device).
 * These were hand-diacritized based on standard Egyptian colloquial
 * pronunciation patterns, not verified by ear against real TTS output —
 * results may still vary by device and engine.
 */
object EgyptianSpokenForms {
    private val forms: Map<String, String> = mapOf(
        // food
        "food_apple" to "تُفّاحَة",
        "food_banana" to "مَوْزَة",
        "food_milk" to "لَبَن",
        "food_pizza" to "بيتْزا",
        "food_water" to "مَيَّة",

        // animals
        "animal_dog" to "كَلْب",
        "animal_cat" to "قُطَّة",
        "animal_bird" to "عُصْفور",
        "animal_fish" to "سَمَكَة",

        // clothes
        "clothes_shirt" to "قَميص",
        "clothes_shoes" to "حِذاء",
        "clothes_hat" to "قُبَّعَة",

        // home
        "home_bed" to "سَرير",
        "home_door" to "باب",
        "home_chair" to "كُرْسي",

        // transport
        "transport_car" to "عَرَبِيَّة",
        "transport_bus" to "أوتوبيس",
        "transport_bike" to "عَجَلَة",

        // family
        "family_mom" to "مامَا",
        "family_dad" to "بابَا",
        "family_sister" to "أُخْتي",
        "family_brother" to "أَخويا",

        // feelings
        "feel_happy" to "سَعيد",
        "feel_sad" to "زَعْلان",
        "feel_angry" to "غَضْبان",
        "feel_sleepy" to "نَعْسان",
        "feel_scared" to "خايِف",
        "feel_love" to "بَحِبّ",
        "feel_tired" to "تَعْبان",

        // colors
        "color_red" to "أَحْمَر",
        "color_orange" to "بُرْتُقالي",
        "color_yellow" to "أَصْفَر",
        "color_green" to "أَخْضَر",
        "color_blue" to "أَزْرَق",
        "color_purple" to "بَنَفْسَجي",
        "color_brown" to "بُنّي",
        "color_black" to "أَسْوَد",
        "color_white" to "أَبْيَض",

        // numbers
        "num_1" to "واحِد",
        "num_2" to "اِتْنين",
        "num_3" to "تَلاتَة",
        "num_4" to "أَرْبَعَة",
        "num_5" to "خَمْسَة",
        "num_6" to "سِتَّة",
        "num_7" to "سَبْعَة",
        "num_8" to "تَمانْيَة",
        "num_9" to "تِسْعَة",
        "num_10" to "عَشَرَة",

        // toys
        "toy_ball" to "كورَة",
        "toy_doll" to "عَروسَة",
        "toy_car" to "عَرَبِيَّة لَعِب",

        // nature
        "nature_sun" to "شَمْس",
        "nature_tree" to "شَجَرَة",
        "nature_flower" to "وَرْدَة",

        // jobs
        "job_doctor" to "دُكْتور",
        "job_teacher" to "مُعَلِّم",
        "job_police" to "شُرْطي",
        "job_firefighter" to "رَجُل إِطْفاء",
        "job_chef" to "طَبّاخ",
        "job_farmer" to "مُزارِع",
        "job_pilot" to "طَيّار",
        "job_nurse" to "مُمَرِّضَة",

        // shapes
        "shape_circle" to "دائِرَة",
        "shape_square" to "مُرَبَّع",
        "shape_triangle" to "مُثَلَّث",
        "shape_star" to "نَجْمَة",
        "shape_heart" to "قَلْب",
        "shape_diamond" to "مُعَيَّن"
    )

    /** Returns the diacritized spoken form for a built-in word if one exists, else null. */
    fun forWordId(wordId: String): String? = forms[wordId]
}
