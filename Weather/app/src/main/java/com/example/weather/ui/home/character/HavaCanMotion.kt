package com.example.weather.ui.home.character

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/**
 * Holds HavaCan's current [HavaCanState] and when it started, and turns that into a [HavaCanPose]
 * for a given clock time. All motion is a pure function of time, so a single frame clock drives
 * the whole scene and nothing animates while the app is in the background.
 */
@Stable
class HavaCanMotion {
    var state by mutableStateOf(HavaCanState.Idle)
        private set
    private var stateStartedAt by mutableFloatStateOf(0f)

    /** Bumped on every state entry so timers restart even when the same state repeats. */
    var stateStamp by mutableStateOf(0)
        private set

    fun send(event: HavaCanEvent, runner: Boolean, now: Float) {
        val next = HavaCanStateMachine.reduce(state, event, runner)
        if (next == state && event == HavaCanEvent.ReactionFinished) return
        state = next
        stateStartedAt = now
        stateStamp++
    }

    /**
     * @param clock seconds since the scene started
     * @param gear 0..1 headband and shoes
     * @param gait 0..1 how much HavaCan is running
     * @param u scene unit in px
     */
    fun pose(clock: Float, gear: Float, gait: Float, u: Float, intro: Float, spin: Float, still: Boolean): HavaCanPose {
        if (still) return HavaCanPose(gear = gear, alpha = intro.coerceIn(0f, 1f))

        val t = clock - stateStartedAt
        val step = sin(clock * 11f)

        // Calm idle life: float, breathe, sway, blink about every 3.7 s
        var offsetY = sin(clock * 1.9f) * 3f * u * (1 - gait)
        var squashY = 1f + sin(clock * 1.9f) * 0.015f
        var squashX = 1f - sin(clock * 1.9f) * 0.01f
        var tilt = spin * 10f
        var leftArm = 2.75f + sin(clock * 1.6f) * 0.08f
        var rightArm = 0.39f - sin(clock * 1.6f) * 0.08f
        var mouth = 1f
        val blinkPhase = clock % 3.7f
        val blink = if (blinkPhase < 0.16f) sin(PI.toFloat() * blinkPhase / 0.16f) else 0f

        // Running gait: bouncy steps, lean forward, arms pumping
        offsetY -= abs(step) * 7f * u * gait
        tilt += 9f * gait
        leftArm += step * 0.7f * gait
        rightArm -= step * 0.7f * gait
        val leftLift = max(0f, step) * 9f * u * gait
        val rightLift = max(0f, -step) * 9f * u * gait
        val leftForward = -step * 6f * u * gait
        val rightForward = step * 6f * u * gait

        when (state) {
            HavaCanState.Happy -> {
                val p = (t / 0.7f).coerceIn(0f, 1f)
                offsetY -= hop(p) * 22f * u
                squashY *= 1f + landing(p) * -0.12f
                squashX *= 1f + landing(p) * 0.1f
                rightArm = -0.9f + sin(t * 22f) * 0.35f
                mouth = 1.25f
            }
            HavaCanState.WeatherChange -> {
                val p = (t / 0.6f).coerceIn(0f, 1f)
                val pulse = sin(PI.toFloat() * p)
                squashY *= 1f - pulse * 0.12f
                squashX *= 1f + pulse * 0.1f
                leftArm -= pulse * 0.6f
                rightArm += pulse * 0.6f
            }
            HavaCanState.RunnerTransition -> {
                val p = (t / 0.9f).coerceIn(0f, 1f)
                offsetY -= hop(segment(p, 0.1f, 0.6f)) * 18f * u
                tilt += sin(p * PI.toFloat() * 2f) * 10f * (1 - p)
                leftArm = 2.75f - hop(segment(p, 0.1f, 0.6f)) * 1.2f
                rightArm = 0.39f + hop(segment(p, 0.1f, 0.6f)) * -1.2f
                mouth = 1.2f
            }
            HavaCanState.ChatOpening -> {
                val p = (t / 0.7f).coerceIn(0f, 1f)
                offsetY -= hop(p) * 26f * u
                squashX *= 1f + hop(p) * 0.06f
                squashY *= 1f + hop(p) * 0.06f
                leftArm = 3.6f
                rightArm = -0.45f
                mouth = 1.3f
            }
            HavaCanState.Idle, HavaCanState.Running -> Unit
        }

        // Intro: drop in from above with a soft bounce
        val introIn = intro.coerceIn(0f, 1f)
        offsetY -= (1 - bounceOut(introIn)) * 90f * u

        return HavaCanPose(
            offsetY = offsetY,
            squashX = squashX,
            squashY = squashY,
            tiltDegrees = tilt,
            leftArm = leftArm,
            rightArm = rightArm,
            leftFootLift = leftLift,
            rightFootLift = rightLift,
            leftFootForward = leftForward,
            rightFootForward = rightForward,
            blink = blink,
            mouthOpen = mouth,
            gear = gear,
            speedLines = gait,
            alpha = (introIn * 3f).coerceIn(0f, 1f),
        )
    }

    private fun hop(p: Float) = sin(PI.toFloat() * p.coerceIn(0f, 1f))

    /** Squash right after landing, at the end of a hop. */
    private fun landing(p: Float) = if (p > 0.75f) sin(PI.toFloat() * (p - 0.75f) / 0.25f) else 0f

    private fun segment(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)

    private fun bounceOut(x: Float): Float {
        val n1 = 7.5625f
        val d1 = 2.75f
        return when {
            x < 1 / d1 -> n1 * x * x
            x < 2 / d1 -> { val y = x - 1.5f / d1; n1 * y * y + 0.75f }
            x < 2.5 / d1 -> { val y = x - 2.25f / d1; n1 * y * y + 0.9375f }
            else -> { val y = x - 2.625f / d1; n1 * y * y + 0.984375f }
        }
    }
}
