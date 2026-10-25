package com.example.weather.ui.plans

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.weather.R
import com.example.weather.domain.plans.PlanOutlook
import com.example.weather.model.HourlyWeather
import com.example.weather.model.PlanSource
import com.example.weather.ui.home.character.HavaCanArt
import com.example.weather.ui.home.character.HavaCanPoseStyle
import com.example.weather.ui.home.components.HavaCanLogo
import com.example.weather.ui.home.components.SoftCard
import com.example.weather.ui.home.components.WeatherIcon
import com.example.weather.ui.theme.GoodGreen
import com.example.weather.ui.theme.HavaBlue
import com.example.weather.ui.theme.HavaBlueLight
import com.example.weather.ui.theme.Navy
import com.example.weather.ui.theme.NavySoft
import com.example.weather.ui.theme.SkyTint
import com.example.weather.ui.theme.Sun
import com.example.weather.ui.theme.SunDeep
import com.example.weather.ui.theme.SunLight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TURKISH = Locale.forLanguageTag("tr")
private val DayHeader = DateTimeFormatter.ofPattern("d MMMM EEEE", TURKISH)

@Composable
fun PlansScreen(
    state: PlansUiState,
    clock: () -> Float,
    onCityClick: () -> Unit,
    onBellClick: () -> Unit,
    onConnectCalendar: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onOpenPlan: (PlanItem) -> Unit,
    onPlanB: (PlanItem) -> Unit,
    onAddPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            HavaCanLogo(Modifier.padding(start = 4.dp))
            Spacer(Modifier.weight(1f))
            CityChip(state.homeCity.name, onCityClick)
            IconButton(onClick = onBellClick) {
                Icon(painterResource(R.drawable.ic_bell), stringResource(R.string.cd_notifications), tint = Navy)
            }
        }
        Column(Modifier.padding(start = 4.dp)) {
            Text(state.today.format(DayHeader).capitalizeTr(), style = MaterialTheme.typography.bodyLarge, color = NavySoft)
            Text(
                stringResource(R.string.plans_title),
                style = MaterialTheme.typography.displaySmall,
                color = Navy,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (!state.calendarConnected) CalendarCard(onConnectCalendar) else CalendarConnectedRow()
        WeekStrip(state, onSelectDate)
        state.suggestion?.let { SuggestionCard(it, clock) { onPlanB(it) } }

        val selected = state.selectedDate
        Text(
            if (selected == null) stringResource(R.string.upcoming_plans) else stringResource(R.string.plans_for_day, dayLabel(selected, state.today)),
            style = MaterialTheme.typography.titleLarge,
            color = Navy,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp).semantics { heading() },
        )
        when {
            !state.hasAnyPlans -> EmptyPlans(clock)
            state.items.isEmpty() -> Text(stringResource(R.string.no_plans_on_day), style = MaterialTheme.typography.bodyLarge, color = NavySoft, modifier = Modifier.padding(start = 4.dp))
            else -> Timeline(state.items, state.today, onOpenPlan, onPlanB)
        }
        OutlinedButton(
            onClick = onAddPlan,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            border = BorderStroke(1.5.dp, HavaBlue),
        ) {
            Icon(painterResource(R.drawable.ic_add), null, tint = HavaBlue)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.add_plan), style = MaterialTheme.typography.titleSmall, color = HavaBlue)
        }
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun CityChip(name: String, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(Navy.copy(alpha = 0.05f))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 40.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_place), null, tint = HavaBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(name, style = MaterialTheme.typography.labelLarge, color = Navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(painterResource(R.drawable.ic_expand_more), null, tint = Navy, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun CalendarCard(onConnect: () -> Unit) {
    SoftCard {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(HavaBlueLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_calendar), null, tint = HavaBlue)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.calendar_connect_title), style = MaterialTheme.typography.titleMedium, color = Navy)
                Text(stringResource(R.string.calendar_connect_body), style = MaterialTheme.typography.bodySmall, color = NavySoft)
            }
            OutlinedButton(onClick = onConnect, border = BorderStroke(1.5.dp, HavaBlue)) {
                Text(stringResource(R.string.calendar_connect), color = HavaBlue)
            }
        }
    }
}

@Composable
private fun CalendarConnectedRow() {
    Row(Modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_check_circle), null, tint = GoodGreen, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.calendar_connected), style = MaterialTheme.typography.bodyMedium, color = NavySoft)
    }
}

@Composable
private fun WeekStrip(state: PlansUiState, onSelect: (LocalDate) -> Unit) {
    SoftCard {
        Row(Modifier.fillMaxWidth().padding(8.dp)) {
            state.days.forEach { day ->
                val selected = day.date == state.selectedDate || (state.selectedDate == null && day.isToday)
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) Brush.verticalGradient(listOf(Color(0xFF3D82FF), Color(0xFF0F47C2))) else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)))
                        .selectable(selected = day.date == state.selectedDate, role = Role.Tab) { onSelect(day.date) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val color = if (selected) Color.White else Navy
                    Text(day.dayName, style = MaterialTheme.typography.labelMedium, color = if (selected) Color.White else NavySoft)
                    Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, color = color)
                    if (day.condition != null) {
                        WeatherIcon(day.condition, isDay = true, size = 26.dp)
                    } else {
                        Spacer(Modifier.height(26.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(item: PlanItem, clock: () -> Float, onPlanB: () -> Unit) {
    val alternative = item.alternative ?: return
    val risk = item.outlook as? PlanOutlook.Risk ?: return
    val earlier = alternative.hour < item.plan.localTime.hour
    SoftCard(background = Brush.linearGradient(listOf(Color(0xFFE8F0FF), Color(0xFFF7FAFF)))) {
        Box {
            HavaCanArt(
                HavaCanPoseStyle.Cheer,
                clock,
                Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp).size(116.dp),
            )
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.suggestion_label), style = MaterialTheme.typography.titleSmall, color = HavaBlue)
                Text(
                    stringResource(if (earlier) R.string.suggestion_earlier else R.string.suggestion_later),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Navy,
                    modifier = Modifier.padding(end = 110.dp),
                )
                Text(
                    stringResource(R.string.suggestion_reason, item.plan.title, item.plan.timeLabel, risk.reason.label.lowercase(TURKISH)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = NavySoft,
                    modifier = Modifier.padding(end = 96.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeTile(item.plan.timeLabel, risk.hour, good = false)
                    Icon(painterResource(R.drawable.ic_arrow_forward), null, tint = NavySoft)
                    TimeTile(alternative.hourLabel, alternative, good = true)
                    Spacer(Modifier.weight(1f))
                }
                Button(onClick = onPlanB, colors = ButtonDefaults.buttonColors(containerColor = HavaBlue), modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.see_plan_b), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(4.dp))
                    Icon(painterResource(R.drawable.ic_chevron_right), null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun TimeTile(label: String, hour: HourlyWeather, good: Boolean) {
    Column(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (good) Color(0xFFEAF7E4) else Color(0xFFFDE8E8))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = Navy)
        WeatherIcon(hour.condition, hour.isDay, size = 34.dp)
    }
}

@Composable
private fun Timeline(items: List<PlanItem>, today: LocalDate, onOpen: (PlanItem) -> Unit, onPlanB: (PlanItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        var lastDate: LocalDate? = null
        items.forEach { item ->
            val date = item.plan.localDate
            if (date != lastDate && date != today) {
                Text(dayLabel(date, today), style = MaterialTheme.typography.titleSmall, color = NavySoft, modifier = Modifier.padding(start = 6.dp, top = 4.dp))
            }
            lastDate = date
            PlanRow(item, onClick = { if (item.outlook is PlanOutlook.Risk) onPlanB(item) else onOpen(item) })
        }
    }
}

@Composable
private fun PlanRow(item: PlanItem, onClick: () -> Unit) {
    val dot = when (item.outlook) {
        is PlanOutlook.Risk -> Sun
        is PlanOutlook.Fine -> HavaBlue
        else -> NavySoft.copy(alpha = 0.4f)
    }
    SoftCard {
        Row(
            Modifier
                .clickable(role = Role.Button, onClick = onClick)
                .height(IntrinsicSize.Min)
                .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp)
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.fillMaxHeight().width(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(14.dp).border(3.dp, dot.copy(alpha = 0.3f), CircleShape).padding(3.dp).background(dot, CircleShape))
                Box(Modifier.width(2.dp).weight(1f).background(dot.copy(alpha = 0.25f)))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val run = item.plan.run
                val end = item.plan.endDateTime
                Text(
                    if (end != null) "${item.plan.timeLabel}–%02d.%02d".format(end.hour, end.minute) else item.plan.timeLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Navy,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (run != null) {
                        Icon(painterResource(R.drawable.ic_run), stringResource(R.string.run_plan_type), tint = HavaBlue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(item.plan.title, style = MaterialTheme.typography.titleMedium, color = Navy)
                }
                if (run != null) {
                    Text(
                        listOfNotNull(run.distanceKm?.let { "$it km" }, "${run.durationMinutes} dk").joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NavySoft,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_place), null, tint = NavySoft, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        listOfNotNull(item.plan.placeName, item.plan.city.name).joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NavySoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlookChip(item)
                    if (item.plan.source == PlanSource.Calendar) Chip(stringResource(R.string.chip_calendar), HavaBlueLight, HavaBlue, null)
                    if (item.plan.isRun) {
                        Chip(stringResource(if (item.plan.follow) R.string.chip_follow_on else R.string.chip_follow_off), HavaBlueLight, if (item.plan.follow) HavaBlue else NavySoft, null)
                    }
                }
            }
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = Navy)
        }
    }
}

@Composable
fun OutlookChip(item: PlanItem) {
    when (val o = item.outlook) {
        is PlanOutlook.Risk -> Chip(o.reason.label, SunLight, Color(0xFF7A5200), R.drawable.ic_warning, SunDeep)
        is PlanOutlook.Fine -> if (item.plan.type == com.example.weather.model.PlanType.Indoor) {
            Chip(stringResource(R.string.chip_indoor), SkyTint, NavySoft, R.drawable.ic_home)
        } else {
            Chip(stringResource(R.string.chip_fine), Color(0xFFEAF7E4), Color(0xFF2E7D32), R.drawable.ic_check_circle, GoodGreen)
        }
        is PlanOutlook.NoForecastYet -> Chip(stringResource(R.string.chip_waiting), SkyTint, NavySoft, R.drawable.ic_schedule)
        PlanOutlook.Unknown -> Chip(stringResource(R.string.chip_unknown), SkyTint, NavySoft, R.drawable.ic_cloud_off)
        PlanOutlook.Past -> Unit
    }
}

@Composable
private fun Chip(text: String, background: Color, content: Color, icon: Int?, iconTint: Color = content) {
    Row(
        Modifier.clip(CircleShape).background(background).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painterResource(icon), null, tint = iconTint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

@Composable
private fun EmptyPlans(clock: () -> Float) {
    SoftCard {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.no_plans_title), style = MaterialTheme.typography.titleLarge, color = Navy)
                Text(stringResource(R.string.no_plans_body), style = MaterialTheme.typography.bodyLarge, color = NavySoft)
            }
            HavaCanArt(HavaCanPoseStyle.Cheer, clock, Modifier.size(90.dp))
        }
    }
}

@Composable
fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.today)
    today.plusDays(1) -> stringResource(R.string.tomorrow)
    else -> date.format(DayHeader).capitalizeTr()
}

fun String.capitalizeTr() = replaceFirstChar { it.titlecase(TURKISH) }
