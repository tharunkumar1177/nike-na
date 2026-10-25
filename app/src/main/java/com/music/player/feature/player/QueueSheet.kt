/*
 * "Up next" queue sheet: current track, upcoming tracks with drag-to-reorder by handle,
 * swipe-left to remove with undo, tap to jump, and shuffle/repeat toggles.
 * Implements blueprints/queue.md.
 */
package com.music.player.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.player.core.model.Artwork
import com.music.player.core.model.MediaUris
import com.music.player.core.playback.PlayerState
import com.music.player.core.playback.QueueEntry
import com.music.player.core.playback.RepeatMode
import com.music.player.core.ui.components.ArtworkImage
import com.music.player.core.ui.components.Formatting
import com.music.player.core.ui.components.clickableButton
import com.music.player.core.ui.theme.Elevation
import com.music.player.core.ui.theme.Sizes
import com.music.player.core.ui.theme.Spacing
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Queue operations used by the sheet. */
interface QueueActions {
    /** Jump to queue position [index]. */
    fun skipTo(index: Int)

    /** Move item [from] to [to] (absolute queue positions). */
    fun move(from: Int, to: Int)

    /** Remove the item at [index]. */
    fun remove(index: Int)

    /** Re-insert [trackId] at [index]. */
    fun insert(index: Int, trackId: Long)

    /** Toggle shuffle. */
    fun toggleShuffle()

    /** Cycle repeat. */
    fun cycleRepeat()
}

/** Modal queue sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(state: PlayerState, actions: QueueActions, onDismiss: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var upNext by remember(state.queue, state.currentIndex) { mutableStateOf(state.upNext) }
    var dragFrom by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        val fromPos = upNext.indexOfFirst { it.index == from.key }
        val toPos = upNext.indexOfFirst { it.index == to.key }
        if (fromPos >= 0 && toPos >= 0) {
            upNext = upNext.toMutableList().apply { add(toPos, removeAt(fromPos)) }
        }
    }

    /** Removes [entry] and offers undo. */
    fun removeWithUndo(entry: QueueEntry) {
        actions.remove(entry.index)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar("Removed from queue", actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) actions.insert(entry.index, entry.trackId)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Box(Modifier.fillMaxHeight(0.9f)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                item(key = "header") { Header(state, actions) }
                state.current?.let { current ->
                    item(key = "now") {
                        Column {
                            Label("Now playing")
                            QueueRow(current, isCurrent = true, onClick = {}, handle = null)
                        }
                    }
                }
                item(key = "upnext-label") {
                    Label(if (upNext.isEmpty()) "Nothing up next" else "Up next • ${Formatting.songs(upNext.size)}")
                }
                items(upNext, key = { it.index }) { entry ->
                    ReorderableItem(reorder, key = entry.index) { isDragging ->
                        val dismiss = rememberSwipeToDismissBoxState()
                        LaunchedEffect(dismiss.currentValue) {
                            if (dismiss.currentValue == SwipeToDismissBoxValue.EndToStart) removeWithUndo(entry)
                        }
                        SwipeToDismissBox(
                            state = dismiss,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = { DismissBackground() },
                        ) {
                            Surface(
                                tonalElevation = if (isDragging) Elevation.dragged else 0.dp,
                                shadowElevation = if (isDragging) Elevation.dragged else 0.dp,
                            ) {
                                QueueRow(
                                    entry,
                                    isCurrent = false,
                                    onClick = { actions.skipTo(entry.index) },
                                    handle = {
                                        IconButton(
                                            onClick = {},
                                            modifier = Modifier.draggableHandle(
                                                onDragStarted = { dragFrom = entry.index },
                                                onDragStopped = {
                                                    val from = dragFrom
                                                    val newPos = upNext.indexOfFirst { it.index == entry.index }
                                                    if (from != null && newPos >= 0) {
                                                        actions.move(from, state.currentIndex + 1 + newPos)
                                                    }
                                                    dragFrom = null
                                                },
                                            ),
                                        ) {
                                            Icon(Icons.Rounded.DragHandle, contentDescription = "Reorder ${entry.title}")
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
        }
    }
}

/** Title with shuffle and repeat toggles mirroring the player. */
@Composable
private fun Header(state: PlayerState, actions: QueueActions) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = Spacing.lg, end = Spacing.sm, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Queue", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        val active = MaterialTheme.colorScheme.primary
        val idle = MaterialTheme.colorScheme.onSurfaceVariant
        IconButton(onClick = actions::toggleShuffle) {
            Icon(Icons.Rounded.Shuffle, contentDescription = if (state.shuffle) "Shuffle on" else "Shuffle off", tint = if (state.shuffle) active else idle)
        }
        IconButton(onClick = actions::cycleRepeat) {
            Icon(
                if (state.repeat == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = "Repeat ${state.repeat.name.lowercase()}",
                tint = if (state.repeat == RepeatMode.Off) idle else active,
            )
        }
    }
}

/** Small section label. */
@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
    )
}

/** One queue row; the current track is highlighted with a bars indicator. */
@Composable
private fun QueueRow(entry: QueueEntry, isCurrent: Boolean, onClick: () -> Unit, handle: (@Composable () -> Unit)?) {
    val accent = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified
    ListItem(
        modifier = Modifier.clickableButton(onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Box(Modifier.size(Sizes.listArtwork)) {
                ArtworkImage(
                    if (entry.albumId >= 0) Artwork(MediaUris.album(entry.albumId), "album:${entry.albumId}") else null,
                    entry.title,
                    Modifier.fillMaxSize(),
                    initialsStyle = MaterialTheme.typography.labelMedium,
                )
                if (isCurrent) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f), MaterialTheme.shapes.small),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.GraphicEq, contentDescription = "Now playing", tint = Color.White) }
                }
            }
        },
        headlineContent = {
            Text(
                entry.title,
                color = accent,
                fontWeight = if (isCurrent) FontWeight.SemiBold else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = { Text(entry.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = handle,
    )
}

/** Red delete background revealed while swiping left. */
@Composable
private fun DismissBackground() {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = Spacing.lg),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onErrorContainer)
    }
}
