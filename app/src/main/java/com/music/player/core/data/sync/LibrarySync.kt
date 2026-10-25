/*
 * Keeps the Room cache in step with MediaStore: generation-based incremental sync per volume,
 * id-set diff for deletions, and a debounced ContentObserver for changes made by other apps.
 * Implements architecture.md "Library synchronization".
 */
package com.music.player.core.data.sync

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.music.player.core.data.db.LibraryDao
import com.music.player.core.data.prefs.UserPreferences
import com.music.player.core.data.prefs.VolumeCursor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Runs library syncs; one at a time, never on the main thread. */
class LibrarySync(
    private val context: Context,
    private val scanner: MediaStoreScanner,
    private val dao: LibraryDao,
    private val prefs: UserPreferences,
    private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private val _scanning = MutableStateFlow(false)
    private var observerJob: Job? = null

    /** True while a sync is running. */
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    /** Starts an initial sync and change observation. Safe to call repeatedly. */
    @OptIn(FlowPreview::class)
    fun start() {
        if (observerJob?.isActive == true) return
        observerJob = scope.launch {
            sync(force = false)
            mediaStoreChanges().debounce(DEBOUNCE_MS).collect { sync(force = false) }
        }
    }

    /** Requests a sync in the background; [force] ignores stored cursors and rescans everything. */
    fun requestSync(force: Boolean = false) {
        scope.launch { sync(force) }
    }

    /** Performs one sync. Failures are logged and leave the cache as it was. */
    suspend fun sync(force: Boolean) = mutex.withLock {
        withContext(Dispatchers.IO) {
            _scanning.value = true
            try {
                if (force) prefs.clearVolumeCursors()
                val minDurationMs = prefs.minDurationSec.first() * 1000L
                val volumes = scanner.volumes(context)
                dao.deleteOutsideVolumes(volumes.toList())
                volumes.forEach { syncVolume(it, minDurationMs) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                Log.w(TAG, "Library access revoked during sync", e)
            } catch (e: Exception) {
                Log.e(TAG, "Library sync failed", e)
            } finally {
                _scanning.value = false
            }
        }
    }

    /** Syncs one MediaStore volume incrementally, or fully when its cursor is invalid. */
    private suspend fun syncVolume(volume: String, minDurationMs: Long) {
        val version = MediaStore.getVersion(context, volume)
        val generation = MediaStore.getGeneration(context, volume)
        val last = prefs.volumeCursor(volume)
        val full = SyncPlanner.needsFullScan(last?.version, last?.generation, version, generation)
        scanner.changedRows(volume, if (full) null else last?.generation, minDurationMs) { page ->
            dao.upsert(page)
        }
        val deletions = SyncPlanner.deletions(dao.idsForVolume(volume), scanner.currentIds(volume, minDurationMs))
        if (deletions.isNotEmpty()) dao.deleteByIds(deletions)
        prefs.setVolumeCursor(volume, VolumeCursor(version, generation))
    }

    /** Emits whenever any external audio row changes. */
    private fun mediaStoreChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(null) {
            /** Forwards the change notification. */
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(Unit)
            }
        }
        val uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        context.contentResolver.registerContentObserver(uri, true, observer)
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }

    private companion object {
        const val TAG = "LibrarySync"
        const val DEBOUNCE_MS = 1_000L
    }
}
