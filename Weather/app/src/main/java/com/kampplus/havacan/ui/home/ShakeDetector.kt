package com.kampplus.havacan.ui.home

import kotlin.math.sqrt

/**
 * Detects a deliberate shake from accelerometer readings: two strong peaks close together,
 * then a cooldown so one shake never fires twice. Pure Kotlin so it can be unit tested.
 */
class ShakeDetector(
    private val thresholdG: Float = 2.5f,
    private val peakWindowMillis: Long = 600,
    private val cooldownMillis: Long = 1_500
) {
    private var firstPeakAt = -1L
    private var lastShakeAt = Long.MIN_VALUE / 2
    private var lastPeakAt = -1L

    /** Feed one reading (m/s²); returns true when a shake is recognised. */
    fun onReading(x: Float, y: Float, z: Float, timeMillis: Long): Boolean {
        val g = sqrt(x * x + y * y + z * z) / EARTH_GRAVITY
        if (g < thresholdG) return false
        if (timeMillis - lastShakeAt < cooldownMillis) return false
        // Readings of the same peak arrive a few ms apart; count each peak once
        if (lastPeakAt >= 0 && timeMillis - lastPeakAt < 120) return false
        lastPeakAt = timeMillis

        if (firstPeakAt < 0 || timeMillis - firstPeakAt > peakWindowMillis) {
            firstPeakAt = timeMillis
            return false
        }
        firstPeakAt = -1
        lastShakeAt = timeMillis
        return true
    }

    private companion object {
        const val EARTH_GRAVITY = 9.80665f
    }
}
