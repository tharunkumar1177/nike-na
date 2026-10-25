/*
 * Playlists and favorites: app-owned collections in Room that reference tracks by id.
 * Entries for tracks no longer on the device stay and are reported with a null track.
 */
package com.music.player.core.data

import com.music.player.core.data.db.LibraryDao
import com.music.player.core.data.db.PlaylistDao
import com.music.player.core.data.db.PlaylistEntity
import com.music.player.core.model.Playlist
import com.music.player.core.model.PlaylistEntry
import com.music.player.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

/** Captured state of a deleted playlist, for undo. */
data class DeletedPlaylist(val row: PlaylistEntity, val trackIds: List<Long>)

/** Playlist and favorites operations. */
class PlaylistRepository(
    private val playlists: PlaylistDao,
    private val library: LibraryDao,
) {
    /** All playlists, most recently changed first. */
    val all: Flow<List<Playlist>> = playlists.observePlaylists()

    /** Favorite track ids, newest first. */
    val favoriteIds: Flow<Set<Long>> = playlists.observeFavoriteIds().map { it.toSet() }

    /** Favorite tracks that are still on the device, newest favorite first. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val favoriteTracks: Flow<List<Track>> = combine(playlists.observeFavoriteIds(), library.observeTrackCount()) { ids, _ -> ids }
        .mapLatest { ids -> library.tracksByIds(ids) }

    /** One playlist, or null once deleted. */
    fun playlist(id: Long): Flow<Playlist?> = playlists.observePlaylist(id)

    /** Ordered entries of a playlist with resolved tracks. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun entries(id: Long): Flow<List<PlaylistEntry>> = playlists.observeEntries(id).mapLatest { rows ->
        val byId = library.tracksByIds(rows.map { it.trackId }.distinct()).associateBy { it.id }
        rows.map { PlaylistEntry(it.entryId, it.trackId, it.position, byId[it.trackId]) }
    }

    /** Creates a playlist named [name] containing [trackIds]; returns its id. */
    suspend fun create(name: String, trackIds: List<Long> = emptyList()): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val id = playlists.insertPlaylist(PlaylistEntity(name = name.trim(), createdAtMs = now, updatedAtMs = now))
        if (trackIds.isNotEmpty()) playlists.append(id, trackIds, now)
        id
    }

    /** Renames a playlist. */
    suspend fun rename(id: Long, name: String) = withContext(Dispatchers.IO) {
        playlists.rename(id, name.trim(), System.currentTimeMillis())
    }

    /** Appends tracks to a playlist. */
    suspend fun add(id: Long, trackIds: List<Long>) = withContext(Dispatchers.IO) {
        if (trackIds.isNotEmpty()) playlists.append(id, trackIds, System.currentTimeMillis())
    }

    /** Replaces the playlist order with [trackIds] (reorder, remove, undo). */
    suspend fun setOrder(id: Long, trackIds: List<Long>) = withContext(Dispatchers.IO) {
        playlists.replaceEntries(id, trackIds, System.currentTimeMillis())
    }

    /** Deletes a playlist and returns what is needed to restore it. */
    suspend fun delete(id: Long): DeletedPlaylist? = withContext(Dispatchers.IO) {
        val row = playlists.playlistRow(id) ?: return@withContext null
        val ids = playlists.entries(id).map { it.trackId }
        playlists.deletePlaylist(id)
        DeletedPlaylist(row, ids)
    }

    /** Restores a playlist removed by [delete]. */
    suspend fun restore(deleted: DeletedPlaylist) = withContext(Dispatchers.IO) {
        playlists.restore(deleted.row, deleted.trackIds)
    }

    /** Sets whether [trackId] is a favorite. */
    suspend fun setFavorite(trackId: Long, favorite: Boolean) = withContext(Dispatchers.IO) {
        if (favorite) playlists.addFavorite(trackId, System.currentTimeMillis()) else playlists.removeFavorite(trackId)
    }
}
