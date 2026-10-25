package com.example.weather.ui.chat

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.weather.R
import com.example.weather.ui.home.character.HavaCanPose
import com.example.weather.ui.home.character.drawHavaCan
import com.example.weather.ui.theme.HavaBlue
import com.example.weather.ui.theme.Sun
import com.example.weather.ui.voice.VoiceGuideState
import com.example.weather.ui.voice.VoicePhase
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * HavaCan floating in a ring of light with sound waves either side. The waves follow the
 * microphone level while listening and move with the voice while HavaCan speaks. [clock] is the
 * shared scene clock (paused when the phone asks for reduced motion) and is read only while drawing.
 */
@Composable
fun AssistantHero(state: VoiceGuideState, clock: () -> Float, modifier: Modifier = Modifier) {
    val latest: State<VoiceGuideState> = rememberUpdatedState(state)
    val description = stringResource(R.string.cd_havacan_hero)
    Canvas(modifier.semantics { contentDescription = description }) {
        val s by latest
        val t = clock()
        val c = Offset(size.width / 2, size.height * 0.5f)
        val r = size.minDimension * 0.27f

        drawGlow(c, r, t)
        drawWaves(c, r, t, s.phase, s.level)
        drawSparkles(c, r, t)

        val speaking = s.phase == VoicePhase.Speaking
        val listening = s.phase == VoicePhase.Listening
        val bob = sin(t * 1.9f) * r * 0.04f
        val wave = sin(t * 5f) * 0.25f
        val blinkPhase = t % 3.7f
        val blink = if (blinkPhase < 0.16f) sin(PI.toFloat() * blinkPhase / 0.16f) else 0f
        drawHavaCan(
            c,
            r,
            HavaCanPose(
                offsetY = bob,
                tiltDegrees = if (listening) 6f else sin(t * 1.3f) * 2f,
                leftArm = if (listening) 3.9f else 3.5f - wave,
                rightArm = if (speaking) -0.6f + wave else -0.3f + wave * 0.5f,
                blink = blink,
                mouthOpen = if (speaking) 0.9f + abs(sin(t * 13f)) * 0.6f else 1.25f,
            ),
        )
    }
}

private fun DrawScope.drawGlow(c: Offset, r: Float, t: Float) {
    drawCircle(Brush.radialGradient(listOf(HavaBlue.copy(alpha = 0.18f), Color.Transparent), c, r * 2.3f), r * 2.3f, c)
    // Tilted rings of light around HavaCan, slowly turning
    for (i in 0..2) {
        val w = r * (3.0f + i * 0.45f)
        val h = r * (0.9f + i * 0.18f)
        rotate(-8f + sin(t * 0.6f + i) * 3f, c) {
            drawOval(
                color = HavaBlue.copy(alpha = 0.22f - i * 0.05f),
                topLeft = Offset(c.x - w / 2, c.y + r * 0.35f - h / 2),
                size = Size(w, h),
                style = Stroke(width = r * 0.05f),
            )
        }
    }
}

private fun DrawScope.drawWaves(c: Offset, r: Float, t: Float, phase: VoicePhase, level: Float) {
    val bars = 9
    val gap = r * 0.17f
    val maxH = r * 1.15f
    for (side in listOf(-1f, 1f)) {
        for (i in 0 until bars) {
            val x = c.x + side * (r * 1.3f + i * gap)
            // Taller near HavaCan, fading out towards the edges
            val envelope = 1f - i / bars.toFloat() * 0.65f
            val jitter = 0.5f + 0.5f * sin(t * 9f + i * 1.7f + side)
            val amount = when (phase) {
                VoicePhase.Listening -> 0.25f + level * 0.75f * jitter
                VoicePhase.Speaking -> 0.35f + 0.6f * jitter
                VoicePhase.Thinking -> 0.25f + 0.35f * (0.5f + 0.5f * sin(t * 6f - i * 0.6f))
                VoicePhase.Idle -> 0.18f + 0.14f * (0.5f + 0.5f * sin(t * 2.2f + i * 0.9f))
            }
            val h = maxH * envelope * amount
            drawLine(
                color = HavaBlue.copy(alpha = 0.35f + 0.55f * envelope),
                start = Offset(x, c.y - h / 2),
                end = Offset(x, c.y + h / 2),
                strokeWidth = r * 0.075f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawSparkles(c: Offset, r: Float, t: Float) {
    val dots = listOf(
        Triple(0.0f, 1.55f, HavaBlue), Triple(1.1f, 1.75f, Sun), Triple(2.3f, 1.6f, HavaBlue),
        Triple(3.4f, 1.85f, Sun), Triple(4.4f, 1.5f, HavaBlue), Triple(5.4f, 1.7f, Sun),
    )
    dots.forEachIndexed { i, (angle, dist, color) ->
        val a = angle + t * 0.15f
        val p = c + Offset(cos(a) * r * dist * 1.25f, sin(a) * r * dist * 0.62f - sin(t * 1.5f + i) * r * 0.06f)
        val size = r * (if (i % 2 == 0) 0.11f else 0.08f)
        drawCircle(Brush.radialGradient(listOf(Color.White, color), p + Offset(-size * 0.3f, -size * 0.3f), size * 1.4f), size, p)
    }
}
