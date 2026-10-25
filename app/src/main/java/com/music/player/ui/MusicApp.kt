/*
 * App root: theme seeded from the current artwork, permission gate, bottom navigation with the
 * four top-level destinations, the shared track actions, the player overlay, the queue sheet,
 * and the snackbar host. See architecture.md "UI shell".
 */
package com.music.player.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.music.player.core.model.LibraryAccess
import com.music.player.core.model.Track
import com.music.player.core.model.uri
import com.music.player.core.ui.components.LocalTrackActions
import com.music.player.core.ui.components.TrackActions
import com.music.player.core.ui.components.TrackActionsSheet
import com.music.player.core.ui.theme.MusicTheme
import com.music.player.core.ui.theme.Motion
import com.music.player.core.ui.theme.Sizes
import com.music.player.core.ui.theme.Spacing
import com.music.player.core.ui.theme.rememberArtworkSeed
import com.music.player.core.ui.theme.rememberReducedMotion
import com.music.player.feature.library.AlbumScreen
import com.music.player.feature.library.ArtistScreen
import com.music.player.feature.library.FolderScreen
import com.music.player.feature.library.LibraryScreen
import com.music.player.feature.library.PermissionScreen
import com.music.player.feature.player.PlayerSheet
import com.music.player.feature.player.PlayerUiActions
import com.music.player.feature.player.QueueActions
import com.music.player.feature.player.QueueSheet
import com.music.player.feature.playlists.AddToPlaylistSheet
import com.music.player.feature.playlists.FavoritesScreen
import com.music.player.feature.playlists.PlaylistEdits
import com.music.player.feature.playlists.PlaylistScreen
import com.music.player.feature.playlists.PlaylistsScreen
import com.music.player.feature.search.SearchScreen
import com.music.player.feature.settings.SettingsScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/** One bottom-navigation destination. */
private data class TopLevel(val route: Any, val label: String, val icon: ImageVector)

/** Bottom navigation destinations in display order. */
private val TopLevels = listOf(
    TopLevel(LibraryRoute, "Library", Icons.Rounded.LibraryMusic),
    TopLevel(PlaylistsRoute, "Playlists", Icons.AutoMirrored.Rounded.QueueMusic),
    TopLevel(SearchRoute, "Search", Icons.Rounded.Search),
    TopLevel(SettingsRoute, "Settings", Icons.Rounded.Settings),
)

/**
 * Root composable.
 *
 * @param openPlayerRequests emits when the notification asks to show Now Playing.
 */
@Composable
fun MusicApp(openPlayerRequests: Flow<Unit>, viewModel: MainViewModel = koinViewModel()) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val access by viewModel.access.collectAsStateWithLifecycle()
    val state by viewModel.playerState.collectAsStateWithLifecycle()
    val seed by rememberArtworkSeed(state.current?.albumId, appearance.artworkColors)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshAccess() }

    MusicTheme(themeMode = appearance.themeMode, seedColor = seed) {
        when (access) {
            LibraryAccess.Denied -> PermissionScreen(onResult = viewModel::refreshAccess)
            LibraryAccess.Granted -> Shell(viewModel, openPlayerRequests)
        }
    }
}

/** Navigation shell with the player overlay; shown once audio access is granted. */
@Composable
private fun Shell(viewModel: MainViewModel, openPlayerRequests: Flow<Unit>) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val reduced = rememberReducedMotion()
    val nav = rememberNavController()
    val state by viewModel.playerState.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val expansion = remember { Animatable(0f) }
    var barHeightPx by remember { mutableIntStateOf(0) }
    var actionTrack by remember { mutableStateOf<Track?>(null) }
    var playlistTarget by remember { mutableStateOf<List<Track>?>(null) }
    var queueOpen by rememberSaveable { mutableStateOf(false) }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.message("Moved to trash")
    }

    /** Animates the player to [target] (0 = mini, 1 = full). */
    fun settlePlayer(target: Float) {
        scope.launch { if (reduced) expansion.snapTo(target) else expansion.animateTo(target, Motion.playerSpring()) }
    }

    /** Navigates to a detail destination, collapsing the player first. */
    fun openDetail(route: Any) {
        settlePlayer(0f)
        nav.navigate(route) { launchSingleTop = true }
    }

    /** Runs [block] with the full track for the current queue item. */
    fun withCurrentTrack(block: (Track) -> Unit) {
        val id = state.current?.trackId ?: return
        scope.launch { viewModel.track(id)?.let(block) }
    }

    /** Asks the system to move [track] to the trash; the library updates through MediaStore. */
    fun requestDelete(track: Track) {
        val request = MediaStore.createTrashRequest(context.contentResolver, listOf(track.uri), true)
        deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            launch {
                snackbar.currentSnackbarData?.dismiss()
                val result = snackbar.showSnackbar(message.text, message.actionLabel, duration = SnackbarDuration.Short)
                if (result == SnackbarResult.ActionPerformed) message.onAction?.invoke()
            }
        }
    }
    LaunchedEffect(openPlayerRequests) {
        openPlayerRequests.collect { if (viewModel.playerState.value.hasMedia) settlePlayer(1f) }
    }

    val trackActions = remember(viewModel) {
        object : TrackActions {
            override fun play(tracks: List<Track>, index: Int) = viewModel.play(tracks, index)
            override fun shuffle(tracks: List<Track>) = viewModel.shuffle(tracks)
            override fun showActions(track: Track) {
                actionTrack = track
            }
            override fun playNext(tracks: List<Track>) = viewModel.playNext(tracks)
            override fun addToQueue(tracks: List<Track>) = viewModel.addToQueue(tracks)
            override fun addToPlaylist(tracks: List<Track>) {
                playlistTarget = tracks
            }
        }
    }
    val edits = remember(viewModel) {
        object : PlaylistEdits {
            override fun deletePlaylist(playlistId: Long, name: String) = viewModel.deletePlaylist(playlistId, name)
            override fun removeFromPlaylist(playlistId: Long, title: String, remaining: List<Long>, previous: List<Long>) =
                viewModel.removeFromPlaylist(playlistId, title, remaining, previous)
            override fun removeFavorite(track: Track) = viewModel.removeFavorite(track)
        }
    }
    val playerActions = remember(viewModel) {
        object : PlayerUiActions {
            override fun togglePlayPause() = viewModel.togglePlayPause()
            override fun next() = viewModel.next()
            override fun previous() = viewModel.previous()
            override fun seekTo(ms: Long) = viewModel.seekTo(ms)
            override fun toggleShuffle() = viewModel.toggleShuffle()
            override fun cycleRepeat() = viewModel.cycleRepeat()
            override fun toggleFavorite() {
                val id = state.current?.trackId ?: return
                viewModel.setFavorite(id, id !in favorites)
            }
            override fun openQueue() {
                queueOpen = true
            }
            override fun addToPlaylist() = withCurrentTrack { playlistTarget = listOf(it) }
            override fun goToAlbum() {
                val albumId = state.current?.albumId ?: return
                if (albumId >= 0) openDetail(AlbumRoute(albumId))
            }
            override fun goToArtist() = withCurrentTrack { openDetail(ArtistRoute(it.artistId)) }
            override fun share() = withCurrentTrack { shareTrack(context, it) }
        }
    }
    val queueActions = remember(viewModel) {
        object : QueueActions {
            override fun skipTo(index: Int) = viewModel.skipTo(index)
            override fun move(from: Int, to: Int) = viewModel.moveQueueItem(from, to)
            override fun remove(index: Int) = viewModel.removeQueueItem(index)
            override fun insert(index: Int, trackId: Long) = viewModel.insertQueueItem(index, trackId)
            override fun toggleShuffle() = viewModel.toggleShuffle()
            override fun cycleRepeat() = viewModel.cycleRepeat()
        }
    }

    val barHeight = with(density) { barHeightPx.toDp() }
    val miniSpace = if (state.hasMedia) Sizes.miniPlayerHeight + Spacing.xs * 2 else 0.dp
    val currentTrackId = state.current?.trackId

    CompositionLocalProvider(LocalTrackActions provides trackActions) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = { BottomBar(nav, Modifier.onSizeChanged { barHeightPx = it.height }) },
            ) { inner ->
                val padding = PaddingValues(bottom = inner.calculateBottomPadding() + miniSpace)
                NavHost(nav, startDestination = LibraryRoute, modifier = Modifier.fillMaxSize()) {
                    composable<LibraryRoute> {
                        LibraryScreen(
                            contentPadding = padding,
                            currentTrackId = currentTrackId,
                            onAlbum = { openDetail(AlbumRoute(it)) },
                            onArtist = { openDetail(ArtistRoute(it)) },
                            onFolder = { openDetail(FolderRoute(it)) },
                        )
                    }
                    composable<PlaylistsRoute> {
                        PlaylistsScreen(
                            contentPadding = padding,
                            onPlaylist = { openDetail(PlaylistRoute(it)) },
                            onFavorites = { openDetail(FavoritesRoute) },
                        )
                    }
                    composable<SearchRoute> {
                        SearchScreen(
                            contentPadding = padding,
                            currentTrackId = currentTrackId,
                            onAlbum = { openDetail(AlbumRoute(it)) },
                            onArtist = { openDetail(ArtistRoute(it)) },
                        )
                    }
                    composable<SettingsRoute> { SettingsScreen(contentPadding = padding) }
                    composable<AlbumRoute> { entry ->
                        AlbumScreen(
                            albumId = entry.toRoute<AlbumRoute>().albumId,
                            contentPadding = padding,
                            currentTrackId = currentTrackId,
                            onBack = { nav.popBackStack() },
                            onArtist = { openDetail(ArtistRoute(it)) },
                        )
                    }
                    composable<ArtistRoute> { entry ->
                        ArtistScreen(
                            artistId = entry.toRoute<ArtistRoute>().artistId,
                            contentPadding = padding,
                            currentTrackId = currentTrackId,
                            onBack = { nav.popBackStack() },
                            onAlbum = { openDetail(AlbumRoute(it)) },
                        )
                    }
                    composable<FolderRoute> { entry ->
                        FolderScreen(
                            path = entry.toRoute<FolderRoute>().path,
                            contentPadding = padding,
                            currentTrackId = currentTrackId,
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable<PlaylistRoute> { entry ->
                        PlaylistScreen(
                            playlistId = entry.toRoute<PlaylistRoute>().playlistId,
                            contentPadding = padding,
                            currentTrackId = currentTrackId,
                            edits = edits,
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable<FavoritesRoute> {
                        FavoritesScreen(
                            contentPadding = padding,
                            currentTrackId = currentTrackId,
                            edits = edits,
                            onBack = { nav.popBackStack() },
                        )
                    }
                }
            }

            if (state.hasMedia) {
                PlayerSheet(
                    state = state,
                    position = viewModel.position,
                    expansion = expansion,
                    collapsedBottom = barHeight,
                    isFavorite = currentTrackId != null && currentTrackId in favorites,
                    actions = playerActions,
                )
            }

            SnackbarHost(
                snackbar,
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = barHeight + miniSpace),
            )
        }

        actionTrack?.let { track ->
            val favorite = track.id in favorites
            TrackActionsSheet(
                track = track,
                isFavorite = favorite,
                onDismiss = { actionTrack = null },
                onPlayNext = { viewModel.playNext(listOf(track)) },
                onAddToQueue = { viewModel.addToQueue(listOf(track)) },
                onAddToPlaylist = { playlistTarget = listOf(track) },
                onToggleFavorite = { viewModel.setFavorite(track.id, !favorite) },
                onGoToAlbum = { openDetail(AlbumRoute(track.albumId)) },
                onGoToArtist = { openDetail(ArtistRoute(track.artistId)) },
                onShare = { shareTrack(context, track) },
                onDelete = { requestDelete(track) },
            )
        }
        playlistTarget?.let { tracks ->
            AddToPlaylistSheet(
                playlists = playlists,
                onDismiss = { playlistTarget = null },
                onPick = { playlist ->
                    playlistTarget = null
                    viewModel.addToPlaylist(playlist.id, playlist.name, tracks)
                },
                onCreate = { name ->
                    playlistTarget = null
                    viewModel.createPlaylist(name, tracks)
                },
            )
        }
        if (queueOpen && state.hasMedia) {
            QueueSheet(state = state, actions = queueActions, onDismiss = { queueOpen = false })
        }
    }
}

/**
 * Bottom navigation bar. The tab stays highlighted while its detail screens are open, and
 * reselecting it returns to the tab's root.
 */
@Composable
private fun BottomBar(nav: NavHostController, modifier: Modifier = Modifier) {
    val entry by nav.currentBackStackEntryAsState()
    var lastTopLevel by rememberSaveable { mutableIntStateOf(0) }
    val destination = entry?.destination
    val topLevelIndex = TopLevels.indexOfFirst { item -> destination?.hierarchy?.any { it.hasRoute(item.route::class) } == true }
    val selectedIndex = if (topLevelIndex >= 0) topLevelIndex else lastTopLevel
    SideEffect { lastTopLevel = selectedIndex }
    NavigationBar(modifier) {
        TopLevels.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (selected) {
                        nav.popBackStack(item.route, inclusive = false)
                    } else {
                        nav.navigate(item.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(item.label) },
            )
        }
    }
}

/** Opens the system share sheet for [track]'s audio file. */
private fun shareTrack(context: Context, track: Track) {
    val send = Intent(Intent.ACTION_SEND)
        .setType(track.mimeType.ifBlank { "audio/*" })
        .putExtra(Intent.EXTRA_STREAM, track.uri)
        .putExtra(Intent.EXTRA_TITLE, track.title)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, track.title))
}
