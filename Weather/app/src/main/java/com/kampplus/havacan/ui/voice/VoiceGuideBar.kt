package com.kampplus.havacan.ui.voice

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.kampplus.havacan.R
import com.kampplus.havacan.ui.home.character.drawHavaCanFace
import com.kampplus.havacan.ui.home.rememberReduceMotion
import com.kampplus.havacan.ui.theme.Hava
import com.kampplus.havacan.ui.theme.HavaBlue
import com.kampplus.havacan.ui.theme.Navy
import com.kampplus.havacan.ui.theme.NavySoft
import com.kampplus.havacan.ui.theme.RiskRed
import kotlinx.coroutines.launch

/**
 * The voice guide on every main screen (the chat has its own microphone). Idle it's a small
 * microphone button; during a session it grows into a bar saying what HavaCan is doing, with a
 * large stop button always in reach.
 */
@Composable
fun VoiceGuideBar(state: VoiceGuideState, onMic: () -> Unit, onStop: () -> Unit, modifier: Modifier = Modifier) {
    // Where the user left the floating button; it stays there while switching screens
    var saved by rememberSaveable(stateSaver = OffsetSaver) { mutableStateOf(Offset.Zero) }
    val position = remember { Animatable(saved, Offset.VectorConverter) }
    AnimatedContent(
        targetState = state.sessionActive || state.phase != VoicePhase.Idle,
        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.9f)) },
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter,
        label = "voiceBar"
    ) { active ->
        if (active) SessionBar(state, onMic, onStop) else StartButton(onMic, position, onSettled = { saved = it })
    }
}

private val ButtonSize = 58.dp

/**
 * The idle microphone. It can be dragged anywhere above the bottom bar and thrown; it then
 * glides to the nearest side so it never covers content in the middle of the screen.
 */
@Composable
private fun StartButton(onMic: () -> Unit, position: Animatable<Offset, AnimationVector2D>, onSettled: (Offset) -> Unit) {
    val description = stringResource(R.string.cd_voice_start)
    val reduceMotion = rememberReduceMotion()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val screenHeight = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val travel = with(density) { (screenHeight * 0.62f).toDp() }
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(travel + ButtonSize + 14.dp).padding(horizontal = 16.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        val maxX = with(density) { (maxWidth - ButtonSize).toPx() }
        // Up to about two thirds of the screen above its home spot, never under the status bar
        val minY = -with(density) { travel.toPx() }
        fun clamp(o: Offset) = Offset(o.x.coerceIn(0f, maxX), o.y.coerceIn(minY, 0f))
        Box(
            Modifier
                .offset { position.value.round() }
                .pointerInput(maxX) {
                    val tracker = VelocityTracker()
                    var travelled = Offset.Zero
                    detectDragGestures(
                        onDragStart = {
                            tracker.resetTracking()
                            travelled = Offset.Zero
                        },
                        onDragEnd = {
                            val v = tracker.calculateVelocity()
                            val here = position.value
                            // A throw carries it on; it then settles at the side it's heading to
                            val aimX = here.x + v.x * 0.25f
                            val target = clamp(Offset(if (aimX > maxX / 2) maxX else 0f, here.y + v.y * 0.15f))
                            scope.launch {
                                if (reduceMotion) {
                                    position.snapTo(target)
                                } else {
                                    position.animateTo(
                                        target,
                                        spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow),
                                        initialVelocity = Offset(v.x, v.y)
                                    )
                                }
                                onSettled(position.value)
                            }
                        }
                    ) { change, drag ->
                        change.consume()
                        travelled += drag
                        tracker.addPosition(change.uptimeMillis, travelled)
                        scope.launch { position.snapTo(clamp(position.value + drag)) }
                    }
                }
                .size(ButtonSize + 14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!reduceMotion) {
                val halo = rememberInfiniteTransition(label = "halo")
                val t by halo.animateFloat(0f, 1f, infiniteRepeatable(tween(2000), RepeatMode.Restart), label = "haloT")
                Box(
                    Modifier
                        .size(ButtonSize)
                        .scale(1f + t * 0.3f)
                        .alpha((1f - t) * 0.35f)
                        .background(HavaBlue, CircleShape)
                )
            }
            Surface(
                onClick = onMic,
                shape = CircleShape,
                color = Color.Transparent,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(ButtonSize)
                    .border(2.5.dp, Color.White, CircleShape)
                    .semantics { contentDescription = description }
            ) {
                Box(
                    Modifier.background(Brush.verticalGradient(listOf(Color(0xFF5B9BFF), HavaBlue, Color(0xFF0F47C2)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.ic_mic), null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

private val OffsetSaver = androidx.compose.runtime.saveable.Saver<Offset, List<Float>>(
    save = { listOf(it.x, it.y) },
    restore = { Offset(it[0], it[1]) }
)

@Composable
private fun SessionBar(state: VoiceGuideState, onMic: () -> Unit, onStop: () -> Unit) {
    val listening = state.phase == VoicePhase.Listening
    val status = when (state.phase) {
        VoicePhase.Listening -> stringResource(R.string.voice_listening)
        VoicePhase.Thinking -> stringResource(R.string.voice_thinking)
        VoicePhase.Speaking -> stringResource(R.string.voice_speaking)
        VoicePhase.Idle -> stringResource(R.string.voice_tap_to_talk)
    }
    val paneTitle = stringResource(R.string.voice_guide_title)
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = Color.White,
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).semantics { this.paneTitle = paneTitle }
    ) {
        Row(
            Modifier.padding(8.dp).heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val pulse by animateFloatAsState(if (listening) 1f + state.level * 0.2f else 1f, label = "pulse")
            val micDescription = when (state.phase) {
                VoicePhase.Listening -> stringResource(R.string.cd_stop_listening)
                VoicePhase.Speaking -> stringResource(R.string.cd_interrupt)
                else -> stringResource(R.string.cd_talk)
            }
            Surface(
                onClick = onMic,
                shape = CircleShape,
                color = HavaBlue,
                modifier = Modifier.size(56.dp).scale(pulse).semantics {
                    contentDescription = micDescription
                    stateDescription = status
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (state.phase == VoicePhase.Speaking || state.phase == VoicePhase.Thinking) {
                        Canvas(Modifier.size(40.dp)) { drawHavaCanFace(Offset(size.width / 2, size.height / 2), size.minDimension / 2.1f) }
                    } else {
                        Icon(
                            painterResource(if (listening) R.drawable.ic_stop else R.drawable.ic_mic),
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                // Status for sighted users; TalkBack gets the mic button's state and the spoken answer instead
                Text(status, style = MaterialTheme.typography.titleSmall, color = Hava.ink, fontWeight = FontWeight.ExtraBold)
                val detail = state.notice ?: state.heard.takeIf { it.isNotBlank() }?.let { "“$it”" }
                if (detail != null) {
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.notice != null) RiskRed else Hava.inkSoft,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            val stopDescription = stringResource(R.string.cd_voice_stop)
            Surface(
                onClick = onStop,
                shape = CircleShape,
                color = RiskRed.copy(alpha = 0.1f),
                modifier = Modifier.widthIn(min = 56.dp).size(56.dp).semantics { contentDescription = stopDescription }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painterResource(R.drawable.ic_clear),
                        null,
                        tint = RiskRed,
                        modifier = Modifier.size(28.dp).background(Color.Transparent)
                    )
                }
            }
        }
    }
}
