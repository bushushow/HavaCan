package com.example.weather.ui.run

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import com.example.weather.ui.theme.SkyTint
import com.example.weather.ui.plans.capitalizeTr
import com.example.weather.model.WeatherCondition
import java.time.LocalDateTime
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.Typography
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.weather.R
import com.example.weather.domain.running.RunDayOutlook
import com.example.weather.domain.running.RunSlot
import com.example.weather.ui.home.components.SoftCard
import com.example.weather.ui.home.components.WeatherIcon
import com.example.weather.ui.theme.GoodGreen
import com.example.weather.ui.theme.HavaBlue
import com.example.weather.ui.theme.HavaBlueLight
import com.example.weather.ui.theme.Navy
import com.example.weather.ui.theme.NavySoft
import com.example.weather.ui.theme.Nunito
import com.example.weather.ui.theme.RiskRed
import com.example.weather.ui.theme.Sun
import com.example.weather.ui.theme.SunDeep
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val TURKISH = Locale.forLanguageTag("tr")
private val LongDate = DateTimeFormatter.ofPattern("d MMMM EEEE", TURKISH)
private val DayName = DateTimeFormatter.ofPattern("EEE", TURKISH)
private const val STRIP_DAYS = 14L

private fun LocalTime.label() = "%02d.%02d".format(hour, minute)

class RunPlannerActions(
    val onBack: () -> Unit,
    val onDay: (DayChoice) -> Unit,
    val onDate: (LocalDate) -> Unit,
    val onSlot: (RunSlot) -> Unit,
    val onStart: (LocalTime) -> Unit,
    val onRetry: () -> Unit,
    val onGoal: (RunGoal) -> Unit,
    val onDuration: (Int) -> Unit,
    val onPickCity: () -> Unit,
    val onPlaceName: (String) -> Unit,
    val onReminderOn: (Boolean) -> Unit,
    val onReminderMinutes: (Long) -> Unit,
    val onFollow: (Boolean) -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
    val onSavedShown: (RunSaved) -> Unit,
)

/**
 * Koşu Planla. Every value on the screen comes from [state]: suggested hours and scores from the
 * Open-Meteo forecast of the chosen day and place, nothing from the design mock-up.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunPlannerScreen(state: RunPlannerUiState, clock: () -> Float, reduceMotion: Boolean, actions: RunPlannerActions) {
    val arrive = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val hop = remember { Animatable(0f) }
    val cheer = remember { Animatable(0f) }
    val confetti = remember { Animatable(0f) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var showLocation by rememberSaveable { mutableStateOf(false) }

    // HavaCan runs in once when the screen opens
    LaunchedEffect(Unit) { arrive.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
    // A little hop whenever the user picks a time or a goal (not on the first frame)
    var firstPick by remember { mutableStateOf(true) }
    LaunchedEffect(state.start, state.goal) {
        if (firstPick) { firstPick = false; return@LaunchedEffect }
        if (!reduceMotion) {
            hop.snapTo(0f)
            hop.animateTo(1f, tween(480, easing = LinearEasing))
        }
    }
    val onSavedShown by rememberUpdatedState(actions.onSavedShown)
    LaunchedEffect(state.saved) {
        val saved = state.saved ?: return@LaunchedEffect
        if (!reduceMotion) {
            launch { confetti.animateTo(1f, tween(1300, easing = LinearEasing)) }
            cheer.animateTo(1f, tween(500))
            cheer.animateTo(0.6f, tween(400))
        }
        delay(2200)
        onSavedShown(saved)
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            Box(Modifier.fillMaxWidth()) {
                Canvas(Modifier.matchParentSize()) {
                    drawRunPark(clock(), arrive.value, hop.value, cheer.value)
                    drawConfetti(confetti.value, Offset(size.width * 0.73f, size.height * 0.45f))
                }
                Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp)) {
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = actions.onBack),
                            contentAlignment = Alignment.Center,
                        ) { Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back), tint = Navy) }
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.run_title), style = MaterialTheme.typography.titleLarge, color = Navy)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        stringResource(R.string.run_headline),
                        style = MaterialTheme.typography.displaySmall,
                        color = Navy,
                        modifier = Modifier.widthIn(max = 230.dp).semantics { heading() },
                    )
                    Spacer(Modifier.height(96.dp))
                    DayStrip(state, actions.onDate, onPickOther = { showDate = true })
                }
            }

            Column(
                Modifier.padding(horizontal = 16.dp).padding(top = 18.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionTitle(stringResource(R.string.run_suggested))
                SuggestedHours(state, actions, onOtherTime = { showTime = true })

                SectionTitle(stringResource(R.string.run_goal))
                GoalRow(state.goal, actions.onGoal)

                DurationRow(state, actions.onDuration)
                LocationRow(state, onClick = { showLocation = true })
                ReminderCard(state, actions)

                state.error?.let { Text(it, color = RiskRed, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp)) }
                SaveButton(state, actions.onSave)
                if (state.isEditing) {
                    TextButton(onClick = actions.onDelete, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(stringResource(R.string.run_delete), color = RiskRed)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = state.saved != null,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
        ) {
            state.saved?.let { SavedCard(it) { actions.onSavedShown(it) } }
        }
    }

    if (showDate) DatePickerFor(state, onPick = { actions.onDate(it); showDate = false }, onDismiss = { showDate = false })
    if (showTime) StartTimeDialog(state, onPick = { actions.onStart(it); showTime = false }, onDismiss = { showTime = false })
    if (showLocation) LocationDialog(state, actions, onDismiss = { showLocation = false })
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = Navy, modifier = Modifier.padding(start = 4.dp, top = 6.dp).semantics { heading() })
}

/**
 * The next two weeks as a strip of day cells, each with the forecast's weather for that day;
 * the calendar cell at the end reaches any later date.
 */
@Composable
private fun DayStrip(state: RunPlannerUiState, onDate: (LocalDate) -> Unit, onPickOther: () -> Unit) {
    val daily = (state.forecast as? ForecastState.Ready)?.forecast?.daily.orEmpty().associateBy { it.date }
    val days = (0L until STRIP_DAYS).map { state.today.plusDays(it) }
    val listState = rememberLazyListState()
    // Keep the chosen day in view, e.g. after picking one from the calendar
    LaunchedEffect(state.date) {
        val index = days.indexOf(state.date).takeIf { it >= 0 } ?: days.size
        listState.animateScrollToItem((index - 1).coerceAtLeast(0))
    }
    SoftCard {
        Column(Modifier.padding(top = 14.dp, bottom = 12.dp)) {
            Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.run_pick_date), style = MaterialTheme.typography.titleSmall, color = NavySoft, modifier = Modifier.weight(1f))
                Text(state.date.format(LongDate).capitalizeTr(), style = MaterialTheme.typography.titleSmall, color = HavaBlue)
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(days, key = { it.toString() }) { day ->
                    val top = when (day) {
                        state.today -> stringResource(R.string.today)
                        state.today.plusDays(1) -> stringResource(R.string.tomorrow)
                        else -> day.format(DayName).capitalizeTr()
                    }
                    DayCell(top, day.dayOfMonth.toString(), daily[day.toString()]?.condition, day == state.date) { onDate(day) }
                }
                item(key = "other") {
                    val beyond = state.date.isAfter(days.last())
                    DayCell(
                        top = if (beyond) state.date.format(DayName).capitalizeTr() else stringResource(R.string.run_other_day),
                        number = if (beyond) state.date.dayOfMonth.toString() else null,
                        condition = null,
                        selected = beyond,
                        icon = R.drawable.ic_calendar,
                        onClick = onPickOther,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    top: String,
    number: String?,
    condition: WeatherCondition?,
    selected: Boolean,
    icon: Int? = null,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(if (selected) 1f else 0.96f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium), label = "dayScale")
    val textColor by animateColorAsState(if (selected) Color.White else Navy, tween(200), label = "dayText")
    Column(
        Modifier
            .width(66.dp)
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Brush.verticalGradient(listOf(Color(0xFF3D82FF), HavaBlue)) else SolidColor(SkyTint))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(top, style = MaterialTheme.typography.labelLarge, color = if (selected) Color.White.copy(alpha = 0.9f) else NavySoft, maxLines = 1)
        if (number != null) Text(number, style = MaterialTheme.typography.titleLarge, color = textColor)
        when {
            icon != null -> Icon(painterResource(icon), null, tint = if (selected) Color.White else HavaBlue, modifier = Modifier.padding(top = 2.dp).size(26.dp))
            condition != null -> WeatherIcon(condition, isDay = true, size = 26.dp)
            else -> Spacer(Modifier.height(26.dp))
        }
    }
}

/** A rounded choice; the selected one turns blue with a soft springy pop. */
@Composable
private fun Pill(
    text: String?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    icon: Int? = null,
    description: String? = null,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(if (selected) HavaBlue else Color.White, tween(220), label = "pill")
    val content by animateColorAsState(if (selected) Color.White else Navy, tween(220), label = "pillText")
    val scale by animateFloatAsState(if (selected) 1f else 0.97f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium), label = "pillScale")
    Row(
        modifier
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(1.dp, if (selected) Color.Transparent else Navy.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { if (description != null) contentDescription = description }
            .heightIn(min = 52.dp)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(painterResource(icon), null, tint = if (selected) Color.White else HavaBlue, modifier = Modifier.size(24.dp))
        if (icon != null && text != null) Spacer(Modifier.width(6.dp))
        if (text != null) Text(text, style = MaterialTheme.typography.titleMedium, color = content, maxLines = 1)
    }
}

@Composable
private fun SuggestedHours(state: RunPlannerUiState, actions: RunPlannerActions, onOtherTime: () -> Unit) {
    val forecast = state.forecast
    when (forecast) {
        ForecastState.Loading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(26.dp), color = HavaBlue, strokeWidth = 2.5.dp)
        }
        ForecastState.Failed -> NoticeCard(
            title = stringResource(R.string.run_forecast_failed_title),
            body = stringResource(R.string.run_forecast_failed_body),
            icon = R.drawable.ic_cloud_off,
            action = stringResource(R.string.run_retry) to actions.onRetry,
        )
        is ForecastState.Ready -> when (val outlook = state.outlook) {
            is RunDayOutlook.NoForecastYet -> NoticeCard(
                title = stringResource(R.string.run_no_forecast_title),
                body = stringResource(R.string.run_no_forecast_body, outlook.daysUntilAvailable.toInt()),
                icon = R.drawable.ic_schedule,
            )
            RunDayOutlook.NoHoursLeft -> NoticeCard(
                title = stringResource(R.string.run_no_hours_left),
                body = stringResource(R.string.run_try_tomorrow),
                icon = R.drawable.ic_schedule,
                action = stringResource(R.string.tomorrow) to { actions.onDay(DayChoice.Tomorrow) },
            )
            is RunDayOutlook.Ready -> if (outlook.suggestions.isEmpty()) {
                NoticeCard(
                    title = stringResource(R.string.run_no_good_title),
                    body = outlook.blocker?.message ?: stringResource(R.string.run_sub_manual),
                    icon = R.drawable.ic_warning,
                )
            }
            null -> Unit
        }
    }

    val rows = buildList {
        addAll(state.suggestions.map { it to false })
        // A time the user picked themselves joins the list, scored the same way when we can
        val manual = state.selectedSlot
        if (state.manualStart && manual != null) add(manual to true)
    }
    if (rows.isNotEmpty()) {
        SoftCard {
            Column(Modifier.padding(6.dp)) {
                rows.forEach { (slot, manual) ->
                    SlotRow(slot, state.durationMinutes, manual, selected = slot.start.toLocalTime() == state.start) { actions.onSlot(slot) }
                }
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onOtherTime) {
            Icon(painterResource(R.drawable.ic_schedule), null, tint = HavaBlue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.run_other_time), color = HavaBlue, style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.weight(1f))
        val start = state.start
        if (start != null && state.manualStart && state.selectedSlot == null) {
            // Picked by hand with no forecast for it: say so instead of inventing weather
            Text(
                stringResource(R.string.run_your_time, start.label()),
                style = MaterialTheme.typography.labelLarge,
                color = HavaBlue,
                modifier = Modifier.clip(CircleShape).background(HavaBlueLight).padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
    if (forecast is ForecastState.Ready && rows.isNotEmpty()) {
        Text(
            stringResource(R.string.run_source, forecast.forecast.current.time.substringAfter('T').replace(':', '.')),
            style = MaterialTheme.typography.labelSmall,
            color = NavySoft.copy(alpha = 0.75f),
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** One start time: when, the sky, and the weather facts in a line, selectable like a radio row. */
@Composable
private fun SlotRow(slot: RunSlot, durationMinutes: Int, manual: Boolean, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(if (selected) HavaBlueLight else Color.Transparent, tween(200), label = "slotBg")
    val scale by animateFloatAsState(if (selected) 1f else 0.985f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium), label = "slotScale")
    val hours = slot.hours
    val first = slot.first
    val start = slot.start.toLocalTime()
    Row(
        Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(1.5.dp, if (selected) HavaBlue else Color.Transparent, RoundedCornerShape(18.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(76.dp)) {
            Text(start.label(), style = MaterialTheme.typography.titleLarge, color = Navy)
            Text(
                if (manual) stringResource(R.string.run_your_pick) else "–" + start.plusMinutes(durationMinutes.toLong()).label(),
                style = MaterialTheme.typography.labelMedium,
                color = NavySoft,
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
            val blocker = slot.blocker
            if (blocker != null) {
                Text(blocker.message, style = MaterialTheme.typography.bodyMedium, color = RiskRed)
            } else {
                Text(
                    "Hissedilen ${hours.map { it.apparentTemperature }.average().roundToInt()}° · " +
                        (hours.mapNotNull { it.windSpeed }.maxOrNull()?.let { "Rüzgar ${it.roundToInt()} km/sa" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = NavySoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        hours.mapNotNull { it.precipitationProbability }.maxOrNull()?.let { "Yağış %$it" },
                        hours.mapNotNull { it.uvIndex }.maxOrNull()?.let { "UV ${it.roundToInt()}" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = NavySoft,
                    maxLines = 1,
                )
            }
        }
        WeatherIcon(first.condition, first.isDay, size = 34.dp)
        Spacer(Modifier.width(6.dp))
        Text("${first.temperature.roundToInt()}°", style = MaterialTheme.typography.titleLarge, color = Navy, modifier = Modifier.width(44.dp))
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(if (selected) HavaBlue else Color.Transparent)
                .border(1.5.dp, if (selected) HavaBlue else NavySoft.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(painterResource(R.drawable.ic_check), null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun NoticeCard(title: String, body: String, icon: Int, action: Pair<String, () -> Unit>? = null) {
    SoftCard {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(HavaBlueLight), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), null, tint = HavaBlue)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Navy)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = NavySoft)
                if (action != null) {
                    TextButton(onClick = action.second, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Text(action.first, color = HavaBlue, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalRow(goal: RunGoal, onGoal: (RunGoal) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RunGoal.entries.forEach { option ->
            val text = option.distanceKm?.let { stringResource(R.string.run_goal_km, it) } ?: stringResource(R.string.run_goal_free)
            Pill(text, option == goal, Modifier.weight(1f)) { onGoal(option) }
        }
    }
}

@Composable
private fun DurationRow(state: RunPlannerUiState, onDuration: (Int) -> Unit) {
    SoftCard {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_schedule), null, tint = HavaBlue, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.run_duration), style = MaterialTheme.typography.bodyMedium, color = NavySoft)
                Text(stringResource(R.string.run_minutes, state.durationMinutes), style = MaterialTheme.typography.titleMedium, color = Navy)
                state.start?.let { s ->
                    Text("${s.label()}–${state.end?.label()}", style = MaterialTheme.typography.bodySmall, color = NavySoft)
                }
            }
            // Scroll the wheels right here; no extra tap needed
            DurationWheels(
                minutes = state.durationMinutes.toLong(),
                maxHours = RunPlannerViewModel.DURATION_RANGE.last / 60,
                visibleCount = 3,
                onMinutes = { onDuration(it.toInt()) },
            )
        }
    }
}

@Composable
private fun LocationRow(state: RunPlannerUiState, onClick: () -> Unit) {
    val city = state.city
    val place = listOfNotNull(state.placeName.trim().ifBlank { null }, city.name, city.admin1?.takeIf { it != city.name && state.placeName.isBlank() })
    SoftCard {
        SettingRow(R.drawable.ic_place, stringResource(R.string.run_location), place.joinToString(", "), 0f, onClick)
    }
}

@Composable
private fun SettingRow(icon: Int, label: String, value: String, chevronRotation: Float, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, tint = HavaBlue, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = NavySoft)
            Text(value, style = MaterialTheme.typography.titleMedium, color = Navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(painterResource(R.drawable.ic_chevron_right), null, tint = Navy, modifier = Modifier.rotate(chevronRotation))
    }
}

@Composable
private fun ReminderCard(state: RunPlannerUiState, actions: RunPlannerActions) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val at = state.reminderAt
    SoftCard {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_bell), null, tint = HavaBlue, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(16.dp))
                // Tapping opens the "how long before" wheels; the switch turns the reminder on or off
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) { picking = true }
                        .padding(vertical = 4.dp),
                ) {
                    if (state.reminderOn) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.run_reminder_on, RunPlannerViewModel.beforeLabel(state.reminderMinutes)),
                                style = MaterialTheme.typography.titleMedium,
                                color = Navy,
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.run_reminder_change), tint = HavaBlue, modifier = Modifier.size(16.dp))
                        }
                        if (at != null) {
                            Text(
                                stringResource(R.string.run_reminder_at, (if (at.toLocalDate() != state.date) stringResource(R.string.run_reminder_day_before) + " " else "") + at.toLocalTime().label()),
                                style = MaterialTheme.typography.bodySmall,
                                color = NavySoft,
                            )
                        }
                    } else {
                        Text(stringResource(R.string.run_reminder_off), style = MaterialTheme.typography.titleMedium, color = Navy)
                    }
                }
                Switch(checked = state.reminderOn, onCheckedChange = actions.onReminderOn)
            }
            // Weather watch: HavaCan re-checks the forecast and warns when the plan turns risky
            Row(
                Modifier
                    .padding(start = 42.dp, top = 6.dp)
                    .clip(CircleShape)
                    .toggleable(state.follow, role = Role.Checkbox, onValueChange = actions.onFollow)
                    .heightIn(min = 40.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val dot by animateColorAsState(if (state.follow) HavaBlue else NavySoft.copy(alpha = 0.35f), label = "followDot")
                Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(if (state.follow) R.string.run_follow_on else R.string.run_follow_off),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.follow) NavySoft else NavySoft.copy(alpha = 0.7f),
                )
            }
        }
    }
    if (picking) {
        val start = state.start?.let { LocalDateTime.of(state.date, it) }
        val passed = stringResource(R.string.run_reminder_passed)
        val tooShort = stringResource(R.string.run_reminder_too_short)
        val atLabel = stringResource(R.string.run_reminder_at_note)
        WheelDurationDialog(
            title = stringResource(R.string.run_reminder_pick),
            initialMinutes = state.reminderMinutes,
            onPick = { actions.onReminderMinutes(it); picking = false },
            onDismiss = { picking = false },
            note = { minutes -> start?.minusMinutes(minutes)?.let { atLabel.format(it.toLocalTime().label()) } },
            validate = { minutes ->
                when {
                    minutes < 5 -> tooShort
                    start != null && start.minusMinutes(minutes).isBefore(LocalDateTime.now()) -> passed
                    else -> null
                }
            },
        )
    }
}

@Composable
private fun SaveButton(state: RunPlannerUiState, onSave: () -> Unit) {
    val busy = state.saving || state.saved != null
    Button(
        onClick = onSave,
        enabled = !busy,
        colors = ButtonDefaults.buttonColors(containerColor = HavaBlue, disabledContainerColor = HavaBlue.copy(alpha = 0.7f), disabledContentColor = Color.White),
        shape = RoundedCornerShape(30.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
    ) {
        if (state.saving) {
            CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.5.dp)
        } else {
            Icon(painterResource(R.drawable.ic_run), null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(if (state.isEditing) R.string.run_update else R.string.run_save),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun SavedCard(saved: RunSaved, onGo: () -> Unit) {
    val reminderText = when (saved.reminder) {
        ReminderResult.Scheduled -> stringResource(R.string.run_reminder_scheduled, RunPlannerViewModel.beforeLabel(saved.plan.reminderMinutesBefore ?: 0))
        ReminderResult.Off -> stringResource(R.string.run_reminder_off_msg)
        ReminderResult.TooLate -> stringResource(R.string.run_reminder_too_late)
        ReminderResult.NotificationsBlocked -> stringResource(R.string.run_reminder_blocked)
    }
    SoftCard(background = Brush.linearGradient(listOf(Color(0xFFEAF2FF), Color.White))) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Sun), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_check), null, tint = Navy)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.run_saved_title), style = MaterialTheme.typography.titleLarge, color = Navy)
                Text(reminderText, style = MaterialTheme.typography.bodyMedium, color = if (saved.reminder == ReminderResult.NotificationsBlocked) RiskRed else NavySoft)
                if (saved.followBlocked && saved.reminder != ReminderResult.NotificationsBlocked) {
                    Text(stringResource(R.string.run_reminder_blocked), style = MaterialTheme.typography.bodyMedium, color = RiskRed)
                }
            }
            OutlinedButton(onClick = onGo, border = BorderStroke(1.5.dp, HavaBlue)) {
                Text(stringResource(R.string.plans_title), color = HavaBlue)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerFor(state: RunPlannerUiState, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val today = state.today
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(today)
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } ?: onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) { DatePicker(pickerState, title = null) }
}

@Composable
private fun LocationDialog(state: RunPlannerUiState, actions: RunPlannerActions, onDismiss: () -> Unit) {
    var place by rememberSaveable { mutableStateOf(state.placeName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.run_location_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = place,
                    onValueChange = { place = it.take(60) },
                    placeholder = { Text(stringResource(R.string.run_place_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                OutlinedButton(
                    onClick = {
                        actions.onPlaceName(place)
                        onDismiss()
                        actions.onPickCity()
                    },
                    border = BorderStroke(1.5.dp, HavaBlue),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(painterResource(R.drawable.ic_place), null, tint = HavaBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.run_change_city) + " · " + state.city.name,
                        color = HavaBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(stringResource(R.string.run_location_note), style = MaterialTheme.typography.bodySmall, color = NavySoft)
            }
        },
        confirmButton = { TextButton(onClick = { actions.onPlaceName(place); onDismiss() }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun StartTimeDialog(state: RunPlannerUiState, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val passed = stringResource(R.string.run_start_passed)
    WheelTimeDialog(
        title = stringResource(R.string.run_start_pick),
        initial = state.start ?: LocalTime.of(7, 0),
        onPick = onPick,
        onDismiss = onDismiss,
        note = { time -> "${time.label()}–${time.plusMinutes(state.durationMinutes.toLong()).label()}" },
        validate = { time -> if (LocalDateTime.of(state.date, time).isBefore(LocalDateTime.now())) passed else null },
    )
}
