package com.example.weather.ui.home

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.weather.R
import com.example.weather.domain.running.RunningAdvice
import com.example.weather.model.HourlyWeather
import com.example.weather.ui.home.character.HavaCanEvent
import com.example.weather.ui.home.character.HavaCanMotion
import com.example.weather.ui.home.character.HavaCanStateMachine
import com.example.weather.ui.home.components.HomeTopBar
import com.example.weather.ui.home.components.HourlyStrip
import com.example.weather.ui.home.components.RunnerHourlyStrip
import com.example.weather.ui.home.components.RunnerInsightCard
import com.example.weather.ui.home.components.SallaKombinCard
import com.example.weather.ui.home.components.WeatherHero
import com.example.weather.ui.home.components.WeatherInsightCard
import com.example.weather.ui.home.scene.SceneLook
import com.example.weather.ui.home.scene.WeatherScene
import com.example.weather.ui.home.scene.segment
import kotlinx.coroutines.delay

/** Callbacks the home screen sends up; the app decides what each one opens. */
data class HomeActions(
    val onCityClick: () -> Unit,
    val onBellClick: () -> Unit,
    val onPlanRun: (RunningAdvice) -> Unit,
    val onSalla: () -> Unit,
)

/**
 * The HavaCan home screen. Normal and Koşucu Modu are one screen in two states: a single
 * [updateTransition] drives the gear, the track and the running gait on staggered curves, while
 * the strip and the smart card cross-fade with [AnimatedContent], so the whole world changes as
 * one ~1 s motion and reverses cleanly if the user toggles quickly.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    motion: HavaCanMotion,
    clock: () -> Float,
    reduceMotion: Boolean,
    sallaSignal: Int,
    actions: HomeActions,
    onToggleRunner: () -> Unit,
    onHourSelected: (HourlyWeather) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val runner = state.mode == HomeMode.Runner

    // --- Intro: content first, then the island rises and HavaCan drops in, cards settle last
    var introPlayed by rememberSaveable { mutableStateOf(false) }
    val intro = remember { Animatable(if (introPlayed || reduceMotion) 1f else 0f) }
    var showGreeting by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!introPlayed && !reduceMotion) {
            intro.animateTo(1f, tween(1100, easing = LinearEasing))
            showGreeting = true
            delay(2200)
            showGreeting = false
        }
        introPlayed = true
    }

    // --- Normal ↔ Runner, one coordinated transition
    val transition = updateTransition(state.mode, label = "mode")
    val gear by transition.animateFloat(
        transitionSpec = {
            when {
                reduceMotion -> snap()
                targetState == HomeMode.Runner -> tween(450, delayMillis = 120, easing = FastOutSlowInEasing)
                else -> tween(300, easing = FastOutSlowInEasing)
            }
        },
        label = "gear",
    ) { if (it == HomeMode.Runner) 1f else 0f }
    val track by transition.animateFloat(
        transitionSpec = {
            when {
                reduceMotion -> snap()
                targetState == HomeMode.Runner -> tween(600, delayMillis = 250, easing = FastOutSlowInEasing)
                else -> tween(450, easing = FastOutSlowInEasing)
            }
        },
        label = "track",
    ) { if (it == HomeMode.Runner) 1f else 0f }
    val gait by transition.animateFloat(
        transitionSpec = {
            when {
                reduceMotion -> snap()
                targetState == HomeMode.Runner -> tween(300, delayMillis = 700)
                else -> tween(200)
            }
        },
        label = "gait",
    ) { if (it == HomeMode.Runner) 1f else 0f }

    // --- Character reactions
    val currentRunner by rememberUpdatedState(runner)
    LaunchedEffect(runner) {
        val resting = HavaCanStateMachine.resting(runner)
        if (motion.state != resting) motion.send(HavaCanEvent.ModeChanged(runner), runner, clock())
    }
    LaunchedEffect(motion.stateStamp) {
        HavaCanStateMachine.reactionMillis(motion.state)?.let {
            delay(it)
            motion.send(HavaCanEvent.ReactionFinished, currentRunner, clock())
        }
    }
    val hour = state.selectedHour
    val condition = if (state.isNowSelected) state.forecast?.current?.condition else hour?.condition
    val isDay = if (state.isNowSelected) state.forecast?.current?.isDay ?: true else hour?.isDay ?: true
    var lastCondition by remember { mutableStateOf(condition) }
    LaunchedEffect(condition) {
        if (lastCondition != null && condition != lastCondition) {
            motion.send(HavaCanEvent.WeatherChanged, currentRunner, clock())
        }
        lastCondition = condition
    }

    // --- Sky look, each layer eased separately so sun→rain fades instead of cutting
    val look = animatedLook(SceneLook.of(condition, isDay), reduceMotion)

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding(),
    ) {
        HomeTopBar(
            cityName = state.city.name,
            runnerActive = runner,
            onCityClick = actions.onCityClick,
            onRunnerToggle = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggleRunner()
            },
            onBellClick = actions.onBellClick,
            modifier = Modifier.stagger(intro.value, 0f, 0.3f),
        )

        Box(Modifier.fillMaxWidth().height(330.dp)) {
            WeatherScene(
                look = look,
                motion = motion,
                gear = gear,
                track = track,
                gait = gait,
                intro = intro.value,
                still = reduceMotion,
                showGreeting = showGreeting,
                greeting = stringResource(R.string.greeting),
                onCharacterTap = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    motion.send(HavaCanEvent.Tapped, runner, clock())
                },
                clock = clock,
                modifier = Modifier.fillMaxSize(),
            )
            WeatherHero(
                state = state,
                onRetry = onRetry,
                modifier = Modifier
                    .padding(start = 20.dp, top = 4.dp)
                    .fillMaxWidth(0.56f)
                    .stagger(intro.value, 0f, 0.35f),
            )
        }

        Column(
            Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val forecast = state.forecast
            if (forecast != null) {
                AnimatedContent(
                    targetState = state.mode,
                    transitionSpec = { modeSwap(reduceMotion) },
                    label = "strip",
                    modifier = Modifier.stagger(intro.value, 0.45f, 0.8f),
                ) { mode ->
                    val advice = state.runningAdvice
                    if (mode == HomeMode.Runner && advice != null) {
                        RunnerHourlyStrip(advice, state.selectedTime, onHourSelected)
                    } else {
                        HourlyStrip(state.hourly, state.selectedTime, onHourSelected)
                    }
                }
                AnimatedContent(
                    targetState = state.mode,
                    transitionSpec = { modeSwap(reduceMotion, delay = 80) },
                    label = "card",
                    modifier = Modifier.stagger(intro.value, 0.55f, 0.9f),
                ) { mode ->
                    val advice = state.runningAdvice
                    val insight = state.insight
                    if (mode == HomeMode.Runner && advice != null) {
                        RunnerInsightCard(
                            advice = advice,
                            clock = clock,
                            onPlan = { actions.onPlanRun(advice) },
                            onInspect = { advice.scores.firstOrNull()?.let { onHourSelected(it.hour) } },
                        )
                    } else if (insight != null) {
                        WeatherInsightCard(insight, clock, onShowHour = onHourSelected)
                    }
                }
            }
            SallaKombinCard(
                runner = runner,
                wiggleSignal = sallaSignal,
                clock = clock,
                onClick = actions.onSalla,
                modifier = Modifier.stagger(intro.value, 0.62f, 1f),
            )
            Spacer(Modifier.height(120.dp))
        }
    }
}

private fun AnimatedContentTransitionScope<HomeMode>.modeSwap(reduceMotion: Boolean, delay: Int = 0) =
    if (reduceMotion) {
        fadeIn(snap()) togetherWith fadeOut(snap())
    } else {
        (fadeIn(tween(320, delayMillis = 180 + delay)) +
            slideInVertically(tween(420, delayMillis = 180 + delay, easing = FastOutSlowInEasing)) { it / 8 } +
            scaleIn(tween(420, delayMillis = 180 + delay), initialScale = 0.96f)) togetherWith
            (fadeOut(tween(160)) + scaleOut(tween(200), targetScale = 0.97f))
    }.using(SizeTransform(clip = false))

/** Fades and lifts a block in during its slice of the intro. */
private fun Modifier.stagger(intro: Float, start: Float, end: Float) = graphicsLayer {
    val p = segment(intro, start, end)
    alpha = p
    translationY = (1 - p) * 24.dp.toPx()
}

@Composable
private fun animatedLook(target: SceneLook, reduceMotion: Boolean): SceneLook {
    val spec = if (reduceMotion) snap<Float>() else tween(900, easing = FastOutSlowInEasing)
    val sun by androidx.compose.animation.core.animateFloatAsState(target.sun, spec, label = "sun")
    val night by androidx.compose.animation.core.animateFloatAsState(target.night, spec, label = "night")
    val clouds by androidx.compose.animation.core.animateFloatAsState(target.clouds, spec, label = "clouds")
    val overcast by androidx.compose.animation.core.animateFloatAsState(target.overcast, spec, label = "overcast")
    val rain by androidx.compose.animation.core.animateFloatAsState(target.rain, spec, label = "rain")
    val snow by androidx.compose.animation.core.animateFloatAsState(target.snow, spec, label = "snow")
    return SceneLook(sun, night, clouds, overcast, rain, snow)
}

/** True when the user turned animations off (Settings › Accessibility › Remove animations). */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Listens to the accelerometer only while the screen is resumed and [enabled]. */
@Composable
fun ShakeEffect(enabled: Boolean, onShake: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val callback by rememberUpdatedState(onShake)
    DisposableEffect(enabled, lifecycle) {
        if (!enabled) return@DisposableEffect onDispose {}
        val sensors = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val detector = ShakeDetector()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (detector.onReading(event.values[0], event.values[1], event.values[2], event.timestamp / 1_000_000)) {
                    callback()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME ->
                    accelerometer?.let { sensors.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
                Lifecycle.Event.ON_PAUSE -> sensors.unregisterListener(listener)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            sensors.unregisterListener(listener)
        }
    }
}
