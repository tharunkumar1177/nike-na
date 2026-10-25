/*
 * Type-safe navigation destinations.
 */
package com.music.player.ui

import kotlinx.serialization.Serializable

/** Library tabs root. */
@Serializable
data object LibraryRoute

/** Playlists root. */
@Serializable
data object PlaylistsRoute

/** Search root. */
@Serializable
data object SearchRoute

/** Settings root. */
@Serializable
data object SettingsRoute

/** Album detail. */
@Serializable
data class AlbumRoute(val albumId: Long)

/** Artist detail. */
@Serializable
data class ArtistRoute(val artistId: Long)

/** Folder detail by MediaStore relative path. */
@Serializable
data class FolderRoute(val path: String)

/** Playlist detail. */
@Serializable
data class PlaylistRoute(val playlistId: Long)

/** Favorites collection. */
@Serializable
data object FavoritesRoute
