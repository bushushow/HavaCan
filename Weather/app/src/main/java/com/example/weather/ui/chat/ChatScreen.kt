package com.example.weather.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.weather.R
import com.example.weather.domain.voice.CardKind
import com.example.weather.domain.voice.VoiceDestination
import com.example.weather.ui.home.character.drawHavaCanFace
import com.example.weather.ui.home.components.HavaCanLogo
import com.example.weather.ui.theme.HavaBlue
import com.example.weather.ui.theme.HavaBlueLight
import com.example.weather.ui.theme.Navy
import com.example.weather.ui.theme.NavySoft
import com.example.weather.ui.theme.RiskRed
import com.example.weather.ui.theme.SkyTint
import com.example.weather.ui.voice.ChatMessage
import com.example.weather.ui.voice.VoiceGuideState
import com.example.weather.ui.voice.VoicePhase

/** Space the bottom navigation takes above the system navigation bar. */
private val BottomBarSpace = 84.dp
private val HavaCanBubble = Color(0xFFEAF1FF)

class ChatActions(
    val onSend: (String) -> Unit,
    /** Microphone: listen, stop listening, or cut HavaCan short. Permission is the caller's job. */
    val onMic: () -> Unit,
    /** Headset in the header: the voice guide session, or its settings when it's off. */
    val onHeadset: () -> Unit,
    val onBell: () -> Unit,
    val onSpeak: (ChatMessage) -> Unit,
    val onOpen: (VoiceDestination) -> Unit,
    val onDismissNotice: () -> Unit,
)

/**
 * HavaCan Asistan: one conversation for typing and talking. The microphone sits in the message
 * bar (send replaces it as soon as there's text); voice settings live in Profil, not here.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    state: VoiceGuideState,
    messages: List<ChatMessage>,
    clock: () -> Float,
    actions: ChatActions,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val list = rememberLazyListState()
    val thinking = state.phase == VoicePhase.Thinking
    // Header + hero + greeting come first in the list
    val leading = 2
    LaunchedEffect(messages.size, thinking) {
        if (messages.isNotEmpty()) list.animateScrollToItem(leading + messages.size + (if (thinking) 1 else 0) - 1)
    }

    val density = LocalDensity.current
    val ime = WindowInsets.ime.getBottom(density)
    val nav = WindowInsets.navigationBars.getBottom(density)
    val bottom = with(density) { if (ime > nav) ime.toDp() + 8.dp else nav.toDp() + BottomBarSpace }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(bottom = bottom),
    ) {
        ChatHeader(state, actions)
        LazyColumn(
            state = list,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "hero") { AssistantHero(state, clock, Modifier.fillMaxWidth().height(230.dp)) }
            item(key = "greeting") { Greeting() }
            items(messages, key = { it.id }) { message ->
                if (message.fromUser) UserBubble(message) else HavaCanBubbleRow(message, actions)
            }
            if (thinking) item(key = "thinking") { ThinkingBubble(clock) }
            item(key = "suggestions") {
                FlowRow(
                    Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val outfit = stringResource(R.string.chat_suggest_outfit)
                    val plans = stringResource(R.string.chat_suggest_plans)
                    SuggestionChip(outfit, R.drawable.ic_hanger, Modifier.weight(1f)) { actions.onSend(outfit) }
                    SuggestionChip(plans, R.drawable.ic_calendar, Modifier.weight(1f)) { actions.onSend(plans) }
                }
            }
        }
        StatusLine(state, actions.onDismissNotice)
        MessageBar(
            text = draft,
            onTextChange = { draft = it },
            state = state,
            onSend = {
                actions.onSend(draft)
                draft = ""
            },
            onQuick = actions.onSend,
            onMic = actions.onMic,
        )
    }
}

@Composable
private fun ChatHeader(state: VoiceGuideState, actions: ChatActions) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }, verticalAlignment = Alignment.Bottom) {
            HavaCanLogo()
            Text(
                " " + stringResource(R.string.chat_title_suffix),
                style = MaterialTheme.typography.headlineSmall,
                color = HavaBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val active = state.sessionActive
        val description = when {
            !state.settings.guideEnabled -> stringResource(R.string.cd_voice_settings)
            active -> stringResource(R.string.cd_voice_stop)
            else -> stringResource(R.string.cd_voice_start)
        }
        Surface(
            onClick = actions.onHeadset,
            shape = CircleShape,
            color = if (active) HavaBlue else HavaBlueLight,
            modifier = Modifier.size(52.dp).semantics { contentDescription = description },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_headset), null, tint = if (active) Color.White else HavaBlue, modifier = Modifier.size(26.dp))
            }
        }
        IconButton(onClick = actions.onBell) {
            Icon(painterResource(R.drawable.ic_bell), stringResource(R.string.cd_notifications), tint = Navy)
        }
    }
}

@Composable
private fun Greeting() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 28.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier.widthIn(max = 340.dp).semantics(mergeDescendants = true) {},
        ) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text(stringResource(R.string.chat_hello), style = MaterialTheme.typography.titleLarge, color = Navy, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(R.string.chat_hello_sub), style = MaterialTheme.typography.titleMedium, color = NavySoft)
            }
        }
    }
}

@Composable
private fun UserBubble(message: ChatMessage) {
    val description = stringResource(R.string.cd_user_message, message.time, message.title)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Surface(
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 26.dp, bottomEnd = 8.dp),
            color = HavaBlue,
            shadowElevation = 3.dp,
            modifier = Modifier.widthIn(max = 320.dp).padding(start = 48.dp).clearAndSetSemantics { contentDescription = description },
        ) {
            Column(Modifier.padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)) {
                Text(message.title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(
                    message.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun HavaCanBubbleRow(message: ChatMessage, actions: ChatActions) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Avatar()
        Surface(
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 26.dp, bottomStart = 26.dp, bottomEnd = 26.dp),
            color = HavaCanBubble,
            modifier = Modifier.weight(1f),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val text = stringResource(R.string.cd_havacan_message, listOfNotNull(message.title, message.body).joinToString(" "))
                Column(
                    Modifier.padding(horizontal = 6.dp, vertical = 4.dp).clearAndSetSemantics { contentDescription = text },
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(message.title, style = MaterialTheme.typography.titleMedium, color = Navy, fontWeight = FontWeight.ExtraBold)
                    message.body?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = NavySoft) }
                }
                val card = message.card
                if (card != null) {
                    ReplyCard(message, actions)
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { SpeakButton(message, actions) }
                }
            }
        }
    }
}

@Composable
private fun ReplyCard(message: ChatMessage, actions: ChatActions) {
    val card = message.card ?: return
    val open = card.opens
    val openLabel = stringResource(R.string.cd_open_card, card.title)
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .then(if (open != null) Modifier.clickable(onClickLabel = openLabel, role = Role.Button) { actions.onOpen(open) } else Modifier)
                .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)
                .heightIn(min = 52.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(painterResource(card.kind.icon()), null, tint = HavaBlue, modifier = Modifier.size(34.dp))
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text(card.title, style = MaterialTheme.typography.titleSmall, color = Navy, fontWeight = FontWeight.Bold)
                Text(card.detail, style = MaterialTheme.typography.bodyMedium, color = NavySoft)
            }
            SpeakButton(message, actions)
            if (open != null) Icon(painterResource(R.drawable.ic_chevron_right), null, tint = Navy, modifier = Modifier.size(28.dp))
        }
    }
}

private fun CardKind.icon(): Int = when (this) {
    CardKind.Weather -> R.drawable.ic_sun
    CardKind.Run -> R.drawable.ic_run
    CardKind.Plans -> R.drawable.ic_calendar
    CardKind.Outfit -> R.drawable.ic_hanger
    CardKind.Plan -> R.drawable.ic_check_circle
}

@Composable
private fun SpeakButton(message: ChatMessage, actions: ChatActions) {
    val description = stringResource(R.string.cd_speak_message)
    Surface(
        onClick = { actions.onSpeak(message) },
        shape = CircleShape,
        color = HavaBlueLight,
        modifier = Modifier.size(48.dp).semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_volume), null, tint = HavaBlue, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun Avatar() {
    Surface(shape = CircleShape, color = Color.White, shadowElevation = 3.dp, modifier = Modifier.size(46.dp)) {
        Canvas(Modifier.fillMaxSize().padding(4.dp)) { drawHavaCanFace(Offset(size.width / 2, size.height / 2), size.minDimension / 2.1f) }
    }
}

@Composable
private fun ThinkingBubble(clock: () -> Float) {
    val description = stringResource(R.string.voice_thinking)
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Avatar()
        Surface(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 22.dp), color = HavaCanBubble) {
            Canvas(Modifier.padding(horizontal = 20.dp, vertical = 16.dp).width(44.dp).height(12.dp)) {
                val t = clock()
                for (i in 0..2) {
                    val lift = kotlin.math.sin(t * 7f - i * 0.8f).coerceAtLeast(0f)
                    drawCircle(HavaBlue.copy(alpha = 0.45f + lift * 0.55f), radius = size.height / 2.4f, center = Offset(size.width * (0.17f + i * 0.33f), size.height / 2 - lift * 3f))
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(label: String, icon: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.5.dp, HavaBlue),
        modifier = modifier.heightIn(min = 56.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {
            Icon(painterResource(icon), null, tint = HavaBlue, modifier = Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, color = HavaBlue, fontWeight = FontWeight.Bold)
        }
    }
}

/** Only shows when there's something to say: listening/thinking/speaking, or a problem. */
@Composable
private fun StatusLine(state: VoiceGuideState, onDismissNotice: () -> Unit) {
    val notice = state.notice
    val status = when (state.phase) {
        VoicePhase.Listening -> stringResource(R.string.voice_listening)
        VoicePhase.Thinking -> stringResource(R.string.voice_thinking)
        VoicePhase.Speaking -> stringResource(R.string.voice_speaking)
        VoicePhase.Idle -> if (state.sessionActive) stringResource(R.string.voice_tap_to_talk) else null
    }
    AnimatedVisibility(visible = status != null || notice != null) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
            if (status != null) {
                val heard = state.heard.takeIf { state.phase == VoicePhase.Listening && it.isNotBlank() }
                Text(
                    listOfNotNull(status, heard?.let { "“$it”" }).joinToString("  "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = HavaBlue,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    // Never announced while the microphone is open, or TalkBack would talk into it
                    modifier = Modifier.semantics { if (state.phase == VoicePhase.Thinking) liveRegion = LiveRegionMode.Polite },
                )
            }
            if (notice != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(notice, style = MaterialTheme.typography.bodyMedium, color = RiskRed, modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite })
                    IconButton(onClick = onDismissNotice) {
                        Icon(painterResource(R.drawable.ic_clear), stringResource(R.string.close), tint = NavySoft)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBar(
    text: String,
    onTextChange: (String) -> Unit,
    state: VoiceGuideState,
    onSend: () -> Unit,
    onQuick: (String) -> Unit,
    onMic: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(36.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .shadow(10.dp, shape, ambientColor = Navy.copy(alpha = 0.1f), spotColor = Navy.copy(alpha = 0.1f))
            .background(Color.White, shape)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            val quickDescription = stringResource(R.string.cd_quick_questions)
            Surface(
                onClick = { menu = true },
                shape = CircleShape,
                color = SkyTint,
                border = BorderStroke(1.dp, HavaBlueLight),
                modifier = Modifier.size(52.dp).semantics { contentDescription = quickDescription },
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_add), null, tint = Navy) }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                QUICK_QUESTIONS.forEach { res ->
                    val question = stringResource(res)
                    DropdownMenuItem(text = { Text(question) }, onClick = { menu = false; onQuick(question) })
                }
            }
        }
        val hint = stringResource(R.string.chat_hint)
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            textStyle = MaterialTheme.typography.bodyLarge.merge(TextStyle(color = Navy)),
            cursorBrush = SolidColor(HavaBlue),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) onSend() }),
            modifier = Modifier.weight(1f).semantics { contentDescription = hint },
            decorationBox = { inner ->
                Box(
                    Modifier
                        .heightIn(min = 52.dp)
                        .background(SkyTint, RoundedCornerShape(26.dp))
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (text.isEmpty()) Text(hint, style = MaterialTheme.typography.bodyLarge, color = NavySoft.copy(alpha = 0.8f))
                    inner()
                }
            },
        )
        MicOrSendButton(hasText = text.isNotBlank(), state = state, onSend = onSend, onMic = onMic)
    }
}

@Composable
private fun MicOrSendButton(hasText: Boolean, state: VoiceGuideState, onSend: () -> Unit, onMic: () -> Unit) {
    val listening = state.phase == VoicePhase.Listening
    val pulse by animateFloatAsState(if (listening) 1f + state.level * 0.18f else 1f, label = "mic")
    val description = when {
        hasText -> stringResource(R.string.cd_send)
        listening -> stringResource(R.string.cd_stop_listening)
        state.phase == VoicePhase.Speaking -> stringResource(R.string.cd_interrupt)
        else -> stringResource(R.string.cd_talk)
    }
    val stateText = if (listening) stringResource(R.string.voice_listening) else null
    Box(
        Modifier
            .size(64.dp)
            .scale(pulse)
            .background(Brush.radialGradient(listOf(Color(0xFF3D82FF), HavaBlue)), CircleShape)
            .clickable(role = Role.Button) { if (hasText) onSend() else onMic() }
            .semantics {
                contentDescription = description
                stateText?.let { stateDescription = it }
            },
        contentAlignment = Alignment.Center,
    ) {
        val icon = when {
            hasText -> R.drawable.ic_send
            listening -> R.drawable.ic_stop
            else -> R.drawable.ic_mic
        }
        Crossfade(icon, label = "micIcon") { res ->
            Icon(painterResource(res), null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
    }
}

private val QUICK_QUESTIONS = listOf(
    R.string.quick_weather_today,
    R.string.quick_run_time,
    R.string.quick_outfit,
    R.string.quick_plans,
    R.string.quick_new_plan,
    R.string.quick_help,
)
