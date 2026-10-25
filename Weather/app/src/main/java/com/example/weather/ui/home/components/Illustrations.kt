package com.example.weather.ui.home.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

// Card illustrations drawn in code. Placeholders in the HavaCan style until the 3D renders arrive
// (see docs/havacan-assets.md).

private val Blue = Color(0xFF1D63F0)
private val BlueLight = Color(0xFF5B8CFF)
private val BlueDeep = Color(0xFF0F3FB0)
private val Yellow = Color(0xFFFFC72C)
private val YellowDeep = Color(0xFFF5A300)
private val Navy = Color(0xFF0B1B3F)
private val Drop = Color(0xFF2F74FF)

/** Blue and yellow umbrella with a few drops; [sway] in degrees. */
fun DrawScope.drawUmbrella(c: Offset, s: Float, sway: Float) {
    rotate(sway, pivot = c) {
        val canopy = Rect(c.x - 46f * s, c.y - 40f * s, c.x + 46f * s, c.y + 40f * s)
        val panels = listOf(Blue, Yellow, Blue, Yellow)
        panels.forEachIndexed { i, color ->
            drawArc(
                Brush.verticalGradient(listOf(color, if (color == Blue) BlueDeep else YellowDeep), canopy.top, c.y),
                startAngle = 180f + i * 45f,
                sweepAngle = 45f,
                useCenter = true,
                topLeft = canopy.topLeft,
                size = canopy.size,
            )
        }
        // Scalloped edge
        repeat(4) { i ->
            drawOval(
                Color(0xFFF2F6FF),
                topLeft = Offset(canopy.left + i * 23f * s, c.y - 6f * s),
                size = Size(23f * s, 12f * s),
            )
        }
        drawLine(Navy, c + Offset(0f, -42f * s), c + Offset(0f, -50f * s), 3f * s, StrokeCap.Round)
        drawLine(Navy, c, c + Offset(0f, 42f * s), 3.5f * s, StrokeCap.Round)
        drawArc(
            Yellow,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = c + Offset(-14f * s, 34f * s),
            size = Size(14f * s, 14f * s),
            style = Stroke(5f * s, cap = StrokeCap.Round),
        )
    }
    listOf(Offset(-40f, 30f), Offset(-20f, 52f), Offset(36f, 34f), Offset(54f, 58f)).forEach { o ->
        val p = c + o * s
        val path = Path().apply {
            moveTo(p.x, p.y - 6f * s)
            quadraticTo(p.x + 4.5f * s, p.y + 1f * s, p.x, p.y + 4.5f * s)
            quadraticTo(p.x - 4.5f * s, p.y + 1f * s, p.x, p.y - 6f * s)
        }
        drawPath(path, Drop)
    }
}

/** A running shoe in HavaCan blue with yellow stripes. */
fun DrawScope.drawRunningShoe(c: Offset, s: Float, tilt: Float) {
    rotate(tilt, pivot = c) {
        val sole = Path().apply {
            moveTo(c.x - 58f * s, c.y + 12f * s)
            lineTo(c.x + 50f * s, c.y + 12f * s)
            quadraticTo(c.x + 66f * s, c.y + 10f * s, c.x + 62f * s, c.y + 24f * s)
            lineTo(c.x - 54f * s, c.y + 26f * s)
            quadraticTo(c.x - 64f * s, c.y + 22f * s, c.x - 58f * s, c.y + 12f * s)
        }
        drawPath(sole, Color.White)
        drawPath(sole, Color(0xFFDDE3EE), style = Stroke(1.5f * s))
        val upper = Path().apply {
            moveTo(c.x - 56f * s, c.y + 14f * s)
            cubicTo(c.x - 60f * s, c.y - 18f * s, c.x - 34f * s, c.y - 34f * s, c.x - 12f * s, c.y - 30f * s)
            lineTo(c.x + 6f * s, c.y - 10f * s)
            cubicTo(c.x + 30f * s, c.y - 6f * s, c.x + 58f * s, c.y - 2f * s, c.x + 60f * s, c.y + 14f * s)
            close()
        }
        drawPath(upper, Brush.verticalGradient(listOf(BlueLight, Blue, BlueDeep), c.y - 34f * s, c.y + 14f * s))
        listOf(-24f, -6f).forEach { x ->
            drawLine(Yellow, c + Offset(x, -16f) * s, c + Offset(x + 18f, 10f) * s, 7f * s, StrokeCap.Round)
        }
        // Laces
        listOf(-10f, -2f, 6f).forEach { x ->
            drawLine(Color.White, c + Offset(x - 4f, -18f + x * 0.4f) * s, c + Offset(x + 6f, -14f + x * 0.4f) * s, 2.5f * s, StrokeCap.Round)
        }
        drawOval(Color.White.copy(alpha = 0.35f), topLeft = c + Offset(-48f, -14f) * s, size = Size(22f * s, 8f * s))
    }
}

/** Blue puffer jacket with a yellow zip and lining. [wiggle] in degrees. */
fun DrawScope.drawJacket(c: Offset, s: Float, wiggle: Float) {
    rotate(wiggle, pivot = c) {
        val body = Rect(c.x - 34f * s, c.y - 30f * s, c.x + 34f * s, c.y + 38f * s)
        // Solid fills plus a light overlay: a vertical gradient here rendered black on the API 37 emulator
        listOf(-1f, 1f).forEach { side ->
            val left = c.x + side * 34f * s - (if (side < 0) 20f * s else 0f)
            val sleeve = Rect(left, c.y - 24f * s, left + 20f * s, c.y + 34f * s)
            drawPath(Path().apply { addRoundRect(RoundRect(sleeve, CornerRadius(10f * s))) }, BlueDeep)
            drawPath(Path().apply { addRoundRect(RoundRect(sleeve.deflate(3f * s), CornerRadius(8f * s))) }, Blue)
        }
        drawPath(Path().apply { addRoundRect(RoundRect(body, CornerRadius(16f * s))) }, Blue)
        drawPath(
            Path().apply { addRoundRect(RoundRect(Rect(body.left, body.top, body.right, body.top + body.height * 0.45f), CornerRadius(16f * s))) },
            BlueLight.copy(alpha = 0.55f),
        )
        // Puffer quilting
        listOf(-8f, 8f, 24f).forEach { y ->
            drawLine(BlueDeep.copy(alpha = 0.5f), Offset(body.left + 4f * s, c.y + y * s), Offset(body.right - 4f * s, c.y + y * s), 2f * s)
        }
        // Hood and yellow lining
        drawArc(Blue, 180f, 180f, true, topLeft = Offset(c.x - 26f * s, c.y - 48f * s), size = Size(52f * s, 40f * s))
        drawArc(Yellow, 180f, 180f, true, topLeft = Offset(c.x - 16f * s, c.y - 40f * s), size = Size(32f * s, 26f * s))
        drawLine(Yellow, Offset(c.x, c.y - 28f * s), Offset(c.x, c.y + 38f * s), 5f * s)
        drawOval(Color.White.copy(alpha = 0.3f), topLeft = Offset(body.left + 6f * s, body.top + 8f * s), size = Size(16f * s, 30f * s))
    }
}

/** Little sun peeking from behind a cloud. */
fun DrawScope.drawSunAndCloud(c: Offset, s: Float, bob: Float) {
    val sun = c + Offset(14f * s, -14f * s + bob)
    drawCircle(Yellow.copy(alpha = 0.25f), 34f * s, sun)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF1A6), Yellow, YellowDeep), sun + Offset(-8f * s, -8f * s), 38f * s), 26f * s, sun)
    val cloud = c + Offset(-10f * s, 16f * s)
    val brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFD5E2F7)), cloud.y - 24f * s, cloud.y + 16f * s)
    drawCircle(brush, 16f * s, cloud + Offset(-18f, 4f) * s)
    drawCircle(brush, 22f * s, cloud + Offset(0f, -6f) * s)
    drawCircle(brush, 15f * s, cloud + Offset(20f, 4f) * s)
    drawRoundRect(brush, topLeft = cloud + Offset(-32f, 2f) * s, size = Size(64f * s, 16f * s), cornerRadius = CornerRadius(8f * s))
}
