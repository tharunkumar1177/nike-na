/*
 * Conversion between domain tracks and Media3 MediaItems, following architecture.md
 * "Track identity": mediaId is trackId.toString(); the URI is derived from the id.
 */
@file:OptIn(UnstableApi::class)

package com.music.player.core.playback

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.music.player.core.model.MediaUris
import com.music.player.core.model.Track
import com.music.player.core.model.uri

private const val EXTRA_ALBUM_ID = "albumId"

/** Fully playable item with metadata and artwork URI (used inside the service). */
fun Track.toMediaItem(): MediaItem {
    val extras = Bundle().apply { putLong(EXTRA_ALBUM_ID, albumId) }
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setAlbumArtist(albumArtist.ifEmpty { null })
        .setTrackNumber(trackNumber.takeIf { it > 0 })
        .setDurationMs(durationMs)
        .setArtworkUri(MediaUris.albumArtStream(albumId))
        .setIsPlayable(true)
        .setIsBrowsable(false)
        .setExtras(extras)
        .build()
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMimeType(mimeType.ifEmpty { null })
        .setMediaMetadata(metadata)
        .build()
}

/** Id-only request item sent from a controller; the service resolves it with [toMediaItem]. */
fun Track.toRequestItem(): MediaItem = MediaItem.Builder().setMediaId(id.toString()).build()

/** Track id of this item, or null if the mediaId is not a track id. */
val MediaItem.trackId: Long? get() = mediaId.toLongOrNull()

/** Album id stored in this item's metadata extras, or null. */
val MediaItem.albumId: Long? get() = mediaMetadata.extras?.getLong(EXTRA_ALBUM_ID, -1L)?.takeIf { it >= 0 }
