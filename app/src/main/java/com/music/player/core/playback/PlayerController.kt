/*
 * Client side of the playback boundary: wraps a Media3 MediaController connected to
 * PlaybackService and exposes StateFlow<PlayerState>, a position flow, user messages, and
 * intent methods. UI code never touches ExoPlayer directly. Main-thread only.
 */
@file:OptIn(UnstableApi::class)

package com.music.player.core.playback

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.music.player.core.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/** App-wide playback client. Call [connect] when the UI starts and [disconnect] when it stops. */
class PlayerController(private val context: Context, private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(PlayerState())
    private val _position = MutableStateFlow(0L)
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val nextCursor = PlayNextCursor()
    private val pending = ArrayDeque<(MediaController) -> Unit>()
    private var controller: MediaController? = null
    private var connectJob: Job? = null
    private var positionJob: Job? = null
    private val listener = Listener()

    /** Current player snapshot. */
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** Playback position in milliseconds, updated about four times a second while playing. */
    val position: StateFlow<Long> = _position.asStateFlow()

    /** One-shot user-facing messages (e.g. a track that could not be played). */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Connects to the playback service if not already connected or connecting. */
    fun connect() {
        if (controller != null || connectJob?.isActive == true) return
        connectJob = scope.launch {
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val built = runCatching { MediaController.Builder(context, token).buildAsync().await() }.getOrNull()
                ?: return@launch
            controller = built
            built.addListener(listener)
            refresh(built, timelineChanged = true)
            while (pending.isNotEmpty()) pending.removeFirst().invoke(built)
            startPositionUpdates()
        }
    }

    /** Releases the controller so the service may stop when idle. State keeps its last value. */
    fun disconnect() {
        connectJob?.cancel()
        positionJob?.cancel()
        controller?.run {
            removeListener(listener)
            release()
        }
        controller = null
        _state.value = _state.value.copy(connected = false)
    }

    /** Replaces the queue with [tracks] and plays from [startIndex]. */
    fun play(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        withController { c ->
            nextCursor.reset()
            c.setMediaItems(tracks.map { it.toRequestItem() }, startIndex.coerceIn(tracks.indices), 0L)
            c.prepare()
            c.play()
        }
    }

    /** Replaces the queue with [tracks] in shuffled order, starting at a random track. */
    fun shuffle(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        withController { c ->
            nextCursor.reset()
            c.setMediaItems(tracks.map { it.toRequestItem() }, Random.nextInt(tracks.size), 0L)
            c.shuffleModeEnabled = true
            c.prepare()
            c.play()
        }
    }

    /** Inserts [tracks] to play after the current track (and after earlier play-next picks). */
    fun playNext(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        withController { c ->
            if (c.mediaItemCount == 0) {
                play(tracks)
                return@withController
            }
            val index = nextCursor.insertionIndex(c.currentMediaItemIndex, c.mediaItemCount)
            c.addMediaItems(index, tracks.map { it.toRequestItem() })
            nextCursor.onInserted(tracks.size)
            _messages.tryEmit(if (tracks.size == 1) "Playing next" else "${tracks.size} songs playing next")
        }
    }

    /** Appends [tracks] to the end of the queue. */
    fun addToQueue(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        withController { c ->
            if (c.mediaItemCount == 0) {
                play(tracks)
                return@withController
            }
            c.addMediaItems(tracks.map { it.toRequestItem() })
            _messages.tryEmit(if (tracks.size == 1) "Added to queue" else "${tracks.size} songs added to queue")
        }
    }

    /** Toggles play/pause; restarts from the beginning when playback had ended. */
    fun togglePlayPause() = withController { c ->
        when {
            c.isPlaying -> c.pause()
            c.playbackState == Player.STATE_ENDED -> {
                c.seekTo(0, 0L)
                c.play()
            }
            else -> {
                if (c.playbackState == Player.STATE_IDLE) c.prepare()
                c.play()
            }
        }
    }

    /** Seeks within the current track. */
    fun seekTo(positionMs: Long) = withController { c ->
        c.seekTo(positionMs.coerceAtLeast(0L))
        _position.value = positionMs
    }

    /** Skips to the next track. */
    fun next() = withController { it.seekToNext() }

    /** Restarts the current track or goes to the previous one (standard 3-second rule). */
    fun previous() = withController { it.seekToPrevious() }

    /** Jumps to queue position [index]. */
    fun skipTo(index: Int) = withController { c ->
        if (index in 0 until c.mediaItemCount) {
            c.seekTo(index, 0L)
            c.play()
        }
    }

    /** Turns shuffle on or off. */
    fun setShuffle(enabled: Boolean) = withController { it.shuffleModeEnabled = enabled }

    /** Cycles repeat Off -> All -> One -> Off. */
    fun cycleRepeat() = withController { c ->
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    /** Moves the queue item at [from] to [to]. */
    fun moveQueueItem(from: Int, to: Int) = withController { c ->
        if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount && from != to) {
            nextCursor.reset()
            c.moveMediaItem(from, to)
        }
    }

    /** Removes the queue item at [index]. */
    fun removeQueueItem(index: Int) = withController { c ->
        if (index in 0 until c.mediaItemCount) {
            nextCursor.reset()
            c.removeMediaItem(index)
        }
    }

    /** Re-inserts [trackId] at [index] (undo of a removal). */
    fun insertQueueItem(index: Int, trackId: Long) = withController { c ->
        c.addMediaItem(index.coerceIn(0, c.mediaItemCount), MediaItem.Builder().setMediaId(trackId.toString()).build())
    }

    /** Runs [action] now if connected, otherwise after connecting. */
    private fun withController(action: (MediaController) -> Unit) {
        val c = controller
        if (c != null) {
            action(c)
        } else {
            pending.addLast(action)
            connect()
        }
    }

    /** Polls the position while playing; Media3 does not push continuous position updates. */
    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                controller?.let { _position.value = it.currentPosition.coerceAtLeast(0L) }
                delay(if (_state.value.isPlaying) POSITION_TICK_MS else IDLE_TICK_MS)
            }
        }
    }

    /** Rebuilds [state] from [c]; the queue list is rebuilt only when the timeline changed. */
    private fun refresh(c: MediaController, timelineChanged: Boolean) {
        val queue = if (timelineChanged) {
            (0 until c.mediaItemCount).map { i -> c.getMediaItemAt(i).toEntry(i) }
        } else {
            _state.value.queue
        }
        val index = c.currentMediaItemIndex.takeIf { c.mediaItemCount > 0 } ?: -1
        val current = queue.getOrNull(index) ?: c.currentMediaItem?.toEntry(index)
        _state.value = PlayerState(
            connected = true,
            current = current,
            isPlaying = c.isPlaying,
            isBuffering = c.playbackState == Player.STATE_BUFFERING,
            durationMs = c.duration.takeIf { it > 0 } ?: current?.durationMs ?: 0L,
            shuffle = c.shuffleModeEnabled,
            repeat = when (c.repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatMode.All
                Player.REPEAT_MODE_ONE -> RepeatMode.One
                else -> RepeatMode.Off
            },
            queue = queue,
            currentIndex = index,
        )
        _position.value = c.currentPosition.coerceAtLeast(0L)
    }

    /** Maps a session item to a queue row. */
    private fun MediaItem.toEntry(index: Int) = QueueEntry(
        index = index,
        trackId = trackId ?: -1L,
        title = mediaMetadata.title?.toString().orEmpty(),
        artist = mediaMetadata.artist?.toString().orEmpty(),
        albumId = albumId ?: -1L,
        durationMs = mediaMetadata.durationMs ?: 0L,
    )

    /** Controller listener translating Media3 events into state and messages. */
    private inner class Listener : Player.Listener {
        /** Refreshes state for any batch of events. */
        override fun onEvents(player: Player, events: Player.Events) {
            val c = controller ?: return
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) nextCursor.reset()
            refresh(c, timelineChanged = events.contains(Player.EVENT_TIMELINE_CHANGED))
        }

        /** Surfaces a playback failure; the service skips ahead on its own. */
        override fun onPlayerError(error: PlaybackException) {
            val title = _state.value.current?.title
            _messages.tryEmit(if (title.isNullOrEmpty()) "Couldn't play this song" else "Couldn't play \u201C$title\u201D")
        }
    }

    private companion object {
        const val POSITION_TICK_MS = 250L
        const val IDLE_TICK_MS = 1_000L
    }
}
