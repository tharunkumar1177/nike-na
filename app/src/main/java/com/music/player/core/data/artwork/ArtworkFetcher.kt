/*
 * Coil 3 integration for album covers: loads thumbnails through ContentResolver.loadThumbnail,
 * which returns embedded art at the requested size without decoding the full image.
 */
package com.music.player.core.data.artwork

import android.content.ContentResolver
import android.util.Size
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import coil3.size.pxOrElse
import com.music.player.core.model.Artwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Fetches one [Artwork]. Throws when the item has no cover so Coil shows the error slot (placeholder). */
class ArtworkFetcher(
    private val data: Artwork,
    private val options: Options,
    private val resolver: ContentResolver,
) : Fetcher {

    /** Loads the thumbnail sized to the request, defaulting to 512px when the size is undefined. */
    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        val width = options.size.width.pxOrElse { DEFAULT_PX }.coerceIn(1, MAX_PX)
        val height = options.size.height.pxOrElse { DEFAULT_PX }.coerceIn(1, MAX_PX)
        val bitmap = resolver.loadThumbnail(data.uri, Size(width, height), null)
        ImageFetchResult(image = bitmap.asImage(), isSampled = true, dataSource = DataSource.DISK)
    }

    /** Creates fetchers for [Artwork] requests. */
    class Factory(private val resolver: ContentResolver) : Fetcher.Factory<Artwork> {
        /** Returns a fetcher for [data]. */
        override fun create(data: Artwork, options: Options, imageLoader: ImageLoader): Fetcher =
            ArtworkFetcher(data, options, resolver)
    }

    private companion object {
        const val DEFAULT_PX = 512
        const val MAX_PX = 1024
    }
}

/** Memory-cache key for [Artwork]; Coil appends the request size. */
class ArtworkKeyer : Keyer<Artwork> {
    /** Returns the stable artwork key. */
    override fun key(data: Artwork, options: Options): String = data.key
}
