/*
 * Static brand color schemes for Music, used when neither artwork nor system dynamic color applies.
 * Dark mode is tuned for OLED with a true black background.
 */
package com.music.player.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Brand primary, also used as the default artwork seed. */
val BrandPrimary = Color(0xFF6750A4)

private val BrandSecondary = Color(0xFF625B71)
private val BrandTertiary = Color(0xFF7D5260)
private val OledBlack = Color(0xFF000000)
private val OledSurfaceContainer = Color(0xFF1C1B1F)

/** Light brand scheme. */
val BrandLightScheme: ColorScheme = lightColorScheme(
    primary = BrandPrimary,
    secondary = BrandSecondary,
    tertiary = BrandTertiary,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
)

/** Dark brand scheme with OLED black surfaces. */
val BrandDarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8),
).toOled()

/**
 * Returns a copy of this dark scheme with true black background and surface, keeping
 * container tones slightly lifted so cards stay distinguishable.
 */
fun ColorScheme.toOled(): ColorScheme = copy(
    background = OledBlack,
    surface = OledBlack,
    surfaceContainerLowest = OledBlack,
    surfaceContainer = OledSurfaceContainer,
)
