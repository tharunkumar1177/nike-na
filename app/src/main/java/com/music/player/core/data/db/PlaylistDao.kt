/*
 * Data access for playlists, their ordered entries, and favorites.
 */
package com.music.player.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.music.player.core.model.Playlist
import kotlinx.coroutines.flow.Flow

/** Raw join row of a playlist entry and its (possibly missing) track id. */
data class EntryRow(val entryId: Long, val trackId: Long, val position: Int)

/** Playlist and favorites persistence. */
@Dao
interface PlaylistDao {

    /** All playlists with counts and total duration of available tracks, most recently updated first. */
    @Query(
        """
        SELECT p.id AS id, p.name AS name, COUNT(e.entryId) AS trackCount,
               COALESCE(SUM(t.durationMs), 0) AS durationMs, p.updatedAtMs AS updatedAtMs
        FROM playlists p
        LEFT JOIN playlist_entries e ON e.playlistId = p.id
        LEFT JOIN tracks t ON t.id = e.trackId
        GROUP BY p.id ORDER BY p.updatedAtMs DESC
        """,
    )
    fun observePlaylists(): Flow<List<Playlist>>

    /** One playlist aggregate. */
    @Query(
        """
        SELECT p.id AS id, p.name AS name, COUNT(e.entryId) AS trackCount,
               COALESCE(SUM(t.durationMs), 0) AS durationMs, p.updatedAtMs AS updatedAtMs
        FROM playlists p
        LEFT JOIN playlist_entries e ON e.playlistId = p.id
        LEFT JOIN tracks t ON t.id = e.trackId
        WHERE p.id = :playlistId GROUP BY p.id
        """,
    )
    fun observePlaylist(playlistId: Long): Flow<Playlist?>

    /** Ordered entries of one playlist. Re-emits when tracks change so availability stays current. */
    @Query(
        "SELECT e.entryId AS entryId, e.trackId AS trackId, e.position AS position " +
            "FROM playlist_entries e LEFT JOIN tracks t ON t.id = e.trackId " +
            "WHERE e.playlistId = :playlistId ORDER BY e.position",
    )
    fun observeEntries(playlistId: Long): Flow<List<EntryRow>>

    /** Ordered entries snapshot. */
    @Query("SELECT entryId, trackId, position FROM playlist_entries WHERE playlistId = :playlistId ORDER BY position")
    suspend fun entries(playlistId: Long): List<EntryRow>

    /** Creates a playlist and returns its id. */
    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    /** Renames a playlist. */
    @Query("UPDATE playlists SET name = :name, updatedAtMs = :now WHERE id = :playlistId")
    suspend fun rename(playlistId: Long, name: String, now: Long)

    /** Marks a playlist as changed. */
    @Query("UPDATE playlists SET updatedAtMs = :now WHERE id = :playlistId")
    suspend fun touch(playlistId: Long, now: Long)

    /** Deletes a playlist; entries cascade. */
    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    /** Snapshot of a playlist row for undo. */
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun playlistRow(playlistId: Long): PlaylistEntity?

    /** Inserts entries. */
    @Insert
    suspend fun insertEntries(entries: List<PlaylistEntryEntity>)

    /** Removes all entries of a playlist. */
    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId")
    suspend fun clearEntries(playlistId: Long)

    /** Highest position in a playlist, or -1 when empty. */
    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_entries WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: Long): Int

    /** Appends [trackIds] to the end of [playlistId]. */
    @Transaction
    suspend fun append(playlistId: Long, trackIds: List<Long>, now: Long) {
        val start = maxPosition(playlistId) + 1
        insertEntries(trackIds.mapIndexed { i, id -> PlaylistEntryEntity(playlistId = playlistId, trackId = id, position = start + i) })
        touch(playlistId, now)
    }

    /** Replaces the entries of [playlistId] with [trackIds] in order (used by reorder, remove, undo). */
    @Transaction
    suspend fun replaceEntries(playlistId: Long, trackIds: List<Long>, now: Long) {
        clearEntries(playlistId)
        insertEntries(trackIds.mapIndexed { i, id -> PlaylistEntryEntity(playlistId = playlistId, trackId = id, position = i) })
        touch(playlistId, now)
    }

    /** Restores a deleted playlist with its entries (undo). */
    @Transaction
    suspend fun restore(playlist: PlaylistEntity, trackIds: List<Long>) {
        val id = insertPlaylist(playlist)
        insertEntries(trackIds.mapIndexed { i, t -> PlaylistEntryEntity(playlistId = id, trackId = t, position = i) })
    }

    /** Favorite track ids. */
    @Query("SELECT trackId FROM favorites ORDER BY addedAtMs DESC")
    fun observeFavoriteIds(): Flow<List<Long>>

    /** Marks a track as favorite. */
    @Query("INSERT OR REPLACE INTO favorites (trackId, addedAtMs) VALUES (:trackId, :now)")
    suspend fun addFavorite(trackId: Long, now: Long)

    /** Removes a favorite. */
    @Query("DELETE FROM favorites WHERE trackId = :trackId")
    suspend fun removeFavorite(trackId: Long)
}
