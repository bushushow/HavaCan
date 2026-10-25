package com.example.weather.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weather.R
import com.example.weather.domain.running.RunningAdvice
import com.example.weather.ui.city.CityRoute
import com.example.weather.ui.home.HomeActions
import com.example.weather.ui.home.HomeScreen
import com.example.weather.ui.home.HomeViewModel
import com.example.weather.ui.home.ShakeEffect
import com.example.weather.ui.home.character.HavaCanEvent
import com.example.weather.ui.home.character.HavaCanMotion
import com.example.weather.ui.home.character.drawHavaCanFace
import com.example.weather.ui.home.components.HavaCanBottomNavigation
import com.example.weather.ui.home.components.HomeTab
import com.example.weather.ui.home.rememberReduceMotion
import com.example.weather.ui.home.scene.rememberSceneClock
import com.example.weather.ui.theme.Navy
import com.example.weather.ui.theme.NavySoft
import kotlin.math.roundToInt

private const val TAG = "Hava"

private enum class Sheet { None, RunPlan, Salla, Chat, Notifications }

/** App shell: bottom navigation, the home screen, the Kamp+ city search and small info sheets. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HavaApp(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reduceMotion = rememberReduceMotion()
    val clock = rememberSceneClock(paused = reduceMotion)
    val motion = remember { HavaCanMotion() }

    var tab by rememberSaveable { mutableStateOf(HomeTab.Weather) }
    var showCityPicker by rememberSaveable { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(Sheet.None) }
    var runAdvice by remember { mutableStateOf<RunningAdvice?>(null) }
    var sallaSignal by remember { mutableIntStateOf(0) }
    var havaCanPulse by remember { mutableIntStateOf(0) }

    val openSalla = {
        sallaSignal++
        sheet = Sheet.Salla
    }
    ShakeEffect(enabled = tab == HomeTab.Weather && !showCityPicker && sheet == Sheet.None, onShake = openSalla)

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (tab) {
            HomeTab.Weather, HomeTab.HavaCan -> HomeScreen(
                state = state,
                motion = motion,
                clock = clock,
                reduceMotion = reduceMotion,
                sallaSignal = sallaSignal,
                actions = HomeActions(
                    onCityClick = { showCityPicker = true },
                    onBellClick = { sheet = Sheet.Notifications },
                    onPlanRun = { runAdvice = it; sheet = Sheet.RunPlan },
                    onSalla = openSalla,
                ),
                onToggleRunner = viewModel::toggleRunnerMode,
                onHourSelected = { viewModel.onHourSelected(it.time) },
                onRetry = viewModel::retry,
            )
            HomeTab.Plans -> ComingSoon(stringResource(R.string.nav_plans))
            HomeTab.Wardrobe -> ComingSoon(stringResource(R.string.nav_wardrobe))
            HomeTab.Profile -> ComingSoon(stringResource(R.string.nav_profile))
        }

        HavaCanBottomNavigation(
            selected = if (tab == HomeTab.HavaCan) HomeTab.Weather else tab,
            havaCanPulse = havaCanPulse,
            onSelect = { selected ->
                if (selected == HomeTab.HavaCan) {
                    // The chat screen comes later: HavaCan reacts and says so, instead of a dead button
                    havaCanPulse++
                    tab = HomeTab.Weather
                    motion.send(HavaCanEvent.ChatRequested, state.mode.name == "Runner", clock())
                    sheet = Sheet.Chat
                } else {
                    tab = selected
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        AnimatedVisibility(
            visible = showCityPicker,
            enter = slideInVertically(tween(350)) { it / 3 } + fadeIn(tween(250)),
            exit = slideOutVertically(tween(250)) { it / 3 } + fadeOut(tween(200)),
        ) {
            BackHandler { showCityPicker = false }
            CityRoute(
                onCityClick = { city ->
                    Log.d(TAG, "Tıklandı: ${city.name}")
                    viewModel.onCityChosen(city)
                    showCityPicker = false
                },
            )
        }
    }

    if (sheet != Sheet.None) {
        ModalBottomSheet(onDismissRequest = { sheet = Sheet.None }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (sheet) {
                    Sheet.RunPlan -> RunPlanContent(runAdvice)
                    Sheet.Salla -> InfoContent(
                        stringResource(R.string.salla_title),
                        stringResource(R.string.salla_sheet_body) +
                            if (state.mode.name == "Runner") "\n\n" + stringResource(R.string.salla_sheet_runner) else "",
                    )
                    Sheet.Chat -> InfoContent(stringResource(R.string.chat_soon_title), stringResource(R.string.chat_soon_body))
                    Sheet.Notifications -> InfoContent(stringResource(R.string.cd_notifications), stringResource(R.string.notifications_empty))
                    Sheet.None -> Unit
                }
                TextButton(onClick = { sheet = Sheet.None }, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

@Composable
private fun InfoContent(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, color = Navy)
    Text(body, style = MaterialTheme.typography.bodyLarge, color = NavySoft)
}

@Composable
private fun RunPlanContent(advice: RunningAdvice?) {
    Text(stringResource(R.string.run_plan_title), style = MaterialTheme.typography.headlineSmall, color = Navy)
    if (advice is RunningAdvice.BestWindow) {
        Text(
            "${advice.window.dayLabel} · ${advice.window.rangeLabel}",
            style = MaterialTheme.typography.titleLarge,
            color = Navy,
        )
        advice.window.hours.forEach { s ->
            Text(
                stringResource(R.string.run_plan_hour, s.hour.hourLabel, s.hour.apparentTemperature.roundToInt(), s.score),
                style = MaterialTheme.typography.bodyLarge,
                color = Navy,
            )
        }
        Text(advice.window.reasons.joinToString(" • "), style = MaterialTheme.typography.bodyLarge, color = NavySoft)
    }
    Text(stringResource(R.string.run_plan_note), style = MaterialTheme.typography.bodyMedium, color = NavySoft)
}

/** Honest placeholder for screens we'll design next. */
@Composable
private fun ComingSoon(name: String) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(Modifier.size(96.dp)) { drawHavaCanFace(Offset(size.width / 2, size.height / 2), size.minDimension / 2.2f) }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.soon_title, name), style = MaterialTheme.typography.headlineSmall, color = Navy)
        Text(
            stringResource(R.string.soon_body),
            style = MaterialTheme.typography.bodyLarge,
            color = NavySoft,
            textAlign = TextAlign.Center,
        )
    }
}
