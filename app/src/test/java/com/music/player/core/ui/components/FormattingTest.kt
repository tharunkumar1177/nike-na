/*
 * Unit tests for duration and count formatting.
 */
package com.music.player.core.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Verifies [Formatting]. */
class FormattingTest {

    /** Short and long durations format with the right fields. */
    @Test
    fun formatsDurations() {
        assertThat(Formatting.duration(0)).isEqualTo("0:00")
        assertThat(Formatting.duration(65_000)).isEqualTo("1:05")
        assertThat(Formatting.duration(3_725_000)).isEqualTo("1:02:05")
        assertThat(Formatting.duration(-5)).isEqualTo("0:00")
    }

    /** Totals use minutes and hours. */
    @Test
    fun formatsTotals() {
        assertThat(Formatting.totalDuration(42 * 60_000L)).isEqualTo("42 min")
        assertThat(Formatting.totalDuration(65 * 60_000L)).isEqualTo("1 hr 5 min")
    }

    /** Counts are pluralized. */
    @Test
    fun pluralizes() {
        assertThat(Formatting.songs(1)).isEqualTo("1 song")
        assertThat(Formatting.songs(3)).isEqualTo("3 songs")
        assertThat(Formatting.albums(1)).isEqualTo("1 album")
    }
}
