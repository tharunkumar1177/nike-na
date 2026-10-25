/*
 * Pure rules for turning raw MediaStore values into clean library fields, and for planning
 * which cached rows a sync must delete. No Android dependencies, so they are unit tested on the JVM.
 */
package com.music.player.core.data.sync

/** Tag normalization and fallbacks. */
object TagRules {
    const val UNKNOWN_ARTIST = "Unknown artist"
    const val UNKNOWN_ALBUM = "Unknown album"
    private const val MEDIASTORE_UNKNOWN = "<unknown>"
    private const val DISC_FACTOR = 1000

    /** Returns [raw] trimmed, or null when blank or MediaStore's `<unknown>` marker. */
    fun clean(raw: String?): String? {
        val v = raw?.trim()
        return if (v.isNullOrEmpty() || v.equals(MEDIASTORE_UNKNOWN, ignoreCase = true)) null else v
    }

    /** Title, falling back to the file name without its extension. */
    fun title(raw: String?, fileName: String): String =
        clean(raw) ?: fileName.substringBeforeLast('.', fileName).ifBlank { fileName }

    /** Artist, falling back to [UNKNOWN_ARTIST]. */
    fun artist(raw: String?): String = clean(raw) ?: UNKNOWN_ARTIST

    /** Album, falling back to the containing folder name, then [UNKNOWN_ALBUM]. */
    fun album(raw: String?, folderPath: String): String = clean(raw) ?: folderName(folderPath).ifEmpty { UNKNOWN_ALBUM }

    /** Last segment of a relative path like `Music/Rock/` -> `Rock`; empty for the volume root. */
    fun folderName(relativePath: String): String = relativePath.trimEnd('/').substringAfterLast('/')

    /** Track number within its disc from MediaStore's combined `TRACK` value (disc * 1000 + track). */
    fun trackNumber(rawTrack: Int): Int = if (rawTrack <= 0) 0 else rawTrack % DISC_FACTOR

    /** Disc number from MediaStore's combined `TRACK` value; 1 when unspecified. */
    fun discNumber(rawTrack: Int): Int = if (rawTrack < DISC_FACTOR) 1 else rawTrack / DISC_FACTOR
}

/** Sync planning. */
object SyncPlanner {
    /** Ids cached for a volume that MediaStore no longer reports and must be deleted. */
    fun deletions(cachedIds: Collection<Long>, currentIds: Collection<Long>): Set<Long> {
        val current = currentIds.toHashSet()
        return cachedIds.filterNotTo(HashSet()) { it in current }
    }

    /**
     * Whether a volume needs a full rescan: never synced, MediaStore version changed (the
     * provider was rebuilt and generations reset), or the generation went backwards.
     */
    fun needsFullScan(lastVersion: String?, lastGeneration: Long?, version: String, generation: Long): Boolean =
        lastVersion == null || lastGeneration == null || lastVersion != version || generation < lastGeneration
}
