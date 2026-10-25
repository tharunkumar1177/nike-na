/*
 * Search screen: a focused search field with type-ahead results grouped into Songs, Albums,
 * and Artists. Implements blueprints/search-sort.md.
 */
package com.music.player.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.player.core.ui.components.AlbumTile
import com.music.player.core.ui.components.ArtistRow
import com.music.player.core.ui.components.EmptyState
import com.music.player.core.ui.components.LocalTrackActions
import com.music.player.core.ui.components.SongRow
import com.music.player.core.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

/** Most songs listed before the albums and artists groups. */
private const val MAX_SONGS = 50

/** Width of an album tile in the horizontal results row. */
private val ResultAlbumWidth = 140.dp

/** Search screen; the field takes focus when the screen opens with an empty query. */
@Composable
fun SearchScreen(
    contentPadding: PaddingValues,
    currentTrackId: Long?,
    onAlbum: (Long) -> Unit,
    onArtist: (Long) -> Unit,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { if (query.isEmpty()) focus.requestFocus() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs)
                .focusRequester(focus),
            placeholder = { Text("Songs, albums, artists") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setQuery("") }) { Icon(Icons.Rounded.Close, contentDescription = "Clear search") }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        )
        when {
            query.isBlank() -> EmptyState(
                message = "Search your library",
                hint = "Find songs, albums, and artists as you type.",
                icon = Icons.Rounded.Search,
            )
            results.query.isNotBlank() && results.isEmpty -> EmptyState(
                message = "No results for \u201C${query.trim()}\u201D",
                hint = "Check the spelling or try fewer words.",
                icon = Icons.Rounded.SearchOff,
            )
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
            ) {
                val songs = results.tracks.take(MAX_SONGS)
                if (songs.isNotEmpty()) {
                    item(key = "songs-h") { Header("Songs") }
                    itemsIndexed(songs, key = { _, t -> "t${t.id}" }) { index, track ->
                        SongRow(
                            track = track,
                            isCurrent = track.id == currentTrackId,
                            onClick = {
                                focusManager.clearFocus()
                                actions.play(songs, index)
                            },
                            onMore = { actions.showActions(track) },
                        )
                    }
                }
                if (results.albums.isNotEmpty()) {
                    item(key = "albums-h") { Header("Albums") }
                    item(key = "albums") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Spacing.md),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            items(results.albums, key = { it.id }) { album ->
                                AlbumTile(album, onClick = { onAlbum(album.id) }, modifier = Modifier.width(ResultAlbumWidth))
                            }
                        }
                    }
                }
                if (results.artists.isNotEmpty()) {
                    item(key = "artists-h") { Header("Artists") }
                    items(results.artists, key = { "a${it.id}" }) { artist ->
                        ArtistRow(artist, onClick = { onArtist(artist.id) })
                    }
                }
            }
        }
    }
}

/** Group header. */
@Composable
private fun Header(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = Spacing.xs),
    )
}
