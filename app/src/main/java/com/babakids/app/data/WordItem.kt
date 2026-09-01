package com.babakids.app.data

/**
 * Core content model. Every learnable "card" in the app is one WordItem.
 * emoji: placeholder visual for the built-in word list. `imagePath`, when
 * set (custom words added from Parent Mode), points to a real photo saved
 * in the app's private storage and takes priority over the emoji.
 * `parentRecordingPath`, when set, is the parent's own voice recording of
 * the word and takes priority over TTS when the app speaks this word.
 *
 * `wordEn` is the English mirror of a built-in word, used when the app's
 * language setting is English. It's null for parent-added custom words
 * (there's no translation UI for those yet) — displayWord() falls back to
 * the Arabic word in that case rather than showing nothing.
 *
 * `wordLanguage` is only meaningful for custom (parent-added) words: it
 * decides whether the word shows up in the Arabic-mode word lists or the
 * English-mode ones — not a translation, just which language "section" it
 * belongs to. Built-in words ignore this field entirely (they already
 * appear in both via word/wordEn).
 */
data class WordItem(
    val id: String,
    val word: String,          // the Arabic word shown + spoken, e.g. "ماء"
    val emoji: String,         // placeholder visual until real art is added
    val category: String,      // must match a Category.id
    val difficulty: Int = 1,   // 1 = easiest, higher = harder
    val imageRes: Int? = null, // optional built-in drawable resource id (future)
    val imagePath: String? = null,          // optional real photo (parent-uploaded)
    val parentRecordingPath: String? = null, // optional custom parent audio file
    val wordEn: String? = null, // optional English mirror
    val wordLanguage: String = "ar", // "ar" or "en" — which language section a custom word belongs to
    val starsRequired: Int = 0, // 0 = always unlocked; higher = the child needs that many total stars to unlock it
    val animationStyle: String = "none", // "none", "sleep", "bounce", "wiggle", "shake" — see SituationVisual
    // True only when a parent has retyped this word's name via Word Edit
    // Mode. Skips the EgyptianSpokenForms diacritized lookup below so the
    // freshly-typed text is read as-is by the device's TTS, instead of an
    // old hand-tuned spoken form meant for the *original* wording.
    val bypassDialectSpokenForm: Boolean = false
) {
    fun displayWord(english: Boolean): String = if (english) (wordEn ?: word) else word

    /**
     * The text actually sent to TTS — same as displayWord() except in
     * Egyptian-dialect Arabic mode, where a diacritized spoken form is
     * substituted if one exists (see EgyptianSpokenForms). The on-screen
     * text (displayWord) never changes; this only affects pronunciation.
     */
    fun spokenWord(english: Boolean, dialect: String): String {
        if (!bypassDialectSpokenForm && !english && dialect == ParentSettingsManager.DIALECT_EGYPTIAN) {
            EgyptianSpokenForms.forWordId(id)?.let { return it }
        }
        return displayWord(english)
    }
}

data class Category(
    val id: String,
    val title: String,
    val titleEn: String,
    val emoji: String,
    val imagePath: String? = null // optional parent-set photo, replaces the emoji when present
) {
    fun displayTitle(english: Boolean): String = if (english) titleEn else title
}

object AppData {

    // "my_words" and "learned_words" are NOT in this list on purpose —
    // both are accessed via a small icon next to the settings gear on
    // Home, not as a category card in the grid (see HomeScreen.kt). Both
    // still use CategoryScreen/LearnedWordsScreen with categoryId
    // "my_words" / a per-child id, they're just entered differently.
    //
    // Order: letters and numbers first (the most foundational skills),
    // then roughly easiest -> hardest for a young child — simple
    // concrete nouns and colors/shapes first, more abstract categories
    // (time, directions, materials) toward the end.
    val categories = listOf(
        Category("letters", "الحروف", "Letters", "🔤"),
        Category("numbers", "الأرقام", "Numbers", "🔢"),
        Category("colors", "الألوان", "Colors", "🎨"),
        Category("shapes", "الأشكال", "Shapes", "⭐"),
        Category("animals", "الحيوانات", "Animals", "🐶"),
        Category("family", "العائلة", "Family", "👨‍👩‍👧"),
        Category("body", "جسم الإنسان", "Body Parts", "🖐️"),
        Category("feelings", "المشاعر", "Feelings", "😊"),
        Category("food", "الطعام", "Food", "🍎"),
        Category("fruits", "الفواكه", "Fruits", "🍇"),
        Category("vegetables", "الخضار", "Vegetables", "🥦"),
        Category("drinks", "المشروبات", "Drinks", "🥤"),
        Category("clothes", "الملابس", "Clothes", "👕"),
        Category("home", "المنزل", "Home", "🏠"),
        Category("toys", "الألعاب", "Toys", "🧸"),
        // "situations" is the internal id (kept for stability — word ids
        // like "situation_sleep" reference it); displayed to the user as
        // "أنا عايز" / "I Want".
        Category("situations", "أنا عايز", "I Want", "🗣️"),
        Category("nature", "الطبيعة", "Nature", "🌳"),
        Category("weather", "الطقس", "Weather", "⛅"),
        Category("transport", "المواصلات", "Transport", "🚗"),
        Category("actions", "الأفعال", "Actions", "🏃"),
        Category("opposites", "الأضداد", "Opposites", "↔️"),
        Category("jobs", "الوظائف", "Jobs", "👨‍⚕️"),
        Category("occupations2", "مهن أكتر", "More Jobs", "💼"),
        Category("school", "المدرسة", "School", "✏️"),
        Category("sports", "الرياضة", "Sports", "⚽"),
        Category("music", "الموسيقى", "Music", "🎵"),
        Category("celebrations", "الاحتفالات", "Celebrations", "🎉"),
        Category("shopping", "التسوق", "Shopping", "🛒"),
        Category("time", "الوقت والأيام", "Time & Days", "🗓️"),
        Category("directions", "الاتجاهات", "Directions", "🧭"),
        Category("seasons", "الفصول", "Seasons", "🍂"),
        Category("tools", "أدوات", "Tools", "🔧"),
        Category("electronics", "الإلكترونيات", "Electronics", "💻"),
        Category("materials", "الخامات", "Materials", "🪵"),
        Category("space", "الفضاء", "Space", "🚀"),
        Category("farm", "المزرعة", "Farm", "🚜")
    )

    // Not part of `categories` (so it never shows as a grid card) but
    // still resolvable by id when navigated to directly via the heart
    // icon on Home — see categoryFor().
    val myWordsCategory = Category("my_words", "كلماتي", "My Words", "❤️")

    /** Resolves a category by id, including "my_words" which is intentionally not in the grid list. */
    fun categoryFor(id: String): Category =
        if (id == "my_words") myWordsCategory else categories.first { it.id == id }

    /** Everything except the "letters" category, which has a fully separate item set per language. */
    val words = listOf(
        // actions
        WordItem("actions_eat", "ياكل", "🍽️", "actions", 1, wordEn = "Eat", starsRequired = 0),
        WordItem("actions_drink", "يشرب", "🥤", "actions", 1, wordEn = "Drink", starsRequired = 0),
        WordItem("actions_sleep", "ينام", "😴", "actions", 2, wordEn = "Sleep", starsRequired = 0),
        WordItem("actions_run", "يجري", "🏃", "actions", 2, wordEn = "Run", starsRequired = 0),
        WordItem("actions_jump", "ينط", "🤸", "actions", 2, wordEn = "Jump", starsRequired = 0),
        WordItem("actions_walk", "يمشي", "🚶", "actions", 2, wordEn = "Walk", starsRequired = 0),
        WordItem("actions_sit", "يقعد", "🪑", "actions", 2, wordEn = "Sit", starsRequired = 0),
        WordItem("actions_stand", "يقف", "🧍", "actions", 3, wordEn = "Stand", starsRequired = 0),
        WordItem("actions_play", "يلعب", "🧸", "actions", 3, wordEn = "Play", starsRequired = 0),
        WordItem("actions_read", "يقرا", "📖", "actions", 3, wordEn = "Read", starsRequired = 0),
        WordItem("actions_write", "يكتب", "✍️", "actions", 3, wordEn = "Write", starsRequired = 0),
        WordItem("actions_sing", "يغني", "🎤", "actions", 3, wordEn = "Sing", starsRequired = 0),
        WordItem("actions_dance", "يرقص", "💃", "actions", 3, wordEn = "Dance", starsRequired = 0),
        WordItem("actions_swim", "يعوم", "🏊", "actions", 3, wordEn = "Swim", starsRequired = 0),
        WordItem("actions_fly", "يطير", "🕊️", "actions", 3, wordEn = "Fly", starsRequired = 0),
        WordItem("actions_climb", "يتسلق", "🧗", "actions", 3, wordEn = "Climb", starsRequired = 5),
        WordItem("actions_laugh", "يضحك", "😄", "actions", 3, wordEn = "Laugh", starsRequired = 10),
        WordItem("actions_cry", "يعيط", "😢", "actions", 3, wordEn = "Cry", starsRequired = 15),
        WordItem("actions_look", "يبص", "👀", "actions", 3, wordEn = "Look", starsRequired = 20),
        WordItem("actions_listen", "يسمع", "👂", "actions", 3, wordEn = "Listen", starsRequired = 25),
        WordItem("actions_talk", "يتكلم", "🗣️", "actions", 3, wordEn = "Talk", starsRequired = 30),
        WordItem("actions_cook", "يطبخ", "🍳", "actions", 3, wordEn = "Cook", starsRequired = 35),
        WordItem("actions_wash", "يغسل", "🧼", "actions", 3, wordEn = "Wash", starsRequired = 40),
        WordItem("actions_open", "يفتح", "🔓", "actions", 3, wordEn = "Open", starsRequired = 45),
        WordItem("actions_close", "يقفل", "🔒", "actions", 3, wordEn = "Close", starsRequired = 50),
        WordItem("actions_give", "يدي", "🤲", "actions", 3, wordEn = "Give", starsRequired = 55),
        WordItem("actions_take", "ياخد", "✋", "actions", 3, wordEn = "Take", starsRequired = 60),
        WordItem("actions_help", "يساعد", "🤝", "actions", 3, wordEn = "Help", starsRequired = 65),
        WordItem("actions_hug", "يحضن", "🤗", "actions", 3, wordEn = "Hug", starsRequired = 70),

        // animals
        WordItem("animal_dog", "كلب", "🐶", "animals", 1, wordEn = "Dog", starsRequired = 0),
        WordItem("animal_cat", "قطة", "🐱", "animals", 1, wordEn = "Cat", starsRequired = 0),
        WordItem("animal_bird", "عصفور", "🐦", "animals", 2, wordEn = "Bird", starsRequired = 0),
        WordItem("animal_fish", "سمكة", "🐟", "animals", 2, wordEn = "Fish", starsRequired = 0),
        WordItem("animal_lion", "أسد", "🦁", "animals", 2, wordEn = "Lion", starsRequired = 0),
        WordItem("animal_tiger", "نمر", "🐯", "animals", 2, wordEn = "Tiger", starsRequired = 0),
        WordItem("animal_elephant", "فيل", "🐘", "animals", 2, wordEn = "Elephant", starsRequired = 0),
        WordItem("animal_giraffe", "زرافة", "🦒", "animals", 3, wordEn = "Giraffe", starsRequired = 0),
        WordItem("animal_monkey", "قرد", "🐒", "animals", 3, wordEn = "Monkey", starsRequired = 0),
        WordItem("animal_bear", "دب", "🐻", "animals", 3, wordEn = "Bear", starsRequired = 0),
        WordItem("animal_rabbit", "أرنب", "🐰", "animals", 3, wordEn = "Rabbit", starsRequired = 0),
        WordItem("animal_horse", "حصان", "🐴", "animals", 3, wordEn = "Horse", starsRequired = 0),
        WordItem("animal_cow", "بقرة", "🐄", "animals", 3, wordEn = "Cow", starsRequired = 0),
        WordItem("animal_sheep", "خروف", "🐑", "animals", 3, wordEn = "Sheep", starsRequired = 0),
        WordItem("animal_goat", "ماعز", "🐐", "animals", 3, wordEn = "Goat", starsRequired = 0),
        WordItem("animal_chicken", "فرخة", "🐔", "animals", 3, wordEn = "Chicken", starsRequired = 0),
        WordItem("animal_duck", "بطة", "🦆", "animals", 3, wordEn = "Duck", starsRequired = 0),
        WordItem("animal_frog", "ضفدع", "🐸", "animals", 3, wordEn = "Frog", starsRequired = 0),
        WordItem("animal_turtle", "سلحفاة", "🐢", "animals", 3, wordEn = "Turtle", starsRequired = 0),
        WordItem("animal_snake", "تعبان", "🐍", "animals", 3, wordEn = "Snake", starsRequired = 0),
        WordItem("animal_fox", "ثعلب", "🦊", "animals", 3, wordEn = "Fox", starsRequired = 0),
        WordItem("animal_wolf", "ذئب", "🐺", "animals", 3, wordEn = "Wolf", starsRequired = 0),
        WordItem("animal_deer", "غزال", "🦌", "animals", 3, wordEn = "Deer", starsRequired = 0),
        WordItem("animal_camel", "جمل", "🐫", "animals", 3, wordEn = "Camel", starsRequired = 0),
        WordItem("animal_zebra", "حمار وحشي", "🦓", "animals", 3, wordEn = "Zebra", starsRequired = 0),
        WordItem("animal_panda", "باندا", "🐼", "animals", 3, wordEn = "Panda", starsRequired = 0),
        WordItem("animal_koala", "كوالا", "🐨", "animals", 3, wordEn = "Koala", starsRequired = 5),
        WordItem("animal_kangaroo", "كنغر", "🦘", "animals", 3, wordEn = "Kangaroo", starsRequired = 10),
        WordItem("animal_penguin", "بطريق", "🐧", "animals", 3, wordEn = "Penguin", starsRequired = 15),
        WordItem("animal_dolphin", "دولفين", "🐬", "animals", 3, wordEn = "Dolphin", starsRequired = 20),
        WordItem("animal_whale", "حوت", "🐳", "animals", 3, wordEn = "Whale", starsRequired = 25),
        WordItem("animal_shark", "قرش", "🦈", "animals", 3, wordEn = "Shark", starsRequired = 30),
        WordItem("animal_octopus", "أخطبوط", "🐙", "animals", 3, wordEn = "Octopus", starsRequired = 35),
        WordItem("animal_crab", "كابوريا", "🦀", "animals", 3, wordEn = "Crab", starsRequired = 40),
        WordItem("animal_bee", "نحلة", "🐝", "animals", 3, wordEn = "Bee", starsRequired = 45),
        WordItem("animal_butterfly", "فراشة", "🦋", "animals", 3, wordEn = "Butterfly", starsRequired = 50),
        WordItem("animal_ant", "نملة", "🐜", "animals", 3, wordEn = "Ant", starsRequired = 55),
        WordItem("animal_spider", "عنكبوت", "🕷️", "animals", 3, wordEn = "Spider", starsRequired = 60),
        WordItem("animal_mouse", "فأر", "🐭", "animals", 3, wordEn = "Mouse", starsRequired = 65),
        WordItem("animal_squirrel", "سنجاب", "🐿️", "animals", 3, wordEn = "Squirrel", starsRequired = 70),
        WordItem("animal_owl", "بومة", "🦉", "animals", 3, wordEn = "Owl", starsRequired = 75),
        WordItem("animal_eagle", "نسر", "🦅", "animals", 3, wordEn = "Eagle", starsRequired = 80),
        WordItem("animal_peacock", "طاووس", "🦚", "animals", 3, wordEn = "Peacock", starsRequired = 85),
        WordItem("animal_parrot", "ببغاء", "🦜", "animals", 3, wordEn = "Parrot", starsRequired = 90),
        WordItem("animal_snail", "حلزون", "🐌", "animals", 3, wordEn = "Snail", starsRequired = 95),
        WordItem("animal_pigeon", "حمامة", "🕊️", "animals", 3, wordEn = "Pigeon", starsRequired = 100),
        WordItem("animal_buffalo", "جاموسة", "🐃", "animals", 3, wordEn = "Buffalo", starsRequired = 105),
        WordItem("animal_rooster", "ديك", "🐓", "animals", 3, wordEn = "Rooster", starsRequired = 110),
        WordItem("animal_goose", "وزة", "🦢", "animals", 3, wordEn = "Goose", starsRequired = 115),
        WordItem("animal_grasshopper", "جندب", "🦗", "animals", 3, wordEn = "Grasshopper", starsRequired = 120),
        WordItem("animal_caterpillar", "يرقة", "🐛", "animals", 3, wordEn = "Caterpillar", starsRequired = 125),

        // body
        WordItem("body_head", "راس", "🗣️", "body", 1, wordEn = "Head", starsRequired = 0),
        WordItem("body_hair", "شعر", "💇", "body", 1, wordEn = "Hair", starsRequired = 0),
        WordItem("body_eyes", "عيون", "👀", "body", 2, wordEn = "Eyes", starsRequired = 0),
        WordItem("body_nose", "مناخير", "👃", "body", 2, wordEn = "Nose", starsRequired = 0),
        WordItem("body_mouth", "بق", "👄", "body", 2, wordEn = "Mouth", starsRequired = 0),
        WordItem("body_ears", "ودان", "👂", "body", 2, wordEn = "Ears", starsRequired = 0),
        WordItem("body_hands", "إيدين", "👐", "body", 2, wordEn = "Hands", starsRequired = 5),
        WordItem("body_feet", "رجلين", "🦶", "body", 3, wordEn = "Feet", starsRequired = 10),
        WordItem("body_fingers", "صوابع", "☝️", "body", 3, wordEn = "Fingers", starsRequired = 15),
        WordItem("body_teeth", "سنان", "🦷", "body", 3, wordEn = "Teeth", starsRequired = 20),
        WordItem("body_tongue", "لسان", "👅", "body", 3, wordEn = "Tongue", starsRequired = 25),
        WordItem("body_belly", "بطن", "🫃", "body", 3, wordEn = "Belly", starsRequired = 30),

        // celebrations
        WordItem("celebrations_birthday", "عيد ميلاد", "🎂", "celebrations", 1, wordEn = "Birthday", starsRequired = 0),
        WordItem("celebrations_cake", "تورتة", "🎂", "celebrations", 1, wordEn = "Cake", starsRequired = 0),
        WordItem("celebrations_gift", "هدية", "🎁", "celebrations", 2, wordEn = "Gift", starsRequired = 0),
        WordItem("celebrations_candle", "شمعة", "🕯️", "celebrations", 2, wordEn = "Candle", starsRequired = 0),
        WordItem("celebrations_party", "حفلة", "🎉", "celebrations", 2, wordEn = "Party", starsRequired = 5),
        WordItem("celebrations_eid", "العيد", "🌙", "celebrations", 2, wordEn = "Eid", starsRequired = 10),
        WordItem("celebrations_ramadan", "رمضان", "🌙", "celebrations", 2, wordEn = "Ramadan", starsRequired = 15),
        WordItem("celebrations_fireworks", "ألعاب نارية", "🎆", "celebrations", 3, wordEn = "Fireworks", starsRequired = 20),

        // clothes
        WordItem("clothes_shirt", "قميص", "👕", "clothes", 1, wordEn = "Shirt", starsRequired = 0),
        WordItem("clothes_shoes", "حذاء", "👟", "clothes", 1, wordEn = "Shoes", starsRequired = 0),
        WordItem("clothes_hat", "قبعة", "🧢", "clothes", 2, wordEn = "Hat", starsRequired = 0),
        WordItem("clothes_pants", "بنطلون", "👖", "clothes", 2, wordEn = "Pants", starsRequired = 0),
        WordItem("clothes_socks", "شراب", "🧦", "clothes", 2, wordEn = "Socks", starsRequired = 0),
        WordItem("clothes_jacket", "چاكيت", "🧥", "clothes", 2, wordEn = "Jacket", starsRequired = 0),
        WordItem("clothes_dress", "فستان", "👗", "clothes", 2, wordEn = "Dress", starsRequired = 0),
        WordItem("clothes_tshirt", "تيشيرت", "👕", "clothes", 3, wordEn = "T-shirt", starsRequired = 0),
        WordItem("clothes_shorts", "شورت", "🩳", "clothes", 3, wordEn = "Shorts", starsRequired = 0),
        WordItem("clothes_gloves", "جوانتي", "🧤", "clothes", 3, wordEn = "Gloves", starsRequired = 0),
        WordItem("clothes_scarf", "شال", "🧣", "clothes", 3, wordEn = "Scarf", starsRequired = 0),
        WordItem("clothes_pajamas", "بيجامة", "🥱", "clothes", 3, wordEn = "Pajamas", starsRequired = 0),
        WordItem("clothes_belt", "حزام", "👔", "clothes", 3, wordEn = "Belt", starsRequired = 0),
        WordItem("clothes_sweater", "بلوفر", "🧶", "clothes", 3, wordEn = "Sweater", starsRequired = 5),
        WordItem("clothes_boots", "بوت", "🥾", "clothes", 3, wordEn = "Boots", starsRequired = 10),
        WordItem("clothes_sandals", "شبشب", "🩴", "clothes", 3, wordEn = "Sandals", starsRequired = 15),
        WordItem("clothes_swimsuit", "مايوه", "🩱", "clothes", 3, wordEn = "Swimsuit", starsRequired = 20),
        WordItem("clothes_coat", "معطف", "🧥", "clothes", 3, wordEn = "Coat", starsRequired = 25),
        WordItem("clothes_glasses", "نضارة", "👓", "clothes", 3, wordEn = "Glasses", starsRequired = 30),
        WordItem("clothes_crown", "تاج", "👑", "clothes", 3, wordEn = "Crown", starsRequired = 35),
        WordItem("clothes_ring", "خاتم", "💍", "clothes", 3, wordEn = "Ring", starsRequired = 40),
        WordItem("clothes_bracelet", "سوار", "📿", "clothes", 3, wordEn = "Bracelet", starsRequired = 45),
        WordItem("clothes_necklace", "قلادة", "📿", "clothes", 3, wordEn = "Necklace", starsRequired = 50),
        WordItem("clothes_handbag", "شنطة يد", "👜", "clothes", 3, wordEn = "Handbag", starsRequired = 55),
        WordItem("clothes_cap", "كاب", "🧢", "clothes", 3, wordEn = "Cap", starsRequired = 60),

        // colors
        WordItem("color_red", "أحمر", "🔴", "colors", 1, wordEn = "Red", starsRequired = 0),
        WordItem("color_orange", "برتقالي", "🟠", "colors", 1, wordEn = "Orange", starsRequired = 0),
        WordItem("color_yellow", "أصفر", "🟡", "colors", 2, wordEn = "Yellow", starsRequired = 0),
        WordItem("color_green", "أخضر", "🟢", "colors", 2, wordEn = "Green", starsRequired = 0),
        WordItem("color_blue", "أزرق", "🔵", "colors", 2, wordEn = "Blue", starsRequired = 0),
        WordItem("color_purple", "بنفسجي", "🟣", "colors", 2, wordEn = "Purple", starsRequired = 0),
        WordItem("color_brown", "بني", "🟤", "colors", 2, wordEn = "Brown", starsRequired = 0),
        WordItem("color_black", "أسود", "⚫", "colors", 3, wordEn = "Black", starsRequired = 0),
        WordItem("color_white", "أبيض", "⚪", "colors", 3, wordEn = "White", starsRequired = 0),
        WordItem("color_gray", "رمادي", "⬜", "colors", 3, wordEn = "Gray", starsRequired = 5),
        WordItem("color_gold", "دهبي", "🟨", "colors", 3, wordEn = "Gold", starsRequired = 10),
        WordItem("color_silver", "فضي", "⬜", "colors", 3, wordEn = "Silver", starsRequired = 15),
        WordItem("color_pink", "وردي", "🌸", "colors", 3, wordEn = "Pink", starsRequired = 20),
        WordItem("color_turquoise", "تركواز", "🔷", "colors", 3, wordEn = "Turquoise", starsRequired = 25),
        WordItem("color_beige", "بيج", "🟨", "colors", 3, wordEn = "Beige", starsRequired = 30),
        WordItem("color_navy", "كحلي", "🔵", "colors", 3, wordEn = "Navy", starsRequired = 35),
        WordItem("color_mint", "نعناعي", "🟢", "colors", 3, wordEn = "Mint", starsRequired = 40),

        // directions
        WordItem("directions_left", "شمال", "⬅️", "directions", 1, wordEn = "Left", starsRequired = 0),
        WordItem("directions_right", "يمين", "➡️", "directions", 1, wordEn = "Right", starsRequired = 0),
        WordItem("directions_front", "قدام", "⏩", "directions", 2, wordEn = "Front", starsRequired = 0),
        WordItem("directions_behind", "ورا", "⏪", "directions", 2, wordEn = "Behind", starsRequired = 0),
        WordItem("directions_between", "بين", "↔️", "directions", 2, wordEn = "Between", starsRequired = 5),
        WordItem("directions_inside", "جوه", "📥", "directions", 2, wordEn = "Inside", starsRequired = 10),
        WordItem("directions_outside", "برا", "📤", "directions", 2, wordEn = "Outside", starsRequired = 15),
        WordItem("directions_middle", "نص", "🎯", "directions", 3, wordEn = "Middle", starsRequired = 20),

        // drinks
        WordItem("drinks_juice", "عصير", "🧃", "drinks", 1, wordEn = "Juice", starsRequired = 0),
        WordItem("drinks_tea", "شاي", "🍵", "drinks", 1, wordEn = "Tea", starsRequired = 0),
        WordItem("drinks_coffee", "قهوة", "☕", "drinks", 2, wordEn = "Coffee", starsRequired = 0),
        WordItem("drinks_lemonade", "ليموناضة", "🍋", "drinks", 2, wordEn = "Lemonade", starsRequired = 0),
        WordItem("drinks_soda", "ميه غازية", "🥤", "drinks", 2, wordEn = "Soda", starsRequired = 0),
        WordItem("drinks_hotchoc", "شوكولاتة سخنة", "☕", "drinks", 2, wordEn = "Hot Chocolate", starsRequired = 5),
        WordItem("drinks_smoothie", "سموذي", "🥤", "drinks", 2, wordEn = "Smoothie", starsRequired = 10),
        WordItem("drinks_milkshake", "ميلك شيك", "🥛", "drinks", 3, wordEn = "Milkshake", starsRequired = 15),
        WordItem("drinks_coconutwater", "ميه جوز هند", "🥥", "drinks", 3, wordEn = "Coconut Water", starsRequired = 20),
        WordItem("drinks_mangojuice", "عصير مانجة", "🥭", "drinks", 3, wordEn = "Mango Juice", starsRequired = 25),

        // electronics
        WordItem("electronics_computer", "كمبيوتر", "💻", "electronics", 1, wordEn = "Computer", starsRequired = 0),
        WordItem("electronics_tablet", "تابلت", "📱", "electronics", 1, wordEn = "Tablet", starsRequired = 0),
        WordItem("electronics_headphones", "سماعات", "🎧", "electronics", 2, wordEn = "Headphones", starsRequired = 0),
        WordItem("electronics_charger", "شاحن", "🔌", "electronics", 2, wordEn = "Charger", starsRequired = 0),
        WordItem("electronics_keyboard", "كيبورد", "⌨️", "electronics", 2, wordEn = "Keyboard", starsRequired = 5),
        WordItem("electronics_printer", "پرينتر", "🖨️", "electronics", 2, wordEn = "Printer", starsRequired = 10),
        WordItem("electronics_speaker", "سماعة", "🔊", "electronics", 2, wordEn = "Speaker", starsRequired = 15),

        // family
        WordItem("family_mom", "ماما", "👩", "family", 1, wordEn = "Mom", starsRequired = 0),
        WordItem("family_dad", "بابا", "👨", "family", 1, wordEn = "Dad", starsRequired = 0),
        WordItem("family_sister", "أختي", "👧", "family", 2, wordEn = "Sister", starsRequired = 0),
        WordItem("family_brother", "أخويا", "👦", "family", 2, wordEn = "Brother", starsRequired = 0),
        WordItem("family_grandma", "تيتة", "👵", "family", 2, wordEn = "Grandma", starsRequired = 0),
        WordItem("family_grandpa", "جدو", "👴", "family", 2, wordEn = "Grandpa", starsRequired = 0),
        WordItem("family_aunt", "خالتي", "👩", "family", 2, wordEn = "Aunt", starsRequired = 0),
        WordItem("family_uncle", "عمو", "👨", "family", 3, wordEn = "Uncle", starsRequired = 0),
        WordItem("family_cousin", "ابن عمي", "🧒", "family", 3, wordEn = "Cousin", starsRequired = 0),
        WordItem("family_baby", "بيبي", "👶", "family", 3, wordEn = "Baby", starsRequired = 5),
        WordItem("family_friend", "صاحبي", "🧑‍🤝‍🧑", "family", 3, wordEn = "Friend", starsRequired = 10),
        WordItem("family_husband", "جوز", "🤵", "family", 3, wordEn = "Husband", starsRequired = 15),
        WordItem("family_wife", "مراة", "👰", "family", 3, wordEn = "Wife", starsRequired = 20),
        WordItem("family_son", "ابني", "👦", "family", 3, wordEn = "Son", starsRequired = 25),
        WordItem("family_daughter", "بنتي", "👧", "family", 3, wordEn = "Daughter", starsRequired = 30),
        WordItem("family_twin", "توأم", "👯", "family", 3, wordEn = "Twin", starsRequired = 35),
        WordItem("family_familyword", "عيلة", "👨‍👩‍👧‍👦", "family", 3, wordEn = "Family", starsRequired = 40),

        // farm
        WordItem("farm_farm", "مزرعة", "🚜", "farm", 1, wordEn = "Farm", starsRequired = 0),
        WordItem("farm_barn", "حظيرة", "🏚️", "farm", 1, wordEn = "Barn", starsRequired = 0),
        WordItem("farm_hay", "قش", "🌾", "farm", 2, wordEn = "Hay", starsRequired = 0),
        WordItem("farm_scarecrow", "خيال المآتة", "🎃", "farm", 2, wordEn = "Scarecrow", starsRequired = 0),
        WordItem("farm_well", "بئر", "🪣", "farm", 2, wordEn = "Well", starsRequired = 5),
        WordItem("farm_fence", "سور", "🚧", "farm", 2, wordEn = "Fence", starsRequired = 10),
        WordItem("farm_windmill", "طاحونة هوا", "🎡", "farm", 2, wordEn = "Windmill", starsRequired = 15),

        // feelings
        WordItem("feel_happy", "سعيد", "😊", "feelings", 1, wordEn = "Happy", starsRequired = 0),
        WordItem("feel_sad", "زعلان", "😢", "feelings", 1, wordEn = "Sad", starsRequired = 0),
        WordItem("feel_angry", "غضبان", "😡", "feelings", 2, wordEn = "Angry", starsRequired = 0),
        WordItem("feel_sleepy", "نعسان", "😴", "feelings", 2, wordEn = "Sleepy", starsRequired = 0),
        WordItem("feel_scared", "خايف", "😨", "feelings", 2, wordEn = "Scared", starsRequired = 0),
        WordItem("feel_love", "بحب", "😍", "feelings", 2, wordEn = "Love", starsRequired = 0),
        WordItem("feel_tired", "تعبان", "🤢", "feelings", 2, wordEn = "Tired", starsRequired = 0),
        WordItem("feel_excited", "متحمس", "🤩", "feelings", 3, wordEn = "Excited", starsRequired = 0),
        WordItem("feel_bored", "زهقان", "😑", "feelings", 3, wordEn = "Bored", starsRequired = 0),
        WordItem("feel_surprised", "متفاجئ", "😮", "feelings", 3, wordEn = "Surprised", starsRequired = 0),
        WordItem("feel_shy", "خجول", "🙈", "feelings", 3, wordEn = "Shy", starsRequired = 0),
        WordItem("feel_proud", "فخور", "😌", "feelings", 3, wordEn = "Proud", starsRequired = 0),
        WordItem("feel_confused", "مش فاهم", "😕", "feelings", 3, wordEn = "Confused", starsRequired = 0),
        WordItem("feel_worried", "قلقان", "😟", "feelings", 3, wordEn = "Worried", starsRequired = 5),
        WordItem("feel_brave", "شجاع", "💪", "feelings", 3, wordEn = "Brave", starsRequired = 10),
        WordItem("feel_calm", "هادئ", "😇", "feelings", 3, wordEn = "Calm", starsRequired = 15),
        WordItem("feel_jealous", "غيران", "😒", "feelings", 3, wordEn = "Jealous", starsRequired = 20),
        WordItem("feel_hungry", "جعان", "🤤", "feelings", 3, wordEn = "Hungry", starsRequired = 25),
        WordItem("feel_thirsty", "عطشان", "🥵", "feelings", 3, wordEn = "Thirsty", starsRequired = 30),
        WordItem("feel_sick", "مريض", "🤒", "feelings", 3, wordEn = "Sick", starsRequired = 35),
        WordItem("feel_confident", "واثق", "😎", "feelings", 3, wordEn = "Confident", starsRequired = 40),
        WordItem("feel_embarrassed", "محرج", "😳", "feelings", 3, wordEn = "Embarrassed", starsRequired = 45),
        WordItem("feel_joyful", "فرحان", "😃", "feelings", 3, wordEn = "Joyful", starsRequired = 50),
        WordItem("feel_relieved", "مرتاح", "😌", "feelings", 3, wordEn = "Relieved", starsRequired = 55),
        WordItem("feel_annoyed", "متضايق", "😤", "feelings", 3, wordEn = "Annoyed", starsRequired = 60),

        // food
        WordItem("food_apple", "تفاحة", "🍎", "food", 1, wordEn = "Apple", starsRequired = 0),
        WordItem("food_banana", "موزة", "🍌", "food", 1, wordEn = "Banana", starsRequired = 0),
        WordItem("food_milk", "لبن", "🥛", "food", 2, wordEn = "Milk", starsRequired = 0),
        WordItem("food_pizza", "بيتزا", "🍕", "food", 2, wordEn = "Pizza", starsRequired = 0),
        WordItem("food_water", "ماء", "💧", "food", 2, wordEn = "Water", starsRequired = 0),
        WordItem("food_egg", "بيضة", "🥚", "food", 2, wordEn = "Egg", starsRequired = 0),
        WordItem("food_cheese", "جبنة", "🧀", "food", 2, wordEn = "Cheese", starsRequired = 0),
        WordItem("food_bread", "عيش", "🍞", "food", 3, wordEn = "Bread", starsRequired = 0),
        WordItem("food_honey", "عسل", "🍯", "food", 3, wordEn = "Honey", starsRequired = 5),
        WordItem("food_butter", "زبدة", "🧈", "food", 3, wordEn = "Butter", starsRequired = 10),
        WordItem("food_rice", "رز", "🍚", "food", 3, wordEn = "Rice", starsRequired = 15),
        WordItem("food_pasta", "مكرونة", "🍝", "food", 3, wordEn = "Pasta", starsRequired = 20),
        WordItem("food_soup", "شوربة", "🍲", "food", 3, wordEn = "Soup", starsRequired = 25),
        WordItem("food_salad", "سلطة", "🥗", "food", 3, wordEn = "Salad", starsRequired = 30),
        WordItem("food_koshary", "كشري", "🍛", "food", 3, wordEn = "Koshary", starsRequired = 35),

        // fruits
        WordItem("fruits_orange", "برتقالة", "🍊", "fruits", 1, wordEn = "Orange", starsRequired = 0),
        WordItem("fruits_grape", "عنب", "🍇", "fruits", 1, wordEn = "Grapes", starsRequired = 0),
        WordItem("fruits_strawberry", "فراولة", "🍓", "fruits", 2, wordEn = "Strawberry", starsRequired = 0),
        WordItem("fruits_watermelon", "بطيخة", "🍉", "fruits", 2, wordEn = "Watermelon", starsRequired = 0),
        WordItem("fruits_mango", "مانجة", "🥭", "fruits", 2, wordEn = "Mango", starsRequired = 0),
        WordItem("fruits_pineapple", "أناناس", "🍍", "fruits", 2, wordEn = "Pineapple", starsRequired = 0),
        WordItem("fruits_peach", "خوخة", "🍑", "fruits", 2, wordEn = "Peach", starsRequired = 0),
        WordItem("fruits_pear", "كمثرى", "🍐", "fruits", 3, wordEn = "Pear", starsRequired = 0),
        WordItem("fruits_cherry", "كريز", "🍒", "fruits", 3, wordEn = "Cherry", starsRequired = 0),
        WordItem("fruits_lemon", "ليمونة", "🍋", "fruits", 3, wordEn = "Lemon", starsRequired = 0),
        WordItem("fruits_kiwi", "كيوي", "🥝", "fruits", 3, wordEn = "Kiwi", starsRequired = 5),
        WordItem("fruits_coconut", "جوز هند", "🥥", "fruits", 3, wordEn = "Coconut", starsRequired = 10),
        WordItem("fruits_fig", "تين", "🌰", "fruits", 3, wordEn = "Fig", starsRequired = 15),
        WordItem("fruits_plum", "برقوق", "🍑", "fruits", 3, wordEn = "Plum", starsRequired = 20),
        WordItem("fruits_guava", "جوافة", "🍈", "fruits", 3, wordEn = "Guava", starsRequired = 25),
        WordItem("fruits_pomegranate", "رمانة", "🔴", "fruits", 3, wordEn = "Pomegranate", starsRequired = 30),
        WordItem("fruits_melon", "شمام", "🍈", "fruits", 3, wordEn = "Melon", starsRequired = 35),
        WordItem("fruits_apricot", "مشمش", "🍑", "fruits", 3, wordEn = "Apricot", starsRequired = 40),
        WordItem("fruits_date", "بلح", "🌴", "fruits", 3, wordEn = "Date", starsRequired = 45),
        WordItem("fruits_avocado", "أفوكادو", "🥑", "fruits", 3, wordEn = "Avocado", starsRequired = 50),

        // home
        WordItem("home_bed", "سرير", "🛏️", "home", 1, wordEn = "Bed", starsRequired = 0),
        WordItem("home_door", "باب", "🚪", "home", 1, wordEn = "Door", starsRequired = 0),
        WordItem("home_chair", "كرسي", "🪑", "home", 2, wordEn = "Chair", starsRequired = 0),
        WordItem("home_table", "ترابيزة", "🍽️", "home", 2, wordEn = "Table", starsRequired = 0),
        WordItem("home_sofa", "كنبة", "🛋️", "home", 2, wordEn = "Sofa", starsRequired = 0),
        WordItem("home_window", "شباك", "🪟", "home", 2, wordEn = "Window", starsRequired = 0),
        WordItem("home_lamp", "لمبة", "💡", "home", 2, wordEn = "Lamp", starsRequired = 0),
        WordItem("home_mirror", "مراية", "🪞", "home", 3, wordEn = "Mirror", starsRequired = 0),
        WordItem("home_clock", "ساعة", "🕐", "home", 3, wordEn = "Clock", starsRequired = 0),
        WordItem("home_tv", "تليفزيون", "📺", "home", 3, wordEn = "TV", starsRequired = 0),
        WordItem("home_phone", "تليفون", "📱", "home", 3, wordEn = "Phone", starsRequired = 0),
        WordItem("home_key", "مفتاح", "🔑", "home", 3, wordEn = "Key", starsRequired = 0),
        WordItem("home_fridge", "تلاجة", "🧊", "home", 3, wordEn = "Fridge", starsRequired = 0),
        WordItem("home_oven", "فرن", "🔥", "home", 3, wordEn = "Oven", starsRequired = 0),
        WordItem("home_sink", "حوض", "🚰", "home", 3, wordEn = "Sink", starsRequired = 0),
        WordItem("home_bathtub", "بانيو", "🛁", "home", 3, wordEn = "Bathtub", starsRequired = 0),
        WordItem("home_towel", "فوطة", "🧻", "home", 3, wordEn = "Towel", starsRequired = 0),
        WordItem("home_pillow", "مخدة", "🛏️", "home", 3, wordEn = "Pillow", starsRequired = 5),
        WordItem("home_blanket", "بطانية", "🧣", "home", 3, wordEn = "Blanket", starsRequired = 10),
        WordItem("home_carpet", "سجادة", "🟫", "home", 3, wordEn = "Carpet", starsRequired = 15),
        WordItem("home_stairs", "سلم", "🪜", "home", 3, wordEn = "Stairs", starsRequired = 20),
        WordItem("home_kitchen", "مطبخ", "🍳", "home", 3, wordEn = "Kitchen", starsRequired = 25),
        WordItem("home_bathroom", "حمام", "🚽", "home", 3, wordEn = "Bathroom", starsRequired = 30),
        WordItem("home_garden", "جنينة", "🌷", "home", 3, wordEn = "Garden", starsRequired = 35),
        WordItem("home_balcony", "بلكونة", "🏡", "home", 3, wordEn = "Balcony", starsRequired = 40),
        WordItem("home_curtain", "ستارة", "🪟", "home", 3, wordEn = "Curtain", starsRequired = 45),
        WordItem("home_fan", "مروحة", "🌀", "home", 3, wordEn = "Fan", starsRequired = 50),
        WordItem("home_ac", "تكييف", "❄️", "home", 3, wordEn = "AC", starsRequired = 55),
        WordItem("home_stove", "بوتاجاز", "🔥", "home", 3, wordEn = "Stove", starsRequired = 60),
        WordItem("home_washer", "غسالة", "🫧", "home", 3, wordEn = "Washing Machine", starsRequired = 65),
        WordItem("home_iron", "مكواة", "♨️", "home", 3, wordEn = "Iron", starsRequired = 70),
        WordItem("home_closet", "دولاب", "🚪", "home", 3, wordEn = "Closet", starsRequired = 75),
        WordItem("home_shelf", "رف", "📚", "home", 3, wordEn = "Shelf", starsRequired = 80),

        // jobs
        WordItem("job_doctor", "دكتور", "👨‍⚕️", "jobs", 1, wordEn = "Doctor", starsRequired = 0),
        WordItem("job_teacher", "معلم", "👩‍🏫", "jobs", 1, wordEn = "Teacher", starsRequired = 0),
        WordItem("job_police", "شرطي", "👮", "jobs", 2, wordEn = "Police Officer", starsRequired = 0),
        WordItem("job_firefighter", "رجل إطفاء", "👨‍🚒", "jobs", 2, wordEn = "Firefighter", starsRequired = 0),
        WordItem("job_chef", "طباخ", "👨‍🍳", "jobs", 2, wordEn = "Chef", starsRequired = 0),
        WordItem("job_farmer", "مزارع", "👨‍🌾", "jobs", 2, wordEn = "Farmer", starsRequired = 0),
        WordItem("job_pilot", "طيار", "👨‍✈️", "jobs", 2, wordEn = "Pilot", starsRequired = 0),
        WordItem("job_nurse", "ممرضة", "👩‍⚕️", "jobs", 3, wordEn = "Nurse", starsRequired = 0),
        WordItem("job_engineer", "مهندس", "👷", "jobs", 3, wordEn = "Engineer", starsRequired = 0),
        WordItem("job_artist", "فنان", "🎨", "jobs", 3, wordEn = "Artist", starsRequired = 0),
        WordItem("job_singer", "مغني", "🎤", "jobs", 3, wordEn = "Singer", starsRequired = 0),
        WordItem("job_dancer", "راقصة", "💃", "jobs", 3, wordEn = "Dancer", starsRequired = 0),
        WordItem("job_driver", "سواق", "🚖", "jobs", 3, wordEn = "Driver", starsRequired = 0),
        WordItem("job_waiter", "جرسون", "🧑‍🍳", "jobs", 3, wordEn = "Waiter", starsRequired = 5),
        WordItem("job_barber", "حلاق", "💈", "jobs", 3, wordEn = "Barber", starsRequired = 10),
        WordItem("job_tailor", "خياط", "🧵", "jobs", 3, wordEn = "Tailor", starsRequired = 15),
        WordItem("job_fisherman", "صياد", "🎣", "jobs", 3, wordEn = "Fisherman", starsRequired = 20),
        WordItem("job_soldier", "جندي", "🎖️", "jobs", 3, wordEn = "Soldier", starsRequired = 25),
        WordItem("job_scientist", "عالم", "🔬", "jobs", 3, wordEn = "Scientist", starsRequired = 30),
        WordItem("job_athlete", "رياضي", "🏃", "jobs", 3, wordEn = "Athlete", starsRequired = 35),
        WordItem("job_coach", "مدرب", "🏋️", "jobs", 3, wordEn = "Coach", starsRequired = 40),
        WordItem("job_designer", "مصمم", "🎨", "jobs", 3, wordEn = "Designer", starsRequired = 45),
        WordItem("job_translator", "مترجم", "🗣️", "jobs", 3, wordEn = "Translator", starsRequired = 50),
        WordItem("job_programmer", "مبرمج", "💻", "jobs", 3, wordEn = "Programmer", starsRequired = 55),
        WordItem("job_salesperson", "بائع", "🛍️", "jobs", 3, wordEn = "Salesperson", starsRequired = 60),

        // materials
        WordItem("materials_wood", "خشب", "🪵", "materials", 1, wordEn = "Wood", starsRequired = 0),
        WordItem("materials_metal", "معدن", "⚙️", "materials", 1, wordEn = "Metal", starsRequired = 0),
        WordItem("materials_glass", "زجاج", "🥃", "materials", 2, wordEn = "Glass", starsRequired = 0),
        WordItem("materials_plastic", "بلاستيك", "🧴", "materials", 2, wordEn = "Plastic", starsRequired = 0),
        WordItem("materials_paper", "ورق", "📄", "materials", 2, wordEn = "Paper", starsRequired = 5),
        WordItem("materials_cotton", "قطن", "☁️", "materials", 2, wordEn = "Cotton", starsRequired = 10),
        WordItem("materials_leather", "جلد", "👞", "materials", 2, wordEn = "Leather", starsRequired = 15),
        WordItem("materials_stone", "حجر", "🪨", "materials", 3, wordEn = "Stone", starsRequired = 20),

        // music
        WordItem("music_guitar", "جيتار", "🎸", "music", 1, wordEn = "Guitar", starsRequired = 0),
        WordItem("music_piano", "بيانو", "🎹", "music", 1, wordEn = "Piano", starsRequired = 0),
        WordItem("music_drums", "طبول", "🥁", "music", 2, wordEn = "Drums", starsRequired = 0),
        WordItem("music_violin", "كمان", "🎻", "music", 2, wordEn = "Violin", starsRequired = 0),
        WordItem("music_flute", "فلوت", "🎼", "music", 2, wordEn = "Flute", starsRequired = 0),
        WordItem("music_trumpet", "ترومبيت", "🎺", "music", 2, wordEn = "Trumpet", starsRequired = 5),
        WordItem("music_singing", "غنا", "🎤", "music", 2, wordEn = "Singing", starsRequired = 10),
        WordItem("music_microphone", "ميكروفون", "🎙️", "music", 3, wordEn = "Microphone", starsRequired = 15),
        WordItem("music_radio", "راديو", "📻", "music", 3, wordEn = "Radio", starsRequired = 20),
        WordItem("music_song", "أغنية", "🎶", "music", 3, wordEn = "Song", starsRequired = 25),

        // nature
        WordItem("nature_sun", "شمس", "☀️", "nature", 1, wordEn = "Sun", starsRequired = 0),
        WordItem("nature_tree", "شجرة", "🌳", "nature", 1, wordEn = "Tree", starsRequired = 0),
        WordItem("nature_flower", "وردة", "🌸", "nature", 2, wordEn = "Flower", starsRequired = 0),
        WordItem("nature_moon", "قمر", "🌙", "nature", 2, wordEn = "Moon", starsRequired = 0),
        WordItem("nature_sky", "سما", "🌌", "nature", 2, wordEn = "Sky", starsRequired = 0),
        WordItem("nature_cloud", "سحابة", "☁️", "nature", 2, wordEn = "Cloud", starsRequired = 0),
        WordItem("nature_rain", "مطر", "🌧️", "nature", 2, wordEn = "Rain", starsRequired = 0),
        WordItem("nature_snow", "تلج", "❄️", "nature", 3, wordEn = "Snow", starsRequired = 0),
        WordItem("nature_wind", "هوا", "💨", "nature", 3, wordEn = "Wind", starsRequired = 0),
        WordItem("nature_mountain", "جبل", "⛰️", "nature", 3, wordEn = "Mountain", starsRequired = 0),
        WordItem("nature_river", "نهر", "🏞️", "nature", 3, wordEn = "River", starsRequired = 0),
        WordItem("nature_sea", "بحر", "🌊", "nature", 3, wordEn = "Sea", starsRequired = 0),
        WordItem("nature_beach", "شاطئ", "🏖️", "nature", 3, wordEn = "Beach", starsRequired = 0),
        WordItem("nature_forest", "غابة", "🌲", "nature", 3, wordEn = "Forest", starsRequired = 0),
        WordItem("nature_desert", "صحرا", "🏜️", "nature", 3, wordEn = "Desert", starsRequired = 0),
        WordItem("nature_rainbow", "قوس قزح", "🌈", "nature", 3, wordEn = "Rainbow", starsRequired = 0),
        WordItem("nature_thunder", "رعد", "⛈️", "nature", 3, wordEn = "Thunder", starsRequired = 0),
        WordItem("nature_lightning", "برق", "⚡", "nature", 3, wordEn = "Lightning", starsRequired = 5),
        WordItem("nature_leaf", "ورقة شجر", "🍃", "nature", 3, wordEn = "Leaf", starsRequired = 10),
        WordItem("nature_grass", "عشب", "🌿", "nature", 3, wordEn = "Grass", starsRequired = 15),
        WordItem("nature_rock", "صخرة", "🪨", "nature", 3, wordEn = "Rock", starsRequired = 20),
        WordItem("nature_sand", "رمل", "🏖️", "nature", 3, wordEn = "Sand", starsRequired = 25),
        WordItem("nature_fire", "نار", "🔥", "nature", 3, wordEn = "Fire", starsRequired = 30),
        WordItem("nature_ice", "جليد", "🧊", "nature", 3, wordEn = "Ice", starsRequired = 35),
        WordItem("nature_volcano", "بركان", "🌋", "nature", 3, wordEn = "Volcano", starsRequired = 40),
        WordItem("nature_island", "جزيرة", "🏝️", "nature", 3, wordEn = "Island", starsRequired = 45),
        WordItem("nature_lake", "بحيرة", "🏞️", "nature", 3, wordEn = "Lake", starsRequired = 50),
        WordItem("nature_waterfall", "شلال", "💦", "nature", 3, wordEn = "Waterfall", starsRequired = 55),
        WordItem("nature_cave", "كهف", "🕳️", "nature", 3, wordEn = "Cave", starsRequired = 60),
        WordItem("nature_soil", "تراب", "🟤", "nature", 3, wordEn = "Soil", starsRequired = 65),
        WordItem("nature_nest", "عش", "🪺", "nature", 3, wordEn = "Nest", starsRequired = 70),
        WordItem("nature_valley", "وادي", "⛰️", "nature", 3, wordEn = "Valley", starsRequired = 75),
        WordItem("nature_sunrise", "شروق", "🌅", "nature", 3, wordEn = "Sunrise", starsRequired = 80),
        WordItem("nature_sunset", "غروب", "🌇", "nature", 3, wordEn = "Sunset", starsRequired = 85),

        // numbers
        WordItem("num_1", "واحد", "1️⃣", "numbers", 1, wordEn = "One", starsRequired = 0),
        WordItem("num_2", "اتنين", "2️⃣", "numbers", 1, wordEn = "Two", starsRequired = 0),
        WordItem("num_3", "تلاتة", "3️⃣", "numbers", 2, wordEn = "Three", starsRequired = 0),
        WordItem("num_4", "أربعة", "4️⃣", "numbers", 2, wordEn = "Four", starsRequired = 0),
        WordItem("num_5", "خمسة", "5️⃣", "numbers", 2, wordEn = "Five", starsRequired = 0),
        WordItem("num_6", "ستة", "6️⃣", "numbers", 2, wordEn = "Six", starsRequired = 0),
        WordItem("num_7", "سبعة", "7️⃣", "numbers", 2, wordEn = "Seven", starsRequired = 0),
        WordItem("num_8", "تمانية", "8️⃣", "numbers", 3, wordEn = "Eight", starsRequired = 0),
        WordItem("num_9", "تسعة", "9️⃣", "numbers", 3, wordEn = "Nine", starsRequired = 0),
        WordItem("num_10", "عشرة", "🔟", "numbers", 3, wordEn = "Ten", starsRequired = 0),
        WordItem("num_11", "حداشر", "1️⃣1️⃣", "numbers", 3, wordEn = "Eleven", starsRequired = 0),
        WordItem("num_12", "اتناشر", "1️⃣2️⃣", "numbers", 3, wordEn = "Twelve", starsRequired = 0),
        WordItem("num_13", "تلتاشر", "1️⃣3️⃣", "numbers", 3, wordEn = "Thirteen", starsRequired = 5),
        WordItem("num_14", "اربعتاشر", "1️⃣4️⃣", "numbers", 3, wordEn = "Fourteen", starsRequired = 10),
        WordItem("num_15", "خمستاشر", "1️⃣5️⃣", "numbers", 3, wordEn = "Fifteen", starsRequired = 15),
        WordItem("num_16", "ستاشر", "1️⃣6️⃣", "numbers", 3, wordEn = "Sixteen", starsRequired = 20),
        WordItem("num_17", "سبعتاشر", "1️⃣7️⃣", "numbers", 3, wordEn = "Seventeen", starsRequired = 25),
        WordItem("num_18", "تمنتاشر", "1️⃣8️⃣", "numbers", 3, wordEn = "Eighteen", starsRequired = 30),
        WordItem("num_19", "تسعتاشر", "1️⃣9️⃣", "numbers", 3, wordEn = "Nineteen", starsRequired = 35),
        WordItem("num_20", "عشرين", "2️⃣0️⃣", "numbers", 3, wordEn = "Twenty", starsRequired = 40),
        WordItem("num_25", "خمسة وعشرين", "2️⃣5️⃣", "numbers", 3, wordEn = "Twenty-Five", starsRequired = 45),
        WordItem("num_30", "تلاتين", "3️⃣0️⃣", "numbers", 3, wordEn = "Thirty", starsRequired = 50),
        WordItem("num_50", "خمسين", "5️⃣0️⃣", "numbers", 3, wordEn = "Fifty", starsRequired = 55),
        WordItem("num_100", "ميه", "💯", "numbers", 3, wordEn = "One Hundred", starsRequired = 60),

        // occupations2
        WordItem("occupations2_dentist", "دكتور أسنان", "🦷", "occupations2", 1, wordEn = "Dentist", starsRequired = 0),
        WordItem("occupations2_vet", "دكتور بيطري", "🐾", "occupations2", 1, wordEn = "Vet", starsRequired = 0),
        WordItem("occupations2_judge", "قاضي", "⚖️", "occupations2", 2, wordEn = "Judge", starsRequired = 0),
        WordItem("occupations2_lawyer", "محامي", "💼", "occupations2", 2, wordEn = "Lawyer", starsRequired = 0),
        WordItem("occupations2_accountant", "محاسب", "🧮", "occupations2", 2, wordEn = "Accountant", starsRequired = 0),
        WordItem("occupations2_electrician", "كهربائي", "💡", "occupations2", 2, wordEn = "Electrician", starsRequired = 5),
        WordItem("occupations2_plumber", "سباك", "🔧", "occupations2", 2, wordEn = "Plumber", starsRequired = 10),
        WordItem("occupations2_carpenter", "نجار", "🪚", "occupations2", 3, wordEn = "Carpenter", starsRequired = 15),
        WordItem("occupations2_photographer", "مصور", "📷", "occupations2", 3, wordEn = "Photographer", starsRequired = 20),
        WordItem("occupations2_journalist", "صحفي", "📰", "occupations2", 3, wordEn = "Journalist", starsRequired = 25),

        // opposites
        WordItem("opposites_big", "كبير", "🐘", "opposites", 1, wordEn = "Big", starsRequired = 0),
        WordItem("opposites_small", "صغير", "🐭", "opposites", 1, wordEn = "Small", starsRequired = 0),
        WordItem("opposites_hot2", "سخن", "🔥", "opposites", 2, wordEn = "Hot", starsRequired = 0),
        WordItem("opposites_cold2", "بارد", "🧊", "opposites", 2, wordEn = "Cold", starsRequired = 0),
        WordItem("opposites_fast", "سريع", "🐆", "opposites", 2, wordEn = "Fast", starsRequired = 0),
        WordItem("opposites_slow", "بطيء", "🐢", "opposites", 2, wordEn = "Slow", starsRequired = 0),
        WordItem("opposites_up", "فوق", "⬆️", "opposites", 2, wordEn = "Up", starsRequired = 0),
        WordItem("opposites_down", "تحت", "⬇️", "opposites", 3, wordEn = "Down", starsRequired = 0),
        WordItem("opposites_full", "مليان", "🫙", "opposites", 3, wordEn = "Full", starsRequired = 0),
        WordItem("opposites_empty", "فاضي", "📭", "opposites", 3, wordEn = "Empty", starsRequired = 0),
        WordItem("opposites_wet", "مبلول", "💦", "opposites", 3, wordEn = "Wet", starsRequired = 5),
        WordItem("opposites_dry", "ناشف", "🏜️", "opposites", 3, wordEn = "Dry", starsRequired = 10),
        WordItem("opposites_near", "قريب", "📍", "opposites", 3, wordEn = "Near", starsRequired = 15),
        WordItem("opposites_far", "بعيد", "🛰️", "opposites", 3, wordEn = "Far", starsRequired = 20),
        WordItem("opposites_heavy", "تقيل", "🏋️", "opposites", 3, wordEn = "Heavy", starsRequired = 25),
        WordItem("opposites_light2", "خفيف", "🪶", "opposites", 3, wordEn = "Light", starsRequired = 30),
        WordItem("opposites_old", "قديم", "🕰️", "opposites", 3, wordEn = "Old", starsRequired = 35),
        WordItem("opposites_new", "جديد", "✨", "opposites", 3, wordEn = "New", starsRequired = 40),
        WordItem("opposites_clean2", "نضيف", "🧽", "opposites", 3, wordEn = "Clean", starsRequired = 45),
        WordItem("opposites_dirty", "وسخ", "💩", "opposites", 3, wordEn = "Dirty", starsRequired = 50),

        // school
        WordItem("school_pencil", "قلم رصاص", "✏️", "school", 1, wordEn = "Pencil", starsRequired = 0),
        WordItem("school_pen", "قلم حبر", "🖊️", "school", 1, wordEn = "Pen", starsRequired = 0),
        WordItem("school_book", "كتاب", "📚", "school", 2, wordEn = "Book", starsRequired = 0),
        WordItem("school_bag", "شنطة", "🎒", "school", 2, wordEn = "Bag", starsRequired = 0),
        WordItem("school_ruler", "مسطرة", "📏", "school", 2, wordEn = "Ruler", starsRequired = 0),
        WordItem("school_eraser", "استيكة", "🧼", "school", 2, wordEn = "Eraser", starsRequired = 0),
        WordItem("school_scissors", "مقص", "✂️", "school", 2, wordEn = "Scissors", starsRequired = 0),
        WordItem("school_glue", "صمغ", "🧴", "school", 3, wordEn = "Glue", starsRequired = 5),
        WordItem("school_notebook", "كراسة", "📓", "school", 3, wordEn = "Notebook", starsRequired = 10),
        WordItem("school_crayons", "ألوان", "🖍️", "school", 3, wordEn = "Crayons", starsRequired = 15),
        WordItem("school_desk", "مكتب", "🪑", "school", 3, wordEn = "Desk", starsRequired = 20),
        WordItem("school_board", "سبورة", "🖇️", "school", 3, wordEn = "Board", starsRequired = 25),
        WordItem("school_classroom", "فصل", "🏫", "school", 3, wordEn = "Classroom", starsRequired = 30),

        // seasons
        WordItem("seasons_spring", "الربيع", "🌷", "seasons", 1, wordEn = "Spring", starsRequired = 0),
        WordItem("seasons_summer", "الصيف", "☀️", "seasons", 1, wordEn = "Summer", starsRequired = 0),
        WordItem("seasons_autumn", "الخريف", "🍂", "seasons", 2, wordEn = "Autumn", starsRequired = 5),
        WordItem("seasons_winter", "الشتا", "❄️", "seasons", 2, wordEn = "Winter", starsRequired = 10),

        // shapes
        WordItem("shape_circle", "دائرة", "⭕", "shapes", 1, wordEn = "Circle", starsRequired = 0),
        WordItem("shape_square", "مربع", "⬛", "shapes", 1, wordEn = "Square", starsRequired = 0),
        WordItem("shape_triangle", "مثلث", "🔺", "shapes", 2, wordEn = "Triangle", starsRequired = 0),
        WordItem("shape_star", "نجمة", "⭐", "shapes", 2, wordEn = "Star", starsRequired = 0),
        WordItem("shape_heart", "قلب", "❤️", "shapes", 2, wordEn = "Heart", starsRequired = 0),
        WordItem("shape_diamond", "معين", "🔶", "shapes", 2, wordEn = "Diamond", starsRequired = 0),
        WordItem("shape_oval", "بيضاوي", "⭕", "shapes", 2, wordEn = "Oval", starsRequired = 0),
        WordItem("shape_rectangle", "مستطيل", "▬", "shapes", 3, wordEn = "Rectangle", starsRequired = 0),
        WordItem("shape_cube", "مكعب", "🧊", "shapes", 3, wordEn = "Cube", starsRequired = 5),
        WordItem("shape_pentagon", "خماسي", "⬠", "shapes", 3, wordEn = "Pentagon", starsRequired = 10),
        WordItem("shape_hexagon", "سداسي", "⬡", "shapes", 3, wordEn = "Hexagon", starsRequired = 15),
        WordItem("shape_cone", "مخروط", "🔺", "shapes", 3, wordEn = "Cone", starsRequired = 20),
        WordItem("shape_arrow", "سهم", "➡️", "shapes", 3, wordEn = "Arrow", starsRequired = 25),
        WordItem("shape_crescent", "هلال", "🌙", "shapes", 3, wordEn = "Crescent", starsRequired = 30),
        WordItem("shape_spiral", "حلزوني", "🌀", "shapes", 3, wordEn = "Spiral", starsRequired = 35),

        // shopping
        WordItem("shopping_money", "فلوس", "💵", "shopping", 1, wordEn = "Money", starsRequired = 0),
        WordItem("shopping_coin", "قرش", "🪙", "shopping", 1, wordEn = "Coin", starsRequired = 0),
        WordItem("shopping_shop", "محل", "🏪", "shopping", 2, wordEn = "Shop", starsRequired = 0),
        WordItem("shopping_market", "سوق", "🏬", "shopping", 2, wordEn = "Market", starsRequired = 5),
        WordItem("shopping_cart", "عربية تسوق", "🛒", "shopping", 2, wordEn = "Cart", starsRequired = 10),
        WordItem("shopping_price", "سعر", "🏷️", "shopping", 2, wordEn = "Price", starsRequired = 15),

        // situations
        WordItem("situation_sleep", "عايز أنام", "😴", "situations", 1, wordEn = "I want to sleep", starsRequired = 0, animationStyle = "sleep"),
        WordItem("situation_eat", "عايز آكل", "🍽️", "situations", 1, wordEn = "I want to eat", starsRequired = 0, animationStyle = "bounce"),
        WordItem("situation_drink", "عايز أشرب", "🥤", "situations", 2, wordEn = "I want to drink", starsRequired = 0, animationStyle = "bounce"),
        WordItem("situation_play", "عايز ألعب", "🧸", "situations", 2, wordEn = "I want to play", starsRequired = 0, animationStyle = "wiggle"),
        WordItem("situation_bathroom", "عايز الحمام", "🚽", "situations", 2, wordEn = "I need the bathroom", starsRequired = 0, animationStyle = "shake"),
        WordItem("situation_tired", "أنا تعبان", "😪", "situations", 2, wordEn = "I'm tired", starsRequired = 0, animationStyle = "sleep"),
        WordItem("situation_scared", "أنا خايف", "😨", "situations", 2, wordEn = "I'm scared", starsRequired = 0, animationStyle = "shake"),
        WordItem("situation_hug", "عايز حضن", "🤗", "situations", 3, wordEn = "I want a hug", starsRequired = 0, animationStyle = "wiggle"),
        WordItem("situation_tummyhurt", "بطني بتوجعني", "🤕", "situations", 3, wordEn = "My tummy hurts", starsRequired = 0, animationStyle = "shake"),
        WordItem("situation_outside", "عايز أطلع برا", "🌳", "situations", 3, wordEn = "I want to go outside", starsRequired = 0, animationStyle = "wiggle"),
        WordItem("situation_bored", "أنا زهقان", "😑", "situations", 3, wordEn = "I'm bored", starsRequired = 5, animationStyle = "sleep"),
        WordItem("situation_wantmom", "عايز ماما", "👩", "situations", 3, wordEn = "I want mommy", starsRequired = 10, animationStyle = "wiggle"),
        WordItem("situation_wantdad", "عايز بابا", "👨", "situations", 3, wordEn = "I want daddy", starsRequired = 15, animationStyle = "wiggle"),
        WordItem("situation_cold", "أنا برداي", "🥶", "situations", 3, wordEn = "I'm cold", starsRequired = 20, animationStyle = "shake"),
        WordItem("situation_hot", "أنا حراني", "🥵", "situations", 3, wordEn = "I'm hot", starsRequired = 25, animationStyle = "shake"),
        WordItem("situation_hurt", "أنا موجوع", "🤒", "situations", 3, wordEn = "Something hurts", starsRequired = 30, animationStyle = "shake"),
        WordItem("situation_dress", "عايز ألبس", "👕", "situations", 3, wordEn = "I want to get dressed", starsRequired = 35, animationStyle = "bounce"),
        WordItem("situation_done", "خلصت", "✅", "situations", 3, wordEn = "I'm done", starsRequired = 40, animationStyle = "bounce"),
        WordItem("situation_help", "عايز مساعدة", "🆘", "situations", 3, wordEn = "I need help", starsRequired = 45, animationStyle = "shake"),
        WordItem("situation_happy", "أنا مبسوط", "😊", "situations", 3, wordEn = "I'm happy", starsRequired = 50, animationStyle = "wiggle"),

        // space
        WordItem("space_planet", "كوكب", "🪐", "space", 1, wordEn = "Planet", starsRequired = 0),
        WordItem("space_earth", "الأرض", "🌍", "space", 1, wordEn = "Earth", starsRequired = 0),
        WordItem("space_mars", "المريخ", "🔴", "space", 2, wordEn = "Mars", starsRequired = 0),
        WordItem("space_astronaut", "رائد فضاء", "👨‍🚀", "space", 2, wordEn = "Astronaut", starsRequired = 0),
        WordItem("space_spaceship", "سفينة فضاء", "🛸", "space", 2, wordEn = "Spaceship", starsRequired = 0),
        WordItem("space_galaxy", "مجرة", "🌌", "space", 2, wordEn = "Galaxy", starsRequired = 5),
        WordItem("space_comet", "مذنب", "☄️", "space", 2, wordEn = "Comet", starsRequired = 10),
        WordItem("space_satellite", "قمر صناعي", "🛰️", "space", 3, wordEn = "Satellite", starsRequired = 15),
        WordItem("space_telescope", "تليسكوب", "🔭", "space", 3, wordEn = "Telescope", starsRequired = 20),
        WordItem("space_alien", "كائن فضائي", "👽", "space", 3, wordEn = "Alien", starsRequired = 25),

        // sports
        WordItem("sports_football", "كورة قدم", "⚽", "sports", 1, wordEn = "Football", starsRequired = 0),
        WordItem("sports_basketball", "كرة سلة", "🏀", "sports", 1, wordEn = "Basketball", starsRequired = 0),
        WordItem("sports_swimming", "سباحة", "🏊", "sports", 2, wordEn = "Swimming", starsRequired = 0),
        WordItem("sports_running", "جري", "🏃", "sports", 2, wordEn = "Running", starsRequired = 0),
        WordItem("sports_cycling", "ركوب عجل", "🚴", "sports", 2, wordEn = "Cycling", starsRequired = 0),
        WordItem("sports_tennis", "تنس", "🎾", "sports", 2, wordEn = "Tennis", starsRequired = 0),
        WordItem("sports_goal", "جون", "🥅", "sports", 2, wordEn = "Goal", starsRequired = 0),
        WordItem("sports_race", "سباق", "🏁", "sports", 3, wordEn = "Race", starsRequired = 5),
        WordItem("sports_gymnastics", "جمباز", "🤸", "sports", 3, wordEn = "Gymnastics", starsRequired = 10),
        WordItem("sports_karate", "كاراتيه", "🥋", "sports", 3, wordEn = "Karate", starsRequired = 15),
        WordItem("sports_skating", "تزلج", "⛸️", "sports", 3, wordEn = "Skating", starsRequired = 20),
        WordItem("sports_boxing", "ملاكمة", "🥊", "sports", 3, wordEn = "Boxing", starsRequired = 25),
        WordItem("sports_volleyball", "كرة طائرة", "🏐", "sports", 3, wordEn = "Volleyball", starsRequired = 30),
        WordItem("sports_pingpong", "تنس طاولة", "🏓", "sports", 3, wordEn = "Ping Pong", starsRequired = 35),

        // time
        WordItem("time_sunday", "الأحد", "🗓️", "time", 1, wordEn = "Sunday", starsRequired = 0),
        WordItem("time_monday", "الإتنين", "🗓️", "time", 1, wordEn = "Monday", starsRequired = 0),
        WordItem("time_tuesday", "التلات", "🗓️", "time", 2, wordEn = "Tuesday", starsRequired = 0),
        WordItem("time_wednesday", "الأربع", "🗓️", "time", 2, wordEn = "Wednesday", starsRequired = 0),
        WordItem("time_thursday", "الخميس", "🗓️", "time", 2, wordEn = "Thursday", starsRequired = 0),
        WordItem("time_friday", "الجمعة", "🗓️", "time", 2, wordEn = "Friday", starsRequired = 0),
        WordItem("time_saturday", "السبت", "🗓️", "time", 2, wordEn = "Saturday", starsRequired = 0),
        WordItem("time_morning", "الصبح", "🌅", "time", 3, wordEn = "Morning", starsRequired = 5),
        WordItem("time_noon", "الضهر", "🌞", "time", 3, wordEn = "Noon", starsRequired = 10),
        WordItem("time_night2", "الليل", "🌙", "time", 3, wordEn = "Night", starsRequired = 15),
        WordItem("time_today", "النهارده", "📅", "time", 3, wordEn = "Today", starsRequired = 20),
        WordItem("time_tomorrow", "بكرة", "➡️", "time", 3, wordEn = "Tomorrow", starsRequired = 25),
        WordItem("time_yesterday", "إمبارح", "⬅️", "time", 3, wordEn = "Yesterday", starsRequired = 30),

        // tools
        WordItem("tools_hammer", "شاكوش", "🔨", "tools", 1, wordEn = "Hammer", starsRequired = 0),
        WordItem("tools_screwdriver", "مفك", "🪛", "tools", 1, wordEn = "Screwdriver", starsRequired = 0),
        WordItem("tools_lock", "قفل", "🔒", "tools", 2, wordEn = "Lock", starsRequired = 0),
        WordItem("tools_umbrella", "شمسية", "☂️", "tools", 2, wordEn = "Umbrella", starsRequired = 0),
        WordItem("tools_wallet", "محفظة", "👛", "tools", 2, wordEn = "Wallet", starsRequired = 0),
        WordItem("tools_camera", "كاميرا", "📷", "tools", 2, wordEn = "Camera", starsRequired = 0),
        WordItem("tools_watch", "ساعة يد", "⌚", "tools", 2, wordEn = "Watch", starsRequired = 5),
        WordItem("tools_flashlight", "كشاف", "🔦", "tools", 3, wordEn = "Flashlight", starsRequired = 10),
        WordItem("tools_rope", "حبل", "🪢", "tools", 3, wordEn = "Rope", starsRequired = 15),
        WordItem("tools_ladder", "سلم نقال", "🪜", "tools", 3, wordEn = "Ladder", starsRequired = 20),
        WordItem("tools_broom", "مكنسة", "🧹", "tools", 3, wordEn = "Broom", starsRequired = 25),
        WordItem("tools_bucket", "جردل", "🪣", "tools", 3, wordEn = "Bucket", starsRequired = 30),

        // toys
        WordItem("toy_ball", "كورة", "⚽", "toys", 1, wordEn = "Ball", starsRequired = 0),
        WordItem("toy_doll", "عروسة", "🪆", "toys", 1, wordEn = "Doll", starsRequired = 0),
        WordItem("toy_car", "عربية لعب", "🚙", "toys", 2, wordEn = "Toy car", starsRequired = 0),
        WordItem("toy_kite", "طيارة ورق", "🪁", "toys", 2, wordEn = "Kite", starsRequired = 0),
        WordItem("toy_blocks", "مكعبات", "🧱", "toys", 2, wordEn = "Blocks", starsRequired = 0),
        WordItem("toy_puzzle", "بازل", "🧩", "toys", 2, wordEn = "Puzzle", starsRequired = 0),
        WordItem("toy_teddy", "دبدوب", "🧸", "toys", 2, wordEn = "Teddy Bear", starsRequired = 0),
        WordItem("toy_robot", "روبوت", "🤖", "toys", 3, wordEn = "Robot", starsRequired = 0),
        WordItem("toy_yoyo", "يويو", "🪀", "toys", 3, wordEn = "Yo-yo", starsRequired = 0),
        WordItem("toy_balloon", "بالونة", "🎈", "toys", 3, wordEn = "Balloon", starsRequired = 0),
        WordItem("toy_skateboard", "سكيت بورد", "🛹", "toys", 3, wordEn = "Skateboard", starsRequired = 5),
        WordItem("toy_jumprope", "حبل نط", "🪢", "toys", 3, wordEn = "Jump Rope", starsRequired = 10),
        WordItem("toy_bubbles", "فقاقيع", "🫧", "toys", 3, wordEn = "Bubbles", starsRequired = 15),
        WordItem("toy_playdough", "صلصال", "🟠", "toys", 3, wordEn = "Playdough", starsRequired = 20),
        WordItem("toy_drone", "درون", "🚁", "toys", 3, wordEn = "Drone", starsRequired = 25),
        WordItem("toy_dollhouse", "بيت عرايس", "🏠", "toys", 3, wordEn = "Dollhouse", starsRequired = 30),
        WordItem("toy_pogo", "نطاطة", "🦘", "toys", 3, wordEn = "Pogo Stick", starsRequired = 35),
        WordItem("toy_kitchenset", "طقم مطبخ لعب", "🍳", "toys", 3, wordEn = "Kitchen Playset", starsRequired = 40),
        WordItem("toy_actionfigure", "دمية أكشن", "🦸", "toys", 3, wordEn = "Action Figure", starsRequired = 45),
        WordItem("toy_spintop", "فرفوطة", "🌀", "toys", 3, wordEn = "Spinning Top", starsRequired = 50),

        // transport
        WordItem("transport_car", "عربية", "🚗", "transport", 1, wordEn = "Car", starsRequired = 0),
        WordItem("transport_bus", "أتوبيس", "🚌", "transport", 1, wordEn = "Bus", starsRequired = 0),
        WordItem("transport_bike", "عجلة", "🚲", "transport", 2, wordEn = "Bike", starsRequired = 0),
        WordItem("transport_train", "قطار", "🚂", "transport", 2, wordEn = "Train", starsRequired = 0),
        WordItem("transport_plane", "طيارة", "✈️", "transport", 2, wordEn = "Plane", starsRequired = 0),
        WordItem("transport_boat", "مركب", "⛵", "transport", 2, wordEn = "Boat", starsRequired = 0),
        WordItem("transport_ship", "سفينة", "🚢", "transport", 2, wordEn = "Ship", starsRequired = 0),
        WordItem("transport_motorcycle", "موتوسيكل", "🏍️", "transport", 3, wordEn = "Motorcycle", starsRequired = 0),
        WordItem("transport_truck", "عربية نقل", "🚚", "transport", 3, wordEn = "Truck", starsRequired = 0),
        WordItem("transport_taxi", "تاكسي", "🚕", "transport", 3, wordEn = "Taxi", starsRequired = 0),
        WordItem("transport_ambulance", "إسعاف", "🚑", "transport", 3, wordEn = "Ambulance", starsRequired = 0),
        WordItem("transport_firetruck", "عربية إطفاء", "🚒", "transport", 3, wordEn = "Fire Truck", starsRequired = 5),
        WordItem("transport_helicopter", "هليكوبتر", "🚁", "transport", 3, wordEn = "Helicopter", starsRequired = 10),
        WordItem("transport_tractor", "جرار", "🚜", "transport", 3, wordEn = "Tractor", starsRequired = 15),
        WordItem("transport_submarine", "غواصة", "🤿", "transport", 3, wordEn = "Submarine", starsRequired = 20),
        WordItem("transport_rocket", "صاروخ", "🚀", "transport", 3, wordEn = "Rocket", starsRequired = 25),
        WordItem("transport_scooter", "سكوتر", "🛴", "transport", 3, wordEn = "Scooter", starsRequired = 30),
        WordItem("transport_metro", "مترو", "🚇", "transport", 3, wordEn = "Metro", starsRequired = 35),
        WordItem("transport_microbus", "ميكروباص", "🚐", "transport", 3, wordEn = "Microbus", starsRequired = 40),
        WordItem("transport_felucca", "فلوكة", "⛵", "transport", 3, wordEn = "Felucca", starsRequired = 45),
        WordItem("transport_balloon2", "منطاد", "🎈", "transport", 3, wordEn = "Hot Air Balloon", starsRequired = 50),

        // vegetables
        WordItem("vegetables_tomato", "طماطم", "🍅", "vegetables", 1, wordEn = "Tomato", starsRequired = 0),
        WordItem("vegetables_potato", "بطاطس", "🥔", "vegetables", 1, wordEn = "Potato", starsRequired = 0),
        WordItem("vegetables_carrot", "جزر", "🥕", "vegetables", 2, wordEn = "Carrot", starsRequired = 0),
        WordItem("vegetables_cucumber", "خيار", "🥒", "vegetables", 2, wordEn = "Cucumber", starsRequired = 0),
        WordItem("vegetables_onion", "بصلة", "🧅", "vegetables", 2, wordEn = "Onion", starsRequired = 0),
        WordItem("vegetables_garlic", "توم", "🧄", "vegetables", 2, wordEn = "Garlic", starsRequired = 0),
        WordItem("vegetables_pepper", "فلفل", "🫑", "vegetables", 2, wordEn = "Pepper", starsRequired = 0),
        WordItem("vegetables_eggplant", "باذنجان", "🍆", "vegetables", 3, wordEn = "Eggplant", starsRequired = 0),
        WordItem("vegetables_corn", "ذرة", "🌽", "vegetables", 3, wordEn = "Corn", starsRequired = 0),
        WordItem("vegetables_peas", "بسلة", "🫛", "vegetables", 3, wordEn = "Peas", starsRequired = 5),
        WordItem("vegetables_lettuce", "خس", "🥬", "vegetables", 3, wordEn = "Lettuce", starsRequired = 10),
        WordItem("vegetables_pumpkin", "قرع", "🎃", "vegetables", 3, wordEn = "Pumpkin", starsRequired = 15),
        WordItem("vegetables_broccoli", "بروكلي", "🥦", "vegetables", 3, wordEn = "Broccoli", starsRequired = 20),
        WordItem("vegetables_cabbage", "كرنب", "🥬", "vegetables", 3, wordEn = "Cabbage", starsRequired = 25),
        WordItem("vegetables_mushroom", "مشروم", "🍄", "vegetables", 3, wordEn = "Mushroom", starsRequired = 30),
        WordItem("vegetables_beans", "فاصوليا", "🫘", "vegetables", 3, wordEn = "Beans", starsRequired = 35),
        WordItem("vegetables_radish", "فجل", "🌶️", "vegetables", 3, wordEn = "Radish", starsRequired = 40),
        WordItem("vegetables_cauliflower", "قرنبيط", "🥦", "vegetables", 3, wordEn = "Cauliflower", starsRequired = 45),

        // weather
        WordItem("weather_sunny", "مشمس", "☀️", "weather", 1, wordEn = "Sunny", starsRequired = 0),
        WordItem("weather_rainy", "ماطر", "🌧️", "weather", 1, wordEn = "Rainy", starsRequired = 0),
        WordItem("weather_cloudy", "غايم", "☁️", "weather", 2, wordEn = "Cloudy", starsRequired = 0),
        WordItem("weather_windy", "فيه هوا", "💨", "weather", 2, wordEn = "Windy", starsRequired = 0),
        WordItem("weather_snowy", "فيه تلج", "❄️", "weather", 2, wordEn = "Snowy", starsRequired = 0),
        WordItem("weather_hot", "حر", "🥵", "weather", 2, wordEn = "Hot", starsRequired = 5),
        WordItem("weather_cold", "برد", "🥶", "weather", 2, wordEn = "Cold", starsRequired = 10),
        WordItem("weather_foggy", "شبورة", "🌫️", "weather", 3, wordEn = "Foggy", starsRequired = 15),
        WordItem("weather_stormy", "عاصف", "🌪️", "weather", 3, wordEn = "Stormy", starsRequired = 20),
    
    )

    // The Arabic alphabet, complete (28 letters). Word is the letter's name
    // (spoken form), which reads far better through TTS than the bare glyph.
    val arabicLetters = listOf(
        // "أَلِف" is deliberately written with diacritics: the bare spelling
        // "ألف" is identical to the Arabic word for "one thousand", and TTS
        // engines read it that way — so the letter was being pronounced as
        // a number. The diacritics force the letter-name reading.
        "أ" to "أَلِف", "ب" to "باء", "ت" to "تاء", "ث" to "ثاء", "ج" to "جيم",
        "ح" to "حاء", "خ" to "خاء", "د" to "دال", "ذ" to "ذال", "ر" to "راء",
        "ز" to "زاي", "س" to "سين", "ش" to "شين", "ص" to "صاد", "ض" to "ضاد",
        "ط" to "طاء", "ظ" to "ظاء", "ع" to "عين", "غ" to "غين", "ف" to "فاء",
        "ق" to "قاف", "ك" to "كاف", "ل" to "لام", "م" to "ميم", "ن" to "نون",
        "ه" to "هاء", "و" to "واو", "ي" to "ياء"
    ).mapIndexed { index, (glyph, name) ->
        // Same 50%-of-category-free pattern as every other category: half
        // the alphabet (14 of 28 letters) free, the other half +5 stars
        // each — a real challenge, not every letter unlocked from day one.
        val freeCount = 14
        val stars = if (index < freeCount) 0 else 5 * (index - freeCount + 1)
        WordItem("letter_ar_$index", name, glyph, "letters", if (stars == 0) 1 else 2, starsRequired = stars)
    }

    // The English alphabet, A-Z — only shown when the app language is English.
    val englishLetters = ('A'..'Z').mapIndexed { index, letter ->
        val freeCount = 13
        val stars = if (index < freeCount) 0 else 5 * (index - freeCount + 1)
        WordItem(
            id = "letter_en_$index",
            word = letter.toString(),
            emoji = letter.toString(), // the actual letter itself, not a generic icon — matches how Arabic letters already work below
            category = "letters",
            wordEn = letter.toString(),
            starsRequired = stars
        )
    }

    /** Words for a category in the given language. "letters" has an entirely separate item set per language. */
    fun wordsFor(categoryId: String, english: Boolean = false): List<WordItem> =
        if (categoryId == "letters") {
            if (english) englishLetters else arabicLetters
        } else {
            words.filter { it.category == categoryId }
        }
}
