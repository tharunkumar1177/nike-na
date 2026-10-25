/*
 * Persists the playback queue snapshot in DataStore so it survives process death.
 */
package com.music.player.core.playback

import com.music.player.core.data.prefs.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Load/save of [QueueSnapshot] through [prefs]; writes happen on [scope]. */
class QueueStore(private val prefs: UserPreferences, private val scope: CoroutineScope) {

    /** Reads the last saved snapshot, or null when none or unreadable. */
    suspend fun load(): QueueSnapshot? = QueueSnapshot.decode(prefs.getString(KEY))

    /** Saves [snapshot] without blocking the caller; survives the caller's scope being cancelled. */
    fun saveAsync(snapshot: QueueSnapshot) {
        val encoded = QueueSnapshot.encode(snapshot)
        scope.launch { prefs.putString(KEY, encoded) }
    }

    private companion object {
        const val KEY = "playback_queue"
    }
}
