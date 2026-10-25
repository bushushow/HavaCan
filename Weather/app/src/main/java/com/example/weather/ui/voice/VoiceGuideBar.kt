package com.example.weather.ui.voice

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.weather.R
import com.example.weather.ui.home.character.drawHavaCanFace
import com.example.weather.ui.theme.HavaBlue
import com.example.weather.ui.theme.Navy
import com.example.weather.ui.theme.NavySoft
import com.example.weather.ui.theme.RiskRed

/**
 * The voice guide on every main screen (the chat has its own microphone). Idle it's a small
 * microphone button; during a session it grows into a bar saying what HavaCan is doing, with a
 * large stop button always in reach.
 */
@Composable
fun VoiceGuideBar(state: VoiceGuideState, onMic: () -> Unit, onStop: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = state.sessionActive || state.phase != VoicePhase.Idle,
        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.9f)) },
        modifier = modifier,
        label = "voiceBar",
    ) { active ->
        if (active) SessionBar(state, onMic, onStop) else StartButton(onMic)
    }
}

@Composable
private fun StartButton(onMic: () -> Unit) {
    val description = stringResource(R.string.cd_voice_start)
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
        Surface(
            onClick = onMic,
            shape = CircleShape,
            color = HavaBlue,
            shadowElevation = 6.dp,
            modifier = Modifier.size(56.dp).semantics { contentDescription = description },
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_mic), null, tint = Color.White, modifier = Modifier.size(28.dp)) }
        }
    }
}

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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).semantics { this.paneTitle = paneTitle },
    ) {
        Row(
            Modifier.padding(8.dp).heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (state.phase == VoicePhase.Speaking || state.phase == VoicePhase.Thinking) {
                        Canvas(Modifier.size(40.dp)) { drawHavaCanFace(Offset(size.width / 2, size.height / 2), size.minDimension / 2.1f) }
                    } else {
                        Icon(painterResource(if (listening) R.drawable.ic_stop else R.drawable.ic_mic), null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                // Status for sighted users; TalkBack gets the mic button's state and the spoken answer instead
                Text(status, style = MaterialTheme.typography.titleSmall, color = Navy, fontWeight = FontWeight.ExtraBold)
                val detail = state.notice ?: state.heard.takeIf { it.isNotBlank() }?.let { "“$it”" }
                if (detail != null) {
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.notice != null) RiskRed else NavySoft,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            val stopDescription = stringResource(R.string.cd_voice_stop)
            Surface(
                onClick = onStop,
                shape = CircleShape,
                color = RiskRed.copy(alpha = 0.1f),
                modifier = Modifier.widthIn(min = 56.dp).size(56.dp).semantics { contentDescription = stopDescription },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_clear), null, tint = RiskRed, modifier = Modifier.size(28.dp).background(Color.Transparent))
                }
            }
        }
    }
}
