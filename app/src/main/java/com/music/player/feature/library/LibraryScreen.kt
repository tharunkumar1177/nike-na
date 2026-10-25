/*
 * Library screen: Songs, Albums, Artists, and Folders tabs with swipeable pages, per-tab sort,
 * and scanning/empty states.
 */
package com.music.player.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.player.core.model.LibraryTab
import com.music.player.core.model.SortField
import com.music.player.core.model.SortSpec
import com.music.player.core.ui.components.AlbumTile
import com.music.player.core.ui.components.ArtistRow
import com.music.player.core.ui.components.EmptyState
import com.music.player.core.ui.components.FolderRow
import com.music.player.core.ui.components.LoadingState
import com.music.player.core.ui.components.LocalTrackActions
import com.music.player.core.ui.components.PlayShuffleHeader
import com.music.player.core.ui.components.SongRow
import com.music.player.core.ui.theme.Spacing
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Library root.
 *
 * @param contentPadding bottom space for the mini player and navigation bar.
 * @param currentTrackId highlights the playing song.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    contentPadding: PaddingValues,
    currentTrackId: Long?,
    onAlbum: (Long) -> Unit,
    onArtist: (Long) -> Unit,
    onFolder: (String) -> Unit,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val tabs = LibraryTab.entries
    val pager = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val tab = tabs[pager.currentPage]
    val sort by viewModel.sorts.getValue(tab).collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        TopAppBar(
            title = { Text("Library", style = MaterialTheme.typography.headlineMedium) },
            actions = { SortMenu(tab, sort) { viewModel.setSort(tab, it) } },
            scrollBehavior = scrollBehavior,
        )
        PrimaryTabRow(selectedTabIndex = pager.currentPage) {
            tabs.forEachIndexed { i, t ->
                Tab(
                    selected = pager.currentPage == i,
                    onClick = { scope.launch { pager.animateScrollToPage(i) } },
                    text = { Text(t.name, style = MaterialTheme.typography.labelLarge) },
                )
            }
        }
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
            when (tabs[page]) {
                LibraryTab.Songs -> SongsTab(viewModel, contentPadding, currentTrackId)
                LibraryTab.Albums -> AlbumsTab(viewModel, contentPadding, onAlbum)
                LibraryTab.Artists -> ArtistsTab(viewModel, contentPadding, onArtist)
                LibraryTab.Folders -> FoldersTab(viewModel, contentPadding, onFolder)
            }
        }
    }
}

/** Songs list with a Play all / Shuffle header. */
@Composable
private fun SongsTab(viewModel: LibraryViewModel, padding: PaddingValues, currentTrackId: Long?) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current
    LibraryStates(viewModel, songs?.size) {
        val list = songs.orEmpty()
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            item(key = "header") { PlayShuffleHeader(onPlay = { actions.play(list) }, onShuffle = { actions.shuffle(list) }) }
            itemsIndexed(list, key = { _, t -> t.id }) { index, track ->
                SongRow(
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    onClick = { actions.play(list, index) },
                    onMore = { actions.showActions(track) },
                )
            }
        }
    }
}

/** Two-column album grid. */
@Composable
private fun AlbumsTab(viewModel: LibraryViewModel, padding: PaddingValues, onAlbum: (Long) -> Unit) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    LibraryStates(viewModel, albums?.size) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.md,
                end = Spacing.md,
                top = Spacing.md,
                bottom = padding.calculateBottomPadding() + Spacing.md,
            ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            items(albums.orEmpty(), key = { it.id }) { album -> AlbumTile(album, onClick = { onAlbum(album.id) }) }
        }
    }
}

/** Artist list. */
@Composable
private fun ArtistsTab(viewModel: LibraryViewModel, padding: PaddingValues, onArtist: (Long) -> Unit) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    LibraryStates(viewModel, artists?.size) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            items(artists.orEmpty(), key = { it.id }) { artist -> ArtistRow(artist, onClick = { onArtist(artist.id) }) }
        }
    }
}

/** Folder list. */
@Composable
private fun FoldersTab(viewModel: LibraryViewModel, padding: PaddingValues, onFolder: (String) -> Unit) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    LibraryStates(viewModel, folders?.size) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            items(folders.orEmpty(), key = { it.path }) { folder -> FolderRow(folder, onClick = { onFolder(folder.path) }) }
        }
    }
}

/** Shows loading, scanning, or empty states, otherwise [content]. [count] is null while loading. */
@Composable
private fun LibraryStates(viewModel: LibraryViewModel, count: Int?, content: @Composable () -> Unit) {
    val scanning by viewModel.scanning.collectAsStateWithLifecycle()
    when {
        count == null -> Box(Modifier.fillMaxSize())
        count == 0 && scanning -> LoadingState("Scanning your music…")
        count == 0 -> EmptyState(
            message = "No music found",
            hint = "Add audio files to your device and they'll show up here.",
            icon = Icons.Rounded.LibraryMusic,
            action = { Button(onClick = viewModel::rescan) { Text("Scan again") } },
        )
        else -> content()
    }
}

/** Sort action with a dropdown of fields and a direction toggle. */
@Composable
private fun SortMenu(tab: LibraryTab, current: SortSpec, onChange: (SortSpec) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val fields = when (tab) {
        LibraryTab.Songs -> listOf(SortField.Title, SortField.Artist, SortField.Album, SortField.DateAdded, SortField.Duration)
        LibraryTab.Albums -> listOf(SortField.Title, SortField.Artist, SortField.DateAdded, SortField.Duration)
        LibraryTab.Artists -> listOf(SortField.Artist, SortField.Album, SortField.Duration)
        LibraryTab.Folders -> listOf(SortField.Title, SortField.Duration)
    }
    IconButton(onClick = { open = true }) { Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort options") }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        fields.forEach { field ->
            DropdownMenuItem(
                text = { Text(label(tab, field)) },
                onClick = {
                    onChange(current.copy(field = field))
                    open = false
                },
                trailingIcon = { if (current.field == field) Icon(Icons.Rounded.Check, contentDescription = "Selected") },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(if (current.ascending) "Ascending" else "Descending") },
            leadingIcon = { Icon(if (current.ascending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward, contentDescription = null) },
            onClick = {
                onChange(current.copy(ascending = !current.ascending))
                open = false
            },
        )
    }
}

/** Human label for a sort field in the context of [tab]. */
private fun label(tab: LibraryTab, field: SortField): String = when (field) {
    SortField.Title -> if (tab == LibraryTab.Folders) "Name" else "Title"
    SortField.Artist -> if (tab == LibraryTab.Artists) "Name" else "Artist"
    SortField.Album -> if (tab == LibraryTab.Artists) "Number of albums" else "Album"
    SortField.DateAdded -> "Date added"
    SortField.Duration -> if (tab == LibraryTab.Songs || tab == LibraryTab.Albums) "Duration" else "Number of songs"
}
