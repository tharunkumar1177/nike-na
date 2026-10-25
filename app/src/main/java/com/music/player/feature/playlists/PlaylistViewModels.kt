/*
 * ViewModels for the Playlists screen, a playlist's detail, and Favorites.
 */
package com.music.player.feature.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.player.core.data.PlaylistRepository
import com.music.player.core.model.Playlist
import com.music.player.core.model.PlaylistEntry
import com.music.player.core.model.Track
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Playlists list. */
class PlaylistsViewModel(private val repo: PlaylistRepository) : ViewModel() {
    /** All playlists; null until loaded. */
    val playlists: StateFlow<List<Playlist>?> = repo.all.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Favorite tracks for the pinned Favorites tile. */
    val favorites: StateFlow<List<Track>> = repo.favoriteTracks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Creates an empty playlist. */
    fun create(name: String) {
        viewModelScope.launch { repo.create(name) }
    }
}

/** One playlist's detail with reorder and rename; undoable edits go through the app root. */
class PlaylistDetailViewModel(private val playlistId: Long, private val repo: PlaylistRepository) : ViewModel() {
    /** The playlist; null once deleted. */
    val playlist: StateFlow<Playlist?> = repo.playlist(playlistId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Ordered entries; null until loaded. */
    val entries: StateFlow<List<PlaylistEntry>?> = repo.entries(playlistId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Saves a new order (after drag-reorder). */
    fun setOrder(trackIds: List<Long>) {
        viewModelScope.launch { repo.setOrder(playlistId, trackIds) }
    }

    /** Renames the playlist. */
    fun rename(name: String) {
        viewModelScope.launch { repo.rename(playlistId, name) }
    }
}

/** Favorites collection. */
class FavoritesViewModel(repo: PlaylistRepository) : ViewModel() {
    /** Favorite tracks, newest first. */
    val tracks: StateFlow<List<Track>?> = repo.favoriteTracks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
