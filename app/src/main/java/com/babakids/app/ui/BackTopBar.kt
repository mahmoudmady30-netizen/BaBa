package com.babakids.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The premium circular back button from the app's theme — soft shadow,
 * white bevel edge, dark arrow glyph.
 *
 * Arrangement.End is deliberate: under RTL (Arabic), a Row's default
 * Start arrangement places the first child on the *right*, which is why
 * the button used to sit on the wrong side. End pins it to the visual
 * left in RTL, matching where the user asked for it.
 */
@Composable
fun BackTopBar(title: String? = null, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        title?.let {
            Text(it, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF26354A))
            Spacer(Modifier.width(12.dp))
        }
        Box(
            modifier = Modifier
                .size(46.dp)
                .shadow(8.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.95f))
                .border(1.dp, Color.White, CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) { Text("←", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF40516B)) }
    }
}
