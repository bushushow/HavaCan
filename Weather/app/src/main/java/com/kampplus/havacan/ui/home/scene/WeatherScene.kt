package com.kampplus.havacan.ui.home.scene

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kampplus.havacan.ui.home.character.HavaCanMotion
import com.kampplus.havacan.ui.home.character.drawHavaCan
import com.kampplus.havacan.ui.theme.Navy
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * The interactive hero scene: sky, floating island, orbiting beads and HavaCan.
 *
 * One frame clock drives every continuous motion and is read only inside the Canvas draw block,
 * so a running scene re-draws but never re-composes. The clock stops when [still] (reduced
 * motion) is set, and Compose stops delivering frames while the app is in the background.
 */
@Composable
fun WeatherScene(
    look: SceneLook,
    motion: HavaCanMotion,
    /** 0..1 Koşucu Modu: headband/shoes, track and running gait each get their own curve. */
    gear: Float,
    track: Float,
    gait: Float,
    intro: Float,
    still: Boolean,
    showGreeting: Boolean,
    greeting: String,
    onCharacterTap: () -> Unit,
    clock: () -> Float,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    // Horizontal drag turns the island a little; released, it springs back with a wobble
    val spinPx = remember { Animatable(0f) }

    BoxWithConstraints(
        modifier = modifier.pointerInput(still) {
            val maxPx = 140.dp.toPx()
            detectHorizontalDragGestures(
                onDragEnd = {
                    scope.launch {
                        spinPx.animateTo(0f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow))
                    }
                },
                onDragCancel = { scope.launch { spinPx.animateTo(0f, spring()) } }
            ) { change, dx ->
                change.consume()
                scope.launch { spinPx.snapTo((spinPx.value + dx).coerceIn(-maxPx, maxPx)) }
            }
        }
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val geometry = remember(widthPx, heightPx) { SceneGeometry(widthPx, heightPx) }
        val maxSpin = with(density) { 140.dp.toPx() }

        Canvas(Modifier.fillMaxSize()) {
            val time = clock()
            val spin = spinPx.value / maxSpin
            val rise = easeOutBack(segment(intro, 0.05f, 0.55f))
            val skyShift = -spin * 12f * geometry.u

            drawAmbientGlow(geometry, look)
            drawMoonAndStars(geometry, look, time, skyShift)
            drawSun(geometry, look, time, skyShift)
            drawClouds(geometry, look, time, skyShift)
            drawOrbit(geometry, time, spin, rise, front = false)
            drawIsland(geometry, track, spin, rise)
            val pose = motion.pose(
                clock = time,
                gear = gear,
                gait = gait,
                u = geometry.u,
                intro = segment(intro, 0.3f, 1f),
                spin = spin,
                still = still
            )
            drawHavaCan(
                geometry.character + androidx.compose.ui.geometry.Offset(spin * 14f * geometry.u, (1 - rise) * 60f * geometry.u),
                geometry.characterRadius,
                pose
            )
            drawOrbit(geometry, time, spin, rise, front = true)
            drawPrecipitation(geometry, look, time)
        }

        // Invisible, accessible touch target exactly over the drawn character
        val r = geometry.characterRadius
        val hit = with(density) { (r * 2.6f).toDp() }
        Box(
            Modifier
                .offset {
                    IntOffset(
                        (geometry.character.x - r * 1.3f).roundToInt(),
                        (geometry.character.y - r * 1.4f).roundToInt()
                    )
                }
                .size(hit)
                .semantics { contentDescription = "HavaCan" }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onCharacterTap
                )
        )

        AnimatedVisibility(
            visible = showGreeting,
            enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.offset {
                // Up and to the right of HavaCan's head, clear of the temperature on the left
                IntOffset(
                    (geometry.character.x + r * 0.5f).coerceAtMost(widthPx - with(density) { 196.dp.toPx() }).roundToInt(),
                    (geometry.character.y - r * 2.6f).coerceAtLeast(0f).roundToInt()
                )
            }
        ) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleSmall,
                color = Navy,
                modifier = Modifier
                    .widthIn(max = 188.dp)
                    .shadow(6.dp, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                    .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }
            )
        }
    }
}

/** Seconds since first composition, advanced every frame unless [paused]. Read it in draw blocks only. */
@Composable
fun rememberSceneClock(paused: Boolean): () -> Float {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(paused) {
        if (paused) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                time.floatValue += (now - last) / 1_000_000_000f
                last = now
            }
        }
    }
    return { time.floatValue }
}
