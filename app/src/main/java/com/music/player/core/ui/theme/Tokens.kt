/*
 * Shape, spacing, elevation, and motion design tokens for Music.
 * Features use only these tokens, never ad-hoc values for the same purpose.
 */
package com.music.player.core.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Material shape scale: small 8dp, medium 16dp, large 24dp. */
val MusicShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** 4dp-grid spacing scale. */
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

/** Tonal elevation levels. */
object Elevation {
    val bars = 3.dp
    val miniPlayer = 6.dp
    val dragged = 8.dp
}

/** Fixed component sizes shared across features. */
object Sizes {
    val listArtwork = 48.dp
    val listRowHeight = 72.dp
    val miniPlayerHeight = 64.dp
    val nowPlayingArtworkMax = 320.dp
    val minTouchTarget = 48.dp
}

/** Motion durations, easing, and springs. */
object Motion {
    const val STANDARD_MS = 300
    const val QUICK_MS = 150
    const val FADE_THROUGH_MS = 250
    const val COLOR_CROSSFADE_MS = 400
    const val PLAYER_SPRING_DAMPING = 0.8f
    const val PLAYER_SPRING_STIFFNESS = 380f

    /** Spring used to settle the mini player / now playing transition. */
    fun <T> playerSpring(): AnimationSpec<T> =
        spring(dampingRatio = PLAYER_SPRING_DAMPING, stiffness = PLAYER_SPRING_STIFFNESS)

    /** Standard eased tween of [durationMs]. */
    fun <T> standard(durationMs: Int = STANDARD_MS): AnimationSpec<T> =
        tween(durationMillis = durationMs, easing = FastOutSlowInEasing)

    /** Returns [spec] unless motion is reduced, in which case values snap. */
    fun <T> orSnap(reduced: Boolean, spec: AnimationSpec<T>): AnimationSpec<T> =
        if (reduced) snap() else spec

    /** Default low-bounce spring for small UI movements. */
    fun <T> gentleSpring(): AnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
}

/**
 * Returns true when the user disabled animations (system animator duration scale is 0),
 * in which case spatial animations must be skipped.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
