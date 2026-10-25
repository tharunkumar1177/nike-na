/*
 * Root Material 3 theme for the Music app.
 * Color source chain: current artwork seed -> system dynamic color -> static brand scheme.
 * Scheme changes crossfade over 400ms, or snap when animations are disabled.
 */
package com.music.player.core.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.dynamicColorScheme
import com.music.player.core.model.ThemeMode

/**
 * Applies the app color scheme, typography, and shapes to [content].
 *
 * @param themeMode light/dark preference.
 * @param seedColor color extracted from the current artwork, or null to use system dynamic color.
 * @param useDynamicColor whether system wallpaper colors are used when no seed is present.
 */
@Composable
fun MusicTheme(
    themeMode: ThemeMode = ThemeMode.System,
    seedColor: Color? = null,
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val context = LocalContext.current
    val target = remember(dark, seedColor, useDynamicColor) {
        when {
            seedColor != null -> dynamicColorScheme(seedColor = seedColor, isDark = dark, isAmoled = dark)
            useDynamicColor && dark -> dynamicDarkColorScheme(context).toOled()
            useDynamicColor -> dynamicLightColorScheme(context)
            dark -> BrandDarkScheme
            else -> BrandLightScheme
        }
    }
    MaterialTheme(
        colorScheme = animateColorScheme(target),
        typography = MusicTypography,
        shapes = MusicShapes,
        content = content,
    )
}

/** Animates the roles that visibly tint the UI toward [target]; other roles switch directly. */
@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val spec = Motion.orSnap(rememberReducedMotion(), Motion.standard<Color>(Motion.COLOR_CROSSFADE_MS))

    /** Animates a single color role. */
    @Composable
    fun Color.animated(): Color = animateColorAsState(this, spec, label = "scheme").value

    return target.copy(
        primary = target.primary.animated(),
        onPrimary = target.onPrimary.animated(),
        primaryContainer = target.primaryContainer.animated(),
        onPrimaryContainer = target.onPrimaryContainer.animated(),
        secondary = target.secondary.animated(),
        secondaryContainer = target.secondaryContainer.animated(),
        onSecondaryContainer = target.onSecondaryContainer.animated(),
        tertiary = target.tertiary.animated(),
        background = target.background.animated(),
        surface = target.surface.animated(),
        onSurface = target.onSurface.animated(),
        onSurfaceVariant = target.onSurfaceVariant.animated(),
        surfaceContainer = target.surfaceContainer.animated(),
        surfaceContainerHigh = target.surfaceContainerHigh.animated(),
        surfaceContainerHighest = target.surfaceContainerHighest.animated(),
        surfaceContainerLow = target.surfaceContainerLow.animated(),
        surfaceTint = target.surfaceTint.animated(),
    )
}
