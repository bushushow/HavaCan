package com.kampplus.havacan.ui.common

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.kampplus.havacan.model.WeatherCondition

/** Text and icons drawn on the sky. */
val OnSky = Color.White
val OnSkySoft = Color.White.copy(alpha = 0.85f)

/** A sky as a top-to-bottom gradient. */
data class SkyColors(val top: Color, val bottom: Color)

/** The sky of the home city right now; every screen's header is painted with it. */
val LocalSky = compositionLocalOf { skyColors(null, isDay = true, hour = 12) }

/** Sky gradient for the weather and the time of day: day blue, dusk violet-orange, night navy… */
fun skyColors(condition: WeatherCondition?, isDay: Boolean, hour: Int?): SkyColors {
    val wet = condition?.isWet == true || condition == WeatherCondition.Thunderstorm
    val grey = condition == WeatherCondition.Overcast || condition == WeatherCondition.Fog
    val snow = condition == WeatherCondition.Snow
    val dusk = isDay && hour != null && (hour in 17..19 || hour in 6..7)
    return when {
        !isDay -> SkyColors(Color(0xFF0B1B3F), Color(0xFF1E3A8A))
        wet -> SkyColors(Color(0xFF3A4E78), Color(0xFF7D8FB3))
        snow -> SkyColors(Color(0xFF6F98CC), Color(0xFFC9DCF2))
        grey -> SkyColors(Color(0xFF5C7DB0), Color(0xFFB5C5E0))
        dusk -> SkyColors(Color(0xFF5B3FA0), Color(0xFFFF9E6B))
        else -> SkyColors(Color(0xFF2F7BFF), Color(0xFF9CCBFF))
    }
}

/**
 * A screen's header on the sky: it reaches under the status bar and, with [bleed], past the
 * horizontal padding of the screen it sits in, so the colour runs edge to edge. Put text in
 * [OnSky] / [OnSkySoft] inside it.
 */
@Composable
fun SkyHeader(
    modifier: Modifier = Modifier,
    bleed: Dp = 0.dp,
    sky: SkyColors = LocalSky.current,
    content: @Composable ColumnScope.() -> Unit
) {
    val top by animateColorAsState(sky.top, tween(900), label = "skyTop")
    val bottom by animateColorAsState(sky.bottom, tween(900), label = "skyBottom")
    LightStatusBarIcons()
    Column(
        modifier
            .bleed(bleed)
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .statusBarsPadding()
            .padding(horizontal = bleed)
            .padding(bottom = 18.dp),
        content = content
    )
}

/** Widens the element by [amount] on both sides, cancelling a parent's horizontal padding. */
private fun Modifier.bleed(amount: Dp) = if (amount == 0.dp) {
    this
} else {
    layout { measurable, constraints ->
        val extra = amount.roundToPx() * 2
        val placeable = measurable.measure(
            constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra)
        )
        layout(placeable.width - extra, placeable.height) { placeable.place(-extra / 2, 0) }
    }
}

/** White status bar icons while a sky is behind them; restored when the screen goes away. */
@Composable
fun LightStatusBarIcons() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val before = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (before != null) controller.isAppearanceLightStatusBars = before }
    }
}
