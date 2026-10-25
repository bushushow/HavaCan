package com.example.weather.ui.home.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val SunCore = Color(0xFFFFF1A6)
private val SunMid = Color(0xFFFFD12E)
private val SunEdge = Color(0xFFF5A300)
private val Moon = Color(0xFFFFF4C9)
private val CloudTop = Color(0xFFFFFFFF)
private val CloudBottom = Color(0xFFD8E5FA)
private val StormTop = Color(0xFFC9D1E0)
private val StormBottom = Color(0xFF8E9BB4)
private val Drop = Color(0xFF3D7DFF)

/** Soft colour pool behind the island that tells the weather at a glance. */
internal fun DrawScope.drawAmbientGlow(g: SceneGeometry, look: SceneLook) {
    val warm = Color(0xFFFFE9A8).copy(alpha = 0.55f * look.sun * (1 - look.night))
    val grey = Color(0xFFDDE4F0).copy(alpha = 0.6f * look.overcast)
    val night = Color(0xFF1B2D63).copy(alpha = 0.22f * look.night)
    val center = Offset(g.island.x, g.island.y - 70f * g.u)
    listOf(warm, grey, night).forEach { c ->
        if (c.alpha > 0.01f) {
            drawCircle(Brush.radialGradient(listOf(c, c.copy(alpha = 0f)), center, 230f * g.u), 230f * g.u, center)
        }
    }
}

internal fun DrawScope.drawSun(g: SceneGeometry, look: SceneLook, time: Float, shift: Float) {
    val alpha = look.sun * (1 - look.night)
    if (alpha < 0.01f) return
    val c = g.sun + Offset(shift, 0f)
    val r = g.sunRadius
    drawCircle(SunMid.copy(alpha = 0.18f * alpha), r * 1.75f, c)
    rotate(degrees = time * 9f, pivot = c) {
        repeat(10) { i ->
            val a = i * (2 * PI / 10).toFloat()
            val dir = Offset(cos(a), sin(a))
            drawLine(
                SunMid.copy(alpha = alpha),
                c + dir * (r * 1.28f),
                c + dir * (r * 1.58f),
                strokeWidth = 5f * g.u,
                cap = StrokeCap.Round,
            )
        }
    }
    drawCircle(
        Brush.radialGradient(listOf(SunCore, SunMid, SunEdge), c + Offset(-r * 0.3f, -r * 0.35f), r * 1.5f),
        r,
        c,
        alpha = alpha,
    )
}

internal fun DrawScope.drawMoonAndStars(g: SceneGeometry, look: SceneLook, time: Float, shift: Float) {
    val alpha = look.night * (1 - look.overcast * 0.8f)
    if (alpha < 0.01f) return
    val c = g.sun + Offset(shift, 0f)
    val r = g.sunRadius * 0.85f
    val disc = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
    val bite = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c + Offset(r * 0.45f, -r * 0.3f), r * 0.9f)) }
    drawCircle(Moon.copy(alpha = 0.18f * alpha), r * 1.6f, c)
    drawPath(Path.combine(PathOperation.Difference, disc, bite), Moon, alpha = alpha)
    val stars = listOf(0.12f to 0.10f, 0.30f to 0.22f, 0.48f to 0.08f, 0.62f to 0.30f, 0.74f to 0.06f, 0.95f to 0.36f)
    stars.forEachIndexed { i, (x, y) ->
        val twinkle = 0.6f + 0.4f * sin(time * 2.2f + i * 1.7f)
        drawCircle(Moon.copy(alpha = alpha * twinkle), 2.2f * g.u, Offset(size.width * x, size.height * y))
    }
}

/** A puffy cloud built from overlapping circles, lit from above. */
internal fun DrawScope.drawCloud(center: Offset, scale: Float, alpha: Float, overcast: Float, u: Float) {
    if (alpha < 0.01f) return
    val top = lerp(CloudTop, StormTop, overcast)
    val bottom = lerp(CloudBottom, StormBottom, overcast)
    val s = scale * u
    val puffs = listOf(
        Offset(-36f, 8f) to 20f,
        Offset(-12f, -8f) to 27f,
        Offset(18f, -2f) to 24f,
        Offset(40f, 10f) to 16f,
        Offset(4f, 12f) to 22f,
    )
    val brush = Brush.verticalGradient(listOf(top, bottom), startY = center.y - 36f * s, endY = center.y + 32f * s)
    drawOval(
        Color(0xFF0B1B3F).copy(alpha = 0.06f * alpha),
        topLeft = center + Offset(-46f * s, 22f * s),
        size = Size(92f * s, 14f * s),
    )
    puffs.forEach { (o, r) -> drawCircle(brush, r * s, center + o * s, alpha = alpha) }
    // Bright rim on the top puff
    drawCircle(Color.White.copy(alpha = 0.55f * alpha * (1 - overcast)), 9f * s, center + Offset(-18f, -20f) * s)
}

internal fun DrawScope.drawClouds(g: SceneGeometry, look: SceneLook, time: Float, shift: Float) {
    val drift = { i: Int -> sin(time * 0.18f + i * 1.9f) * 7f * g.u }
    val w = size.width
    val h = size.height
    drawCloud(Offset(w * 0.93f + drift(0) + shift, h * 0.36f), 1.25f, look.clouds, look.overcast, g.u)
    drawCloud(Offset(w * 0.6f + drift(1) + shift * 0.6f, h * 0.2f), 0.85f, look.clouds * 0.9f, look.overcast, g.u)
    drawCloud(Offset(w * 0.74f + drift(2) + shift * 0.8f, h * 0.44f), 0.95f, (look.clouds - 0.6f) * 2.5f, look.overcast, g.u)
}

/** Rain streaks or snowflakes falling from the clouds, positions derived from time (no allocation). */
internal fun DrawScope.drawPrecipitation(g: SceneGeometry, look: SceneLook, time: Float) {
    val left = size.width * 0.42f
    val right = size.width * 0.99f
    val top = size.height * 0.32f
    val bottom = size.height * 0.98f
    val rainDrops = (46 * look.rain).toInt()
    for (i in 0 until rainDrops) {
        val seed = (i * 0.618034f) % 1f
        val x = left + (right - left) * ((i * 0.3819f + seed) % 1f)
        val fall = (time * 1.6f + seed * 7f) % 1f
        val y = top + (bottom - top) * fall
        drawLine(
            Drop.copy(alpha = 0.75f * look.rain * hump(fall)),
            Offset(x, y),
            Offset(x - 3f * g.u, y + 12f * g.u),
            strokeWidth = 2.6f * g.u,
            cap = StrokeCap.Round,
        )
    }
    val flakes = (34 * look.snow).toInt()
    for (i in 0 until flakes) {
        val seed = (i * 0.618034f) % 1f
        val fall = (time * 0.35f + seed * 5f) % 1f
        val x = left + (right - left) * ((i * 0.3819f + seed) % 1f) + sin(time * 1.4f + i) * 6f * g.u
        val y = top + (bottom - top) * fall
        // Pale blue rather than white so flakes still read on the ivory background
        drawCircle(Color(0xFFBFD3F7).copy(alpha = 0.95f * look.snow * hump(fall)), 3f * g.u, Offset(x, y))
    }
}
