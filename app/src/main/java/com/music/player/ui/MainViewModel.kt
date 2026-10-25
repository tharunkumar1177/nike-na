/*
 * App-root state: library access, appearance, playback state, favorites, and user messages.
 * Implements the shared track actions on behalf of every screen.
 */
package com.music.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.player.core.data.LibraryRepository
import com.music.player.core.data.PlaylistRepository
import com.music.player.core.data.prefs.AppearanceSettings
import com.music.player.core.data.prefs.UserPreferences
import com.music.player.core.model.Playlist
import com.music.player.core.model.Track
import com.music.player.core.playback.PlayerController
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Root ViewModel. */
class MainViewModel(
    private val library: LibraryRepository,
    private val playlists: PlaylistRepository,
    private val player: PlayerController,
    prefs: UserPreferences,
) : ViewModel() {

    private val localMessages = MutableSharedFlow<UserMessage>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Audio access state. */
    val access = library.access

    /** Playback state. */
    val playerState = player.state

    /** Playback position. */
    val position = player.position

    /** Appearance preferences. */
    val appearance: StateFlow<AppearanceSettings> =
        prefs.appearance.stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceSettings())

    /** Favorite track ids. */
    val favorites: StateFlow<Set<Long>> = playlists.favoriteIds.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** Playlists for the add-to-playlist sheet. */
    val allPlaylists: StateFlow<List<Playlist>> = playlists.all.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Snackbar messages from playback and from actions. */
    val messages: Flow<UserMessage> = merge(player.messages.map { UserMessage(it) }, localMessages)

    /** Re-reads permission state (on resume and after a permission result). */
    fun refreshAccess() = library.refreshAccess()

    /** See [PlayerController.play]. */
    fun play(tracks: List<Track>, index: Int) = player.play(tracks, index)

    /** See [PlayerController.shuffle]. */
    fun shuffle(tracks: List<Track>) = player.shuffle(tracks)

    /** See [PlayerController.playNext]. */
    fun playNext(tracks: List<Track>) = player.playNext(tracks)

    /** See [PlayerController.addToQueue]. */
    fun addToQueue(tracks: List<Track>) = player.addToQueue(tracks)

    /** Toggles play/pause. */
    fun togglePlayPause() = player.togglePlayPause()

    /** Next track. */
    fun next() = player.next()

    /** Previous track or restart. */
    fun previous() = player.previous()

    /** Seeks the current track. */
    fun seekTo(ms: Long) = player.seekTo(ms)

    /** Toggles shuffle. */
    fun toggleShuffle() = player.setShuffle(!player.state.value.shuffle)

    /** Cycles repeat mode. */
    fun cycleRepeat() = player.cycleRepeat()

    /** Jumps to a queue position. */
    fun skipTo(index: Int) = player.skipTo(index)

    /** Moves a queue item. */
    fun moveQueueItem(from: Int, to: Int) = player.moveQueueItem(from, to)

    /** Removes a queue item. */
    fun removeQueueItem(index: Int) = player.removeQueueItem(index)

    /** Restores a removed queue item. */
    fun insertQueueItem(index: Int, trackId: Long) = player.insertQueueItem(index, trackId)

    /** Sets or clears the favorite flag of [trackId]. */
    fun setFavorite(trackId: Long, favorite: Boolean) {
        viewModelScope.launch { playlists.setFavorite(trackId, favorite) }
    }

    /** Adds [tracks] to playlist [playlistId] and confirms with the playlist name. */
    fun addToPlaylist(playlistId: Long, name: String, tracks: List<Track>) {
        viewModelScope.launch {
            playlists.add(playlistId, tracks.map { it.id })
            localMessages.tryEmit(UserMessage("Added to $name"))
        }
    }

    /** Creates playlist [name] containing [tracks]. */
    fun createPlaylist(name: String, tracks: List<Track>) {
        viewModelScope.launch {
            playlists.create(name, tracks.map { it.id })
            localMessages.tryEmit(UserMessage("Created $name"))
        }
    }

    /** Deletes playlist [playlistId] and offers undo. */
    fun deletePlaylist(playlistId: Long, name: String) {
        viewModelScope.launch {
            val deleted = playlists.delete(playlistId) ?: return@launch
            localMessages.tryEmit(UserMessage("Deleted $name", UNDO) { viewModelScope.launch { playlists.restore(deleted) } })
        }
    }

    /** Replaces playlist [playlistId]'s order with [remaining] after a removal and offers undo back to [previous]. */
    fun removeFromPlaylist(playlistId: Long, title: String, remaining: List<Long>, previous: List<Long>) {
        viewModelScope.launch {
            playlists.setOrder(playlistId, remaining)
            localMessages.tryEmit(UserMessage("Removed $title", UNDO) { viewModelScope.launch { playlists.setOrder(playlistId, previous) } })
        }
    }

    /** Removes [track] from favorites and offers undo. */
    fun removeFavorite(track: Track) {
        viewModelScope.launch {
            playlists.setFavorite(track.id, false)
            localMessages.tryEmit(UserMessage("Removed ${track.title} from favorites", UNDO) { setFavorite(track.id, true) })
        }
    }

    /** Resolves a track id (e.g. the current queue item) to a track. */
    suspend fun track(id: Long): Track? = library.tracksByIds(listOf(id)).firstOrNull()

    /** Shows a transient message. */
    fun message(text: String) {
        localMessages.tryEmit(UserMessage(text))
    }

    private companion object {
        const val UNDO = "Undo"
    }
}

/** A snackbar message with an optional action (e.g. Undo). */
data class UserMessage(val text: String, val actionLabel: String? = null, val onAction: (() -> Unit)? = null)
