/*
 * Cover image composable: loads [Artwork] through Coil and shows the generated gradient
 * placeholder while loading or when no cover exists.
 */
package com.music.player.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import coil3.compose.AsyncImage
import com.music.player.core.model.Artwork

/**
 * Square-agnostic cover. Decorative: it has no content description because the adjacent
 * text always names the item.
 *
 * @param title used to derive the placeholder.
 * @param initialsStyle placeholder initials style; null hides initials for tiny tiles.
 */
@Composable
fun ArtworkImage(
    artwork: Artwork?,
    title: String,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    initialsStyle: TextStyle? = MaterialTheme.typography.titleMedium,
) {
    var loaded by remember(artwork?.key) { mutableStateOf(false) }
    Box(modifier.clip(shape)) {
        if (!loaded) ArtworkPlaceholder(title = title, modifier = Modifier.fillMaxSize(), textStyle = initialsStyle)
        if (artwork != null) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { loaded = true },
                onError = { loaded = false },
            )
        }
    }
}
