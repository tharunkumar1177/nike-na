/*
 * DataStore-backed preferences: appearance, library filters, per-tab sort choices, and the
 * per-volume MediaStore sync cursors (generation and version).
 */
package com.music.player.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.music.player.core.model.LibraryTab
import com.music.player.core.model.SortField
import com.music.player.core.model.SortSpec
import com.music.player.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Appearance settings. */
data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val artworkColors: Boolean = true,
)

/** Last synced MediaStore state of one volume. */
data class VolumeCursor(val version: String, val generation: Long)

/** Typed access to the app's preferences [store]. */
class UserPreferences(private val store: DataStore<Preferences>) {

    /** Appearance settings stream. */
    val appearance: Flow<AppearanceSettings> = store.data.map { p ->
        AppearanceSettings(
            themeMode = p[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.System,
            artworkColors = p[ARTWORK_COLORS] ?: true,
        )
    }.distinctUntilChanged()

    /** Minimum track length in seconds for inclusion in the library. */
    val minDurationSec: Flow<Int> = store.data.map { it[MIN_DURATION] ?: DEFAULT_MIN_DURATION_SEC }.distinctUntilChanged()

    /** Sort choice of [tab]. */
    fun sort(tab: LibraryTab): Flow<SortSpec> = store.data.map { p ->
        val field = p[sortFieldKey(tab)]?.let { runCatching { SortField.valueOf(it) }.getOrNull() } ?: SortField.Title
        SortSpec(field, p[sortAscKey(tab)] ?: true)
    }.distinctUntilChanged()

    /** Saves the theme mode. */
    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME] = mode.name }
    }

    /** Enables or disables artwork-derived colors. */
    suspend fun setArtworkColors(enabled: Boolean) {
        store.edit { it[ARTWORK_COLORS] = enabled }
    }

    /** Saves the minimum track length and invalidates sync cursors so the filter re-applies. */
    suspend fun setMinDurationSec(seconds: Int) {
        store.edit { p ->
            p[MIN_DURATION] = seconds.coerceIn(0, MAX_MIN_DURATION_SEC)
            p.asMap().keys.filter { it.name.startsWith(CURSOR_PREFIX) }.forEach { p.remove(it) }
        }
    }

    /** Saves the sort choice of [tab]. */
    suspend fun setSort(tab: LibraryTab, spec: SortSpec) {
        store.edit {
            it[sortFieldKey(tab)] = spec.field.name
            it[sortAscKey(tab)] = spec.ascending
        }
    }

    /** Reads the last synced cursor of [volume], or null when never synced. */
    suspend fun volumeCursor(volume: String): VolumeCursor? {
        val p = store.data.first()
        val version = p[stringPreferencesKey("${CURSOR_PREFIX}ver_$volume")] ?: return null
        val generation = p[longPreferencesKey("${CURSOR_PREFIX}gen_$volume")] ?: return null
        return VolumeCursor(version, generation)
    }

    /** Stores the synced cursor of [volume]. */
    suspend fun setVolumeCursor(volume: String, cursor: VolumeCursor) {
        store.edit {
            it[stringPreferencesKey("${CURSOR_PREFIX}ver_$volume")] = cursor.version
            it[longPreferencesKey("${CURSOR_PREFIX}gen_$volume")] = cursor.generation
        }
    }

    /** Forgets every sync cursor so the next sync is a full scan. */
    suspend fun clearVolumeCursors() {
        store.edit { p -> p.asMap().keys.filter { it.name.startsWith(CURSOR_PREFIX) }.forEach { p.remove(it) } }
    }

    /** Persists an arbitrary string value under [key] (used by playback state persistence). */
    suspend fun putString(key: String, value: String?) {
        store.edit { if (value == null) it.remove(stringPreferencesKey(key)) else it[stringPreferencesKey(key)] = value }
    }

    /** Reads a string stored with [putString]. */
    suspend fun getString(key: String): String? = store.data.first()[stringPreferencesKey(key)]

    private companion object {
        const val DEFAULT_MIN_DURATION_SEC = 30
        const val MAX_MIN_DURATION_SEC = 600
        const val CURSOR_PREFIX = "sync_"
        val THEME = stringPreferencesKey("theme_mode")
        val ARTWORK_COLORS = booleanPreferencesKey("artwork_colors")
        val MIN_DURATION = intPreferencesKey("min_duration_sec")

        /** Key of the sort field for [tab]. */
        fun sortFieldKey(tab: LibraryTab) = stringPreferencesKey("sort_field_${tab.name}")

        /** Key of the sort direction for [tab]. */
        fun sortAscKey(tab: LibraryTab) = booleanPreferencesKey("sort_asc_${tab.name}")
    }
}
