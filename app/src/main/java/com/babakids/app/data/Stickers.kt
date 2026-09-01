package com.babakids.app.data

/** A collectible reward unlocked once the child reaches `starsRequired` total stars. */
data class Sticker(
    val emoji: String,
    val nameAr: String,
    val nameEn: String,
    val starsRequired: Int
) {
    fun displayName(english: Boolean): String = if (english) nameEn else nameAr
}

object StickerCollection {
    val all = listOf(
        Sticker("🐶", "جرو صغير", "Puppy", 10),
        Sticker("🐱", "قطة مرحة", "Kitty", 20),
        Sticker("🦁", "أسد شجاع", "Lion", 30),
        Sticker("🐰", "أرنب لطيف", "Bunny", 40),
        Sticker("🦋", "فراشة", "Butterfly", 50),
        Sticker("🐢", "سلحفاة", "Turtle", 60),
        Sticker("🦄", "يونيكورن", "Unicorn", 70),
        Sticker("🐳", "حوت", "Whale", 80),
        Sticker("🦊", "ثعلب", "Fox", 90),
        Sticker("🐼", "باندا", "Panda", 100),
        Sticker("🦖", "دَيْنَصُور", "Dino", 110),
        Sticker("👑", "تاج البطل", "Champion Crown", 120)
    )

    fun unlockedFor(stars: Int): List<Sticker> = all.filter { stars >= it.starsRequired }
    fun lockedFor(stars: Int): List<Sticker> = all.filter { stars < it.starsRequired }
}
