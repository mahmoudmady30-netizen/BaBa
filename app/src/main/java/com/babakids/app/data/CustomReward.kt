package com.babakids.app.data

/**
 * A real-world reward a parent sets up (e.g. "ice cream!"), unlocked at a
 * chosen star count. `earnedAt`, when set, is the moment (epoch millis)
 * the child actually reached that star count and the popup fired —
 * recorded once, not recalculated from the current star total, so it
 * stays accurate even if stars keep climbing afterward.
 */
data class CustomReward(
    val id: String,
    val title: String,
    val starsRequired: Int,
    val earnedAt: Long? = null
)
