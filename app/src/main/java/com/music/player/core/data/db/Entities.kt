/*
 * Room entities for the Music cache: tracks (mirroring MediaStore), the FTS index over them,
 * playlists with ordered entries, and favorites.
 */
package com.music.player.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached MediaStore audio row. Column names equal the [com.music.player.core.model.Track]
 * property names so DAO queries can return the domain type directly.
 */
@Entity(
    tableName = "tracks",
    indices = [Index("albumId"), Index("artistId"), Index("folderPath")],
)
data class TrackEntity(
    @PrimaryKey val id: Long,
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
    /** MediaStore volume the row belongs to, used for per-volume sync. */
    val volume: String,
)

/** External-content FTS4 index over track text; diacritics are removed by unicode61. */
@Fts4(contentEntity = TrackEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "tracks_fts")
data class TrackFtsEntity(
    val title: String,
    val artist: String,
    val album: String,
    val fileName: String,
)

/** A user playlist. */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

/**
 * One ordered entry of a playlist. [trackId] intentionally has no foreign key to tracks so
 * entries survive a file disappearing (rendered as unavailable).
 */
@Entity(
    tableName = "playlist_entries",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["playlistId", "position"])],
)
data class PlaylistEntryEntity(
    @PrimaryKey(autoGenerate = true) val entryId: Long = 0,
    val playlistId: Long,
    val trackId: Long,
    val position: Int,
)

/** A favorited track. */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val trackId: Long,
    @ColumnInfo(defaultValue = "0") val addedAtMs: Long,
)
