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
import androidx.compose.ui.res.stringResource
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
import com.example.weather.ui.home.character.HavaCanMotion
import com.example.weather.ui.home.components.HavaCanBottomNavigation
import com.example.weather.ui.home.components.HomeTab
import com.example.weather.ui.home.rememberReduceMotion
import com.example.weather.ui.home.scene.rememberSceneClock
import com.example.weather.ui.theme.Navy
import com.example.weather.ui.theme.NavySoft
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.weather.domain.voice.VoiceDestination
import com.example.weather.ui.chat.ChatActions
import com.example.weather.ui.chat.ChatScreen
import com.example.weather.ui.profile.ProfileHooks
import com.example.weather.ui.profile.ProfileRoute
import com.example.weather.data.AppGraph
import com.example.weather.ui.voice.VoiceGuideBar
import com.example.weather.ui.voice.VoiceGuideController
import com.example.weather.ui.voice.rememberMicrophoneAccess

private const val TAG = "Hava"

private enum class Sheet { None, Notifications }

/** App shell: bottom navigation, the home screen, the Kamp+ city search and small info sheets. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HavaApp(
    openPlanId: String? = null,
    onOpenPlanHandled: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
    voice: VoiceGuideController = viewModel(factory = VoiceGuideController.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val voiceState by voice.state.collectAsStateWithLifecycle()
    val messages by voice.messages.collectAsStateWithLifecycle()
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

    // Screens opened by voice (or by a card in the chat); overlays close so the screen is really in front
    val go: (VoiceDestination) -> Unit = { destination ->
        showCityPicker = false
        sheet = Sheet.None
        if (destination != VoiceDestination.RunPlanner) runPlanner = null
        when (destination) {
            VoiceDestination.Home -> tab = HomeTab.Weather
            VoiceDestination.Plans -> tab = HomeTab.Plans
            VoiceDestination.Chat -> tab = HomeTab.HavaCan
            VoiceDestination.Wardrobe -> tab = HomeTab.Wardrobe
            VoiceDestination.Profile -> tab = HomeTab.Profile
            VoiceDestination.RunPlanner -> runPlanner = RunPlannerRequest(System.currentTimeMillis())
        }
    }
    LaunchedEffect(voice) { voice.navigation.collect { go(it) } }
    VoiceSystemHooks(voice)
    val tapMicrophone = rememberMicrophoneAccess { voice.tapMicrophone() }
    val startGuide = rememberMicrophoneAccess { voice.startSession(continuous = true) }
    val enableGuide = rememberMicrophoneAccess { voice.setGuideEnabled(true) }
    // Profil's "Başlat": turns the guide on (no welcome speech, the session talks at once) and starts it
    val enableAndStartGuide = rememberMicrophoneAccess {
        voice.setGuideEnabled(true, welcome = false)
        voice.startSession(continuous = true)
    }
    val profilePrefs by AppGraph.get(LocalContext.current).profile.prefs.collectAsStateWithLifecycle()

    val openSalla = {
        sallaSignal++
        outfitRequest++
        tab = HomeTab.Wardrobe
    }
    ShakeEffect(enabled = tab == HomeTab.Weather && !showCityPicker && sheet == Sheet.None && runPlanner == null, onShake = openSalla)

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (tab) {
            HomeTab.Weather -> HomeScreen(
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
            HomeTab.HavaCan -> ChatScreen(
                state = voiceState,
                messages = messages,
                clock = clock,
                userName = profilePrefs.name,
                actions = ChatActions(
                    onSend = voice::submitTyped,
                    onMic = tapMicrophone,
                    onHeadset = {
                        when {
                            !voiceState.settings.guideEnabled -> tab = HomeTab.Profile
                            voiceState.sessionActive -> voice.stopSession()
                            else -> startGuide()
                        }
                    },
                    onBell = { sheet = Sheet.Notifications },
                    onSpeak = voice::speakMessage,
                    onOpen = go,
                    onDismissNotice = voice::dismissNotice,
                ),
            )
            HomeTab.Profile -> ProfileRoute(
                hooks = ProfileHooks(
                    cityName = state.city.name,
                    voice = voiceState,
                    onPickCity = { cityPickTarget = null; showCityPicker = true },
                    onStartVoice = if (voiceState.settings.guideEnabled) startGuide else enableAndStartGuide,
                    onStopVoice = { voice.stopSession() },
                    onGuideChange = { on -> if (on) enableGuide() else voice.setGuideEnabled(false) },
                    onSpeakTypedChange = voice::setSpeakTypedReplies,
                    onClearChat = voice::clearMessages,
                ),
                clock = clock,
            )
        }

        // Plan sub-screens (editor, Plan B) hide the bar; every other tab always shows it
        if ((tab != HomeTab.Plans && tab != HomeTab.Wardrobe) || showBottomBar) HavaCanBottomNavigation(
            selected = tab,
            havaCanPulse = havaCanPulse,
            onSelect = { selected ->
                if (selected == HomeTab.HavaCan) havaCanPulse++
                tab = selected
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
                onBack = { showCityPicker = false },
                clock = clock,
                reduceMotion = reduceMotion,
                onCityClick = { city ->
                    Log.d(TAG, "Tıklandı: ${city.name}")
                    val target = cityPickTarget
                    if (target != null) target(city) else viewModel.onCityChosen(city)
                    cityPickTarget = null
                    showCityPicker = false
                },
            )
        }

        // The chat has its own microphone; everywhere else the voice guide floats above the bottom bar
        if (tab != HomeTab.HavaCan && (voiceState.settings.guideEnabled || voiceState.sessionActive)) {
            VoiceGuideBar(
                state = voiceState,
                onMic = tapMicrophone,
                onStop = { voice.stopSession() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 92.dp),
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

private val RunRequestSaver = androidx.compose.runtime.saveable.Saver<RunPlannerRequest?, String>(
    save = { r -> r?.let { "${it.token}|${it.planId.orEmpty()}|${it.start.orEmpty()}" } ?: "" },
    restore = { raw ->
        raw.split('|').takeIf { it.size == 3 }?.let { (token, id, start) ->
            RunPlannerRequest(token.toLong(), id.ifEmpty { null }, start.ifEmpty { null })
        }
    },
)

/**
 * Ties the voice guide to the system: stops it the moment the app leaves the screen, tells it
 * whether TalkBack is on, and hands TalkBack the answers to read when it is.
 */
@Composable
private fun VoiceSystemHooks(voice: VoiceGuideController) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> voice.onAppHidden()
                Lifecycle.Event.ON_START -> voice.refreshAvailability()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val context = LocalContext.current
    DisposableEffect(context) {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        voice.setScreenReaderActive(manager.isTouchExplorationEnabled)
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { voice.setScreenReaderActive(it) }
        manager.addTouchExplorationStateChangeListener(listener)
        onDispose { manager.removeTouchExplorationStateChangeListener(listener) }
    }

    val view = LocalView.current
    LaunchedEffect(voice, view) {
        @Suppress("DEPRECATION") // still the way to hand a sentence to TalkBack from outside any node
        voice.announcements.collect { view.announceForAccessibility(it) }
    }
}
