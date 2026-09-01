package com.babakids.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babakids.app.data.CustomReward
import com.babakids.app.data.CustomRewardsRepository
import com.babakids.app.data.Sticker
import com.babakids.app.data.StickerCollection
import com.babakids.app.ui.theme.BaBaGradients
import com.babakids.app.ui.theme.GlossyCard
import com.babakids.app.ui.theme.rememberAdaptiveColumns

/**
 * Primary content here is now the parent's real-world rewards ("ice cream
 * at 20 stars!") — more personally motivating than a generic sticker.
 * The sticker friends collection is kept as a secondary section below,
 * still unlockable the same way, for whenever there's no custom reward
 * set at a given star count.
 */
@Composable
fun StickerCollectionScreen(stars: Int, english: Boolean = false, onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val customRewardsRepository = remember { CustomRewardsRepository(context) }
    val customRewards by customRewardsRepository.rewardsFlow.collectAsState(initial = emptyList())
    val columns = rememberAdaptiveColumns(baseColumns = 3)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { BackTopBar(onBack = onBack) }

        item {
            Text(
                if (english) "🎁 My Rewards" else "🎁 مكافآتي",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (customRewards.isEmpty()) {
            item {
                Text(
                    if (english)
                        "No rewards set up yet — ask a parent to add one from Parent Mode!"
                    else
                        "لسه مفيش مكافآت متضافة — اطلب من ماما أو بابا يضيفوا واحدة من لوحة الوالدين!",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(customRewards) { reward ->
                RewardRow(
                    reward = reward,
                    unlocked = stars >= reward.starsRequired,
                    english = english,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        item {
            Text(
                if (english) "🧸 My Sticker Friends" else "🧸 أصدقائي",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                if (english) "A new friend every 10 stars" else "صديق جديد كل 10 نجوم",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        val stickerRows = StickerCollection.all.chunked(columns)
        items(stickerRows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { sticker ->
                    StickerTile(
                        sticker = sticker,
                        stars = stars,
                        english = english,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Pad the last row so tiles keep a consistent size.
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun RewardRow(reward: CustomReward, unlocked: Boolean, english: Boolean, modifier: Modifier = Modifier) {
    GlossyCard(
        gradient = if (unlocked) BaBaGradients.leaf else BaBaGradients.sky,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    (if (unlocked) "🎉 " else "🔒 ") + reward.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                reward.earnedAt?.let { timestamp ->
                    Text(
                        (if (english) "Earned: " else "اتحققت: ") + formatRewardTimestamp(timestamp),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
            Text(
                "${reward.starsRequired} ⭐",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun StickerTile(sticker: Sticker, stars: Int, english: Boolean, modifier: Modifier = Modifier) {
    val unlocked = stars >= sticker.starsRequired
    GlossyCard(
        gradient = if (unlocked) BaBaGradients.leaf else BaBaGradients.sky,
        modifier = modifier.aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(if (unlocked) sticker.emoji else "🔒", fontSize = 34.sp)
            Text(
                if (unlocked) sticker.displayName(english) else "${sticker.starsRequired} ⭐",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}
