/*
 * Koin dependency graph: application scope, persistence, library sync, playback client,
 * and screen ViewModels.
 */
package com.music.player.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.music.player.core.data.LibraryRepository
import com.music.player.core.data.PlaylistRepository
import com.music.player.core.data.db.MusicDatabase
import com.music.player.core.data.prefs.UserPreferences
import com.music.player.core.data.sync.LibrarySync
import com.music.player.core.data.sync.MediaStoreScanner
import com.music.player.core.playback.PlayerController
import com.music.player.core.playback.QueueStore
import com.music.player.feature.library.AlbumViewModel
import com.music.player.feature.library.ArtistViewModel
import com.music.player.feature.library.FolderViewModel
import com.music.player.feature.library.LibraryViewModel
import com.music.player.feature.playlists.FavoritesViewModel
import com.music.player.feature.playlists.PlaylistDetailViewModel
import com.music.player.feature.playlists.PlaylistsViewModel
import com.music.player.feature.search.SearchViewModel
import com.music.player.feature.settings.SettingsViewModel
import com.music.player.ui.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Qualifier of the process-lifetime coroutine scope. */
val AppScope = named("appScope")

/** Persistence, library, and playback singletons. */
val dataModule = module {
    single<CoroutineScope>(AppScope) { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    single { MusicDatabase.create(androidContext()) }
    single { get<MusicDatabase>().libraryDao() }
    single { get<MusicDatabase>().playlistDao() }
    single {
        UserPreferences(PreferenceDataStoreFactory.create { androidContext().preferencesDataStoreFile("settings") })
    }
    single { MediaStoreScanner(androidContext().contentResolver) }
    single { LibrarySync(androidContext(), get(), get(), get(), get(AppScope)) }
    single { LibraryRepository(androidContext(), get(), get(), get()) }
    single { PlaylistRepository(get(), get()) }
    single { QueueStore(get(), get(AppScope)) }
    single { PlayerController(androidContext(), get(AppScope)) }
}

/** Screen ViewModels; detail screens receive their id as a parameter. */
val viewModelModule = module {
    viewModel { MainViewModel(get(), get(), get(), get()) }
    viewModel { LibraryViewModel(get()) }
    viewModel { (albumId: Long) -> AlbumViewModel(albumId, get()) }
    viewModel { (artistId: Long) -> ArtistViewModel(artistId, get()) }
    viewModel { (path: String) -> FolderViewModel(path, get()) }
    viewModel { PlaylistsViewModel(get()) }
    viewModel { (playlistId: Long) -> PlaylistDetailViewModel(playlistId, get()) }
    viewModel { FavoritesViewModel(get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { SettingsViewModel(get(), get()) }
}
