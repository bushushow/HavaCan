package com.kampplus.havacan.ui.home.character

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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.cos
import kotlin.math.sin

private val BodyHighlight = Color(0xFFFFF6B8)
private val BodyMid = Color(0xFFFFD42E)
private val BodyDeep = Color(0xFFF6B300)
private val BodyRim = Color(0xFFE08E00)
private val LimbLight = Color(0xFF5B8CFF)
private val LimbDark = Color(0xFF1240B8)
private val Face = Color(0xFF0B1B3F)
private val Mouth = Color(0xFF7A1B2B)
private val Tongue = Color(0xFFFF6B7A)
private val Cheek = Color(0xFFFF8FA3)
private val Band = Color(0xFF1D63F0)
private val ShoeBlue = Color(0xFF1D5BE0)
private val ShoeStripe = Color(0xFFFFC72C)

/** Everything that moves on HavaCan for one frame. Computed by [HavaCanMotion], drawn here. */
data class HavaCanPose(
    val offsetY: Float = 0f,
    val offsetX: Float = 0f,
    val squashX: Float = 1f,
    val squashY: Float = 1f,
    val tiltDegrees: Float = 0f,
    /** Arm angles in radians around the body, 0 = right, PI = left. */
    val leftArm: Float = 2.75f,
    val rightArm: Float = 0.39f,
    val leftFootLift: Float = 0f,
    val rightFootLift: Float = 0f,
    val leftFootForward: Float = 0f,
    val rightFootForward: Float = 0f,
    /** 0 eyes open, 1 shut. */
    val blink: Float = 0f,
    val mouthOpen: Float = 1f,
    /** 0 = everyday HavaCan, 1 = headband and running shoes fully on. */
    val gear: Float = 0f,
    val speedLines: Float = 0f,
    val alpha: Float = 1f
)

/**
 * Draws HavaCan: a glossy yellow sphere with a happy face, little blue limbs, and in Koşucu Modu
 * a striped headband and running shoes. [r] is the body radius in px.
 */
fun DrawScope.drawHavaCan(center: Offset, r: Float, pose: HavaCanPose) {
    if (pose.alpha <= 0.01f) return
    val c = center + Offset(pose.offsetX, pose.offsetY)

    // Shadow on the ground shrinks as HavaCan jumps
    val lift = (-pose.offsetY / r).coerceIn(0f, 1.5f)
    drawOval(
        Color(0xFF0B1B3F).copy(alpha = 0.16f * pose.alpha * (1 - lift * 0.5f)),
        topLeft = Offset(center.x - r * (0.8f - lift * 0.2f), center.y + r * 1.12f),
        size = Size(r * (1.6f - lift * 0.4f), r * 0.28f)
    )

    if (pose.speedLines > 0.01f) drawSpeedLines(c, r, pose.speedLines * pose.alpha)

    rotate(pose.tiltDegrees, pivot = c + Offset(0f, r)) {
        scale(pose.squashX, pose.squashY, pivot = c + Offset(0f, r)) {
            drawLimb(c, r, pose.leftArm, pose.alpha, back = true)
            drawFoot(c, r, -1f, pose.leftFootLift, pose.leftFootForward, pose.gear, pose.alpha)
            drawBody(c, r, pose)
            drawFoot(c, r, 1f, pose.rightFootLift, pose.rightFootForward, pose.gear, pose.alpha)
            drawLimb(c, r, pose.rightArm, pose.alpha, back = false)
        }
    }
}

private fun DrawScope.drawBody(c: Offset, r: Float, pose: HavaCanPose) {
    val a = pose.alpha
    drawCircle(
        Brush.radialGradient(
            0f to BodyHighlight,
            0.35f to BodyMid,
            0.78f to BodyDeep,
            1f to BodyRim,
            center = c + Offset(-r * 0.35f, -r * 0.42f),
            radius = r * 1.55f
        ),
        r,
        c,
        alpha = a
    )
    // Gloss
    rotate(-28f, pivot = c + Offset(-r * 0.4f, -r * 0.5f)) {
        drawOval(
            Color.White.copy(alpha = 0.7f * a),
            topLeft = c + Offset(-r * 0.62f, -r * 0.62f),
            size = Size(r * 0.42f, r * 0.22f)
        )
    }
    drawCircle(Color.White.copy(alpha = 0.8f * a), r * 0.05f, c + Offset(-r * 0.12f, -r * 0.7f))

    if (pose.gear > 0.01f) drawHeadband(c, r, pose.gear, a)

    // Cheeks
    listOf(-1f, 1f).forEach { side ->
        drawOval(
            Cheek.copy(alpha = 0.45f * a),
            topLeft = c + Offset(side * r * 0.56f - r * 0.14f, r * 0.08f),
            size = Size(r * 0.28f, r * 0.16f)
        )
    }
    // Happy "^ ^" eyes; blinking flattens them
    val eyeH = r * 0.2f * (1 - pose.blink * 0.75f)
    listOf(-1f, 1f).forEach { side ->
        val e = c + Offset(side * r * 0.32f, -r * 0.1f)
        val path = Path().apply {
            moveTo(e.x - r * 0.15f, e.y + eyeH * 0.5f)
            quadraticTo(e.x, e.y - eyeH, e.x + r * 0.15f, e.y + eyeH * 0.5f)
        }
        drawPath(path, Face.copy(alpha = a), style = Stroke(width = r * 0.075f, cap = StrokeCap.Round))
    }
    // Open smile with tongue
    val mouthH = r * 0.3f * pose.mouthOpen
    val mouth = Rect(c.x - r * 0.22f, c.y + r * 0.14f - mouthH * 0.35f, c.x + r * 0.22f, c.y + r * 0.14f + mouthH)
    drawArc(Mouth.copy(alpha = a), 0f, 180f, useCenter = true, topLeft = mouth.topLeft, size = mouth.size)
    val tongue = Rect(
        mouth.left + mouth.width * 0.25f,
        mouth.top + mouth.height * 0.45f,
        mouth.right - mouth.width * 0.25f,
        mouth.bottom - r * 0.02f
    )
    clipPath(Path().apply { addArc(mouth, 0f, 180f) }) {
        drawOval(Tongue.copy(alpha = a), topLeft = tongue.topLeft, size = tongue.size)
    }
}

/** Sweatband that slides down onto HavaCan's forehead as [gear] goes 0 → 1. */
private fun DrawScope.drawHeadband(c: Offset, r: Float, gear: Float, alpha: Float) {
    val bodyClip = Path().apply { addOval(Rect(c, r)) }
    val drop = (1 - gear) * -r * 0.9f
    clipPath(bodyClip) {
        translate(top = drop) {
            val top = c.y - r * 0.66f
            val h = r * 0.26f
            val band = Path().apply {
                moveTo(c.x - r * 1.1f, top + h * 0.35f)
                quadraticTo(c.x, top - h * 0.45f, c.x + r * 1.1f, top + h * 0.35f)
                lineTo(c.x + r * 1.1f, top + h * 1.35f)
                quadraticTo(c.x, top + h * 0.55f, c.x - r * 1.1f, top + h * 1.35f)
                close()
            }
            drawPath(band, Band, alpha = alpha * gear.coerceIn(0f, 1f))
            listOf(0.38f, 0.62f).forEach { f ->
                val stripe = Path().apply {
                    moveTo(c.x - r * 1.1f, top + h * (0.35f + f * 0.9f))
                    quadraticTo(c.x, top + h * (-0.45f + f * 1.0f) + h * 0.05f, c.x + r * 1.1f, top + h * (0.35f + f * 0.9f))
                }
                drawPath(stripe, Color.White.copy(alpha = 0.95f * alpha * gear), style = Stroke(r * 0.045f))
            }
        }
    }
}

private fun DrawScope.drawLimb(c: Offset, r: Float, angle: Float, alpha: Float, back: Boolean) {
    val p = c + Offset(cos(angle), sin(angle)) * (r * 1.02f)
    val lr = r * 0.2f
    drawCircle(
        Brush.radialGradient(listOf(LimbLight, LimbDark), p + Offset(-lr * 0.4f, -lr * 0.4f), lr * 1.6f),
        lr,
        p,
        alpha = alpha * if (back) 0.95f else 1f
    )
}

/** A little blue foot, or a running shoe when [gear] is on. [side] is -1 left, 1 right. */
private fun DrawScope.drawFoot(c: Offset, r: Float, side: Float, lift: Float, forward: Float, gear: Float, alpha: Float) {
    val base = c + Offset(side * r * 0.36f + forward, r * 0.98f - lift)
    val plain = 1 - gear
    if (plain > 0.01f) {
        drawOval(
            Brush.radialGradient(listOf(LimbLight, LimbDark), base + Offset(-r * 0.08f, -r * 0.08f), r * 0.3f),
            topLeft = base + Offset(-r * 0.18f, -r * 0.12f),
            size = Size(r * 0.36f, r * 0.24f),
            alpha = alpha * plain
        )
    }
    if (gear > 0.01f) {
        val s = 0.4f + 0.6f * gear
        scale(s, s, pivot = base) {
            val w = r * 0.56f
            val h = r * 0.3f
            val toe = base + Offset(-w * 0.3f, -h * 0.55f)
            // Sole
            drawRoundRect(
                Color.White,
                topLeft = Offset(toe.x, base.y + h * 0.18f),
                size = Size(w, h * 0.28f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.14f),
                alpha = alpha * gear
            )
            // Upper
            drawRoundRect(
                Brush.verticalGradient(listOf(LimbLight, ShoeBlue), toe.y, base.y + h * 0.2f),
                topLeft = toe,
                size = Size(w, h * 0.78f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.38f),
                alpha = alpha * gear
            )
            listOf(0.38f, 0.6f).forEach { f ->
                drawLine(
                    ShoeStripe.copy(alpha = alpha * gear),
                    Offset(toe.x + w * f, toe.y + h * 0.12f),
                    Offset(toe.x + w * (f - 0.1f), toe.y + h * 0.66f),
                    strokeWidth = r * 0.05f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun DrawScope.drawSpeedLines(c: Offset, r: Float, alpha: Float) {
    listOf(0.9f to -0.45f, 1.1f to -0.15f, 0.9f to 0.2f).forEachIndexed { i, (dx, dy) ->
        val start = c + Offset(-r * (dx + 0.35f), r * dy)
        drawArc(
            Band.copy(alpha = alpha * (1f - i * 0.2f)),
            startAngle = 140f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = start + Offset(-r * 0.2f, -r * 0.2f),
            size = Size(r * 0.4f, r * 0.4f),
            style = Stroke(r * 0.07f, cap = StrokeCap.Round)
        )
    }
}

/** HavaCan's face only, for the bottom navigation button. */
fun DrawScope.drawHavaCanFace(center: Offset, r: Float) {
    drawBody(center, r, HavaCanPose())
}
