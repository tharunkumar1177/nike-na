/*
 * Artwork request model: a content URI whose embedded or system thumbnail is the cover,
 * plus a stable cache key. Loaded by the Coil fetcher in core.data.artwork.
 */
package com.music.player.core.model

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

/** A cover image source. [key] is stable across sessions and used for image caching. */
data class Artwork(val uri: Uri, val key: String)

/** Builds content URIs for MediaStore identities. */
object MediaUris {
    /** Playable content URI of track [trackId]. */
    fun track(trackId: Long): Uri =
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId)

    /** Content URI of album [albumId], used for its cover thumbnail. */
    fun album(albumId: Long): Uri =
        ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)

    /** Openable album-art stream URI, used where a stream (not a thumbnail call) is required, e.g. notifications. */
    fun albumArtStream(albumId: Long): Uri =
        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
}

/** Cover for this track (its album cover, shared cache entry across the album). */
val Track.artwork: Artwork get() = Artwork(MediaUris.album(albumId), "album:$albumId")

/** Cover for this album. */
val Album.artwork: Artwork get() = Artwork(MediaUris.album(id), "album:$id")

/** Cover representing this artist. */
val Artist.artwork: Artwork get() = Artwork(MediaUris.album(artworkAlbumId), "album:$artworkAlbumId")

/** Playable content URI of this track. */
val Track.uri: Uri get() = MediaUris.track(id)
