package com.example.weather.ui.run

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.weather.ui.home.character.HavaCanPose
import com.example.weather.ui.home.character.drawHavaCan
import com.example.weather.ui.home.scene.drawCloud
import com.example.weather.ui.theme.Ivory
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

private val SkyTop = Color(0xFFBFDDFF)
private val SkyLow = Color(0xFFEAF4FF)
private val Skyline = Color(0xFFAFC6E6)
private val SkylineFar = Color(0xFFCFDDF1)
private val GrassLight = Color(0xFFB4DE7C)
private val GrassDeep = Color(0xFF79BF52)
private val Leaf = Color(0xFF5DA845)
private val LeafLight = Color(0xFF86C75C)
private val Trunk = Color(0xFF8A6244)
private val Track = Color(0xFFD9694E)
private val TrackEdge = Color(0xFFB9533C)
private val Flower = Color(0xFFFFD43B)

/**
 * The park behind Koşu Planla's headline: sky, skyline, trees and a red running track, drawn on
 * the right and fading into the ivory page on the left so the headline stays readable.
 * [time] drives the clouds and the running gait; [arrive] 0 → 1 brings HavaCan in from the
 * right, [hop] is a one-off jump when the user picks something, [cheer] the save celebration.
 */
fun DrawScope.drawRunPark(time: Float, arrive: Float, hop: Float, cheer: Float) {
    val w = size.width
    val h = size.height
    val u = h / 300f
    val left = w * 0.3f

    // Sky
    drawRect(Brush.verticalGradient(listOf(SkyTop, SkyLow), 0f, h * 0.62f), Offset(left, 0f), Size(w - left, h * 0.64f))
    drawCloud(Offset(w * 0.62f + sin(time * 0.15f) * 6f * u, h * 0.13f), 0.8f, 0.95f, 0f, u)
    drawCloud(Offset(w * 0.93f + sin(time * 0.12f + 2f) * 6f * u, h * 0.24f), 0.6f, 0.9f, 0f, u)

    // Skyline on the horizon
    val horizon = h * 0.6f
    listOf(0.48f to 70f, 0.53f to 105f, 0.575f to 82f, 0.63f to 128f, 0.68f to 92f, 0.73f to 116f).forEachIndexed { i, (x, height) ->
        val bw = (22f + (i % 3) * 6f) * u
        drawRoundRect(
            if (i % 2 == 0) SkylineFar else Skyline,
            topLeft = Offset(w * x, horizon - height * u),
            size = Size(bw, height * u),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * u),
        )
    }

    // Rolling grass
    val grass = Path().apply {
        moveTo(left, horizon + 6f * u)
        cubicTo(w * 0.55f, horizon - 14f * u, w * 0.8f, horizon + 4f * u, w, horizon - 18f * u)
        lineTo(w, h)
        lineTo(left, h)
        close()
    }
    drawPath(grass, Brush.verticalGradient(listOf(GrassLight, GrassDeep), horizon - 18f * u, h))

    // Trees: a big one on the right edge, small ones along the horizon
    drawTree(Offset(w * 0.97f, horizon + 8f * u), 1.3f * u)
    drawTree(Offset(w * 0.5f, horizon + 4f * u), 0.62f * u)
    drawTree(Offset(w * 0.86f, horizon - 4f * u), 0.7f * u)

    // Running track sweeping from the bottom towards the trees
    val track = Path().apply {
        moveTo(w * 0.36f, h * 1.02f)
        cubicTo(w * 0.62f, h * 0.84f, w * 0.78f, h * 0.76f, w * 1.04f, h * 0.7f)
    }
    drawPath(track, TrackEdge, style = Stroke(width = 58f * u, cap = StrokeCap.Butt))
    drawPath(track, Track, style = Stroke(width = 52f * u, cap = StrokeCap.Butt))
    drawPath(track, Color.White.copy(alpha = 0.7f), style = Stroke(width = 2f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f * u, 12f * u))))

    // Flowers along the grass
    listOf(0.42f to 0.86f, 0.47f to 0.95f, 0.88f to 0.9f, 0.94f to 0.84f, 0.8f to 0.95f, 0.56f to 0.7f).forEach { (x, y) ->
        drawCircle(Flower, 3.2f * u, Offset(w * x, h * y))
        drawCircle(Color.White, 1.2f * u, Offset(w * x, h * y))
    }

    // Fade the left side and the bottom into the page
    drawRect(Brush.horizontalGradient(0f to Ivory, 1f to Ivory.copy(alpha = 0f), startX = left, endX = w * 0.58f), Offset(left, 0f), Size(w * 0.3f, h))
    drawRect(Brush.verticalGradient(0f to Ivory.copy(alpha = 0f), 1f to Ivory, startY = h * 0.82f, endY = h), Offset(0f, h * 0.82f), Size(w, h * 0.18f))

    drawRunner(time, arrive, hop, cheer, Offset(w * 0.73f, h * 0.6f), 44f * u, w)
}

private fun DrawScope.drawTree(base: Offset, s: Float) {
    drawRoundRect(Trunk, topLeft = base + Offset(-4f * s, -34f * s), size = Size(8f * s, 36f * s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * s))
    listOf(Offset(-16f, -44f) to 20f, Offset(14f, -46f) to 19f, Offset(0f, -64f) to 22f, Offset(0f, -40f) to 18f).forEach { (o, r) ->
        drawCircle(Leaf, r * s, base + o * s)
    }
    drawCircle(LeafLight, 10f * s, base + Offset(-6f, -70f) * s)
}

/** HavaCan in running gear: runs in from the right, keeps a light jog, hops and cheers on cue. */
private fun DrawScope.drawRunner(time: Float, arrive: Float, hop: Float, cheer: Float, rest: Offset, r: Float, width: Float) {
    val running = 1f - cheer
    val step = sin(time * 11f)
    val enterX = (1f - arrive) * (width - rest.x + r * 2f)
    val jump = sin(PI.toFloat() * hop.coerceIn(0f, 1f)) * r * 0.55f + sin(PI.toFloat() * cheer.coerceIn(0f, 1f)) * r * 0.9f
    val pose = HavaCanPose(
        offsetX = enterX,
        offsetY = -abs(step) * r * 0.12f * running - jump,
        tiltDegrees = 9f * running,
        leftArm = if (cheer > 0f) 3.9f else 2.75f + step * 0.7f,
        rightArm = if (cheer > 0f) -0.7f else 0.39f - step * 0.7f,
        leftFootLift = max(0f, step) * r * 0.2f * running,
        rightFootLift = max(0f, -step) * r * 0.2f * running,
        leftFootForward = -step * r * 0.14f * running,
        rightFootForward = step * r * 0.14f * running,
        mouthOpen = 1.2f + cheer * 0.2f,
        gear = 1f,
        speedLines = (1f - arrive).coerceIn(0f, 1f),
    )
    drawHavaCan(rest, r, pose)
}

/** Confetti from HavaCan when the run is saved; [p] runs 0 → 1. */
fun DrawScope.drawConfetti(p: Float, origin: Offset) {
    if (p <= 0f || p >= 1f) return
    val colors = listOf(Color(0xFF1D63F0), Color(0xFFFFC72C), Color(0xFF6CC24A), Color(0xFFE5484D), Color.White)
    val u = size.minDimension / 360f
    repeat(28) { i ->
        val angle = (i / 28f) * 2f * PI.toFloat() + i * 0.37f
        val speed = (120f + (i * 37 % 90)) * u
        val x = origin.x + kotlin.math.cos(angle) * speed * p
        val y = origin.y + sin(angle) * speed * p + 160f * u * p * p
        drawRect(
            colors[i % colors.size].copy(alpha = 1f - p),
            topLeft = Offset(x, y),
            size = Size(7f * u, 4f * u),
        )
    }
}
