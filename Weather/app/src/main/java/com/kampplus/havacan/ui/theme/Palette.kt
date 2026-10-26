package com.kampplus.havacan.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * The colours of content below the sky header, in a light and a dark version. Screens that
 * support dark mode read these through [Hava] instead of the fixed Navy / white constants.
 */
@Immutable
data class HavaPalette(
    val isDark: Boolean,
    val page: Color,
    val card: Color,
    val cardTint: Color,
    val ink: Color,
    val inkSoft: Color,
    val line: Color,
    val blueTint: Color,
    /** The selected tab: a navy pill with yellow text by day, a yellow pill with navy text at night. */
    val pill: Color,
    val onPill: Color,
    val bubble: Color,
    val highlight: Brush,
    val sallaCard: Brush
)

val LightPalette = HavaPalette(
    isDark = false,
    page = Ivory,
    card = Color.White,
    cardTint = SkyTint,
    ink = Navy,
    inkSoft = NavySoft,
    line = Navy.copy(alpha = 0.06f),
    blueTint = HavaBlueLight,
    pill = Navy,
    onPill = Sun,
    bubble = Navy,
    highlight = Brush.linearGradient(listOf(Color(0xFFE8F0FF), Color(0xFFF7FAFF))),
    sallaCard = Brush.horizontalGradient(listOf(SunLight, Color(0xFFFFF9E3)))
)

val DarkPalette = HavaPalette(
    isDark = true,
    page = NightBackground,
    card = NightSurface,
    cardTint = NightSurfaceVariant,
    ink = NightOnSurface,
    inkSoft = NightOnSurfaceVariant,
    line = Color.White.copy(alpha = 0.08f),
    blueTint = Color(0xFF1C3A8A),
    pill = Sun,
    onPill = Navy,
    bubble = HavaBlue,
    highlight = Brush.linearGradient(listOf(Color(0xFF1B2B66), NightSurface)),
    sallaCard = Brush.horizontalGradient(listOf(Color(0xFF3B2F0A), Color(0xFF2A240F)))
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

/** Shorthand for the current palette: `color = Hava.ink`. */
object Hava {
    val p: HavaPalette
        @Composable @ReadOnlyComposable
        get() = LocalPalette.current
    val ink: Color
        @Composable @ReadOnlyComposable
        get() = LocalPalette.current.ink
    val inkSoft: Color
        @Composable @ReadOnlyComposable
        get() = LocalPalette.current.inkSoft
    val card: Color
        @Composable @ReadOnlyComposable
        get() = LocalPalette.current.card
    val cardTint: Color
        @Composable @ReadOnlyComposable
        get() = LocalPalette.current.cardTint
}
