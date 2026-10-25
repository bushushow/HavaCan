package com.example.weather.ui.home.character

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import com.example.weather.ui.home.components.drawUmbrella
import kotlin.math.sin

enum class HavaCanPoseStyle { Cheer, Umbrella, Peek }

/**
 * HavaCan as a decoration for cards and headers, gently bobbing. [clock] comes from the shared
 * scene clock so all characters on screen move in step; it's read only while drawing.
 */
@Composable
fun HavaCanArt(style: HavaCanPoseStyle, clock: () -> Float, modifier: Modifier = Modifier) {
    Canvas(modifier.clipToBounds()) {
        val t = clock()
        val bob = sin(t * 1.9f) * size.minDimension * 0.02f
        val wave = sin(t * 6f) * 0.25f
        when (style) {
            HavaCanPoseStyle.Cheer -> {
                val r = size.minDimension * 0.36f
                drawHavaCan(
                    Offset(size.width * 0.5f, size.height * 0.5f),
                    r,
                    HavaCanPose(offsetY = bob, leftArm = 3.7f - wave, rightArm = -0.55f + wave, mouthOpen = 1.25f),
                )
            }
            HavaCanPoseStyle.Umbrella -> {
                val r = size.minDimension * 0.3f
                val c = Offset(size.width * 0.42f, size.height * 0.64f)
                drawHavaCan(c, r, HavaCanPose(offsetY = bob, leftArm = 3.0f, rightArm = -0.35f, mouthOpen = 1.2f))
                drawUmbrella(c + Offset(r * 1.05f, -r * 1.25f + bob), r / 46f * 0.95f, sway = 14f + sin(t * 1.6f) * 4f)
            }
            HavaCanPoseStyle.Peek -> {
                // Only the top half shows, as if HavaCan pops up from behind the card edge
                val r = size.minDimension * 0.5f
                drawHavaCan(
                    Offset(size.width * 0.5f, size.height * 0.95f + bob),
                    r,
                    HavaCanPose(leftArm = 3.5f - wave, rightArm = -0.35f + wave, mouthOpen = 1.2f),
                )
            }
        }
    }
}
