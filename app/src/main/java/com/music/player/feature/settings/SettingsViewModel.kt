/*
 * Settings state: appearance, library filter, rescan, and library statistics.
 */
package com.music.player.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.player.core.data.LibraryRepository
import com.music.player.core.data.prefs.AppearanceSettings
import com.music.player.core.data.prefs.UserPreferences
import com.music.player.core.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Settings screen ViewModel. */
class SettingsViewModel(private val prefs: UserPreferences, private val library: LibraryRepository) : ViewModel() {
    /** Appearance settings. */
    val appearance: StateFlow<AppearanceSettings> = prefs.appearance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppearanceSettings())

    /** Minimum track length in seconds. */
    val minDurationSec: StateFlow<Int> = prefs.minDurationSec.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 30)

    /** Number of indexed songs. */
    val trackCount: StateFlow<Int> = library.trackCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** True while indexing. */
    val scanning: StateFlow<Boolean> = library.scanning

    /** Sets light/dark preference. */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    /** Enables artwork-derived colors. */
    fun setArtworkColors(enabled: Boolean) {
        viewModelScope.launch { prefs.setArtworkColors(enabled) }
    }

    /** Saves the minimum length and re-syncs so the filter applies. */
    fun setMinDurationSec(seconds: Int) {
        viewModelScope.launch {
            prefs.setMinDurationSec(seconds)
            library.rescan()
        }
    }

    /** Forces a full rescan. */
    fun rescan() = library.rescan()
}
