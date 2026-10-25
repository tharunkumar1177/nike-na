/*
 * UI-facing playback state exposed by PlayerController.
 */
package com.music.player.core.playback

/** Repeat behavior. */
enum class RepeatMode { Off, All, One }

/** One queue row. [index] is its position in play order. */
data class QueueEntry(
    val index: Int,
    val trackId: Long,
    val title: String,
    val artist: String,
    val albumId: Long,
    val durationMs: Long,
)

/** Snapshot of the player. Position is exposed separately to avoid recomposing everything per tick. */
data class PlayerState(
    val connected: Boolean = false,
    val current: QueueEntry? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.Off,
    val queue: List<QueueEntry> = emptyList(),
    val currentIndex: Int = -1,
) {
    /** True when something is loaded and the mini player should show. */
    val hasMedia: Boolean get() = current != null

    /** Items after the current one. */
    val upNext: List<QueueEntry> get() = if (currentIndex < 0) emptyList() else queue.drop(currentIndex + 1)
}
