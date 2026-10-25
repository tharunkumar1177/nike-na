/*
 * Extracts a theme seed color from album artwork with androidx.palette, off the main thread,
 * cached per album id. Feeds MusicTheme's artwork-seeded scheme (architecture.md "Visual system").
 */
package com.music.player.core.ui.theme

import android.content.Context
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.music.player.core.model.Artwork
import com.music.player.core.model.MediaUris
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Process-wide cache of extracted seeds; a missing cover caches as null. */
private object SeedCache {
    private val cache = LruCache<Long, Optional>(128)

    /** Wrapper so a null seed can be cached. */
    class Optional(val color: Color?)

    /** Cached result for [albumId], or null if not yet computed. */
    fun get(albumId: Long): Optional? = cache.get(albumId)

    /** Stores the seed for [albumId]. */
    fun put(albumId: Long, color: Color?) {
        cache.put(albumId, Optional(color))
    }
}

/** Computes the seed color for [albumId]: vibrant swatch, then dominant; null when there is no cover. */
suspend fun extractSeed(context: Context, albumId: Long): Color? {
    SeedCache.get(albumId)?.let { return it.color }
    val request = ImageRequest.Builder(context)
        .data(Artwork(MediaUris.album(albumId), "album:$albumId"))
        .size(SEED_SAMPLE_PX)
        .allowHardware(false)
        .build()
    val result = SingletonImageLoader.get(context).execute(request)
    val color = (result as? SuccessResult)?.image?.toBitmap()?.let { bitmap ->
        withContext(Dispatchers.Default) {
            val palette = Palette.from(bitmap).maximumColorCount(16).generate()
            (palette.vibrantSwatch ?: palette.dominantSwatch)?.rgb?.let { Color(it) }
        }
    }
    SeedCache.put(albumId, color)
    return color
}

/** Seed color state for [albumId]; keeps the previous seed while the next one loads. */
@Composable
fun rememberArtworkSeed(albumId: Long?, enabled: Boolean): State<Color?> {
    val context = LocalContext.current
    return produceState<Color?>(initialValue = null, albumId, enabled) {
        value = if (!enabled || albumId == null || albumId < 0) null else extractSeed(context, albumId)
    }
}

private const val SEED_SAMPLE_PX = 128
