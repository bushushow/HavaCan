package com.example.weather.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
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
import com.example.weather.model.City
import com.example.weather.ui.city.CityRoute
import com.example.weather.ui.plans.PlansRoute
import com.example.weather.ui.run.RunPlannerRequest
import com.example.weather.ui.run.RunPlannerRoute
import com.example.weather.ui.run.RunSaved
import com.example.weather.ui.wardrobe.WardrobeRoute
import androidx.compose.runtime.LaunchedEffect
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

private const val TAG = "Hava"

private enum class Sheet { None, Chat, Notifications }

/** App shell: bottom navigation, the home screen, the Kamp+ city search and small info sheets. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HavaApp(
    openPlanId: String? = null,
    onOpenPlanHandled: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reduceMotion = rememberReduceMotion()
    val clock = rememberSceneClock(paused = reduceMotion)
    val motion = remember { HavaCanMotion() }

    var tab by rememberSaveable { mutableStateOf(HomeTab.Weather) }
    var showCityPicker by rememberSaveable { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(Sheet.None) }
    // Koşu Planla, opened from Koşucu Modu or from a run in Planlarım; null when closed
    var runPlanner by rememberSaveable(stateSaver = RunRequestSaver) { mutableStateOf<RunPlannerRequest?>(null) }
    var lastRunRequest by remember { mutableStateOf<RunPlannerRequest?>(null) }
    LaunchedEffect(runPlanner) { if (runPlanner != null) lastRunRequest = runPlanner }
    // Told to Planlarım after a run is saved, for the confirmation and the calendar offer
    var runNotice by remember { mutableStateOf<RunSaved?>(null) }
    var sallaSignal by remember { mutableIntStateOf(0) }
    var havaCanPulse by remember { mutableIntStateOf(0) }
    var showBottomBar by remember { mutableStateOf(true) }
    // Bumped by the home Salla Kombin card or a shake on the home screen: opens today's outfit
    var outfitRequest by rememberSaveable { mutableIntStateOf(0) }
    // Who gets the city the picker returns: the home screen (null) or the plan editor
    var cityPickTarget by remember { mutableStateOf<((City) -> Unit)?>(null) }

    // A notification about a plan opens the Planlar tab
    LaunchedEffect(openPlanId) { if (openPlanId != null) tab = HomeTab.Plans }

    val openSalla = {
        sallaSignal++
        outfitRequest++
        tab = HomeTab.Wardrobe
    }
    ShakeEffect(enabled = tab == HomeTab.Weather && !showCityPicker && sheet == Sheet.None && runPlanner == null, onShake = openSalla)

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
                    onPlanRun = { advice ->
                        val start = (advice as? RunningAdvice.BestWindow)?.window?.hours?.firstOrNull()?.hour?.time
                        runPlanner = RunPlannerRequest(System.currentTimeMillis(), start = start)
                    },
                    onSalla = openSalla,
                ),
                onToggleRunner = viewModel::toggleRunnerMode,
                onHourSelected = { viewModel.onHourSelected(it.time) },
                onRetry = viewModel::retry,
            )
            HomeTab.Plans -> PlansRoute(
                clock = clock,
                openPlanId = openPlanId,
                onOpenPlanHandled = onOpenPlanHandled,
                onCityClick = { cityPickTarget = null; showCityPicker = true },
                onBellClick = { sheet = Sheet.Notifications },
                onPickPlanCity = { onPicked -> cityPickTarget = onPicked; showCityPicker = true },
                onShowBottomBar = { showBottomBar = it },
                onOpenRunPlan = { id -> runPlanner = RunPlannerRequest(System.currentTimeMillis(), planId = id) },
                runNotice = runNotice,
                onRunNoticeShown = { runNotice = null },
            )
            HomeTab.Wardrobe -> WardrobeRoute(
                clock = clock,
                outfitRequest = outfitRequest,
                onShowBottomBar = { showBottomBar = it },
            )
            HomeTab.Profile -> ComingSoon(stringResource(R.string.nav_profile))
        }

        // Plan sub-screens (editor, Plan B) hide the bar; every other tab always shows it
        if ((tab != HomeTab.Plans && tab != HomeTab.Wardrobe) || showBottomBar) HavaCanBottomNavigation(
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
            visible = runPlanner != null,
            enter = slideInHorizontally(tween(380)) { it / 2 } + fadeIn(tween(250)),
            exit = slideOutHorizontally(tween(280)) { it / 2 } + fadeOut(tween(200)),
        ) {
            val request = runPlanner ?: lastRunRequest
            if (request != null) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    RunPlannerRoute(
                        request = request,
                        clock = clock,
                        reduceMotion = reduceMotion,
                        onPickCity = { onPicked -> cityPickTarget = onPicked; showCityPicker = true },
                        onClose = { runPlanner = null },
                        onSaved = { saved ->
                            if (runPlanner != null) {
                                runPlanner = null
                                runNotice = saved
                                tab = HomeTab.Plans
                            }
                        },
                        onDeleted = {
                            runPlanner = null
                            tab = HomeTab.Plans
                        },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showCityPicker,
            enter = slideInVertically(tween(350)) { it / 3 } + fadeIn(tween(250)),
            exit = slideOutVertically(tween(250)) { it / 3 } + fadeOut(tween(200)),
        ) {
            BackHandler { showCityPicker = false }
            CityRoute(
                onCityClick = { city ->
                    Log.d(TAG, "Tıklandı: ${city.name}")
                    val target = cityPickTarget
                    if (target != null) target(city) else viewModel.onCityChosen(city)
                    cityPickTarget = null
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

private val RunRequestSaver = androidx.compose.runtime.saveable.Saver<RunPlannerRequest?, String>(
    save = { r -> r?.let { "${it.token}|${it.planId.orEmpty()}|${it.start.orEmpty()}" } ?: "" },
    restore = { raw ->
        raw.split('|').takeIf { it.size == 3 }?.let { (token, id, start) ->
            RunPlannerRequest(token.toLong(), id.ifEmpty { null }, start.ifEmpty { null })
        }
    },
)
