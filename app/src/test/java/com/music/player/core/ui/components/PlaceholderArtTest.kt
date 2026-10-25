/*
 * Unit tests for the deterministic artwork placeholder rules.
 */
package com.music.player.core.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Verifies placeholder hue and initials derivation. */
class PlaceholderArtTest {

    /** The same title always yields the same hues, offset by 40 degrees and within range. */
    @Test
    fun huesAreDeterministicAndInRange() {
        val a = PlaceholderArt.hues("Dark Side")
        val b = PlaceholderArt.hues("  dark side ")
        assertThat(a).isEqualTo(b)
        assertThat(a.start).isAtLeast(0f)
        assertThat(a.start).isLessThan(360f)
        assertThat(a.end).isEqualTo((a.start + 40f) % 360f)
    }

    /** Initials take the first letter of the first two words. */
    @Test
    fun initialsUseFirstTwoWords() {
        assertThat(PlaceholderArt.initials("dark side of the moon")).isEqualTo("DS")
        assertThat(PlaceholderArt.initials("Abbey")).isEqualTo("A")
    }

    /** Punctuation-only titles fall back to a note glyph. */
    @Test
    fun initialsFallBackForSymbols() {
        assertThat(PlaceholderArt.initials("--- ...")).isEqualTo("\u266A")
        assertThat(PlaceholderArt.initials("")).isEqualTo("\u266A")
    }
}
