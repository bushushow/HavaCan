package com.kampplus.havacan.ui.home.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val StoneLight = Color(0xFFFFFFFF)
private val StoneMid = Color(0xFFEDE7DA)
private val StoneDark = Color(0xFFCBC1AE)
private val GrassLight = Color(0xFFA6DE6B)
private val GrassDark = Color(0xFF55A83A)
private val WaterLight = Color(0xFF8FDBFF)
private val WaterDark = Color(0xFF2C86E0)
private val Brick = Color(0xFFD9744F)
private val BrickDark = Color(0xFFB8543A)
private val TreeLight = Color(0xFF86CF56)
private val TreeDark = Color(0xFF3E8A2A)
private val Trunk = Color(0xFF8A5A3C)
private val Rock = Color(0xFFB9BEC6)
private val OrbitBlue = Color(0xFF1D63F0)
private val OrbitYellow = Color(0xFFFFC72C)

/**
 * The floating island. [track] morphs the meadow path and pond into a running track (Koşucu Modu).
 * [spin] (-1..1) comes from the user's drag: top features slide further than the base,
 * which reads as the island turning in 3D.
 */
internal fun DrawScope.drawIsland(g: SceneGeometry, track: Float, spin: Float, rise: Float) {
    val u = g.u
    val c = g.island + Offset(spin * 10f * u, (1 - rise) * 60f * u)
    val rx = g.islandRx
    val ry = g.islandRy
    val t = g.islandThickness

    // Shadow on the "ground"
    drawOval(
        Color(0xFF0B1B3F).copy(alpha = 0.08f * rise),
        topLeft = Offset(c.x - rx * 0.9f, c.y + t + ry * 0.55f),
        size = Size(rx * 1.8f, 20f * u)
    )

    // Stone base: a short cylinder, lit from the upper left
    val sideBrush = Brush.horizontalGradient(
        0f to StoneDark,
        0.28f to StoneLight,
        0.6f to StoneMid,
        1f to StoneDark,
        startX = c.x - rx,
        endX = c.x + rx
    )
    drawOval(sideBrush, topLeft = Offset(c.x - rx, c.y + t - ry), size = Size(rx * 2, ry * 2))
    drawRect(sideBrush, topLeft = Offset(c.x - rx, c.y), size = Size(rx * 2, t))
    drawOval(StoneLight, topLeft = Offset(c.x - rx, c.y - ry), size = Size(rx * 2, ry * 2))

    // Meadow top
    val grassRect = Rect(c.x - rx + 9f * u, c.y - ry + 5f * u, c.x + rx - 9f * u, c.y + ry - 6f * u)
    drawOval(
        Brush.radialGradient(listOf(GrassLight, GrassDark), Offset(c.x - rx * 0.25f, c.y - ry * 0.6f), rx * 1.2f),
        topLeft = grassRect.topLeft,
        size = grassRect.size
    )

    val grassPath = Path().apply { addOval(grassRect) }
    clipPath(grassPath) {
        val featureShift = Offset(spin * 26f * u, 0f)
        drawMeadow(c + featureShift, u, alpha = 1 - track)
        drawTrack(c + featureShift, u, progress = track)
    }

    // Trees and rocks around the rim, back ones first
    val trees = listOf(200f, 228f, 262f, 330f, 352f, 18f, 160f)
    trees.forEachIndexed { i, deg ->
        val a = deg * (PI / 180).toFloat()
        val p = Offset(c.x + cos(a) * (rx - 26f * u) + spin * 18f * u, c.y + sin(a) * (ry - 12f * u))
        drawTree(p, u * (0.8f + (i % 3) * 0.12f))
    }
    drawOval(Rock, topLeft = Offset(c.x - rx * 0.62f, c.y + ry * 0.22f), size = Size(22f * u, 12f * u))
    drawOval(Color.White.copy(alpha = 0.6f), topLeft = Offset(c.x - rx * 0.6f, c.y + ry * 0.24f), size = Size(9f * u, 4f * u))
    drawOval(Rock, topLeft = Offset(c.x - rx * 0.5f, c.y + ry * 0.34f), size = Size(14f * u, 9f * u))
}

private fun DrawScope.drawMeadow(c: Offset, u: Float, alpha: Float) {
    if (alpha < 0.01f) return
    // Winding brick path
    val path = Path().apply {
        moveTo(c.x - 150f * u, c.y + 26f * u)
        cubicTo(c.x - 90f * u, c.y + 6f * u, c.x - 40f * u, c.y + 34f * u, c.x + 8f * u, c.y + 14f * u)
        cubicTo(c.x + 40f * u, c.y + 2f * u, c.x + 60f * u, c.y - 22f * u, c.x + 30f * u, c.y - 40f * u)
    }
    drawPath(path, BrickDark, alpha = alpha, style = Stroke(width = 15f * u, cap = StrokeCap.Round))
    drawPath(path, Brick, alpha = alpha, style = Stroke(width = 11f * u, cap = StrokeCap.Round))
    // Pond
    val pond = Rect(c.x + 30f * u, c.y - 22f * u, c.x + 120f * u, c.y + 14f * u)
    drawOval(
        Brush.linearGradient(listOf(WaterLight, WaterDark), pond.topLeft, pond.bottomRight),
        topLeft = pond.topLeft,
        size = pond.size,
        alpha = alpha
    )
    drawOval(
        Color.White.copy(alpha = 0.55f * alpha),
        topLeft = Offset(pond.left + 18f * u, pond.top + 6f * u),
        size = Size(30f * u, 6f * u)
    )
}

/** A stadium-style running track that grows in as Koşucu Modu turns on. */
private fun DrawScope.drawTrack(c: Offset, u: Float, progress: Float) {
    if (progress < 0.01f) return
    val s = lerp(0.6f, 1f, progress)
    val rx = 104f * u * s
    val ry = 28f * u * s
    val oval = Rect(c.x - rx, c.y - ry - 2f * u, c.x + rx, c.y + ry - 2f * u)
    drawOval(BrickDark, topLeft = oval.topLeft, size = oval.size, alpha = progress, style = Stroke(22f * u * s))
    drawOval(Brick, topLeft = oval.topLeft, size = oval.size, alpha = progress, style = Stroke(18f * u * s))
    listOf(-5f, 5f).forEach { off ->
        val lane = oval.inflate(off * u * s)
        drawOval(
            Color.White.copy(alpha = 0.85f * progress),
            topLeft = lane.topLeft,
            size = lane.size,
            style = Stroke(1.4f * u)
        )
    }
    // Finish line
    drawLine(
        Color.White.copy(alpha = progress),
        Offset(c.x + 4f * u, oval.bottom - 9f * u * s),
        Offset(c.x + 4f * u, oval.bottom + 9f * u * s),
        strokeWidth = 3f * u
    )
}

private fun DrawScope.drawTree(base: Offset, u: Float) {
    drawLine(Trunk, base, base + Offset(0f, -10f * u), strokeWidth = 3.5f * u, cap = StrokeCap.Round)
    val crown = base + Offset(0f, -18f * u)
    drawCircle(
        Brush.radialGradient(listOf(TreeLight, TreeDark), crown + Offset(-4f * u, -5f * u), 16f * u),
        11f * u,
        crown
    )
    drawCircle(TreeLight.copy(alpha = 0.9f), 4f * u, crown + Offset(-4f * u, -4f * u))
}

/** Glass ring with blue and yellow beads circling the island; [front] draws the half nearer the viewer. */
internal fun DrawScope.drawOrbit(g: SceneGeometry, time: Float, spin: Float, rise: Float, front: Boolean) {
    val u = g.u
    val c = g.island + Offset(spin * 10f * u, g.islandThickness * 0.5f + (1 - rise) * 60f * u)
    val rx = g.islandRx + 22f * u
    val ry = g.islandRy + 20f * u
    if (!front) {
        drawOval(
            Color.White.copy(alpha = 0.75f * rise),
            topLeft = Offset(c.x - rx, c.y - ry),
            size = Size(rx * 2, ry * 2),
            style = Stroke(2.2f * u)
        )
        drawOval(
            OrbitBlue.copy(alpha = 0.12f * rise),
            topLeft = Offset(c.x - rx, c.y - ry),
            size = Size(rx * 2, ry * 2),
            style = Stroke(5f * u)
        )
    }
    repeat(5) { i ->
        val a = time * 0.35f + spin * 0.8f + i * (2 * PI / 5).toFloat()
        val isFront = sin(a) > 0
        if (isFront != front) return@repeat
        val p = Offset(c.x + cos(a) * rx, c.y + sin(a) * ry)
        val color = if (i % 2 == 0) OrbitBlue else OrbitYellow
        val r = (if (isFront) 7.5f else 5.5f) * u
        drawCircle(
            Brush.radialGradient(listOf(Color.White, color), p + Offset(-r * 0.4f, -r * 0.4f), r * 1.6f),
            r,
            p,
            alpha = rise
        )
    }
}
