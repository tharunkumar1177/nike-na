/*
 * Unit tests for play-next ordering, shuffle/restore ordering, and the queue snapshot codec.
 */
package com.music.player.core.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

/** Verifies [PlayNextCursor], [ShuffleRules], and [QueueSnapshot]. */
class QueueRulesTest {

    /** Repeated play-next calls keep call order after the current track. */
    @Test
    fun playNextKeepsCallOrder() {
        val cursor = PlayNextCursor()
        assertThat(cursor.insertionIndex(currentIndex = 2, queueSize = 10)).isEqualTo(3)
        cursor.onInserted(1)
        assertThat(cursor.insertionIndex(currentIndex = 2, queueSize = 11)).isEqualTo(4)
        cursor.onInserted(2)
        assertThat(cursor.insertionIndex(currentIndex = 2, queueSize = 13)).isEqualTo(6)
    }

    /** Moving to another track starts a new play-next run. */
    @Test
    fun playNextResetsOnTrackChange() {
        val cursor = PlayNextCursor()
        cursor.insertionIndex(0, 5)
        cursor.onInserted(3)
        assertThat(cursor.insertionIndex(currentIndex = 1, queueSize = 8)).isEqualTo(2)
    }

    /** Shuffle keeps the current item first and contains every item exactly once. */
    @Test
    fun shuffleKeepsCurrentFirst() {
        val items = (0 until 20).map { Item(it) }
        val shuffled = ShuffleRules.shuffled(items, currentIndex = 7, random = Random(42))
        assertThat(shuffled.first()).isSameInstanceAs(items[7])
        assertThat(shuffled).containsExactlyElementsIn(items)
    }

    /** Restoring brings back the original order and keeps items added during shuffle after the current one. */
    @Test
    fun restoreReturnsOriginalOrder() {
        val a = Item(1); val b = Item(2); val c = Item(3); val d = Item(4); val added = Item(9)
        val original = listOf(a, b, c, d)
        val shuffledNow = listOf(c, added, a, d, b)
        val (restored, index) = ShuffleRules.restored(shuffledNow, currentIndex = 0, original = original)
        assertThat(restored).containsExactly(a, b, c, added, d).inOrder()
        assertThat(index).isEqualTo(2)
    }

    /** Duplicate tracks are distinct queue items. */
    @Test
    fun restoreHandlesDuplicatesByIdentity() {
        val x1 = Item(1); val x2 = Item(1); val y = Item(2)
        val (restored, _) = ShuffleRules.restored(listOf(y, x2, x1), 0, listOf(x1, y, x2))
        assertThat(restored[0]).isSameInstanceAs(x1)
        assertThat(restored[2]).isSameInstanceAs(x2)
    }

    /** Snapshots round-trip, and corrupt input decodes to null. */
    @Test
    fun snapshotCodec() {
        val s = QueueSnapshot(listOf(5, 9, 12), index = 1, positionMs = 42_000, shuffle = true, repeatMode = 2)
        assertThat(QueueSnapshot.decode(QueueSnapshot.encode(s))).isEqualTo(s)
        assertThat(QueueSnapshot.decode(null)).isNull()
        assertThat(QueueSnapshot.decode("v0|1|2|0|0|1,2")).isNull()
        assertThat(QueueSnapshot.decode("v1|0|0|0|0|1,x")).isNull()
        assertThat(QueueSnapshot.decode("v1|0|0|0|0|")).isNull()
    }

    /** Out-of-range index is clamped. */
    @Test
    fun snapshotClampsIndex() {
        assertThat(QueueSnapshot.decode("v1|9|0|0|0|1,2")?.index).isEqualTo(1)
    }

    /** Identity-distinct test item (deliberately not a data class). */
    private class Item(val id: Int) {
        /** Debug label. */
        override fun toString(): String = "Item($id)"
    }
}
