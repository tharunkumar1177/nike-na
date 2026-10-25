/*
 * Pure list sorting and search-text helpers for the library. JVM-testable.
 */
package com.music.player.core.data

import com.music.player.core.model.Album
import com.music.player.core.model.Artist
import com.music.player.core.model.Folder
import com.music.player.core.model.SortField
import com.music.player.core.model.SortSpec
import com.music.player.core.model.Track
import java.text.Normalizer

/** Sorting rules per list type. Text compares case-insensitively; ties fall back to title. */
object LibrarySorting {
    private val text: Comparator<String> = String.CASE_INSENSITIVE_ORDER

    /** Sorts tracks by [spec]. */
    fun tracks(list: List<Track>, spec: SortSpec): List<Track> {
        val primary: Comparator<Track> = when (spec.field) {
            SortField.Title -> compareBy(text) { it.title }
            SortField.Artist -> compareBy(text) { it.artist }
            SortField.Album -> compareBy<Track, String>(text) { it.album }.thenBy { it.discNumber }.thenBy { it.trackNumber }
            SortField.DateAdded -> compareBy { it.dateAddedSec }
            SortField.Duration -> compareBy { it.durationMs }
        }
        return list.sortedWith(primary.directed(spec.ascending).thenBy(text) { it.title })
    }

    /** Sorts albums by [spec]; Title and Album both sort by album title. */
    fun albums(list: List<Album>, spec: SortSpec): List<Album> {
        val primary: Comparator<Album> = when (spec.field) {
            SortField.Title, SortField.Album -> compareBy(text) { it.title }
            SortField.Artist -> compareBy(text) { it.artist }
            SortField.DateAdded -> compareBy { it.dateAddedSec }
            SortField.Duration -> compareBy { it.durationMs }
        }
        return list.sortedWith(primary.directed(spec.ascending).thenBy(text) { it.title })
    }

    /** Sorts artists by name, or by track count for Duration/DateAdded choices. */
    fun artists(list: List<Artist>, spec: SortSpec): List<Artist> {
        val primary: Comparator<Artist> = when (spec.field) {
            SortField.Title, SortField.Artist -> compareBy(text) { it.name }
            SortField.Album -> compareBy { it.albumCount }
            SortField.DateAdded, SortField.Duration -> compareBy { it.trackCount }
        }
        return list.sortedWith(primary.directed(spec.ascending).thenBy(text) { it.name })
    }

    /** Sorts folders by name, or by track count for non-text choices. */
    fun folders(list: List<Folder>, spec: SortSpec): List<Folder> {
        val primary: Comparator<Folder> = when (spec.field) {
            SortField.Title, SortField.Artist, SortField.Album -> compareBy(text) { it.name }
            SortField.DateAdded, SortField.Duration -> compareBy { it.trackCount }
        }
        return list.sortedWith(primary.directed(spec.ascending).thenBy(text) { it.path })
    }

    /** Returns this comparator or its reverse. */
    private fun <T> Comparator<T>.directed(ascending: Boolean): Comparator<T> = if (ascending) this else reversed()
}

/** Search normalization and FTS expression building. */
object SearchText {
    private val diacritics = Regex("\\p{Mn}+")
    private val nonWord = Regex("[^\\p{L}\\p{N}]+")

    /** Lowercases and strips accents: "Beyoncé" -> "beyonce". */
    fun normalize(s: String): String =
        diacritics.replace(Normalizer.normalize(s, Normalizer.Form.NFD), "").lowercase()

    /** Splits a user query into normalized word tokens. */
    fun tokens(query: String): List<String> =
        normalize(query).split(nonWord).filter { it.isNotEmpty() }

    /** FTS4 MATCH expression requiring every token as a prefix, or null when the query has no words. */
    fun ftsMatch(query: String): String? {
        val t = tokens(query)
        return if (t.isEmpty()) null else t.joinToString(" ") { "$it*" }
    }

    /** True when every token of [query] is a prefix of some word in [text]. */
    fun matches(text: String, queryTokens: List<String>): Boolean {
        if (queryTokens.isEmpty()) return false
        val words = normalize(text).split(nonWord)
        return queryTokens.all { q -> words.any { it.startsWith(q) } }
    }
}
