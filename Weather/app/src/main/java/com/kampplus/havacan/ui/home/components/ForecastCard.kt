package com.kampplus.havacan.ui.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kampplus.havacan.R
import com.kampplus.havacan.model.DailySummary
import com.kampplus.havacan.model.Forecast
import com.kampplus.havacan.model.HourlyWeather
import com.kampplus.havacan.model.WeatherCondition
import com.kampplus.havacan.model.deg
import com.kampplus.havacan.ui.theme.GoodGreen
import com.kampplus.havacan.ui.theme.HavaBlue
import com.kampplus.havacan.ui.theme.HavaBlueDeep
import com.kampplus.havacan.ui.theme.HavaBlueLight
import com.kampplus.havacan.ui.theme.Navy
import com.kampplus.havacan.ui.theme.NavySoft
import com.kampplus.havacan.ui.theme.SkyTint
import com.kampplus.havacan.ui.theme.Sun
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** Run is the 0–100 running score from RunningSuitabilityCalculator, shown in Koşucu Modu. */
enum class ForecastMetric(val label: String) { Run("Koşu"), Temperature("Sıcaklık"), Rain("Yağış"), Wind("Rüzgar") }

private typealias RunScore = (HourlyWeather) -> Int

private val TR: Locale = Locale.forLanguageTag("tr")

/** The hours the chart draws for [date]: the next 24 from now for today, the whole day otherwise. */
internal fun chartHours(forecast: Forecast, date: String): List<HourlyWeather> = if (date == forecast.todayDate) {
    forecast.upcoming.take(24)
} else {
    forecast.hourly.filter { it.date == date }
}

/** The hour a tap on a day selects: now for today, midday (or its first hour) for another day. */
internal fun hourForDay(forecast: Forecast, date: String): HourlyWeather? = if (date == forecast.todayDate) {
    forecast.upcoming.firstOrNull()
} else {
    forecast.hourly.firstOrNull { it.date == date && it.hour == 12 } ?: forecast.hourly.firstOrNull { it.date == date }
}

/** "Bugün", "Sal"… for the day strip; the full name for the header. */
internal fun dayName(date: String, today: String, full: Boolean = false): String {
    if (date == today) return "Bugün"
    val style = if (full) DateTextStyle.FULL else DateTextStyle.SHORT
    return LocalDate.parse(date).dayOfWeek.getDisplayName(style, TR).replaceFirstChar { it.titlecase(TR) }
}

private val ChartPad = 22.dp
private val TileShape = RoundedCornerShape(20.dp)

/**
 * Google-style forecast in HavaCan's look: what the chosen hour brings (rain, humidity, wind),
 * a 24-hour chart for temperature, rain chance or wind, and every forecast day to pick from.
 * All of it is the Open-Meteo forecast already in memory: tabs and days never hit the network.
 */
@Composable
fun ForecastCard(
    forecast: Forecast,
    selected: HourlyWeather?,
    onHourSelected: (HourlyWeather) -> Unit,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    /** Set in Koşucu Modu: adds the "Koşu" tab (hourly running score) and opens on it. */
    runScore: RunScore? = null
) {
    val metrics = if (runScore != null) ForecastMetric.entries else ForecastMetric.entries - ForecastMetric.Run
    var metric by rememberSaveable(runScore != null) { mutableStateOf(metrics.first()) }
    val hour = selected ?: forecast.upcoming.firstOrNull() ?: return
    val hours = chartHours(forecast, hour.date)
    var picking by rememberSaveable { mutableStateOf(false) }
    if (picking) {
        ForecastDatePicker(forecast, hour.date, onDismiss = { picking = false }) { date ->
            picking = false
            hourForDay(forecast, date)?.let(onHourSelected)
        }
    }
    SoftCard(modifier) {
        Column(Modifier.padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HourHeader(hour, forecast.todayDate, onPickDate = { picking = true }, Modifier.padding(horizontal = 18.dp))
            FactTiles(hour, Modifier.padding(horizontal = 14.dp))
            MetricTabs(metrics, metric, { metric = it }, reduceMotion, Modifier.padding(horizontal = 14.dp))
            ForecastChart(hours, metric, runScore, hour.time, onHourSelected, reduceMotion)
            DayStrip(forecast, hour.date) { date -> hourForDay(forecast, date)?.let(onHourSelected) }
        }
    }
}

@Composable
private fun HourHeader(hour: HourlyWeather, today: String, onPickDate: () -> Unit, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text("Tahmin", style = MaterialTheme.typography.titleLarge, color = Navy, fontWeight = FontWeight.ExtraBold)
            Text(
                "${dayName(hour.date, today, full = true)} · ${hour.hourLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = NavySoft
            )
        }
        Row(
            Modifier.background(SkyTint, CircleShape).padding(start = 6.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WeatherIcon(hour.condition, hour.isDay, size = 28.dp)
            Text(hour.condition.label, style = MaterialTheme.typography.labelLarge, color = Navy, fontWeight = FontWeight.Bold)
        }
        Surface(
            onClick = onPickDate,
            shape = CircleShape,
            color = HavaBlueLight,
            modifier = Modifier.size(48.dp).semantics { contentDescription = "Tarih seç" }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_calendar), null, tint = HavaBlue, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun FactTiles(hour: HourlyWeather, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FactTile(
            label = "Yağış",
            value = hour.precipitationProbability?.let { "%$it" } ?: "—",
            tint = HavaBlue,
            modifier = Modifier.weight(1f)
        ) { WeatherIcon(WeatherCondition.Rain, isDay = true, size = 20.dp) }
        FactTile(
            label = "Nem",
            value = hour.humidity?.let { "%$it" } ?: "—",
            tint = Color(0xFF2BA59B),
            modifier = Modifier.weight(1f)
        ) { Icon(painterResource(R.drawable.ic_drop), null, tint = Color(0xFF2BA59B), modifier = Modifier.size(16.dp)) }
        FactTile(
            label = "Rüzgar",
            value = hour.windSpeed?.roundToInt()?.let { "$it km/sa" } ?: "—",
            tint = Navy,
            modifier = Modifier.weight(1f)
        ) { Icon(painterResource(R.drawable.ic_wind), null, tint = NavySoft, modifier = Modifier.size(16.dp)) }
    }
}

@Composable
private fun FactTile(label: String, value: String, tint: Color, modifier: Modifier, icon: @Composable () -> Unit) {
    Row(
        modifier
            .heightIn(min = 40.dp)
            .background(SkyTint, CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clearAndSetSemantics { contentDescription = "$label $value" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
    ) {
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) { icon() }
        Text(value, style = MaterialTheme.typography.labelLarge, color = tint, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = NavySoft, maxLines = 1)
    }
}

/** A pill that slides under the chosen tab. */
@Composable
private fun MetricTabs(
    metrics: List<ForecastMetric>,
    selected: ForecastMetric,
    onSelect: (ForecastMetric) -> Unit,
    reduceMotion: Boolean,
    modifier: Modifier
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(SkyTint, CircleShape)
            .padding(4.dp)
    ) {
        val tabWidth = maxWidth / metrics.size
        val x by animateDpAsState(
            tabWidth * metrics.indexOf(selected).coerceAtLeast(0),
            if (reduceMotion) tween(0) else spring(dampingRatio = 0.75f, stiffness = 500f),
            label = "tabPill"
        )
        Box(
            Modifier
                .offset(x = x)
                .width(tabWidth)
                .fillMaxHeight()
                .shadow(3.dp, CircleShape, ambientColor = Navy.copy(alpha = 0.12f), spotColor = Navy.copy(alpha = 0.12f))
                .background(Color.White, CircleShape)
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight().selectableGroup()) {
            metrics.forEach { m ->
                val on = m == selected
                val color by animateColorAsState(if (on) metricColor(m).darker() else NavySoft, label = "tabText")
                Box(
                    Modifier.weight(1f).fillMaxHeight().selectable(selected = on, role = Role.Tab) { onSelect(m) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        m.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = color,
                        fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun metricColor(m: ForecastMetric) = when (m) {
    ForecastMetric.Run -> GoodGreen
    ForecastMetric.Temperature -> Sun
    ForecastMetric.Rain -> HavaBlue
    ForecastMetric.Wind -> Color(0xFF2BA59B)
}

/** Text-safe shade of a metric colour (yellow is too light for words on white). */
private fun Color.darker() = if (this == Sun) Color(0xFFC77800) else this

private fun HourlyWeather.value(m: ForecastMetric, score: RunScore?): Float = when (m) {
    ForecastMetric.Run -> (score?.invoke(this) ?: 0).toFloat()
    ForecastMetric.Temperature -> temperature.deg().toFloat()
    ForecastMetric.Rain -> (precipitationProbability ?: 0).toFloat()
    ForecastMetric.Wind -> (windSpeed ?: 0.0).toFloat()
}

private fun HourlyWeather.label(m: ForecastMetric, score: RunScore?): String = when (m) {
    ForecastMetric.Run -> "${score?.invoke(this) ?: 0}"
    ForecastMetric.Temperature -> "${temperature.deg()}°"
    ForecastMetric.Rain -> "%${precipitationProbability ?: 0}"
    ForecastMetric.Wind -> "${(windSpeed ?: 0.0).roundToInt()}"
}

/**
 * Smooth area chart that draws itself in when the tab or day changes. Every third hour gets its
 * value, a weather icon and the time; the chosen hour gets a value bubble. A tap picks an hour.
 */
@Composable
private fun ForecastChart(
    hours: List<HourlyWeather>,
    metric: ForecastMetric,
    score: RunScore?,
    selectedTime: String,
    onSelect: (HourlyWeather) -> Unit,
    reduceMotion: Boolean
) {
    if (hours.size < 2) return
    val measurer = rememberTextMeasurer()
    val pick by rememberUpdatedState(onSelect)
    val currentHours by rememberUpdatedState(hours)
    val color = metricColor(metric)
    val reveal = remember(metric, hours.first().time) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(reveal) { reveal.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    val valueStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy)
    val bubbleStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
    val description =
        "${if (metric == ForecastMetric.Run) "Koşu puanı" else metric.label}: " +
            hours.filterIndexed { i, _ -> i % 3 == 0 }.joinToString(", ") { "${it.hourLabel} ${it.label(metric, score)}" }

    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val padPx = with(density) { ChartPad.toPx() }
        val step = (widthPx - padPx * 2) / (hours.size - 1)
        Column {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .semantics { contentDescription = description }
                    .pointerInput(Unit) {
                        detectTapGestures { tap ->
                            val list = currentHours
                            val s = (size.width - padPx * 2) / (list.size - 1)
                            pick(list[((tap.x - padPx) / s).roundToInt().coerceIn(0, list.lastIndex)])
                        }
                    }
            ) {
                val top = 34.dp.toPx()
                val bottom = size.height - 8.dp.toPx()
                val values = hours.map { it.value(metric, score) }
                val min = if (metric == ForecastMetric.Temperature) values.min() - 2f else 0f
                val max = when (metric) {
                    ForecastMetric.Temperature -> values.max() + 2f
                    ForecastMetric.Run -> 100f
                    else -> values.max() * 1.15f + 5f
                }.coerceAtLeast(min + 1f)
                fun x(i: Int) = padPx + i * step
                fun y(v: Float) = bottom - (v - min) / (max - min) * (bottom - top)

                // Faint guide lines
                for (g in 0..2) {
                    val gy = top + (bottom - top) * g / 2f
                    drawLine(
                        Navy.copy(alpha = 0.06f),
                        Offset(padPx, gy),
                        Offset(size.width - padPx, gy),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
                    )
                }
                // Smooth curve: each segment bends through its midpoint
                val line = Path().apply {
                    moveTo(x(0), y(values[0]))
                    for (i in 1..values.lastIndex) {
                        val mx = (x(i - 1) + x(i)) / 2
                        cubicTo(mx, y(values[i - 1]), mx, y(values[i]), x(i), y(values[i]))
                    }
                }
                val area = Path().apply {
                    addPath(line)
                    lineTo(x(values.lastIndex), bottom)
                    lineTo(x(0), bottom)
                    close()
                }
                clipRect(right = size.width * reveal.value) {
                    drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.38f), color.copy(alpha = 0.02f)), top, bottom))
                    drawPath(
                        line,
                        Brush.horizontalGradient(listOf(color, color.darker())),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    hours.forEachIndexed { i, h ->
                        if (i % 3 != 0) return@forEachIndexed
                        drawCircle(Color.White, 4.dp.toPx(), Offset(x(i), y(values[i])))
                        drawCircle(color.darker(), 2.5.dp.toPx(), Offset(x(i), y(values[i])))
                        if (h.time == selectedTime) return@forEachIndexed
                        val v = measurer.measure(h.label(metric, score), valueStyle)
                        drawText(
                            v,
                            topLeft = Offset(
                                (x(i) - v.size.width / 2f).coerceIn(0f, size.width - v.size.width),
                                y(values[i]) - v.size.height - 6.dp.toPx()
                            )
                        )
                    }
                }
                // The chosen hour: dashed marker, a ring on the curve and a value bubble
                hours.indexOfFirst { it.time == selectedTime }.takeIf { it >= 0 }?.let { i ->
                    val p = Offset(x(i), y(values[i]))
                    drawLine(
                        Navy.copy(alpha = 0.3f),
                        Offset(p.x, p.y),
                        Offset(p.x, bottom),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                    drawCircle(color.copy(alpha = 0.25f), 11.dp.toPx(), p)
                    drawCircle(Color.White, 6.dp.toPx(), p)
                    drawCircle(color.darker(), 4.dp.toPx(), p)
                    val t = measurer.measure(hours[i].label(metric, score), bubbleStyle)
                    val bw = t.size.width + 16.dp.toPx()
                    val bh = t.size.height + 8.dp.toPx()
                    val bx = (p.x - bw / 2).coerceIn(0f, size.width - bw)
                    val by = (p.y - bh - 14.dp.toPx()).coerceAtLeast(0f)
                    drawRoundRect(Navy, Offset(bx, by), Size(bw, bh), CornerRadius(bh / 2))
                    drawText(t, topLeft = Offset(bx + 8.dp.toPx(), by + 4.dp.toPx()))
                }
            }
            // Icons and times under every third hour, lined up with the points above
            Box(Modifier.fillMaxWidth().height(56.dp).clipToBounds().clearAndSetSemantics { }) {
                hours.forEachIndexed { i, h ->
                    if (i % 3 != 0) return@forEachIndexed
                    val cx = with(density) { (padPx + i * step).toDp() }
                    Column(
                        Modifier.offset(x = cx - 22.dp).width(44.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WeatherIcon(h.condition, h.isDay, size = 24.dp)
                        Text(
                            h.hourLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (h.time == selectedTime) Navy else NavySoft,
                            fontWeight = if (h.time == selectedTime) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayStrip(forecast: Forecast, selectedDate: String, onSelect: (String) -> Unit) {
    // Only days the hourly forecast covers can be opened on the chart
    val days = forecast.daily.filter { d -> forecast.hourly.any { it.date == d.date } && d.date >= forecast.todayDate }
    if (days.isEmpty()) return
    val low = days.minOf { it.temperatureMin }
    val high = days.maxOf { it.temperatureMax }.coerceAtLeast(low + 1)
    val state = rememberLazyListState()
    LaunchedEffect(selectedDate) {
        val i = days.indexOfFirst { it.date == selectedDate }
        if (i >= 0) state.animateScrollToItem((i - 2).coerceAtLeast(0))
    }
    LazyRow(
        state = state,
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.selectableGroup()
    ) {
        items(days, key = { it.date }) { day ->
            DayCell(day, forecast.todayDate, day.date == selectedDate, low, high) { onSelect(day.date) }
        }
    }
}

@Composable
private fun DayCell(day: DailySummary, today: String, selected: Boolean, low: Double, high: Double, onClick: () -> Unit) {
    val name = dayName(day.date, today)
    val rain = day.precipitationProbabilityMax?.takeIf { it >= 20 }
    val description = "${dayName(day.date, today, full = true)}, ${day.condition.label}, en yüksek ${day.temperatureMax.deg()}°, " +
        "en düşük ${day.temperatureMin.deg()}°" + (day.precipitationProbabilityMax?.let { ", yağış ihtimali yüzde $it" } ?: "")
    val shape = RoundedCornerShape(22.dp)
    val text = if (selected) Color.White else Navy
    val soft = if (selected) Color.White.copy(alpha = 0.8f) else NavySoft
    Column(
        Modifier
            .width(72.dp)
            .then(
                if (selected) {
                    Modifier
                        .shadow(8.dp, shape, ambientColor = HavaBlue.copy(alpha = 0.4f), spotColor = HavaBlue.copy(alpha = 0.4f))
                        .background(Brush.verticalGradient(listOf(Color(0xFF3D82FF), HavaBlueDeep)), shape)
                } else {
                    Modifier.background(SkyTint.copy(alpha = 0.6f), shape)
                }
            )
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .heightIn(min = 132.dp)
            .padding(vertical = 12.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(name, style = MaterialTheme.typography.titleSmall, color = text, fontWeight = FontWeight.ExtraBold)
        Box(
            Modifier.size(38.dp).background(if (selected) Color.White.copy(alpha = 0.18f) else Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            WeatherIcon(day.condition, isDay = true, size = 30.dp)
        }
        Text(
            rain?.let { "%$it" } ?: " ",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) HavaBlueLight else HavaBlue,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${day.temperatureMax.deg()}°",
                style = MaterialTheme.typography.labelLarge,
                color = text,
                fontWeight = FontWeight.ExtraBold
            )
            Text("${day.temperatureMin.deg()}°", style = MaterialTheme.typography.labelLarge, color = soft)
        }
        // Where this day's range sits in the whole period, cool blue to warm yellow
        Canvas(Modifier.width(44.dp).height(5.dp)) {
            val r = size.height / 2
            drawRoundRect(if (selected) Color.White.copy(alpha = 0.25f) else Navy.copy(alpha = 0.08f), cornerRadius = CornerRadius(r))
            val from = ((day.temperatureMin - low) / (high - low)).toFloat().coerceIn(0f, 1f)
            val to = ((day.temperatureMax - low) / (high - low)).toFloat().coerceIn(from, 1f)
            drawRoundRect(
                Brush.horizontalGradient(listOf(Color(0xFF5BA4FF), Sun), startX = 0f, endX = size.width),
                topLeft = Offset(size.width * from, 0f),
                size = Size((size.width * (to - from)).coerceAtLeast(size.height), size.height),
                cornerRadius = CornerRadius(r)
            )
        }
    }
}

/** The days a forecast can be opened on: today up to the last day the hourly forecast covers. */
internal fun pickableDates(forecast: Forecast): Set<String> = forecast.hourly.map { it.date }.filter { it >= forecast.todayDate }.toSet()

/** Calendar with only forecast days enabled; anything later has no forecast to show yet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForecastDatePicker(forecast: Forecast, selected: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val dates = remember(forecast) { pickableDates(forecast) }
    val toMillis = { d: String -> LocalDate.parse(d).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }
    val toDate = { ms: Long -> Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toString() }
    val years = dates.map { LocalDate.parse(it).year }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = toMillis(selected),
        yearRange = (years.minOrNull() ?: LocalDate.now().year)..(years.maxOrNull() ?: LocalDate.now().year),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = toDate(utcTimeMillis) in dates
            override fun isSelectableYear(year: Int) = year in years
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onPick(toDate(it)) } ?: onDismiss() }) { Text("Göster") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    ) {
        DatePicker(
            state,
            title = { Text("Tahmin günü seç", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
            headline = null,
            showModeToggle = false
        )
        Text(
            "Tahmin ${dates.size} gün ileriyi gösteriyor; sonraki günler henüz seçilemez.",
            style = MaterialTheme.typography.bodySmall,
            color = NavySoft,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
}
