/*
 * Pure queue rules used by the playback engine: play-next insertion order, shuffle and
 * un-shuffle ordering, and the persisted queue snapshot codec. JVM-testable.
 */
package com.music.player.core.playback

import kotlin.random.Random

/**
 * Tracks where the next "Play next" insert goes so repeated calls keep their call order:
 * each insert lands after the previous play-next items, not directly after the current track.
 * The run resets whenever the current item changes or the queue is edited by other means.
 */
class PlayNextCursor {
    private var anchor = -1
    private var inserted = 0

    /** Returns the insertion index for a play-next while [currentIndex] is playing in a queue of [queueSize]. */
    fun insertionIndex(currentIndex: Int, queueSize: Int): Int {
        if (currentIndex != anchor) {
            anchor = currentIndex
            inserted = 0
        }
        return (currentIndex + 1 + inserted).coerceIn(0, queueSize)
    }

    /** Records that [count] items were inserted at the last returned index. */
    fun onInserted(count: Int) {
        inserted += count
    }

    /** Ends the current play-next run. */
    fun reset() {
        anchor = -1
        inserted = 0
    }
}

/** Shuffle ordering. Items are compared by identity so duplicate tracks stay distinct. */
object ShuffleRules {

    /** Returns [items] with the item at [currentIndex] first and the rest randomly ordered. */
    fun <T : Any> shuffled(items: List<T>, currentIndex: Int, random: Random = Random.Default): List<T> {
        if (items.isEmpty()) return items
        val current = items[currentIndex.coerceIn(items.indices)]
        val others = items.filterIndexed { i, _ -> i != currentIndex.coerceIn(items.indices) }.shuffled(random)
        return listOf(current) + others
    }

    /**
     * Restores [original] order for the items still present in [items], keeping items added after
     * shuffling (not in [original]) directly after the current item in their present order.
     *
     * @return the restored list and the new index of the current item.
     */
    fun <T : Any> restored(items: List<T>, currentIndex: Int, original: List<T>): Pair<List<T>, Int> {
        if (items.isEmpty()) return items to 0
        val current = items[currentIndex.coerceIn(items.indices)]
        val present = items.filter { item -> original.any { it === item } }
        val added = items.filter { item -> original.none { it === item } && item !== current }
        val ordered = present.sortedBy { item -> original.indexOfFirst { it === item } }.toMutableList()
        if (ordered.none { it === current }) ordered.add(0, current)
        val at = ordered.indexOfFirst { it === current }
        ordered.addAll(at + 1, added)
        return ordered to at
    }
}

/** Persisted playback queue. */
data class QueueSnapshot(
    val trackIds: List<Long>,
    val index: Int,
    val positionMs: Long,
    val shuffle: Boolean,
    val repeatMode: Int,
) {
    companion object {
        private const val VERSION = "v1"

        /** Encodes a snapshot as `v1|index|position|shuffle|repeat|id,id,...`. */
        fun encode(s: QueueSnapshot): String =
            listOf(VERSION, s.index, s.positionMs, if (s.shuffle) 1 else 0, s.repeatMode, s.trackIds.joinToString(","))
                .joinToString("|")

        /** Decodes [raw], returning null for missing, foreign, or corrupt data. */
        fun decode(raw: String?): QueueSnapshot? {
            val parts = raw?.split('|') ?: return null
            if (parts.size != 6 || parts[0] != VERSION) return null
            val ids = if (parts[5].isEmpty()) emptyList() else parts[5].split(',').map { it.toLongOrNull() ?: return null }
            if (ids.isEmpty()) return null
            val index = parts[1].toIntOrNull() ?: return null
            return QueueSnapshot(
                trackIds = ids,
                index = index.coerceIn(ids.indices),
                positionMs = (parts[2].toLongOrNull() ?: 0L).coerceAtLeast(0L),
                shuffle = parts[3] == "1",
                repeatMode = parts[4].toIntOrNull()?.coerceIn(0, 2) ?: 0,
            )
        }
    }
}
