/*
 * Generated gradient artwork used when a track or album has no embedded art.
 * Colors and initials derive deterministically from the title so a given album always looks the same.
 */
package com.music.player.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/** Hue pair (degrees, 0 until 360) for a placeholder gradient. */
data class PlaceholderHues(val start: Float, val end: Float)

/** Pure placeholder rules, separated from Compose for unit testing. */
object PlaceholderArt {
    private const val HUE_OFFSET = 40
    const val SATURATION = 0.70f
    const val LIGHTNESS = 0.40f

    /** Returns the gradient hues for [title]: start = hash mod 360, end = start + 40. */
    fun hues(title: String): PlaceholderHues {
        val start = Math.floorMod(title.trim().lowercase().hashCode(), 360)
        return PlaceholderHues(start.toFloat(), ((start + HUE_OFFSET) % 360).toFloat())
    }

    /**
     * Returns up to two uppercase initials from the first letters of the words in [title],
     * skipping words that start with a non-letter/non-digit; returns "♪" when none qualify.
     */
    fun initials(title: String): String {
        val letters = title.split(Regex("\\s+"))
            .mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() } }
            .take(2)
            .joinToString("") { it.uppercase() }
        return letters.ifEmpty { "\u266A" }
    }
}

/**
 * Draws the placeholder for [title] filling [modifier]'s bounds.
 *
 * @param textStyle style for the initials; null hides them (for very small tiles).
 */
@Composable
fun ArtworkPlaceholder(
    title: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle? = MaterialTheme.typography.titleLarge,
) {
    val brush = remember(title) {
        val hues = PlaceholderArt.hues(title)
        Brush.linearGradient(
            listOf(
                Color.hsl(hues.start, PlaceholderArt.SATURATION, PlaceholderArt.LIGHTNESS),
                Color.hsl(hues.end, PlaceholderArt.SATURATION, PlaceholderArt.LIGHTNESS),
            ),
        )
    }
    Box(modifier = modifier.background(brush), contentAlignment = Alignment.Center) {
        if (textStyle != null) {
            Text(text = remember(title) { PlaceholderArt.initials(title) }, style = textStyle, color = Color.White)
        }
    }
}
