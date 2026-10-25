/*
 * Room database of the Music app: the MediaStore read cache plus app-owned playlists and favorites.
 */
package com.music.player.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Database definition. Schema JSON is exported to `app/schemas/` by the Room Gradle plugin. */
@Database(
    entities = [
        TrackEntity::class,
        TrackFtsEntity::class,
        PlaylistEntity::class,
        PlaylistEntryEntity::class,
        FavoriteEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MusicDatabase : RoomDatabase() {

    /** Library cache access. */
    abstract fun libraryDao(): LibraryDao

    /** Playlist and favorites access. */
    abstract fun playlistDao(): PlaylistDao

    companion object {
        private const val NAME = "music.db"

        /** Builds the on-disk database. */
        fun create(context: Context): MusicDatabase =
            Room.databaseBuilder(context, MusicDatabase::class.java, NAME).build()
    }
}
