package com.example.weather.ui.home.scene

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

internal fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/** Progress of [t] through [start]..[end], clamped to 0..1. */
internal fun segment(t: Float, start: Float, end: Float) = ((t - start) / (end - start)).coerceIn(0f, 1f)

internal fun easeOutCubic(t: Float) = 1f - (1f - t).pow(3)

internal fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    return 1f + c3 * (t - 1f).pow(3) + c1 * (t - 1f).pow(2)
}

/** 0 → 1 → 0 hump over t in 0..1. */
internal fun hump(t: Float) = sin(PI.toFloat() * t.coerceIn(0f, 1f))

/**
 * Where everything sits in the hero scene, shared by the drawing code and the touch overlay
 * so taps on HavaCan land exactly on the drawn character.
 */
internal class SceneGeometry(val width: Float, val height: Float) {
    /** Design unit: the scene is laid out on a 400 × 320 grid. */
    val u: Float = minOf(width / 400f, height / 320f)
    val island = Offset(width * 0.635f, height * 0.75f)
    val islandRx = 138f * u
    val islandRy = 46f * u
    val islandThickness = 24f * u
    val characterRadius = 40f * u
    val character = Offset(island.x - 6f * u, island.y - 58f * u)
    val sun = Offset(width * 0.83f, height * 0.17f)
    val sunRadius = 26f * u
}
