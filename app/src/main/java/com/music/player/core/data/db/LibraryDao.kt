/*
 * Data access for cached library content: tracks, album/artist/folder aggregates, search,
 * and the write operations used by MediaStore sync.
 */
package com.music.player.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.music.player.core.model.Album
import com.music.player.core.model.Artist
import com.music.player.core.model.Folder
import com.music.player.core.model.Track
import kotlinx.coroutines.flow.Flow

/** SQLite bound-variable budget per statement, kept well below the platform limit. */
internal const val SQL_CHUNK = 900

private const val TRACK_COLUMNS =
    "id, title, artist, artistId, album, albumId, albumArtist, durationMs, trackNumber, discNumber, " +
        "year, folderPath, fileName, mimeType, dateAddedSec, sizeBytes"

/** Library queries. All `Flow` queries re-emit when the tracks table changes. */
@Dao
interface LibraryDao {

    /** Every track, ordered by title. Callers re-sort as needed. */
    @Query("SELECT $TRACK_COLUMNS FROM tracks ORDER BY title COLLATE NOCASE")
    fun observeTracks(): Flow<List<Track>>

    /** Number of cached tracks. */
    @Query("SELECT COUNT(*) FROM tracks")
    fun observeTrackCount(): Flow<Int>

    /** Albums aggregated by album id. */
    @Query(
        """
        SELECT albumId AS id, album AS title,
               CASE WHEN MAX(albumArtist) <> '' THEN MAX(albumArtist) ELSE MAX(artist) END AS artist,
               MAX(year) AS year, COUNT(*) AS trackCount, SUM(durationMs) AS durationMs,
               MAX(dateAddedSec) AS dateAddedSec
        FROM tracks GROUP BY albumId ORDER BY title COLLATE NOCASE
        """,
    )
    fun observeAlbums(): Flow<List<Album>>

    /** Single album aggregate. */
    @Query(
        """
        SELECT albumId AS id, album AS title,
               CASE WHEN MAX(albumArtist) <> '' THEN MAX(albumArtist) ELSE MAX(artist) END AS artist,
               MAX(year) AS year, COUNT(*) AS trackCount, SUM(durationMs) AS durationMs,
               MAX(dateAddedSec) AS dateAddedSec
        FROM tracks WHERE albumId = :albumId GROUP BY albumId
        """,
    )
    fun observeAlbum(albumId: Long): Flow<Album?>

    /** Artists aggregated by artist id. */
    @Query(
        """
        SELECT artistId AS id, MAX(artist) AS name, COUNT(*) AS trackCount,
               COUNT(DISTINCT albumId) AS albumCount, MIN(albumId) AS artworkAlbumId
        FROM tracks GROUP BY artistId ORDER BY name COLLATE NOCASE
        """,
    )
    fun observeArtists(): Flow<List<Artist>>

    /** Single artist aggregate. */
    @Query(
        """
        SELECT artistId AS id, MAX(artist) AS name, COUNT(*) AS trackCount,
               COUNT(DISTINCT albumId) AS albumCount, MIN(albumId) AS artworkAlbumId
        FROM tracks WHERE artistId = :artistId GROUP BY artistId
        """,
    )
    fun observeArtist(artistId: Long): Flow<Artist?>

    /** Albums by one artist, newest first. */
    @Query(
        """
        SELECT albumId AS id, album AS title, MAX(artist) AS artist, MAX(year) AS year,
               COUNT(*) AS trackCount, SUM(durationMs) AS durationMs, MAX(dateAddedSec) AS dateAddedSec
        FROM tracks WHERE artistId = :artistId GROUP BY albumId ORDER BY year DESC, title COLLATE NOCASE
        """,
    )
    fun observeArtistAlbums(artistId: Long): Flow<List<Album>>

    /** Folders that directly contain tracks; the name is filled in by the repository. */
    @Query("SELECT folderPath AS path, '' AS name, COUNT(*) AS trackCount FROM tracks GROUP BY folderPath")
    fun observeFolders(): Flow<List<Folder>>

    /** Tracks of one album in disc/track order. */
    @Query(
        "SELECT $TRACK_COLUMNS FROM tracks WHERE albumId = :albumId " +
            "ORDER BY discNumber, trackNumber, title COLLATE NOCASE",
    )
    fun observeAlbumTracks(albumId: Long): Flow<List<Track>>

    /** Tracks of one artist grouped by album. */
    @Query(
        "SELECT $TRACK_COLUMNS FROM tracks WHERE artistId = :artistId " +
            "ORDER BY album COLLATE NOCASE, discNumber, trackNumber",
    )
    fun observeArtistTracks(artistId: Long): Flow<List<Track>>

    /** Tracks directly inside one folder, by file name. */
    @Query("SELECT $TRACK_COLUMNS FROM tracks WHERE folderPath = :path ORDER BY fileName COLLATE NOCASE")
    fun observeFolderTracks(path: String): Flow<List<Track>>

    /** Tracks whose text matches an FTS4 [match] expression (already sanitized). */
    @Query(
        "SELECT tracks.id, tracks.title, tracks.artist, tracks.artistId, tracks.album, tracks.albumId, " +
            "tracks.albumArtist, tracks.durationMs, tracks.trackNumber, tracks.discNumber, tracks.year, " +
            "tracks.folderPath, tracks.fileName, tracks.mimeType, tracks.dateAddedSec, tracks.sizeBytes " +
            "FROM tracks JOIN tracks_fts ON tracks.id = tracks_fts.rowid " +
            "WHERE tracks_fts MATCH :match LIMIT :limit",
    )
    fun search(match: String, limit: Int): Flow<List<Track>>

    /** Tracks for a chunk of ids, unordered. Use [tracksByIds] for any size. */
    @Query("SELECT $TRACK_COLUMNS FROM tracks WHERE id IN (:ids)")
    suspend fun tracksByIdChunk(ids: List<Long>): List<Track>

    /** All cached ids for one volume. */
    @Query("SELECT id FROM tracks WHERE volume = :volume")
    suspend fun idsForVolume(volume: String): List<Long>

    /** All cached ids. */
    @Query("SELECT id FROM tracks")
    suspend fun allIds(): List<Long>

    /** Inserts or replaces rows. */
    @Upsert
    suspend fun upsert(rows: List<TrackEntity>)

    /** Deletes a chunk of ids. Use [deleteByIds] for any size. */
    @Query("DELETE FROM tracks WHERE id IN (:ids)")
    suspend fun deleteIdChunk(ids: List<Long>)

    /** Deletes tracks of volumes that are no longer mounted. */
    @Query("DELETE FROM tracks WHERE volume NOT IN (:volumes)")
    suspend fun deleteOutsideVolumes(volumes: List<String>)

    /** Deletes [ids] in chunks inside one transaction. */
    @Transaction
    suspend fun deleteByIds(ids: Collection<Long>) {
        ids.chunked(SQL_CHUNK).forEach { deleteIdChunk(it) }
    }

    /** Returns tracks for [ids] in the same order, skipping ids no longer cached. */
    suspend fun tracksByIds(ids: List<Long>): List<Track> {
        val byId = ids.distinct().chunked(SQL_CHUNK).flatMap { tracksByIdChunk(it) }.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }
}
