/*
 * Pure text formatting helpers for durations and counts.
 */
package com.music.player.core.ui.components

import java.util.Locale

/** Text formatting used across screens. */
object Formatting {

    /** Formats milliseconds as `m:ss` or `h:mm:ss`; negative values format as `0:00`. */
    fun duration(ms: Long): String {
        val total = (ms.coerceAtLeast(0L) / 1000L)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) else String.format(Locale.ROOT, "%d:%02d", m, s)
    }

    /** Formats a long total such as an album length: `42 min` or `1 hr 5 min`. */
    fun totalDuration(ms: Long): String {
        val minutes = (ms.coerceAtLeast(0L) / 60_000L)
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "$h hr $m min" else "$m min"
    }

    /** `1 song` / `12 songs`. */
    fun songs(count: Int): String = if (count == 1) "1 song" else "$count songs"

    /** `1 album` / `3 albums`. */
    fun albums(count: Int): String = if (count == 1) "1 album" else "$count albums"
}
