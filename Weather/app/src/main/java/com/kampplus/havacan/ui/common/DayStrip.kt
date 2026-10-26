package com.kampplus.havacan.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kampplus.havacan.R
import com.kampplus.havacan.model.WeatherCondition
import com.kampplus.havacan.ui.home.components.SoftCard
import com.kampplus.havacan.ui.home.components.WeatherIcon
import com.kampplus.havacan.ui.plans.capitalizeTr
import com.kampplus.havacan.ui.theme.HavaBlue
import com.kampplus.havacan.ui.theme.Navy
import com.kampplus.havacan.ui.theme.NavySoft
import com.kampplus.havacan.ui.theme.SkyTint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TURKISH = Locale.forLanguageTag("tr")
private val LongDate = DateTimeFormatter.ofPattern("d MMMM EEEE", TURKISH)
private val DayName = DateTimeFormatter.ofPattern("EEE", TURKISH)

/**
 * The next [days] days as a strip of day cells, each with the forecast's weather for that day
 * ([conditions] keyed by "2026-10-27"); the calendar cell at the end reaches any later date.
 * Used by Koşu Planla and Weather Meet.
 */
@Composable
fun DayStrip(
    title: String,
    today: LocalDate,
    selected: LocalDate,
    conditions: Map<String, WeatherCondition>,
    onDate: (LocalDate) -> Unit,
    onPickOther: () -> Unit,
    days: Long = 14
) {
    val dates = (0L until days).map { today.plusDays(it) }
    val listState = rememberLazyListState()
    // Keep the chosen day in view, e.g. after picking one from the calendar
    LaunchedEffect(selected) {
        val index = dates.indexOf(selected).takeIf { it >= 0 } ?: dates.size
        listState.animateScrollToItem((index - 1).coerceAtLeast(0))
    }
    SoftCard {
        Column(Modifier.padding(top = 14.dp, bottom = 12.dp)) {
            Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = NavySoft, modifier = Modifier.weight(1f))
                Text(selected.format(LongDate).capitalizeTr(), style = MaterialTheme.typography.titleSmall, color = HavaBlue)
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dates, key = { it.toString() }) { day ->
                    val top = when (day) {
                        today -> stringResource(R.string.today)
                        today.plusDays(1) -> stringResource(R.string.tomorrow)
                        else -> day.format(DayName).capitalizeTr()
                    }
                    DayCell(top, day.dayOfMonth.toString(), conditions[day.toString()], day == selected) { onDate(day) }
                }
                item(key = "other") {
                    val beyond = selected.isAfter(dates.last())
                    DayCell(
                        top = if (beyond) selected.format(DayName).capitalizeTr() else stringResource(R.string.run_other_day),
                        number = if (beyond) selected.dayOfMonth.toString() else null,
                        condition = null,
                        selected = beyond,
                        icon = R.drawable.ic_calendar,
                        onClick = onPickOther
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(top: String, number: String?, condition: WeatherCondition?, selected: Boolean, icon: Int? = null, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        if (selected) 1f else 0.96f,
        spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "dayScale"
    )
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
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            top,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White.copy(alpha = 0.9f) else NavySoft,
            maxLines = 1
        )
        if (number != null) Text(number, style = MaterialTheme.typography.titleLarge, color = textColor)
        when {
            icon != null -> Icon(
                painterResource(icon),
                null,
                tint = if (selected) Color.White else HavaBlue,
                modifier = Modifier.padding(top = 2.dp).size(26.dp)
            )
            condition != null -> WeatherIcon(condition, isDay = true, size = 26.dp)
            else -> Spacer(Modifier.height(26.dp))
        }
    }
}
