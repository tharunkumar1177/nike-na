/*
 * Playlists screen (Favorites pinned first), playlist detail with drag-to-reorder and remove
 * with undo, Favorites collection, the create/rename dialog, and the add-to-playlist sheet.
 * Implements blueprints/playlists-favorites.md.
 */
package com.music.player.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.player.core.model.Playlist
import com.music.player.core.model.PlaylistEntry
import com.music.player.core.model.Track
import com.music.player.core.ui.components.ArtworkPlaceholder
import com.music.player.core.ui.components.EmptyState
import com.music.player.core.ui.components.Formatting
import com.music.player.core.ui.components.LocalTrackActions
import com.music.player.core.ui.components.PlayShuffleHeader
import com.music.player.core.ui.components.SongRow
import com.music.player.core.ui.components.clickableButton
import com.music.player.core.ui.theme.Elevation
import com.music.player.core.ui.theme.Sizes
import com.music.player.core.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Undoable collection edits, owned by the app root so undo outlives the screen. */
interface PlaylistEdits {
    /** Deletes playlist [playlistId] named [name] and offers undo. */
    fun deletePlaylist(playlistId: Long, name: String)

    /** Saves [remaining] as the playlist order after removing [title]; undo restores [previous]. */
    fun removeFromPlaylist(playlistId: Long, title: String, remaining: List<Long>, previous: List<Long>)

    /** Removes [track] from favorites and offers undo. */
    fun removeFavorite(track: Track)
}

/** Height of an empty state shown inside a scrolling list. */
private val InlineEmptyHeight = 320.dp

/** Playlists root with Favorites pinned and a "New playlist" button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    contentPadding: PaddingValues,
    onPlaylist: (Long) -> Unit,
    onFavorites: () -> Unit,
    viewModel: PlaylistsViewModel = koinViewModel(),
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    var creating by rememberSaveable { mutableStateOf(false) }
    val bottom = contentPadding.calculateBottomPadding()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(title = { Text("Playlists", style = MaterialTheme.typography.headlineMedium) })
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottom + FabClearance)) {
                item(key = "favorites") {
                    ListItem(
                        modifier = Modifier.clickableButton(onFavorites),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Box(
                                Modifier
                                    .size(CollectionArtwork)
                                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Rounded.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                        },
                        headlineContent = { Text("Favorites", style = MaterialTheme.typography.titleMedium) },
                        supportingContent = { Text(Formatting.songs(favorites.size)) },
                    )
                }
                val list = playlists
                if (list != null && list.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            message = "No playlists yet",
                            hint = "Create one, or use \u201CAdd to playlist\u201D on any song.",
                            icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                            modifier = Modifier.fillMaxWidth().height(InlineEmptyHeight),
                        )
                    }
                }
                items(list.orEmpty(), key = { it.id }) { playlist -> PlaylistRow(playlist) { onPlaylist(playlist.id) } }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { creating = true },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("New playlist") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = bottom + Spacing.md, end = Spacing.md),
        )
    }
    if (creating) {
        NameDialog(title = "New playlist", initial = "", onDismiss = { creating = false }) {
            viewModel.create(it)
            creating = false
        }
    }
}

/** Space kept below the last row so the floating button never covers it. */
private val FabClearance = 88.dp

/** Size of a playlist's tile on the Playlists screen. */
private val CollectionArtwork = 56.dp

/** Row for one playlist. */
@Composable
private fun PlaylistRow(playlist: Playlist, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickableButton(onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            ArtworkPlaceholder(
                playlist.name,
                Modifier.size(CollectionArtwork).clip(MaterialTheme.shapes.medium),
                MaterialTheme.typography.titleMedium,
            )
        },
        headlineContent = { Text(playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text("${Formatting.songs(playlist.trackCount)} • ${Formatting.totalDuration(playlist.durationMs)}") },
    )
}

/** Playlist detail: play/shuffle, drag-to-reorder by handle, remove, rename, and delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlistId: Long,
    contentPadding: PaddingValues,
    currentTrackId: Long?,
    edits: PlaylistEdits,
    onBack: () -> Unit,
) {
    val viewModel: PlaylistDetailViewModel = koinViewModel(key = "playlist-$playlistId") { parametersOf(playlistId) }
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val loaded by viewModel.entries.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current
    var renaming by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var entries by remember(loaded) { mutableStateOf(loaded.orEmpty()) }
    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        val f = entries.indexOfFirst { it.entryId == from.key }
        val t = entries.indexOfFirst { it.entryId == to.key }
        if (f >= 0 && t >= 0) entries = entries.toMutableList().apply { add(t, removeAt(f)) }
    }
    val playable = entries.mapNotNull { it.track }
    val name = playlist?.name.orEmpty()

    /** Removes [entry] from the playlist with undo. */
    fun remove(entry: PlaylistEntry) {
        val previous = entries.map { it.trackId }
        val remaining = entries.filter { it.entryId != entry.entryId }.map { it.trackId }
        edits.removeFromPlaylist(playlistId, entry.track?.title ?: "unavailable song", remaining, previous)
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } },
            actions = {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "Playlist options") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                menu = false
                                renaming = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete playlist") },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                            onClick = {
                                menu = false
                                edits.deletePlaylist(playlistId, name)
                                onBack()
                            },
                        )
                    }
                }
            },
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
        ) {
            item(key = "summary") {
                Text(
                    "${Formatting.songs(playlist?.trackCount ?: 0)} • ${Formatting.totalDuration(playlist?.durationMs ?: 0)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.md),
                )
            }
            if (playable.isNotEmpty()) {
                item(key = "play") { PlayShuffleHeader(onPlay = { actions.play(playable) }, onShuffle = { actions.shuffle(playable) }) }
            }
            if (loaded != null && entries.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        message = "This playlist is empty",
                        hint = "Use \u201CAdd to playlist\u201D on any song.",
                        icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        modifier = Modifier.fillMaxWidth().height(InlineEmptyHeight),
                    )
                }
            }
            items(entries, key = { it.entryId }) { entry ->
                ReorderableItem(reorder, key = entry.entryId) { isDragging ->
                    val elevation = if (isDragging) Elevation.dragged else 0.dp
                    Surface(tonalElevation = elevation, shadowElevation = elevation) {
                        val handle: @Composable () -> Unit = {
                            IconButton(
                                onClick = {},
                                modifier = Modifier.draggableHandle(onDragStopped = { viewModel.setOrder(entries.map { it.trackId }) }),
                            ) { Icon(Icons.Rounded.DragHandle, contentDescription = "Reorder") }
                        }
                        val track = entry.track
                        if (track != null) {
                            SongRow(
                                track = track,
                                isCurrent = track.id == currentTrackId,
                                onClick = { actions.play(playable, playable.indexOf(track).coerceAtLeast(0)) },
                                onMore = { actions.showActions(track) },
                                trailing = {
                                    IconButton(onClick = { remove(entry) }) {
                                        Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Remove ${track.title} from playlist")
                                    }
                                    handle()
                                },
                            )
                        } else {
                            UnavailableRow(handle = handle, onRemove = { remove(entry) })
                        }
                    }
                }
            }
        }
    }
    if (renaming) {
        NameDialog(title = "Rename playlist", initial = name, onDismiss = { renaming = false }) {
            viewModel.rename(it)
            renaming = false
        }
    }
}

/** Row for an entry whose file is no longer on the device. */
@Composable
private fun UnavailableRow(handle: @Composable () -> Unit, onRemove: () -> Unit) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.height(Sizes.listRowHeight),
        headlineContent = { Text("Unavailable song", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        supportingContent = { Text("The file is no longer on this device") },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRemove) { Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Remove from playlist") }
                handle()
            }
        },
    )
}

/** Favorites list; tapping the heart removes a song with undo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    contentPadding: PaddingValues,
    currentTrackId: Long?,
    edits: PlaylistEdits,
    onBack: () -> Unit,
    viewModel: FavoritesViewModel = koinViewModel(),
) {
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current
    val list = tracks.orEmpty()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Favorites") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } },
        )
        if (tracks != null && list.isEmpty()) {
            EmptyState(
                message = "No favorites yet",
                hint = "Tap the heart on the player or in a song's menu.",
                icon = Icons.Rounded.Favorite,
            )
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding())) {
                if (list.isNotEmpty()) {
                    item(key = "play") { PlayShuffleHeader(onPlay = { actions.play(list) }, onShuffle = { actions.shuffle(list) }) }
                }
                itemsIndexed(list, key = { _, t -> t.id }) { index, track ->
                    SongRow(
                        track = track,
                        isCurrent = track.id == currentTrackId,
                        onClick = { actions.play(list, index) },
                        onMore = { actions.showActions(track) },
                        trailing = {
                            IconButton(onClick = { edits.removeFavorite(track) }) {
                                Icon(
                                    Icons.Rounded.Favorite,
                                    contentDescription = "Remove ${track.title} from favorites",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

/** Create/rename dialog; Save is enabled only for a non-blank name. */
@Composable
fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(MAX_NAME_LENGTH) },
                singleLine = true,
                label = { Text("Name") },
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text.trim()) }, enabled = text.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Longest playlist name accepted. */
private const val MAX_NAME_LENGTH = 80

/** Sheet to pick a playlist, or create one, for the pending songs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPick: (Playlist) -> Unit,
    onCreate: (String) -> Unit,
) {
    var creating by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text(
            "Add to playlist",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        )
        LazyColumn(Modifier.navigationBarsPadding()) {
            item(key = "new") {
                ListItem(
                    modifier = Modifier.clickableButton { creating = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    headlineContent = { Text("New playlist") },
                )
            }
            items(playlists, key = { it.id }) { p ->
                ListItem(
                    modifier = Modifier.clickableButton { onPick(p) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = {
                        ArtworkPlaceholder(
                            p.name,
                            Modifier.size(Sizes.listArtwork).clip(MaterialTheme.shapes.small),
                            MaterialTheme.typography.labelLarge,
                        )
                    },
                    headlineContent = { Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text(Formatting.songs(p.trackCount)) },
                )
            }
        }
    }
    if (creating) {
        NameDialog(title = "New playlist", initial = "", onDismiss = { creating = false }) {
            creating = false
            onCreate(it)
        }
    }
}
