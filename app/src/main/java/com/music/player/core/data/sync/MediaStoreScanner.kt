/*
 * Reads audio rows from MediaStore for one volume: changed rows since a generation (paged), and
 * the full id set used for deletion detection.
 */
package com.music.player.core.data.sync

import android.content.ContentResolver
import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import com.music.player.core.data.db.TrackEntity

/** Thin MediaStore query layer. All calls block and must run on `Dispatchers.IO`. */
class MediaStoreScanner(private val resolver: ContentResolver) {

    private val projection = arrayOf(
        Media._ID, Media.TITLE, Media.ARTIST, Media.ARTIST_ID, Media.ALBUM, Media.ALBUM_ID,
        Media.ALBUM_ARTIST, Media.DURATION, Media.TRACK, Media.YEAR, Media.RELATIVE_PATH,
        Media.DISPLAY_NAME, Media.MIME_TYPE, Media.DATE_ADDED, Media.SIZE,
    )

    /** Base selection excluding system sounds, recordings, and tracks shorter than the minimum. */
    private fun baseSelection(): String =
        "${Media.IS_RINGTONE} = 0 AND ${Media.IS_NOTIFICATION} = 0 AND ${Media.IS_ALARM} = 0 " +
            "AND ${Media.IS_RECORDING} = 0 AND ${Media.DURATION} >= ?"

    /** Current external volume names (internal storage and mounted SD cards). */
    fun volumes(context: android.content.Context): Set<String> = MediaStore.getExternalVolumeNames(context)

    /**
     * Streams rows of [volume] modified after [sinceGeneration] (or all rows when null) to
     * [onPage] in pages of [pageSize].
     */
    suspend fun changedRows(
        volume: String,
        sinceGeneration: Long?,
        minDurationMs: Long,
        pageSize: Int = PAGE_SIZE,
        onPage: suspend (List<TrackEntity>) -> Unit,
    ) {
        var selection = baseSelection()
        val args = mutableListOf(minDurationMs.toString())
        if (sinceGeneration != null) {
            selection += " AND ${Media.GENERATION_MODIFIED} > ?"
            args += sinceGeneration.toString()
        }
        val queryArgs = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, args.toTypedArray())
        }
        resolver.query(Media.getContentUri(volume), projection, queryArgs, null)?.use { cursor ->
            val page = ArrayList<TrackEntity>(pageSize)
            val columns = Columns(cursor)
            while (cursor.moveToNext()) {
                page += columns.read(cursor, volume)
                if (page.size == pageSize) {
                    onPage(page.toList())
                    page.clear()
                }
            }
            if (page.isNotEmpty()) onPage(page.toList())
        }
    }

    /** Every id currently in [volume] that passes the library filter. */
    fun currentIds(volume: String, minDurationMs: Long): List<Long> {
        val queryArgs = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, baseSelection())
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(minDurationMs.toString()))
        }
        val ids = ArrayList<Long>()
        resolver.query(Media.getContentUri(volume), arrayOf(Media._ID), queryArgs, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(Media._ID)
            while (c.moveToNext()) ids += c.getLong(idCol)
        }
        return ids
    }

    /** Cached column indices for one cursor. */
    private class Columns(c: Cursor) {
        val id = c.getColumnIndexOrThrow(Media._ID)
        val title = c.getColumnIndexOrThrow(Media.TITLE)
        val artist = c.getColumnIndexOrThrow(Media.ARTIST)
        val artistId = c.getColumnIndexOrThrow(Media.ARTIST_ID)
        val album = c.getColumnIndexOrThrow(Media.ALBUM)
        val albumId = c.getColumnIndexOrThrow(Media.ALBUM_ID)
        val albumArtist = c.getColumnIndexOrThrow(Media.ALBUM_ARTIST)
        val duration = c.getColumnIndexOrThrow(Media.DURATION)
        val track = c.getColumnIndexOrThrow(Media.TRACK)
        val year = c.getColumnIndexOrThrow(Media.YEAR)
        val path = c.getColumnIndexOrThrow(Media.RELATIVE_PATH)
        val name = c.getColumnIndexOrThrow(Media.DISPLAY_NAME)
        val mime = c.getColumnIndexOrThrow(Media.MIME_TYPE)
        val added = c.getColumnIndexOrThrow(Media.DATE_ADDED)
        val size = c.getColumnIndexOrThrow(Media.SIZE)

        /** Maps the current cursor row to an entity, applying [TagRules] fallbacks. */
        fun read(c: Cursor, volume: String): TrackEntity {
            val fileName = c.getString(name).orEmpty()
            val folder = c.getString(path).orEmpty()
            val rawTrack = c.getInt(track)
            return TrackEntity(
                id = c.getLong(id),
                title = TagRules.title(c.getString(title), fileName),
                artist = TagRules.artist(c.getString(artist)),
                artistId = c.getLong(artistId),
                album = TagRules.album(c.getString(album), folder),
                albumId = c.getLong(albumId),
                albumArtist = TagRules.clean(c.getString(albumArtist)).orEmpty(),
                durationMs = c.getLong(duration),
                trackNumber = TagRules.trackNumber(rawTrack),
                discNumber = TagRules.discNumber(rawTrack),
                year = c.getInt(year),
                folderPath = folder,
                fileName = fileName,
                mimeType = c.getString(mime).orEmpty(),
                dateAddedSec = c.getLong(added),
                sizeBytes = c.getLong(size),
                volume = volume,
            )
        }
    }

    private companion object {
        const val PAGE_SIZE = 500
    }
}
