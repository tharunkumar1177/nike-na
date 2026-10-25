/*
 * Track actions shared by every screen: a CompositionLocal contract implemented once at the app
 * root, and the bottom sheet listing the actions for one song.
 */
package com.music.player.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.music.player.core.model.Track
import com.music.player.core.model.artwork
import com.music.player.core.ui.theme.Sizes
import com.music.player.core.ui.theme.Spacing

/** Operations any screen can request for tracks. Implemented at the app root. */
interface TrackActions {
    /** Plays [tracks] starting at [index]; the list becomes the queue. */
    fun play(tracks: List<Track>, index: Int = 0)

    /** Plays [tracks] shuffled. */
    fun shuffle(tracks: List<Track>)

    /** Opens the action sheet for [track]. */
    fun showActions(track: Track)

    /** Inserts [tracks] after the current song. */
    fun playNext(tracks: List<Track>)

    /** Appends [tracks] to the queue. */
    fun addToQueue(tracks: List<Track>)

    /** Opens the add-to-playlist sheet for [tracks]. */
    fun addToPlaylist(tracks: List<Track>)
}

/** Root-provided [TrackActions]. */
val LocalTrackActions = staticCompositionLocalOf<TrackActions> { error("TrackActions not provided") }

/** Sheet with all actions for [track]. Every action dismisses the sheet first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionsSheet(
    track: Track,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = Spacing.xs),
        ) {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = {
                    ArtworkImage(track.artwork, track.album, Modifier.size(Sizes.listArtwork))
                },
                headlineContent = { Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1) },
                supportingContent = { Text("${track.artist} • ${track.album}", maxLines = 1) },
            )
            HorizontalDivider()
            Action(Icons.Rounded.SkipNext, "Play next") { onDismiss(); onPlayNext() }
            Action(Icons.AutoMirrored.Rounded.QueueMusic, "Add to queue") { onDismiss(); onAddToQueue() }
            Action(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add to playlist") { onDismiss(); onAddToPlaylist() }
            Action(
                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (isFavorite) "Remove from favorites" else "Add to favorites",
            ) { onDismiss(); onToggleFavorite() }
            Action(Icons.Rounded.Album, "Go to album") { onDismiss(); onGoToAlbum() }
            Action(Icons.Rounded.Person, "Go to artist") { onDismiss(); onGoToArtist() }
            Action(Icons.Rounded.Share, "Share") { onDismiss(); onShare() }
            Action(Icons.Rounded.Delete, "Delete from device", destructive = true) { onDismiss(); onDelete() }
        }
    }
}

/** One action row. */
@Composable
private fun Action(icon: ImageVector, label: String, destructive: Boolean = false, onClick: () -> Unit) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    ListItem(
        modifier = Modifier.clickableButton(onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent, headlineColor = color, leadingIconColor = color),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(label) },
    )
}
