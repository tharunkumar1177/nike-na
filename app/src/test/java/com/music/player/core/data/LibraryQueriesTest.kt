/*
 * Unit tests for library sorting and search text helpers.
 */
package com.music.player.core.data

import com.google.common.truth.Truth.assertThat
import com.music.player.core.model.SortField
import com.music.player.core.model.SortSpec
import com.music.player.core.model.Track
import org.junit.Test

/** Verifies [LibrarySorting] and [SearchText]. */
class LibraryQueriesTest {

    /** Builds a track with the fields relevant to sorting. */
    private fun track(id: Long, title: String, artist: String = "A", added: Long = 0, duration: Long = 0) = Track(
        id = id, title = title, artist = artist, artistId = 1, album = "X", albumId = 1, albumArtist = "",
        durationMs = duration, trackNumber = 1, discNumber = 1, year = 0, folderPath = "Music/",
        fileName = "$title.mp3", mimeType = "audio/mpeg", dateAddedSec = added, sizeBytes = 1,
    )

    /** Title sort is case-insensitive and respects direction. */
    @Test
    fun sortsTracksByTitleIgnoringCase() {
        val list = listOf(track(1, "beta"), track(2, "Alpha"), track(3, "gamma"))
        assertThat(LibrarySorting.tracks(list, SortSpec(SortField.Title)).map { it.id }).containsExactly(2L, 1L, 3L).inOrder()
        assertThat(LibrarySorting.tracks(list, SortSpec(SortField.Title, ascending = false)).map { it.id })
            .containsExactly(3L, 1L, 2L).inOrder()
    }

    /** Date-added sort uses the timestamp with title as tie-breaker. */
    @Test
    fun sortsTracksByDateAdded() {
        val list = listOf(track(1, "b", added = 5), track(2, "a", added = 5), track(3, "c", added = 1))
        assertThat(LibrarySorting.tracks(list, SortSpec(SortField.DateAdded)).map { it.id }).containsExactly(3L, 2L, 1L).inOrder()
    }

    /** Normalization strips accents and case. */
    @Test
    fun normalizesAccents() {
        assertThat(SearchText.normalize("Beyoncé")).isEqualTo("beyonce")
        assertThat(SearchText.tokens("  Sigur  Rós! ")).containsExactly("sigur", "ros").inOrder()
    }

    /** FTS expression uses prefix tokens and is null for symbol-only queries. */
    @Test
    fun buildsFtsMatch() {
        assertThat(SearchText.ftsMatch("dark sid")).isEqualTo("dark* sid*")
        assertThat(SearchText.ftsMatch("\"*()")).isNull()
    }

    /** In-memory matching requires every token as a word prefix. */
    @Test
    fun matchesWordPrefixes() {
        val tokens = SearchText.tokens("pink flo")
        assertThat(SearchText.matches("Pink Floyd", tokens)).isTrue()
        assertThat(SearchText.matches("Floyd Pinkerton", SearchText.tokens("loyd"))).isFalse()
    }
}
