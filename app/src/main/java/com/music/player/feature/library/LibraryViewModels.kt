/*
 * ViewModels for the Library tabs and the album, artist, and folder detail screens.
 */
package com.music.player.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.player.core.data.LibraryRepository
import com.music.player.core.model.Album
import com.music.player.core.model.Artist
import com.music.player.core.model.Folder
import com.music.player.core.model.LibraryTab
import com.music.player.core.model.SortSpec
import com.music.player.core.model.Track
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Library tab lists; null means "not loaded yet". */
class LibraryViewModel(private val library: LibraryRepository) : ViewModel() {

    /** Songs tab. */
    val songs: StateFlow<List<Track>?> = library.tracks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Albums tab. */
    val albums: StateFlow<List<Album>?> = library.albums().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Artists tab. */
    val artists: StateFlow<List<Artist>?> = library.artists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Folders tab. */
    val folders: StateFlow<List<Folder>?> = library.folders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** True while indexing. */
    val scanning: StateFlow<Boolean> = library.scanning

    /** Sort choices per tab. */
    val sorts: Map<LibraryTab, StateFlow<SortSpec>> = LibraryTab.entries.associateWith { tab ->
        library.sort(tab).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SortSpec())
    }

    /** Saves a sort choice. */
    fun setSort(tab: LibraryTab, spec: SortSpec) {
        viewModelScope.launch { library.setSort(tab, spec) }
    }

    /** Forces a full rescan. */
    fun rescan() = library.rescan()
}

/** Album detail. */
class AlbumViewModel(albumId: Long, library: LibraryRepository) : ViewModel() {
    /** The album, null while loading or after deletion. */
    val album: StateFlow<Album?> = library.album(albumId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Its tracks. */
    val tracks: StateFlow<List<Track>> = library.albumTracks(albumId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** Artist detail. */
class ArtistViewModel(artistId: Long, library: LibraryRepository) : ViewModel() {
    /** The artist. */
    val artist: StateFlow<Artist?> = library.artist(artistId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Their albums. */
    val albums: StateFlow<List<Album>> = library.artistAlbums(artistId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Their tracks. */
    val tracks: StateFlow<List<Track>> = library.artistTracks(artistId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** Folder detail. */
class FolderViewModel(path: String, library: LibraryRepository) : ViewModel() {
    /** Tracks directly in the folder. */
    val tracks: StateFlow<List<Track>> = library.folderTracks(path).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
