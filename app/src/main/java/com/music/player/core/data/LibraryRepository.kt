/*
 * Single entry point for library reads: access state, sorted lists, detail streams, search,
 * and id lookups for playback. UI reads only from here (Room-backed `Flow`s).
 */
package com.music.player.core.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.music.player.core.data.db.LibraryDao
import com.music.player.core.data.prefs.UserPreferences
import com.music.player.core.data.sync.LibrarySync
import com.music.player.core.data.sync.TagRules
import com.music.player.core.model.Album
import com.music.player.core.model.Artist
import com.music.player.core.model.Folder
import com.music.player.core.model.LibraryAccess
import com.music.player.core.model.LibraryTab
import com.music.player.core.model.SearchResults
import com.music.player.core.model.SortSpec
import com.music.player.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Library data facade over Room, MediaStore sync, and preferences. */
class LibraryRepository(
    private val context: Context,
    private val dao: LibraryDao,
    private val prefs: UserPreferences,
    private val sync: LibrarySync,
) {
    private val _access = MutableStateFlow(currentAccess())

    /** Whether audio files can be read. */
    val access: StateFlow<LibraryAccess> = _access.asStateFlow()

    /** True while MediaStore is being indexed. */
    val scanning: StateFlow<Boolean> = sync.scanning

    /** Number of indexed tracks. */
    val trackCount: Flow<Int> = dao.observeTrackCount()

    /** Re-reads the permission state; starts syncing once access is granted. */
    fun refreshAccess() {
        val access = currentAccess()
        _access.value = access
        if (access == LibraryAccess.Granted) sync.start()
    }

    /** Forces a full rescan of every volume. */
    fun rescan() {
        if (_access.value == LibraryAccess.Granted) sync.requestSync(force = true)
    }

    /** Sort choice of [tab]. */
    fun sort(tab: LibraryTab): Flow<SortSpec> = prefs.sort(tab)

    /** Saves the sort choice of [tab]. */
    suspend fun setSort(tab: LibraryTab, spec: SortSpec) = prefs.setSort(tab, spec)

    /** All tracks in the Songs tab order. */
    fun tracks(): Flow<List<Track>> =
        combine(dao.observeTracks(), prefs.sort(LibraryTab.Songs), LibrarySorting::tracks).flowOn(Dispatchers.Default)

    /** All albums in the Albums tab order. */
    fun albums(): Flow<List<Album>> =
        combine(dao.observeAlbums(), prefs.sort(LibraryTab.Albums), LibrarySorting::albums).flowOn(Dispatchers.Default)

    /** All artists in the Artists tab order. */
    fun artists(): Flow<List<Artist>> =
        combine(dao.observeArtists(), prefs.sort(LibraryTab.Artists), LibrarySorting::artists).flowOn(Dispatchers.Default)

    /** All folders with display names, in the Folders tab order. */
    fun folders(): Flow<List<Folder>> =
        combine(dao.observeFolders(), prefs.sort(LibraryTab.Folders)) { list, spec ->
            LibrarySorting.folders(list.map { it.copy(name = TagRules.folderName(it.path).ifEmpty { it.path.ifEmpty { "/" } }) }, spec)
        }.flowOn(Dispatchers.Default)

    /** One album, or null once it no longer exists. */
    fun album(albumId: Long): Flow<Album?> = dao.observeAlbum(albumId)

    /** Tracks of one album. */
    fun albumTracks(albumId: Long): Flow<List<Track>> = dao.observeAlbumTracks(albumId)

    /** One artist, or null once it no longer exists. */
    fun artist(artistId: Long): Flow<Artist?> = dao.observeArtist(artistId)

    /** Albums of one artist. */
    fun artistAlbums(artistId: Long): Flow<List<Album>> = dao.observeArtistAlbums(artistId)

    /** Tracks of one artist. */
    fun artistTracks(artistId: Long): Flow<List<Track>> = dao.observeArtistTracks(artistId)

    /** Tracks directly inside the folder at [path]. */
    fun folderTracks(path: String): Flow<List<Track>> = dao.observeFolderTracks(path)

    /** Tracks for [ids] in the given order, skipping ids that no longer exist. */
    suspend fun tracksByIds(ids: List<Long>): List<Track> = withContext(Dispatchers.IO) { dao.tracksByIds(ids) }

    /** Grouped type-ahead search over the cache. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun search(query: Flow<String>): Flow<SearchResults> = query.flatMapLatest { q ->
        val match = SearchText.ftsMatch(q)
        if (match == null) {
            flowOf(SearchResults(q, emptyList(), emptyList(), emptyList()))
        } else {
            val tokens = SearchText.tokens(q)
            combine(
                dao.search(match, SEARCH_TRACK_LIMIT),
                dao.observeAlbums().map { all -> all.filter { SearchText.matches("${it.title} ${it.artist}", tokens) } },
                dao.observeArtists().map { all -> all.filter { SearchText.matches(it.name, tokens) } },
            ) { tracks, albums, artists ->
                SearchResults(q, tracks, albums.take(SEARCH_GROUP_LIMIT), artists.take(SEARCH_GROUP_LIMIT))
            }
        }
    }.flowOn(Dispatchers.Default)

    /** Reads the runtime permission state. */
    private fun currentAccess(): LibraryAccess =
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            LibraryAccess.Granted
        } else {
            LibraryAccess.Denied
        }

    private companion object {
        const val SEARCH_TRACK_LIMIT = 200
        const val SEARCH_GROUP_LIMIT = 30
    }
}
