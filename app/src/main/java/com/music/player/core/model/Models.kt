/*
 * Immutable domain types shared across the Music app: tracks, albums, artists, folders,
 * playlists, library access state, and sorting.
 * Identity follows architecture.md "Track identity": MediaStore ids for library items,
 * Room ids for playlists, and relative paths for folders.
 */
package com.music.player.core.model

/** One audio file indexed from MediaStore. [id] is `MediaStore.Audio.Media._ID`. */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val album: String,
    val albumId: Long,
    val albumArtist: String,
    val durationMs: Long,
    val trackNumber: Int,
    val discNumber: Int,
    val year: Int,
    val folderPath: String,
    val fileName: String,
    val mimeType: String,
    val dateAddedSec: Long,
    val sizeBytes: Long,
)

/** An album aggregated from cached tracks. [id] is MediaStore `ALBUM_ID`. */
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val year: Int,
    val trackCount: Int,
    val durationMs: Long,
    val dateAddedSec: Long,
)

/** An artist aggregated from cached tracks. [artworkAlbumId] picks an album cover to represent them. */
data class Artist(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
    val artworkAlbumId: Long,
)

/** A folder that directly contains indexed tracks. [path] is the MediaStore relative path, e.g. `Music/Rock/`. */
data class Folder(
    val path: String,
    val name: String,
    val trackCount: Int,
)

/** A user playlist stored in Room. */
data class Playlist(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val durationMs: Long,
    val updatedAtMs: Long,
)

/** One position in a playlist; [track] is null when the file is no longer on the device. */
data class PlaylistEntry(
    val entryId: Long,
    val trackId: Long,
    val position: Int,
    val track: Track?,
)

/** User preference for light or dark appearance. */
enum class ThemeMode { System, Light, Dark }

/** Whether the app may read the device's audio files. Audio has no partial-access mode. */
enum class LibraryAccess { Granted, Denied }

/** Fields a list can be sorted by. */
enum class SortField { Title, Artist, Album, DateAdded, Duration }

/** A sort choice: [field] and direction. */
data class SortSpec(val field: SortField = SortField.Title, val ascending: Boolean = true)

/** Library tabs that remember their own sort choice. */
enum class LibraryTab { Songs, Albums, Artists, Folders }

/** Grouped search results. */
data class SearchResults(
    val query: String,
    val tracks: List<Track>,
    val albums: List<Album>,
    val artists: List<Artist>,
) {
    /** True when no group has any result. */
    val isEmpty: Boolean get() = tracks.isEmpty() && albums.isEmpty() && artists.isEmpty()
}
