/*
 * Mini player and Now Playing screen as one gesture-driven surface. Expansion progress (0..1)
 * follows the finger and settles with a spring; it drives the container bounds, corner radius,
 * the artwork's size and position, and cross-fades the mini and full controls.
 * Reduced motion makes every transition instant. Implements blueprints/now-playing.md.
 */
package com.music.player.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.player.core.model.Artwork
import com.music.player.core.model.MediaUris
import com.music.player.core.playback.PlayerState
import com.music.player.core.playback.QueueEntry
import com.music.player.core.playback.RepeatMode
import com.music.player.core.ui.components.ArtworkImage
import com.music.player.core.ui.components.Formatting
import com.music.player.core.ui.components.clickableButton
import com.music.player.core.ui.theme.Elevation
import com.music.player.core.ui.theme.Motion
import com.music.player.core.ui.theme.Sizes
import com.music.player.core.ui.theme.Spacing
import com.music.player.core.ui.theme.rememberReducedMotion
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/** User intents raised by the player surface. */
interface PlayerUiActions {
    /** Play or pause. */
    fun togglePlayPause()

    /** Next track. */
    fun next()

    /** Previous track or restart. */
    fun previous()

    /** Seek to [ms]. */
    fun seekTo(ms: Long)

    /** Toggle shuffle. */
    fun toggleShuffle()

    /** Cycle repeat. */
    fun cycleRepeat()

    /** Toggle favorite of the current track. */
    fun toggleFavorite()

    /** Open the queue sheet. */
    fun openQueue()

    /** Add the current track to a playlist. */
    fun addToPlaylist()

    /** Navigate to the current track's album. */
    fun goToAlbum()

    /** Navigate to the current track's artist. */
    fun goToArtist()

    /** Share the current track. */
    fun share()
}

/**
 * Draws the player above all screens. Hidden when nothing is loaded.
 *
 * @param expansion 0 = mini player, 1 = full screen; hoisted so the app can open it.
 * @param collapsedBottom distance from the screen bottom to the mini player's bottom edge.
 */
@Composable
fun PlayerSheet(
    state: PlayerState,
    position: StateFlow<Long>,
    expansion: Animatable<Float, AnimationVector1D>,
    collapsedBottom: Dp,
    isFavorite: Boolean,
    actions: PlayerUiActions,
) {
    val current = state.current ?: return
    val scope = rememberCoroutineScope()
    val reduced = rememberReducedMotion()
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.getTop(density)
    val navBottom = WindowInsets.navigationBars.getBottom(density)

    /** Animates (or snaps) expansion to [target] with an optional fling velocity in progress/s. */
    fun settleTo(target: Float, velocity: Float = 0f) {
        scope.launch {
            if (reduced) expansion.snapTo(target) else expansion.animateTo(target, Motion.playerSpring(), velocity)
        }
    }

    BackHandler(enabled = expansion.targetValue > 0.5f) { settleTo(0f) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fullW = constraints.maxWidth.toFloat()
        val fullH = constraints.maxHeight.toFloat()
        with(density) {
            val miniH = Sizes.miniPlayerHeight.toPx()
            val margin = Spacing.md.toPx()
            val collapsedTop = (fullH - collapsedBottom.toPx() - Spacing.xs.toPx() - miniH).coerceAtLeast(0f)
            val travel = collapsedTop.coerceAtLeast(1f)
            // The spring overshoots past 1; unclamped, corner radius and elevation go negative and throw.
            val p = expansion.value.coerceIn(0f, 1f)

            val top = collapsedTop * (1f - p)
            val hMargin = margin * (1f - p)
            val width = fullW - 2 * hMargin
            val height = miniH + (fullH - miniH) * p

            // Artwork geometry: mini slot -> centered large slot below the top bar.
            val miniArt = Sizes.listArtwork.toPx()
            val miniArtInset = Spacing.xs.toPx()
            val fullArt = min(min(fullW - Spacing.lg.toPx() * 2, Sizes.nowPlayingArtworkMax.toPx()), fullH * 0.42f)
            val fullArtTop = statusTop + 56.dp.toPx() + Spacing.lg.toPx()
            val artSize = miniArt + (fullArt - miniArt) * p
            val artX = miniArtInset + ((fullW - fullArt) / 2f - miniArtInset) * p
            val artY = miniArtInset + (fullArtTop - miniArtInset) * p

            val dragState = rememberDraggableState { delta ->
                scope.launch { expansion.snapTo((expansion.value - delta / travel).coerceIn(0f, 1f)) }
            }
            val containerColor = lerp(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surface, p)

            Surface(
                modifier = Modifier
                    .offset { IntOffset(hMargin.roundToInt(), top.roundToInt()) }
                    .size(width.toDp(), height.toDp())
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity ->
                            val target = when {
                                velocity < -FLING_VELOCITY -> 1f
                                velocity > FLING_VELOCITY -> 0f
                                expansion.value > 0.5f -> 1f
                                else -> 0f
                            }
                            settleTo(target, -velocity / travel)
                        },
                    ),
                shape = RoundedCornerShape(lerp(16.dp, 0.dp, p)),
                color = containerColor,
                tonalElevation = lerp(Elevation.miniPlayer, 0.dp, p),
                shadowElevation = lerp(Elevation.miniPlayer, 0.dp, p),
            ) {
                Box(Modifier.fillMaxSize()) {
                    if (p > 0f) ExpandedBackground(current, Modifier.alpha(p))
                    if (p < MINI_FADE_END) {
                        MiniContent(
                            entry = current,
                            isPlaying = state.isPlaying,
                            position = position,
                            durationMs = state.durationMs,
                            actions = actions,
                            onExpand = { settleTo(1f) },
                            modifier = Modifier.alpha(1f - p / MINI_FADE_END),
                        )
                    }
                    if (p > FULL_FADE_START) {
                        FullContent(
                            state = state,
                            entry = current,
                            position = position,
                            isFavorite = isFavorite,
                            artworkBottom = (fullArtTop + fullArt).toDp(),
                            navBottom = navBottom.toDp(),
                            reduced = reduced,
                            actions = actions,
                            onCollapse = { settleTo(0f) },
                            modifier = Modifier.alpha((p - FULL_FADE_START) / (1f - FULL_FADE_START)),
                        )
                    }
                    ArtworkImage(
                        artwork = current.artwork(),
                        title = current.title,
                        modifier = Modifier
                            .offset { IntOffset(artX.roundToInt(), artY.roundToInt()) }
                            .size(artSize.toDp())
                            .then(if (p >= 1f) Modifier.swipeToSkip(actions) else Modifier),
                        shape = RoundedCornerShape(lerp(8.dp, 24.dp, p)),
                        initialsStyle = if (p > 0.5f) MaterialTheme.typography.displayMedium else MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

/** Collapsed row: artwork slot, title/artist, play/pause, next, and a thin progress line. */
@Composable
private fun MiniContent(
    entry: QueueEntry,
    isPlaying: Boolean,
    position: StateFlow<Long>,
    durationMs: Long,
    actions: PlayerUiActions,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(Sizes.miniPlayerHeight)
            .clickableButton(onExpand)
            .swipeToSkip(actions),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(Sizes.listArtwork + Spacing.xs * 2))
            Column(Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    entry.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = actions::togglePlayPause) {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                )
            }
            IconButton(onClick = actions::next) { Icon(Icons.Rounded.SkipNext, contentDescription = "Next track") }
            Spacer(Modifier.width(Spacing.xxs))
        }
        MiniProgress(position, durationMs, Modifier.align(Alignment.BottomCenter))
    }
}

/** 2dp progress line; collects position itself so only it recomposes per tick. */
@Composable
private fun MiniProgress(position: StateFlow<Long>, durationMs: Long, modifier: Modifier = Modifier) {
    val pos by position.collectAsStateWithLifecycle()
    LinearProgressIndicator(
        progress = { if (durationMs > 0) (pos.toFloat() / durationMs).coerceIn(0f, 1f) else 0f },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm)
            .height(2.dp),
        strokeCap = StrokeCap.Round,
        drawStopIndicator = {},
    )
}

/** Blurred artwork wash with a scrim so text keeps contrast. */
@Composable
private fun ExpandedBackground(entry: QueueEntry, modifier: Modifier = Modifier) {
    val surface = MaterialTheme.colorScheme.surface
    Box(modifier.fillMaxSize()) {
        AsyncImage(
            model = entry.artwork(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.55f)
                .blur(72.dp),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(surface.copy(alpha = 0.35f), surface.copy(alpha = 0.85f), surface))),
        )
    }
}

/** Full Now Playing layout; the artwork itself is drawn by the parent at [artworkBottom]. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FullContent(
    state: PlayerState,
    entry: QueueEntry,
    position: StateFlow<Long>,
    isFavorite: Boolean,
    artworkBottom: Dp,
    navBottom: Dp,
    reduced: Boolean,
    actions: PlayerUiActions,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(bottom = navBottom),
    ) {
        Spacer(Modifier.height(artworkBottom + Spacing.lg))
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = if (reduced) TextOverflow.Ellipsis else TextOverflow.Clip,
                    modifier = if (reduced) Modifier else Modifier.basicMarquee(),
                )
                Text(
                    entry.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickableButton(actions::goToArtist),
                )
            }
            IconToggleButton(checked = isFavorite, onCheckedChange = { actions.toggleFavorite() }) {
                Icon(
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
        SeekBar(position, state.durationMs, actions::seekTo, Modifier.padding(horizontal = Spacing.lg))
        Spacer(Modifier.height(Spacing.sm))
        Transport(state, actions)
        Spacer(Modifier.height(Spacing.md))
        UpNextRow(state.upNext.firstOrNull(), actions::openQueue)
        Spacer(Modifier.height(Spacing.sm))
    }
    TopBar(onCollapse, actions)
    }
}

/** Collapse chevron, title, and overflow menu, pinned under the status bar. */
@Composable
private fun TopBar(onCollapse: () -> Unit, actions: PlayerUiActions) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCollapse) { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Collapse player") }
        Text(
            "Now playing",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More options") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Add to playlist") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                    onClick = { menu = false; actions.addToPlaylist() },
                )
                DropdownMenuItem(
                    text = { Text("Go to album") },
                    leadingIcon = { Icon(Icons.Rounded.Album, null) },
                    onClick = { menu = false; actions.goToAlbum() },
                )
                DropdownMenuItem(
                    text = { Text("Go to artist") },
                    leadingIcon = { Icon(Icons.Rounded.Person, null) },
                    onClick = { menu = false; actions.goToArtist() },
                )
                DropdownMenuItem(
                    text = { Text("Share") },
                    leadingIcon = { Icon(Icons.Rounded.Share, null) },
                    onClick = { menu = false; actions.share() },
                )
            }
        }
    }
}

/** Slider with elapsed and remaining time; seeks on release; ±10 s accessibility actions. */
@Composable
private fun SeekBar(position: StateFlow<Long>, durationMs: Long, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    val pos by position.collectAsStateWithLifecycle()
    var dragging by remember { mutableStateOf<Float?>(null) }
    val fraction = dragging ?: if (durationMs > 0) (pos.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val shownMs = (fraction * durationMs).toLong()
    Column(modifier) {
        Slider(
            value = fraction,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { onSeek((it * durationMs).toLong()) }
                dragging = null
            },
            enabled = durationMs > 0,
            modifier = Modifier.semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Forward 10 seconds") { onSeek((pos + 10_000).coerceAtMost(durationMs)); true },
                    CustomAccessibilityAction("Back 10 seconds") { onSeek((pos - 10_000).coerceAtLeast(0)); true },
                )
            },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(Formatting.duration(shownMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "-" + Formatting.duration(durationMs - shownMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Shuffle, previous, play/pause, next, repeat. */
@Composable
private fun Transport(state: PlayerState, actions: PlayerUiActions) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val active = MaterialTheme.colorScheme.primary
        val idle = MaterialTheme.colorScheme.onSurfaceVariant
        IconButton(onClick = actions::toggleShuffle) {
            Icon(Icons.Rounded.Shuffle, contentDescription = if (state.shuffle) "Shuffle on" else "Shuffle off", tint = if (state.shuffle) active else idle)
        }
        IconButton(onClick = actions::previous, modifier = Modifier.size(64.dp)) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous track", modifier = Modifier.size(36.dp))
        }
        FilledIconButton(
            onClick = actions::togglePlayPause,
            modifier = Modifier.size(80.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(
                if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                modifier = Modifier.size(40.dp),
            )
        }
        IconButton(onClick = actions::next, modifier = Modifier.size(64.dp)) {
            Icon(Icons.Rounded.SkipNext, contentDescription = "Next track", modifier = Modifier.size(36.dp))
        }
        IconButton(onClick = actions::cycleRepeat) {
            Icon(
                if (state.repeat == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = when (state.repeat) {
                    RepeatMode.Off -> "Repeat off"
                    RepeatMode.All -> "Repeat all"
                    RepeatMode.One -> "Repeat one"
                },
                tint = if (state.repeat == RepeatMode.Off) idle else active,
            )
        }
    }
}

/** "Up next" entry that opens the queue. */
@Composable
private fun UpNextRow(next: QueueEntry?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .clip(MaterialTheme.shapes.medium)
            .clickableButton(onClick)
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Spacing.sm))
        Text(
            if (next != null) "Up next: ${next.title}" else "Queue",
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Horizontal swipe beyond a threshold skips to the next or previous track. */
private fun Modifier.swipeToSkip(actions: PlayerUiActions): Modifier = pointerInput(actions) {
    var total = 0f
    detectHorizontalDragGestures(
        onDragStart = { total = 0f },
        onDragEnd = {
            if (abs(total) > SKIP_THRESHOLD_PX) {
                if (total < 0) actions.next() else actions.previous()
            }
        },
        onHorizontalDrag = { change, amount ->
            change.consume()
            total += amount
        },
    )
}

/** Cover of a queue entry. */
private fun QueueEntry.artwork(): Artwork? =
    if (albumId >= 0) Artwork(MediaUris.album(albumId), "album:$albumId") else null

private const val MINI_FADE_END = 0.3f
private const val FULL_FADE_START = 0.7f
private const val FLING_VELOCITY = 1_200f
private const val SKIP_THRESHOLD_PX = 120f
