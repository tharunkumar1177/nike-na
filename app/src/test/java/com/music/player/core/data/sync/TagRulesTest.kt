/*
 * Unit tests for MediaStore tag fallbacks and sync planning.
 */
package com.music.player.core.data.sync

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Verifies [TagRules] and [SyncPlanner]. */
class TagRulesTest {

    /** Blank and `<unknown>` values are treated as missing. */
    @Test
    fun cleanTreatsUnknownAsMissing() {
        assertThat(TagRules.clean("<unknown>")).isNull()
        assertThat(TagRules.clean("   ")).isNull()
        assertThat(TagRules.clean(null)).isNull()
        assertThat(TagRules.clean("  Abba ")).isEqualTo("Abba")
    }

    /** Missing titles fall back to the file name without extension. */
    @Test
    fun titleFallsBackToFileName() {
        assertThat(TagRules.title(null, "01 - Intro.mp3")).isEqualTo("01 - Intro")
        assertThat(TagRules.title("", "noext")).isEqualTo("noext")
        assertThat(TagRules.title("Real", "file.flac")).isEqualTo("Real")
    }

    /** Missing artist and album use the documented fallbacks. */
    @Test
    fun artistAndAlbumFallbacks() {
        assertThat(TagRules.artist("<unknown>")).isEqualTo(TagRules.UNKNOWN_ARTIST)
        assertThat(TagRules.album(null, "Music/Road Trip/")).isEqualTo("Road Trip")
        assertThat(TagRules.album(null, "")).isEqualTo(TagRules.UNKNOWN_ALBUM)
    }

    /** MediaStore's combined TRACK value splits into disc and track numbers. */
    @Test
    fun trackAndDiscNumbers() {
        assertThat(TagRules.trackNumber(2005)).isEqualTo(5)
        assertThat(TagRules.discNumber(2005)).isEqualTo(2)
        assertThat(TagRules.trackNumber(7)).isEqualTo(7)
        assertThat(TagRules.discNumber(7)).isEqualTo(1)
        assertThat(TagRules.trackNumber(0)).isEqualTo(0)
    }

    /** Deletions are cached ids absent from MediaStore. */
    @Test
    fun deletionsAreCachedMinusCurrent() {
        assertThat(SyncPlanner.deletions(listOf(1L, 2L, 3L), listOf(2L, 4L))).containsExactly(1L, 3L)
        assertThat(SyncPlanner.deletions(emptyList(), listOf(1L))).isEmpty()
    }

    /** A full scan is required when the cursor is missing, the version changed, or the generation regressed. */
    @Test
    fun fullScanConditions() {
        assertThat(SyncPlanner.needsFullScan(null, null, "v1", 10)).isTrue()
        assertThat(SyncPlanner.needsFullScan("v1", 10, "v2", 12)).isTrue()
        assertThat(SyncPlanner.needsFullScan("v1", 10, "v1", 5)).isTrue()
        assertThat(SyncPlanner.needsFullScan("v1", 10, "v1", 12)).isFalse()
    }
}
