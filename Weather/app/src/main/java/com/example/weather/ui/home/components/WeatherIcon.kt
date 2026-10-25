package com.example.weather.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.weather.model.WeatherCondition
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val IconSun = Color(0xFFFFC72C)
private val IconSunDeep = Color(0xFFF5A300)
private val IconCloudTop = Color(0xFFFFFFFF)
private val IconCloudBottom = Color(0xFFC9D8F2)
private val IconDarkCloudTop = Color(0xFFB8C3D8)
private val IconDarkCloudBottom = Color(0xFF8592AE)
private val IconDrop = Color(0xFF2F74FF)
private val IconMoon = Color(0xFFFFE08A)

/** Small glossy weather icon matching the scene's look. */
@Composable
fun WeatherIcon(condition: WeatherCondition, isDay: Boolean, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    Canvas(modifier.size(size).semantics { contentDescription = condition.label }) {
        val s = this.size.minDimension / 32f
        when (condition) {
            WeatherCondition.Clear, WeatherCondition.Unknown -> body(isDay, Offset(16f, 16f) * s, 8f * s, rays = true)
            WeatherCondition.MainlyClear, WeatherCondition.PartlyCloudy -> {
                body(isDay, Offset(20f, 12f) * s, 7f * s, rays = isDay)
                cloud(Offset(13f, 20f) * s, s, dark = false)
            }
            WeatherCondition.Overcast -> {
                cloud(Offset(19f, 13f) * s, s * 0.8f, dark = true)
                cloud(Offset(14f, 19f) * s, s, dark = false)
            }
            WeatherCondition.Fog -> {
                cloud(Offset(16f, 13f) * s, s, dark = false)
                listOf(22f, 27f).forEach { y ->
                    drawLine(IconDarkCloudBottom, Offset(6f, y) * s, Offset(26f, y) * s, 2.4f * s, StrokeCap.Round)
                }
            }
            WeatherCondition.Drizzle, WeatherCondition.Rain, WeatherCondition.HeavyRain -> {
                cloud(Offset(16f, 12f) * s, s, dark = condition != WeatherCondition.Drizzle)
                val drops = if (condition == WeatherCondition.HeavyRain) listOf(9f, 14f, 19f, 24f) else listOf(11f, 17f, 23f)
                drops.forEach { x -> drop(Offset(x, 25f) * s, s) }
            }
            WeatherCondition.Snow -> {
                cloud(Offset(16f, 12f) * s, s, dark = false)
                listOf(10f, 16f, 22f).forEach { x -> drawCircle(IconDrop.copy(alpha = 0.5f), 2f * s, Offset(x, 26f) * s) }
            }
            WeatherCondition.Thunderstorm -> {
                cloud(Offset(16f, 12f) * s, s, dark = true)
                val bolt = Path().apply {
                    moveTo(17f * s, 18f * s); lineTo(12f * s, 26f * s); lineTo(16f * s, 26f * s)
                    lineTo(14f * s, 32f * s); lineTo(21f * s, 23f * s); lineTo(17f * s, 23f * s); close()
                }
                drawPath(bolt, IconSun)
            }
        }
    }
}

private fun DrawScope.body(isDay: Boolean, c: Offset, r: Float, rays: Boolean) {
    if (!isDay) {
        val disc = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
        val bite = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c + Offset(r * 0.5f, -r * 0.35f), r * 0.85f)) }
        drawPath(Path.combine(PathOperation.Difference, disc, bite), IconMoon)
        return
    }
    if (rays) {
        repeat(8) { i ->
            val a = i * (PI / 4).toFloat()
            val d = Offset(cos(a), sin(a))
            drawLine(IconSun, c + d * (r * 1.35f), c + d * (r * 1.8f), r * 0.28f, StrokeCap.Round)
        }
    }
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF1A6), IconSun, IconSunDeep), c + Offset(-r * 0.3f, -r * 0.3f), r * 1.5f), r, c)
}

private fun DrawScope.cloud(c: Offset, s: Float, dark: Boolean) {
    val brush = Brush.verticalGradient(
        if (dark) listOf(IconDarkCloudTop, IconDarkCloudBottom) else listOf(IconCloudTop, IconCloudBottom),
        startY = c.y - 9f * s,
        endY = c.y + 7f * s,
    )
    drawCircle(brush, 5.5f * s, c + Offset(-6.5f, 1.5f) * s)
    drawCircle(brush, 7.5f * s, c + Offset(0f, -2f) * s)
    drawCircle(brush, 5.5f * s, c + Offset(6.5f, 1.5f) * s)
    drawRoundRect(
        brush,
        topLeft = c + Offset(-11f, 0f) * s,
        size = androidx.compose.ui.geometry.Size(22f * s, 7f * s),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.5f * s),
    )
}

private fun DrawScope.drop(c: Offset, s: Float) {
    val p = Path().apply {
        moveTo(c.x, c.y - 3.5f * s)
        quadraticTo(c.x + 2.6f * s, c.y + 0.5f * s, c.x, c.y + 2.6f * s)
        quadraticTo(c.x - 2.6f * s, c.y + 0.5f * s, c.x, c.y - 3.5f * s)
    }
    drawPath(p, IconDrop)
}
