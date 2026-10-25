/*
 * Typography for Music: the bundled Outfit variable font (SIL OFL, see app/OUTFIT-OFL.txt) for
 * display, headline, title, and label roles; the system font for body text.
 */
package com.music.player.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.music.player.R

/** Builds one Outfit font instance pinned to [weight] through the variable `wght` axis. */
@OptIn(ExperimentalTextApi::class)
private fun outfit(weight: FontWeight): Font = Font(
    resId = R.font.outfit,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Outfit family with the weights the type scale uses. */
val Outfit = FontFamily(
    outfit(FontWeight.Normal),
    outfit(FontWeight.Medium),
    outfit(FontWeight.SemiBold),
    outfit(FontWeight.Bold),
)

private val Base = Typography()

/** Applies Outfit to a non-body text style. */
private fun TextStyle.outfit(weight: FontWeight? = null): TextStyle =
    copy(fontFamily = Outfit, fontWeight = weight ?: fontWeight)

/** App type scale. */
val MusicTypography = Typography(
    displayLarge = Base.displayLarge.outfit().copy(fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = Base.displayMedium.outfit(FontWeight.Medium),
    displaySmall = Base.displaySmall.outfit(FontWeight.Medium),
    headlineLarge = Base.headlineLarge.outfit(FontWeight.SemiBold).copy(fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = Base.headlineMedium.outfit(FontWeight.SemiBold),
    headlineSmall = Base.headlineSmall.outfit(FontWeight.SemiBold),
    titleLarge = Base.titleLarge.outfit(FontWeight.Medium).copy(fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = Base.titleMedium.outfit(FontWeight.Medium),
    titleSmall = Base.titleSmall.outfit(FontWeight.Medium),
    bodyLarge = Base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = Base.bodyMedium,
    bodySmall = Base.bodySmall,
    labelLarge = Base.labelLarge.outfit(FontWeight.Medium),
    labelMedium = Base.labelMedium.outfit(FontWeight.Medium).copy(fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = Base.labelSmall.outfit(FontWeight.Medium),
)
