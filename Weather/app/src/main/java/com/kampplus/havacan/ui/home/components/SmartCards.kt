package com.kampplus.havacan.ui.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kampplus.havacan.R
import com.kampplus.havacan.domain.insight.WeatherInsight
import com.kampplus.havacan.domain.running.RunningAdvice
import com.kampplus.havacan.model.HourlyWeather
import com.kampplus.havacan.model.deg
import com.kampplus.havacan.ui.theme.Hava
import com.kampplus.havacan.ui.theme.HavaBlue
import com.kampplus.havacan.ui.theme.Navy
import com.kampplus.havacan.ui.theme.NavySoft
import com.kampplus.havacan.ui.theme.SkyTint
import com.kampplus.havacan.ui.theme.SunLight
import kotlin.math.roundToInt
import kotlin.math.sin

private val CardGradient = Brush.horizontalGradient(listOf(Color(0xFFF4F8FF), Color(0xFFE7F0FF)))

/** Normal mode smart card: rain now / rain coming / dry day, always from the real forecast. */
@Composable
fun WeatherInsightCard(insight: WeatherInsight, clock: () -> Float, onShowHour: (HourlyWeather) -> Unit, modifier: Modifier = Modifier) {
    val (title, line, headline, hour) = when (insight) {
        is WeatherInsight.RainNow -> Quad(
            stringResource(R.string.insight_rain_now),
            "${stringResource(R.string.now)} · ${insight.hour.condition.label}",
            stringResource(R.string.insight_rain_probability, insight.hour.precipitationProbability ?: 0),
            insight.hour
        )
        is WeatherInsight.RainComing -> Quad(
            stringResource(R.string.insight_rain_coming),
            "${insight.hour.hourLabel} · ${insight.hour.condition.label}",
            stringResource(R.string.insight_rain_probability, insight.hour.precipitationProbability ?: 0),
            insight.hour
        )
        is WeatherInsight.Dry -> Quad(
            stringResource(R.string.insight_dry),
            "${insight.coolest.hourLabel} · ${insight.coolest.condition.label}",
            stringResource(R.string.insight_lowest, insight.coolest.temperature.deg()),
            insight.coolest
        )
    }
    val rainy = insight !is WeatherInsight.Dry
    SoftCard(modifier, background = Hava.p.highlight) {
        Row(
            Modifier.heightIn(min = 160.dp).padding(start = 20.dp, top = 18.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = HavaBlue)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_schedule), null, tint = Hava.ink, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(line, style = MaterialTheme.typography.bodyLarge, color = Hava.ink)
                }
                Text(headline, style = MaterialTheme.typography.headlineSmall, color = Hava.ink)
                Spacer(Modifier.height(4.dp))
                PillButton(stringResource(R.string.insight_show_hour), null) { onShowHour(hour) }
            }
            Canvas(Modifier.width(130.dp).height(130.dp)) {
                val t = clock()
                val c = Offset(size.width * 0.52f, size.height * 0.45f)
                if (rainy) {
                    drawUmbrella(c, size.width / 150f, sway = sin(t * 1.6f) * 5f)
                } else {
                    drawSunAndCloud(c, size.width / 140f, bob = sin(t * 1.4f) * 3f)
                }
            }
        }
    }
}

/** Runner mode card: the best window computed from the real hourly forecast, or an honest "none". */
@Composable
fun RunnerInsightCard(advice: RunningAdvice, clock: () -> Float, onPlan: () -> Unit, onInspect: () -> Unit, modifier: Modifier = Modifier) {
    SoftCard(modifier, background = Hava.p.highlight) {
        Row(
            Modifier.heightIn(min = 120.dp).padding(start = 18.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                when (advice) {
                    is RunningAdvice.BestWindow -> {
                        Text(stringResource(R.string.runner_best_title), style = MaterialTheme.typography.titleMedium, color = HavaBlue)
                        Text(advice.window.rangeLabel, style = MaterialTheme.typography.headlineMedium, color = Hava.ink, maxLines = 1)
                        Text(advice.window.reasons.joinToString(" • "), style = MaterialTheme.typography.bodyMedium, color = Hava.inkSoft)
                        Spacer(Modifier.height(4.dp))
                        PillButton(stringResource(R.string.runner_plan), R.drawable.ic_run, onPlan)
                    }
                    is RunningAdvice.NoGoodTime -> {
                        Text(stringResource(R.string.runner_none_title), style = MaterialTheme.typography.titleMedium, color = HavaBlue)
                        Text(stringResource(R.string.runner_none_detail), style = MaterialTheme.typography.titleLarge, color = Hava.ink)
                        advice.mainBlocker?.let { Text(it.message, style = MaterialTheme.typography.bodyLarge, color = Hava.inkSoft) }
                        Spacer(Modifier.height(4.dp))
                        PillButton(stringResource(R.string.runner_inspect), null, onInspect)
                    }
                }
            }
            Canvas(Modifier.width(92.dp).height(88.dp)) {
                val t = clock()
                val s = size.width / 150f
                drawSunAndCloud(Offset(size.width * 0.72f, size.height * 0.22f), s * 0.5f, bob = sin(t * 1.4f) * 2f)
                drawRunningShoe(Offset(size.width * 0.5f, size.height * 0.66f + sin(t * 3f) * 2f), s, tilt = -8f + sin(t * 3f) * 3f)
            }
        }
    }
}

/**
 * Entry to Salla Kombin. Tapping (or shaking the phone, see ShakeEffect) wiggles the jacket;
 * [wiggleSignal] increments to replay the wiggle from outside.
 */
@Composable
fun SallaKombinCard(runner: Boolean, wiggleSignal: Int, clock: () -> Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val wiggle = remember { Animatable(0f) }
    LaunchedEffect(wiggleSignal) {
        if (wiggleSignal == 0) return@LaunchedEffect
        wiggle.snapTo(18f)
        wiggle.animateTo(0f, spring(dampingRatio = 0.18f, stiffness = 260f))
    }
    val subtitle = stringResource(if (runner) R.string.salla_runner else R.string.salla_normal)
    SoftCard(modifier, background = Hava.p.sallaCard) {
        Row(
            Modifier
                .clickable(role = Role.Button, onClick = onClick)
                .heightIn(min = 76.dp)
                .padding(start = 18.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.salla_title), style = MaterialTheme.typography.titleLarge, color = Hava.ink)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Hava.inkSoft)
            }
            Canvas(Modifier.width(72.dp).fillMaxHeight().heightIn(min = 76.dp)) {
                val t = clock()
                val idle = sin(t * 2.2f) * 2.5f
                drawJacket(Offset(size.width * 0.5f, size.height * 0.55f), size.width / 130f, wiggle.value + idle)
            }
            Box(
                Modifier
                    .size(40.dp)
                    .background(Hava.card.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // The whole card is the button; the chevron is decoration
                Icon(painterResource(R.drawable.ic_chevron_right), null, tint = Hava.ink)
            }
        }
    }
}

@Composable
private fun PillButton(text: String, icon: Int?, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = HavaBlue),
        modifier = Modifier.heightIn(min = 48.dp)
    ) {
        if (icon != null) {
            Icon(painterResource(icon), null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.width(4.dp))
        Icon(painterResource(R.drawable.ic_chevron_right), null, modifier = Modifier.size(18.dp))
    }
}

private data class Quad(val a: String, val b: String, val c: String, val d: HourlyWeather)
