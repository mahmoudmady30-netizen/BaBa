package com.babakids.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The circular badge behind every category/word icon. Upgraded to read as
 * a glossy glass/3D sphere rather than a flat tinted circle: a layered
 * shadow (soft coloured glow + a tighter dark shadow for real lift off
 * the card), a richer radial gradient with more contrast between its
 * bright center and dim edge, a crisp rim-light border, and a small
 * off-center specular highlight ellipse — the single detail that reads
 * as "glass/glossy sphere" to the eye, the same way a highlight dot sells
 * a drawn marble or bubble.
 *
 * The emoji glyph itself also now gets a real drop-shadow (via
 * LocalTextStyle, so every Text(...emoji...) rendered inside this orb
 * picks it up automatically without touching each call site) — a soft
 * dark shadow just below-right of the glyph, which is what makes a flat
 * printed emoji start to read as sitting slightly raised off the
 * background instead of stuck flat to it.
 *
 * Honest limits: this can only push the *rendering* of the existing
 * system emoji glyph so far — it's still Android's own emoji font
 * underneath, not hand-illustrated 3D art. Apple's own polished 3D emoji
 * artwork is copyrighted and can't be bundled into this app, and
 * generating brand new original icon illustrations from scratch isn't
 * something this tool can do. For an exact match to a specific icon set,
 * the real path is the per-category "✏️ edit picture" feature already in
 * the app: a parent can drop in real icon art there and it replaces the
 * emoji entirely, in this exact orb.
 */
@Composable
fun PremiumIconOrb(
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationX = 8f; rotationY = -8f; cameraDistance = 16f * density }
            // Two shadow layers: a soft wide warm glow (the "lift off the
            // page" cue) plus the tighter default shadow for real depth
            // right at the edge, instead of one flat blur.
            .shadow(18.dp, CircleShape, clip = false, ambientColor = Color(0x33FFB300), spotColor = Color(0x40FF7043))
            .shadow(6.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        Color.White.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.08f)
                    )
                )
            )
            .border(1.2.dp, Color.White.copy(alpha = 0.9f), CircleShape)
    ) {
        // Specular highlight — a small soft-edged bright ellipse in the
        // upper-left, the single detail that sells "glossy sphere" at a
        // glance rather than "flat circle with a gradient."
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = size * 0.14f, y = size * 0.10f)
                .size(size * 0.34f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0f))
                    )
                )
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.35f),
                        offset = Offset(2f, 4f),
                        blurRadius = 6f
                    )
                )
            ) {
                content()
            }
        }
    }
}
