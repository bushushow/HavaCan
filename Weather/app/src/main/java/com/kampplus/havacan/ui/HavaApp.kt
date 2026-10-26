package com.kampplus.havacan.ui

import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityManager
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kampplus.havacan.R
import com.kampplus.havacan.di.AppContainer
import com.kampplus.havacan.domain.running.RunningAdvice
import com.kampplus.havacan.domain.voice.VoiceDestination
import com.kampplus.havacan.model.City
import com.kampplus.havacan.ui.chat.ChatActions
import com.kampplus.havacan.ui.chat.ChatScreen
import com.kampplus.havacan.ui.city.CityRoute
import com.kampplus.havacan.ui.common.LocalSky
import com.kampplus.havacan.ui.common.skyColors
import com.kampplus.havacan.ui.home.HomeActions
import com.kampplus.havacan.ui.home.HomeScreen
import com.kampplus.havacan.ui.home.HomeViewModel
import com.kampplus.havacan.ui.home.ShakeEffect
import com.kampplus.havacan.ui.home.character.HavaCanMotion
import com.kampplus.havacan.ui.home.components.HavaCanBottomNavigation
import com.kampplus.havacan.ui.home.components.HomeTab
import com.kampplus.havacan.ui.home.rememberReduceMotion
import com.kampplus.havacan.ui.home.scene.rememberSceneClock
import com.kampplus.havacan.ui.navigation.HavaRoutes
import com.kampplus.havacan.ui.navigation.route
import com.kampplus.havacan.ui.navigation.tabOf
import com.kampplus.havacan.ui.plans.PlansRoute
import com.kampplus.havacan.ui.profile.ProfileHooks
import com.kampplus.havacan.ui.profile.ProfileRoute
import com.kampplus.havacan.ui.run.RunPlannerRoute
import com.kampplus.havacan.ui.run.RunSaved
import com.kampplus.havacan.ui.theme.DarkPalette
import com.kampplus.havacan.ui.theme.Hava
import com.kampplus.havacan.ui.theme.LightPalette
import com.kampplus.havacan.ui.theme.LocalPalette
import com.kampplus.havacan.ui.theme.Navy
import com.kampplus.havacan.ui.theme.NavySoft
import com.kampplus.havacan.ui.voice.VoiceGuideBar
import com.kampplus.havacan.ui.voice.VoiceGuideController
import com.kampplus.havacan.ui.voice.rememberMicrophoneAccess
import com.kampplus.havacan.ui.wardrobe.WardrobeRoute

private const val TAG = "Hava"

private enum class Sheet { None, Notifications }

/** App shell: bottom navigation, the home screen, the Kamp+ city search and small info sheets. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HavaApp(
    openPlanId: String? = null,
    onOpenPlanHandled: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
    voice: VoiceGuideController = viewModel(factory = VoiceGuideController.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val voiceState by voice.state.collectAsStateWithLifecycle()
    val messages by voice.messages.collectAsStateWithLifecycle()
    val reduceMotion = rememberReduceMotion()
    val clock = rememberSceneClock(paused = reduceMotion)
    val motion = remember { HavaCanMotion() }

    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    // The tab the bottom bar highlights; full-screen routes keep the tab they were opened from
    var tab by rememberSaveable { mutableStateOf(HomeTab.Weather) }
    LaunchedEffect(route) { tabOf(route)?.let { tab = it } }
    val onTab = tabOf(route) != null

    var sheet by remember { mutableStateOf(Sheet.None) }
    // Told to Planlarım after a run is saved, for the confirmation and the calendar offer
    var runNotice by remember { mutableStateOf<RunSaved?>(null) }
    var sallaSignal by remember { mutableIntStateOf(0) }
    var havaCanPulse by remember { mutableIntStateOf(0) }
    var showBottomBar by remember { mutableStateOf(true) }
    // Bumped by the home Salla Kombin card or a shake on the home screen: opens today's outfit
    var outfitRequest by rememberSaveable { mutableIntStateOf(0) }
    // Who gets the city the picker returns: the home screen (null), the plan editor, Koşu Planla…
    var cityPickTarget by remember { mutableStateOf<((City) -> Unit)?>(null) }

    // Tabs keep their state when you leave and come back, like the bottom bar of any Android app
    val openTab: (HomeTab) -> Unit = { target ->
        nav.navigate(target.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val openCityPicker: ((City) -> Unit)?.() -> Unit = {
        cityPickTarget = this
        nav.navigate(HavaRoutes.CITY) { launchSingleTop = true }
    }

    // A notification about a plan opens the Planlar tab
    LaunchedEffect(openPlanId) { if (openPlanId != null) openTab(HomeTab.Plans) }

    // Screens opened by voice (or by a card in the chat) use the same routes as taps
    val go: (VoiceDestination) -> Unit = { destination ->
        sheet = Sheet.None
        when (destination) {
            VoiceDestination.Home -> openTab(HomeTab.Weather)
            VoiceDestination.Plans -> openTab(HomeTab.Plans)
            VoiceDestination.Chat -> openTab(HomeTab.HavaCan)
            VoiceDestination.Wardrobe -> openTab(HomeTab.Wardrobe)
            VoiceDestination.Profile -> openTab(HomeTab.Profile)
            VoiceDestination.RunPlanner -> nav.navigate(HavaRoutes.run())
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
    val profilePrefs by AppContainer.get(LocalContext.current).profile.prefs.collectAsStateWithLifecycle()

    val openSalla = {
        sallaSignal++
        outfitRequest++
        openTab(HomeTab.Wardrobe)
    }
    ShakeEffect(enabled = route == HavaRoutes.WEATHER && sheet == Sheet.None, onShake = openSalla)

    // Every screen's header is painted with the home city's sky right now
    val now = state.forecast?.current
    val sky = skyColors(now?.condition, now?.isDay ?: true, now?.time?.substringAfter('T')?.take(2)?.toIntOrNull())
    // Dark mode follows the phone's setting on every screen
    val palette = if (isSystemInDarkTheme()) DarkPalette else LightPalette
    CompositionLocalProvider(LocalSky provides sky, LocalPalette provides palette) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            NavHost(
                navController = nav,
                startDestination = HavaRoutes.WEATHER,
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(160)) }
            ) {
                composable(HavaRoutes.WEATHER) {
                    HomeScreen(
                        state = state,
                        motion = motion,
                        clock = clock,
                        reduceMotion = reduceMotion,
                        sallaSignal = sallaSignal,
                        actions = HomeActions(
                            onCityClick = { null.openCityPicker() },
                            onBellClick = { sheet = Sheet.Notifications },
                            onPlanRun = { advice ->
                                val start = (advice as? RunningAdvice.BestWindow)?.window?.hours?.firstOrNull()?.hour?.time
                                nav.navigate(HavaRoutes.run(start = start))
                            },
                            onSalla = openSalla
                        ),
                        onToggleRunner = viewModel::toggleRunnerMode,
                        onHourSelected = { viewModel.onHourSelected(it.time) },
                        onRetry = viewModel::retry
                    )
                }
                composable(HavaRoutes.PLANS) {
                    PlansRoute(
                        clock = clock,
                        openPlanId = openPlanId,
                        onOpenPlanHandled = onOpenPlanHandled,
                        onCityClick = { null.openCityPicker() },
                        onBellClick = { sheet = Sheet.Notifications },
                        onPickPlanCity = { onPicked -> onPicked.openCityPicker() },
                        onShowBottomBar = { showBottomBar = it },
                        onOpenRunPlan = { id -> nav.navigate(HavaRoutes.run(planId = id)) },
                        runNotice = runNotice,
                        onRunNoticeShown = { runNotice = null }
                    )
                }
                composable(HavaRoutes.WARDROBE) {
                    WardrobeRoute(
                        clock = clock,
                        outfitRequest = outfitRequest,
                        onShowBottomBar = { showBottomBar = it }
                    )
                }
                composable(HavaRoutes.CHAT) {
                    ChatScreen(
                        state = voiceState,
                        messages = messages,
                        clock = clock,
                        userName = profilePrefs.name,
                        actions = ChatActions(
                            onSend = voice::submitTyped,
                            onMic = tapMicrophone,
                            onHeadset = {
                                when {
                                    !voiceState.settings.guideEnabled -> openTab(HomeTab.Profile)
                                    voiceState.sessionActive -> voice.stopSession()
                                    else -> startGuide()
                                }
                            },
                            onBell = { sheet = Sheet.Notifications },
                            onSpeak = voice::speakMessage,
                            onOpen = go,
                            onDismissNotice = voice::dismissNotice
                        )
                    )
                }
                composable(HavaRoutes.PROFILE) {
                    ProfileRoute(
                        hooks = ProfileHooks(
                            cityName = state.city.name,
                            voice = voiceState,
                            onPickCity = { null.openCityPicker() },
                            onStartVoice = if (voiceState.settings.guideEnabled) startGuide else enableAndStartGuide,
                            onStopVoice = { voice.stopSession() },
                            onGuideChange = { on -> if (on) enableGuide() else voice.setGuideEnabled(false) },
                            onSpeakTypedChange = voice::setSpeakTypedReplies,
                            onClearChat = voice::clearMessages
                        ),
                        clock = clock
                    )
                }
                composable(
                    HavaRoutes.RUN,
                    arguments = listOf(
                        navArgument(HavaRoutes.ARG_PLAN_ID) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument(HavaRoutes.ARG_START) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    ),
                    enterTransition = { slideInHorizontally(tween(380)) { it / 2 } + fadeIn(tween(250)) },
                    popExitTransition = { slideOutHorizontally(tween(280)) { it / 2 } + fadeOut(tween(200)) }
                ) {
                    RunPlannerRoute(
                        clock = clock,
                        reduceMotion = reduceMotion,
                        onPickCity = { onPicked -> onPicked.openCityPicker() },
                        onClose = { nav.popBackStack() },
                        onSaved = { saved ->
                            runNotice = saved
                            nav.popBackStack()
                            openTab(HomeTab.Plans)
                        },
                        onDeleted = {
                            nav.popBackStack()
                            openTab(HomeTab.Plans)
                        }
                    )
                }
                composable(
                    HavaRoutes.CITY,
                    enterTransition = { slideInVertically(tween(350)) { it / 3 } + fadeIn(tween(250)) },
                    popExitTransition = { slideOutVertically(tween(250)) { it / 3 } + fadeOut(tween(200)) }
                ) {
                    CityRoute(
                        onBack = { nav.popBackStack() },
                        clock = clock,
                        reduceMotion = reduceMotion,
                        onCityClick = { city ->
                            Log.d(TAG, "Tıklandı: ${city.name}")
                            val target = cityPickTarget
                            if (target != null) target(city) else viewModel.onCityChosen(city)
                            cityPickTarget = null
                            nav.popBackStack()
                        }
                    )
                }
            }

            // Plan and wardrobe sub-screens (editor, Plan B, outfit) hide the bar; full-screen routes too
            if (onTab && ((tab != HomeTab.Plans && tab != HomeTab.Wardrobe) || showBottomBar)) {
                HavaCanBottomNavigation(
                    selected = tab,
                    havaCanPulse = havaCanPulse,
                    onSelect = { selected ->
                        if (selected == HomeTab.HavaCan) havaCanPulse++
                        openTab(selected)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // The chat has its own microphone; everywhere else the voice guide floats above the bottom bar
            if (route != HavaRoutes.CHAT && (voiceState.settings.guideEnabled || voiceState.sessionActive)) {
                VoiceGuideBar(
                    state = voiceState,
                    onMic = tapMicrophone,
                    onStop = { voice.stopSession() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 92.dp)
                )
            }
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (sheet) {
                    Sheet.Notifications -> InfoContent(
                        stringResource(R.string.cd_notifications),
                        stringResource(R.string.notifications_empty)
                    )
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
    Text(title, style = MaterialTheme.typography.headlineSmall, color = Hava.ink)
    Text(body, style = MaterialTheme.typography.bodyLarge, color = Hava.inkSoft)
}

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
