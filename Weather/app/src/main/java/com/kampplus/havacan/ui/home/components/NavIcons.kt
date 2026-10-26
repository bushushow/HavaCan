package com.kampplus.havacan.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val Blue = Color(0xFF5B9BFF)
private val BlueDeep = Color(0xFF1D63F0)
private val Coral = Color(0xFFFF7A6B)
private val CoralDeep = Color(0xFFE5484D)
private val Gold = Color(0xFFFFD84A)
private val GoldDeep = Color(0xFFF5A300)
private val Ink = Color(0xFF0B1B3F)
private val Gloss = Color.White.copy(alpha = 0.45f)

/**
 * Colourful, slightly glossy tab icons in the same style as the weather icon, so the bottom bar
 * reads as one illustrated set. Each is drawn on a 24-unit grid scaled to [size].
 */
@Composable
fun PlansNavIcon(size: Dp = 28.dp) = Canvas(Modifier.size(size)) { drawCalendar(this.size.minDimension / 24f) }

@Composable
fun WardrobeNavIcon(size: Dp = 28.dp) = Canvas(Modifier.size(size)) { drawShirtOnHanger(this.size.minDimension / 24f) }

@Composable
fun ProfileNavIcon(size: Dp = 28.dp) = Canvas(Modifier.size(size)) { drawPerson(this.size.minDimension / 24f) }

/** A desk calendar: coral top band, white page, a golden "today". */
private fun DrawScope.drawCalendar(u: Float) {
    val body = Offset(2.5f * u, 4f * u)
    val bodySize = Size(19f * u, 17.5f * u)
    drawRoundRect(
        Brush.verticalGradient(listOf(Color.White, Color(0xFFE7EEFB)), body.y, body.y + bodySize.height),
        body,
        bodySize,
        CornerRadius(4f * u)
    )
    drawRoundRect(Color(0xFFC9D6EE), body, bodySize, CornerRadius(4f * u), style = Stroke(0.8f * u))
    // Top band
    val band = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = body.x,
                top = body.y,
                right = body.x + bodySize.width,
                bottom = body.y + 6f * u,
                topLeftCornerRadius = CornerRadius(4f * u),
                topRightCornerRadius = CornerRadius(4f * u)
            )
        )
    }
    drawPath(band, Brush.verticalGradient(listOf(Coral, CoralDeep), body.y, body.y + 6f * u))
    drawRoundRect(Gloss, Offset(body.x + 2f * u, body.y + 1.2f * u), Size(8f * u, 1.4f * u), CornerRadius(0.7f * u))
    // Rings
    listOf(7.5f, 16.5f).forEach { x ->
        drawRoundRect(Ink, Offset(x * u - 1f * u, 2f * u), Size(2f * u, 4.6f * u), CornerRadius(1f * u))
    }
    // Days, with today in gold
    for (row in 0..1) {
        for (col in 0..3) {
            val c = Offset((6.3f + col * 3.8f) * u, (13.5f + row * 4f) * u)
            if (row == 0 && col == 2) {
                drawCircle(Brush.radialGradient(listOf(Gold, GoldDeep), c - Offset(0.6f * u, 0.6f * u), 2.4f * u), 1.9f * u, c)
            } else {
                drawCircle(Color(0xFFB8C7E6), 0.9f * u, c)
            }
        }
    }
}

/** A blue shirt on a golden hanger. */
private fun DrawScope.drawShirtOnHanger(u: Float) {
    // Hook and bar
    val hook = Path().apply {
        moveTo(12f * u, 7f * u)
        lineTo(12f * u, 5.2f * u)
        cubicTo(12f * u, 3f * u, 15.2f * u, 3f * u, 15.2f * u, 4.6f * u)
    }
    drawPath(hook, GoldDeep, style = Stroke(1.6f * u, cap = StrokeCap.Round))
    drawLine(Gold, Offset(3.5f * u, 9.5f * u), Offset(12f * u, 6.8f * u), 1.6f * u, StrokeCap.Round)
    drawLine(Gold, Offset(20.5f * u, 9.5f * u), Offset(12f * u, 6.8f * u), 1.6f * u, StrokeCap.Round)
    // Shirt
    val shirt = Path().apply {
        moveTo(8.6f * u, 8.6f * u)
        lineTo(3f * u, 11.4f * u)
        lineTo(4.8f * u, 15f * u)
        lineTo(7f * u, 14.2f * u)
        lineTo(7f * u, 21.5f * u)
        lineTo(17f * u, 21.5f * u)
        lineTo(17f * u, 14.2f * u)
        lineTo(19.2f * u, 15f * u)
        lineTo(21f * u, 11.4f * u)
        lineTo(15.4f * u, 8.6f * u)
        quadraticTo(12f * u, 11.6f * u, 8.6f * u, 8.6f * u)
        close()
    }
    drawPath(shirt, Brush.verticalGradient(listOf(Blue, BlueDeep), 8f * u, 22f * u))
    drawRoundRect(Gloss, Offset(8.2f * u, 12.5f * u), Size(1.6f * u, 6.5f * u), CornerRadius(0.8f * u))
    // Collar
    val collar = Path().apply {
        moveTo(9.4f * u, 9f * u)
        quadraticTo(12f * u, 11.2f * u, 14.6f * u, 9f * u)
    }
    drawPath(collar, Color.White.copy(alpha = 0.85f), style = Stroke(1.1f * u, cap = StrokeCap.Round))
}

/** A friendly figure: golden head, blue shoulders. */
private fun DrawScope.drawPerson(u: Float) {
    val body = Path().apply {
        moveTo(3.5f * u, 22f * u)
        cubicTo(3.5f * u, 15.5f * u, 8f * u, 13.4f * u, 12f * u, 13.4f * u)
        cubicTo(16f * u, 13.4f * u, 20.5f * u, 15.5f * u, 20.5f * u, 22f * u)
        close()
    }
    drawPath(body, Brush.verticalGradient(listOf(Blue, BlueDeep), 13f * u, 22f * u))
    drawRoundRect(Gloss, Offset(6.4f * u, 16.2f * u), Size(4f * u, 1.3f * u), CornerRadius(0.65f * u))
    val head = Offset(12f * u, 8f * u)
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFFFFF4C2), Gold, GoldDeep), head - Offset(1.6f * u, 1.8f * u), 6.5f * u),
        4.9f * u,
        head
    )
    drawCircle(Color.White.copy(alpha = 0.7f), 1f * u, head - Offset(1.8f * u, 2f * u))
}
