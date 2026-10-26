package com.kampplus.havacan.ui.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kampplus.havacan.R
import com.kampplus.havacan.domain.running.HourScore
import com.kampplus.havacan.domain.running.RunningAdvice
import com.kampplus.havacan.model.HourlyWeather
import com.kampplus.havacan.model.deg
import com.kampplus.havacan.ui.theme.GoodGreen
import com.kampplus.havacan.ui.theme.HavaBlue
import com.kampplus.havacan.ui.theme.HavaBlueDeep
import com.kampplus.havacan.ui.theme.Navy
import com.kampplus.havacan.ui.theme.SkyTint
import kotlin.math.roundToInt

/** Normal mode: "Şimdi" plus the next 12 hours as tappable pills joined by a dotted line. */
@Composable
fun HourlyStrip(hours: List<HourlyWeather>, selectedTime: String?, onSelect: (HourlyWeather) -> Unit, modifier: Modifier = Modifier) {
    val selected = selectedTime ?: hours.firstOrNull()?.time
    SoftCard(modifier) {
        LazyRow(
            state = rememberLazyListState(),
            contentPadding = PaddingValues(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(hours, key = { _, h -> h.time }) { index, hour ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HourPill(
                        label = if (index == 0) stringResource(R.string.now) else hour.hourLabel,
                        hour = hour,
                        selected = hour.time == selected,
                        onClick = { onSelect(hour) }
                    )
                    if (index != hours.lastIndex) Connector()
                }
            }
        }
    }
}

@Composable
private fun HourPill(label: String, hour: HourlyWeather, selected: Boolean, onClick: () -> Unit) {
    val content by animateColorAsState(if (selected) Color.White else Navy, label = "pillText")
    val scale by animateFloatAsState(if (selected) 1f else 0.94f, spring(dampingRatio = 0.55f), label = "pillScale")
    val background = if (selected) {
        Brush.verticalGradient(listOf(Color(0xFF3D82FF), HavaBlueDeep))
    } else {
        Brush.verticalGradient(listOf(SkyTint, SkyTint))
    }
    Column(
        modifier = Modifier
            .scale(scale)
            .width(76.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(background)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
        WeatherIcon(hour.condition, hour.isDay, size = 36.dp)
        Text(
            stringResource(R.string.temperature, hour.temperature.deg()),
            style = MaterialTheme.typography.titleLarge,
            color = content
        )
    }
}

@Composable
private fun Connector() {
    Canvas(Modifier.width(16.dp).height(8.dp)) {
        val y = size.height / 2
        drawCircle(HavaBlue, 3.dp.toPx(), Offset(3.dp.toPx(), y))
        drawLine(HavaBlue.copy(alpha = 0.5f), Offset(3.dp.toPx(), y), Offset(size.width, y), 1.5.dp.toPx())
    }
}

/**
 * Runner mode: hours around the best window as columns, with green dots and a green line under
 * the hours that are good for running. Grey dots are hours that scored below the threshold.
 */
@Composable
fun RunnerHourlyStrip(advice: RunningAdvice, selectedTime: String?, onSelect: (HourlyWeather) -> Unit, modifier: Modifier = Modifier) {
    val shown = runnerHours(advice)
    val title = (advice as? RunningAdvice.BestWindow)?.window?.dayLabel ?: stringResource(R.string.coming_hours)
    SoftCard(modifier) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = HavaBlue)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                shown.forEachIndexed { i, score ->
                    val hour = score.hour
                    val isSelected = hour.time == selectedTime
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) SkyTint else Color.Transparent)
                            .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(hour) })
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(hour.hourLabel, style = MaterialTheme.typography.titleSmall, color = Navy)
                        WeatherIcon(hour.condition, hour.isDay, size = 38.dp)
                        Text(
                            stringResource(R.string.temperature, hour.temperature.deg()),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Navy,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (i != shown.lastIndex) {
                        Box(Modifier.width(1.dp).height(96.dp).align(Alignment.CenterVertically).background(Navy.copy(alpha = 0.06f)))
                    }
                }
            }
            val window = (advice as? RunningAdvice.BestWindow)?.window?.hours.orEmpty()
            SuitabilityTrack(
                shown.map {
                    if (it in window) {
                        Fit.Best
                    } else if (it.isGood) {
                        Fit.Good
                    } else {
                        Fit.Poor
                    }
                },
                Modifier.fillMaxWidth().height(22.dp)
            )
        }
    }
}

private enum class Fit { Best, Good, Poor }

/** Bold green for the suggested window, pale green for other good hours, grey for the rest. */
@Composable
private fun SuitabilityTrack(fits: List<Fit>, modifier: Modifier) {
    Canvas(modifier) {
        if (fits.isEmpty()) return@Canvas
        val step = size.width / fits.size
        val y = size.height / 2
        val centers = fits.indices.map { Offset(step * (it + 0.5f), y) }
        for (i in 0 until fits.lastIndex) {
            val a = fits[i]
            val b = fits[i + 1]
            if (a != Fit.Poor && b != Fit.Poor) {
                val strong = a == Fit.Best && b == Fit.Best
                drawLine(GoodGreen.copy(alpha = if (strong) 0.8f else 0.3f), centers[i], centers[i + 1], 4.dp.toPx())
            }
        }
        fits.forEachIndexed { i, fit ->
            when (fit) {
                Fit.Best -> {
                    drawCircle(GoodGreen.copy(alpha = 0.25f), 9.dp.toPx(), centers[i])
                    drawCircle(GoodGreen, 6.dp.toPx(), centers[i])
                }
                Fit.Good -> drawCircle(GoodGreen.copy(alpha = 0.4f), 5.dp.toPx(), centers[i])
                Fit.Poor -> drawCircle(Navy.copy(alpha = 0.15f), 5.dp.toPx(), centers[i])
            }
        }
    }
}

/** Up to five hours: the best window with one hour either side, or the first daylight hours. */
private fun runnerHours(advice: RunningAdvice): List<HourScore> {
    val scores = advice.scores
    return when (advice) {
        is RunningAdvice.BestWindow -> {
            val first = scores.indexOf(advice.window.hours.first())
            val last = scores.indexOf(advice.window.hours.last())
            val from = (first - 1).coerceAtLeast(0)
            val to = (last + 1).coerceAtMost(scores.lastIndex)
            scores.subList(from, to + 1).take(5)
        }
        is RunningAdvice.NoGoodTime -> scores.filter { it.hour.isDay }.ifEmpty { scores }.take(5)
    }
}

/** White rounded card used by the strips and smart cards. */
@Composable
fun SoftCard(modifier: Modifier = Modifier, background: Brush? = null, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = shape, ambientColor = Navy.copy(alpha = 0.08f), spotColor = Navy.copy(alpha = 0.08f))
            .clip(shape)
            .background(background ?: SolidColor(Color.White))
    ) { content() }
}
