/*
 * Background playback service. Owns the ExoPlayer and MediaSession (architecture.md
 * "Playback boundary"): audio focus, becoming-noisy, notification and lock-screen controls via
 * Media3, id-to-item resolution for controller requests, queue persistence and restore,
 * decode-error skipping, and removal of tracks deleted from the device.
 */
@file:OptIn(UnstableApi::class)

package com.music.player.core.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import com.music.player.MainActivity
import com.music.player.core.data.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/** Media3 session service hosting the single app player. */
class PlaybackService : MediaSessionService() {

    private val repository: LibraryRepository by inject()
    private val queueStore: QueueStore by inject()
    private val scope: CoroutineScope = MainScope()
    private var session: MediaSession? = null
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var player: ShufflingPlayer
    private var saveJob: Job? = null

    /** Builds the player and session, restores the last queue, and starts housekeeping. */
    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        exoPlayer.setShuffleOrder(ShuffleOrder.UnshuffledShuffleOrder(0))
        exoPlayer.addListener(PlayerEvents())
        player = ShufflingPlayer(exoPlayer)

        val openPlayer = Intent(this, MainActivity::class.java)
            .setAction(MainActivity.ACTION_OPEN_PLAYER)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val sessionActivity = PendingIntent.getActivity(
            this, 0, openPlayer, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .setCallback(SessionCallback())
            .build()

        scope.launch { restoreQueue() }
        scope.launch { removeDeletedTracksOnLibraryChange() }
        scope.launch { savePositionWhilePlaying() }
    }

    /** Returns the session to any controller (app UI, system UI, Bluetooth, Assistant). */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** Saves the queue before the task is removed; Media3 stops the service if not playing. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        saveNow()
        super.onTaskRemoved(rootIntent)
    }

    /** Saves state and releases the player and session. */
    override fun onDestroy() {
        saveNow()
        session?.run {
            player.release()
            release()
        }
        session = null
        scope.cancel()
        super.onDestroy()
    }

    /** Loads the persisted queue into the player, paused, if the player is still empty. */
    private suspend fun restoreQueue() {
        val snapshot = queueStore.load() ?: return
        val tracks = repository.tracksByIds(snapshot.trackIds)
        if (tracks.isEmpty() || exoPlayer.mediaItemCount > 0) return
        val currentId = snapshot.trackIds[snapshot.index]
        val index = tracks.indexOfFirst { it.id == currentId }
        exoPlayer.setMediaItems(
            tracks.map { it.toMediaItem() },
            index.coerceAtLeast(0),
            if (index >= 0) snapshot.positionMs else 0L,
        )
        exoPlayer.repeatMode = snapshot.repeatMode
        exoPlayer.shuffleModeEnabled = snapshot.shuffle
        exoPlayer.prepare()
    }

    /** Removes queue items whose files disappeared whenever the library changes. */
    private suspend fun removeDeletedTracksOnLibraryChange() {
        repository.trackCount.drop(1).collect {
            val ids = (0 until exoPlayer.mediaItemCount).mapNotNull { exoPlayer.getMediaItemAt(it).trackId }
            if (ids.isEmpty()) return@collect
            val present = repository.tracksByIds(ids).mapTo(HashSet()) { it.id }
            for (i in exoPlayer.mediaItemCount - 1 downTo 0) {
                val id = exoPlayer.getMediaItemAt(i).trackId
                if (id != null && id !in present) exoPlayer.removeMediaItem(i)
            }
        }
    }

    /** Periodically saves the position so a crash or kill loses at most a few seconds. */
    private suspend fun savePositionWhilePlaying() {
        while (scope.isActive) {
            delay(POSITION_SAVE_INTERVAL_MS)
            if (exoPlayer.isPlaying) saveNow()
        }
    }

    /** Debounced save after queue or mode changes. */
    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SAVE_DEBOUNCE_MS)
            saveNow()
        }
    }

    /** Captures and persists the current queue. Empty queues are not saved over a previous one. */
    private fun saveNow() {
        if (!::exoPlayer.isInitialized || exoPlayer.mediaItemCount == 0) return
        val ids = (0 until exoPlayer.mediaItemCount).mapNotNull { exoPlayer.getMediaItemAt(it).trackId }
        if (ids.isEmpty()) return
        val snapshot = QueueSnapshot(
            trackIds = ids,
            index = exoPlayer.currentMediaItemIndex.coerceIn(ids.indices),
            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
            shuffle = exoPlayer.shuffleModeEnabled,
            repeatMode = exoPlayer.repeatMode,
        )
        queueStore.saveAsync(snapshot)
    }

    /** Player listener for persistence and error recovery. */
    private inner class PlayerEvents : Player.Listener {
        /** Schedules a save after queue, item, mode, or play/pause changes. */
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                    Player.EVENT_REPEAT_MODE_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                )
            ) {
                scheduleSave()
            }
        }

        /** Skips an undecodable track and keeps playing; the controller surfaces the message. */
        override fun onPlayerError(error: PlaybackException) {
            if (exoPlayer.hasNextMediaItem()) {
                exoPlayer.seekToNextMediaItem()
                exoPlayer.prepare()
                exoPlayer.play()
            }
        }
    }

    /** Session callback resolving id-only requests and system playback resumption. */
    private inner class SessionCallback : MediaSession.Callback {

        /** Resolves controller-sent id-only items to playable items, dropping unknown ids. */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> = scope.future {
            val ids = mediaItems.mapNotNull { it.trackId }
            repository.tracksByIds(ids).map { it.toMediaItem() }.toMutableList()
        }

        /** Rebuilds the last queue when the system (e.g. a Bluetooth play key) resumes playback. */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaItemsWithStartPosition> = scope.future {
            val snapshot = queueStore.load() ?: throw UnsupportedOperationException("No queue to resume")
            val tracks = repository.tracksByIds(snapshot.trackIds)
            if (tracks.isEmpty()) throw UnsupportedOperationException("Queue tracks no longer exist")
            val currentId = snapshot.trackIds[snapshot.index]
            val index = tracks.indexOfFirst { it.id == currentId }
            MediaItemsWithStartPosition(
                tracks.map { it.toMediaItem() },
                index.coerceAtLeast(0),
                if (index >= 0) snapshot.positionMs else 0L,
            )
        }
    }

    /**
     * Player wrapper that implements shuffle as a real reorder of upcoming items (current stays,
     * the rest shuffle; turning it off restores the original order). The wrapped ExoPlayer uses an
     * identity shuffle order, so its native shuffle flag only reports state to every controller.
     */
    private class ShufflingPlayer(private val exo: ExoPlayer) : ForwardingPlayer(exo) {
        private var originalOrder: List<MediaItem>? = null

        /** Reorders the playlist, then updates the reported shuffle flag. */
        override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
            if (shuffleModeEnabled == exo.shuffleModeEnabled || exo.mediaItemCount == 0) {
                super.setShuffleModeEnabled(shuffleModeEnabled)
                return
            }
            val items = (0 until exo.mediaItemCount).map { exo.getMediaItemAt(it) }
            val current = exo.currentMediaItemIndex
            if (shuffleModeEnabled) {
                originalOrder = items
                applyOrder(ShuffleRules.shuffled(items, current), 0)
            } else {
                val original = originalOrder
                if (original != null) {
                    val (order, index) = ShuffleRules.restored(items, current, original)
                    applyOrder(order, index)
                }
                originalOrder = null
            }
            super.setShuffleModeEnabled(shuffleModeEnabled)
        }

        /** A new playlist ends any shuffle session. */
        override fun setMediaItems(mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long) {
            resetShuffle()
            super.setMediaItems(mediaItems, startIndex, startPositionMs)
        }

        /** A new playlist ends any shuffle session. */
        override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
            resetShuffle()
            super.setMediaItems(mediaItems, resetPosition)
        }

        /** A new playlist ends any shuffle session. */
        override fun setMediaItems(mediaItems: MutableList<MediaItem>) {
            resetShuffle()
            super.setMediaItems(mediaItems)
        }

        /** Clears shuffle state without reordering. */
        private fun resetShuffle() {
            originalOrder = null
            if (exo.shuffleModeEnabled) exo.shuffleModeEnabled = false
        }

        /** Rebuilds the playlist around the current item without interrupting its playback. */
        private fun applyOrder(order: List<MediaItem>, currentIndexInOrder: Int) {
            val current = exo.currentMediaItemIndex
            val count = exo.mediaItemCount
            if (current + 1 < count) exo.removeMediaItems(current + 1, count)
            if (current > 0) exo.removeMediaItems(0, current)
            exo.addMediaItems(0, order.subList(0, currentIndexInOrder))
            exo.addMediaItems(order.subList(currentIndexInOrder + 1, order.size))
        }
    }

    private companion object {
        const val SAVE_DEBOUNCE_MS = 1_000L
        const val POSITION_SAVE_INTERVAL_MS = 10_000L
    }
}
