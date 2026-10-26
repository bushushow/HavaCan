package com.kampplus.havacan.ui.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kampplus.havacan.R
import com.kampplus.havacan.model.deg
import com.kampplus.havacan.ui.city.ErrorReason
import com.kampplus.havacan.ui.home.HomeUiState
import com.kampplus.havacan.ui.home.WeatherLoadState
import com.kampplus.havacan.ui.theme.HavaBlue
import com.kampplus.havacan.ui.theme.HavaBlueLight
import com.kampplus.havacan.ui.theme.Navy
import com.kampplus.havacan.ui.theme.NavySoft
import kotlin.math.roundToInt

/** Big temperature and the facts around it, for "now" or the hour picked in the strip. */
@Composable
fun WeatherHero(state: HomeUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        when (val load = state.load) {
            WeatherLoadState.Loading -> HeroPlaceholder()
            WeatherLoadState.Empty -> HeroMessage(stringResource(R.string.weather_empty), null, onRetry)
            is WeatherLoadState.Error -> HeroMessage(
                stringResource(R.string.weather_error_title),
                stringResource(
                    when (load.reason) {
                        ErrorReason.NoConnection -> R.string.error_no_connection
                        ErrorReason.Server -> R.string.error_server
                        ErrorReason.Unknown -> R.string.error_unknown
                    }
                ),
                onRetry
            )
            is WeatherLoadState.Success -> {
                val forecast = load.forecast
                val hour = state.selectedHour
                val isNow = state.isNowSelected || hour == null
                val temperature = if (isNow) forecast.current.temperature else hour!!.temperature
                val feels = if (isNow) forecast.current.apparentTemperature else hour!!.apparentTemperature
                val condition = if (isNow) forecast.current.condition else hour!!.condition

                Box(Modifier.height(26.dp)) {
                    if (!isNow) {
                        Text(
                            stringResource(R.string.forecast_for, hour!!.hourLabel),
                            style = MaterialTheme.typography.labelLarge,
                            color = HavaBlue,
                            modifier = Modifier
                                .background(HavaBlueLight, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
                AnimatedContent(
                    targetState = temperature.deg(),
                    transitionSpec = {
                        val up = targetState > initialState
                        (slideInVertically { if (up) it / 2 else -it / 2 } + fadeIn()) togetherWith
                            (slideOutVertically { if (up) -it / 2 else it / 2 } + fadeOut())
                    },
                    label = "temperature"
                ) { value ->
                    Text(
                        stringResource(R.string.temperature, value),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 68.sp, lineHeight = 70.sp),
                        color = Navy,
                        maxLines = 1,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                AnimatedContent(targetState = condition.label, label = "condition") { label ->
                    Text(label, style = MaterialTheme.typography.titleLarge, color = Navy, maxLines = 2)
                }
                Text(
                    stringResource(R.string.feels_like, feels.deg()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = NavySoft
                )
                // High and low of the day being shown (today, or the day picked on the forecast card)
                val shownDate = if (isNow) forecast.todayDate else hour!!.date
                (forecast.daily.firstOrNull { it.date == shownDate } ?: forecast.today)?.let { today ->
                    val highLow = stringResource(R.string.cd_high_low, today.temperatureMax.deg(), today.temperatureMin.deg())
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.high_low, today.temperatureMax.deg(), today.temperatureMin.deg()),
                        modifier = Modifier.semantics { contentDescription = highLow },
                        style = MaterialTheme.typography.titleMedium,
                        color = Navy
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroPlaceholder() {
    val pulse by rememberInfiniteTransition(label = "heroLoading").animateFloat(
        0.35f,
        0.8f,
        infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse"
    )
    val description = stringResource(R.string.weather_loading)
    Column(
        Modifier
            .padding(top = 26.dp)
            .alpha(pulse)
            .semantics { this.contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.width(140.dp).height(84.dp).background(HavaBlueLight, RoundedCornerShape(20.dp)))
        Box(Modifier.width(150.dp).height(24.dp).background(HavaBlueLight, RoundedCornerShape(12.dp)))
        Box(Modifier.width(110.dp).height(18.dp).background(HavaBlueLight, RoundedCornerShape(9.dp)))
    }
}

@Composable
private fun HeroMessage(title: String, detail: String?, onRetry: () -> Unit) {
    Column(Modifier.padding(top = 26.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Navy)
        if (detail != null) Text(detail, style = MaterialTheme.typography.bodyMedium, color = NavySoft)
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}
