/*
 * Album, artist, and folder detail screens sharing a large artwork header tinted with an
 * artwork-derived gradient, a Play all / Shuffle header, and song rows.
 */
package com.music.player.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.player.core.data.sync.TagRules
import com.music.player.core.model.Artwork
import com.music.player.core.model.Track
import com.music.player.core.model.artwork
import com.music.player.core.ui.components.AlbumTile
import com.music.player.core.ui.components.ArtworkImage
import com.music.player.core.ui.components.EmptyState
import com.music.player.core.ui.components.Formatting
import com.music.player.core.ui.components.LocalTrackActions
import com.music.player.core.ui.components.PlayShuffleHeader
import com.music.player.core.ui.components.SongRow
import com.music.player.core.ui.components.clickableButton
import com.music.player.core.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Album detail: tracks in disc/track order with numbers instead of artwork. */
@Composable
fun AlbumScreen(albumId: Long, contentPadding: PaddingValues, currentTrackId: Long?, onBack: () -> Unit, onArtist: (Long) -> Unit) {
    val viewModel: AlbumViewModel = koinViewModel(key = "album-$albumId") { parametersOf(albumId) }
    val album by viewModel.album.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val a = album
    val subtitle = a?.let {
        listOfNotNull("Album", it.year.takeIf { y -> y > 0 }?.toString(), Formatting.songs(it.trackCount), Formatting.totalDuration(it.durationMs))
            .joinToString(" • ")
    }.orEmpty()
    DetailList(
        title = a?.title.orEmpty(),
        overline = a?.artist,
        onOverline = tracks.firstOrNull()?.let { t -> { onArtist(t.artistId) } },
        subtitle = subtitle,
        artwork = a?.artwork,
        tracks = tracks,
        contentPadding = contentPadding,
        currentTrackId = currentTrackId,
        numbered = true,
        onBack = onBack,
    )
}

/** Artist detail: albums carousel, then all songs. */
@Composable
fun ArtistScreen(artistId: Long, contentPadding: PaddingValues, currentTrackId: Long?, onBack: () -> Unit, onAlbum: (Long) -> Unit) {
    val viewModel: ArtistViewModel = koinViewModel(key = "artist-$artistId") { parametersOf(artistId) }
    val artist by viewModel.artist.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val ar = artist
    DetailList(
        title = ar?.name.orEmpty(),
        overline = "Artist",
        subtitle = ar?.let { "${Formatting.albums(it.albumCount)} • ${Formatting.songs(it.trackCount)}" }.orEmpty(),
        artwork = ar?.artwork,
        artworkShape = CircleShape,
        tracks = tracks,
        contentPadding = contentPadding,
        currentTrackId = currentTrackId,
        numbered = false,
        onBack = onBack,
        beforeTracks = {
            if (albums.size > 1) {
                item(key = "albums-title") {
                    Text(
                        "Albums",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    )
                }
                item(key = "albums") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(albums, key = { it.id }) { album ->
                            AlbumTile(album, onClick = { onAlbum(album.id) }, modifier = Modifier.width(140.dp))
                        }
                    }
                }
                item(key = "songs-title") {
                    Text(
                        "Songs",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    )
                }
            }
        },
    )
}

/** Folder detail: files in name order. */
@Composable
fun FolderScreen(path: String, contentPadding: PaddingValues, currentTrackId: Long?, onBack: () -> Unit) {
    val viewModel: FolderViewModel = koinViewModel(key = "folder-$path") { parametersOf(path) }
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val name = TagRules.folderName(path).ifEmpty { "/" }
    DetailList(
        title = name,
        overline = "Folder",
        subtitle = "${path.trimEnd('/')} • ${Formatting.songs(tracks.size)}",
        artwork = tracks.firstOrNull()?.artwork,
        tracks = tracks,
        contentPadding = contentPadding,
        currentTrackId = currentTrackId,
        numbered = false,
        onBack = onBack,
        emptyMessage = "This folder is empty",
    )
}

/** Shared detail layout. */
@Composable
private fun DetailList(
    title: String,
    overline: String?,
    subtitle: String,
    artwork: Artwork?,
    tracks: List<Track>,
    contentPadding: PaddingValues,
    currentTrackId: Long?,
    numbered: Boolean,
    onBack: () -> Unit,
    onOverline: (() -> Unit)? = null,
    artworkShape: Shape = MaterialTheme.shapes.large,
    emptyMessage: String = "Nothing here anymore",
    beforeTracks: LazyListScope.() -> Unit = {},
) {
    val actions = LocalTrackActions.current
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding())) {
            item(key = "header") {
                DetailHeader(title, overline, onOverline, subtitle, artwork, artworkShape)
            }
            if (tracks.isNotEmpty()) {
                item(key = "play") { PlayShuffleHeader(onPlay = { actions.play(tracks) }, onShuffle = { actions.shuffle(tracks) }) }
            }
            beforeTracks()
            itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                SongRow(
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    leadingNumber = if (numbered) track.trackNumber else null,
                    onClick = { actions.play(tracks, index) },
                    onMore = { actions.showActions(track) },
                )
            }
            if (tracks.isEmpty() && title.isNotEmpty()) {
                item(key = "empty") { EmptyState(message = emptyMessage, icon = Icons.Rounded.FolderOff, modifier = Modifier.height(320.dp)) }
            }
        }
        FilledTonalIconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(Spacing.xs),
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
    }
}

/** Big artwork over a gradient tinted with the theme's primary container. */
@Composable
private fun DetailHeader(
    title: String,
    overline: String?,
    onOverline: (() -> Unit)?,
    subtitle: String,
    artwork: Artwork?,
    shape: Shape,
) {
    val tint = MaterialTheme.colorScheme.primaryContainer
    val surface = MaterialTheme.colorScheme.surface
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(tint, surface)))
            .statusBarsPadding()
            .padding(top = Spacing.xl, start = Spacing.lg, end = Spacing.lg, bottom = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ArtworkImage(
            artwork,
            title,
            Modifier
                .fillMaxWidth(0.62f)
                .widthIn(max = 280.dp)
                .aspectRatio(1f),
            shape = shape,
            initialsStyle = MaterialTheme.typography.displayMedium,
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (overline != null) {
            Text(
                overline,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = if (onOverline != null) Modifier.padding(top = Spacing.xxs).clickableButton(onOverline) else Modifier.padding(top = Spacing.xxs),
            )
        }
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.xxs),
        )
    }
}
