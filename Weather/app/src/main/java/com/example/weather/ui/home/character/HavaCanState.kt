package com.example.weather.ui.home.character

/** What HavaCan is doing. Rendering reads this; it never decides it. */
enum class HavaCanState {
    Idle,
    Happy,
    WeatherChange,
    RunnerTransition,
    Running,
    ChatOpening,
}

sealed interface HavaCanEvent {
    data object Tapped : HavaCanEvent
    data object WeatherChanged : HavaCanEvent
    data class ModeChanged(val runner: Boolean) : HavaCanEvent
    data object ChatRequested : HavaCanEvent
    /** A one-off reaction (happy, weather, transition, chat) has played out. */
    data object ReactionFinished : HavaCanEvent
}

/** Pure state machine for HavaCan's behaviour; unit tested, no Compose. */
object HavaCanStateMachine {

    fun reduce(state: HavaCanState, event: HavaCanEvent, runner: Boolean): HavaCanState = when (event) {
        HavaCanEvent.Tapped -> HavaCanState.Happy
        // Don't interrupt the mode switch for a weather tweak
        HavaCanEvent.WeatherChanged ->
            if (state == HavaCanState.RunnerTransition || state == HavaCanState.ChatOpening) state else HavaCanState.WeatherChange
        is HavaCanEvent.ModeChanged -> if (event.runner) HavaCanState.RunnerTransition else HavaCanState.Idle
        HavaCanEvent.ChatRequested -> HavaCanState.ChatOpening
        HavaCanEvent.ReactionFinished -> resting(runner)
    }

    fun resting(runner: Boolean) = if (runner) HavaCanState.Running else HavaCanState.Idle

    /** How long a one-off reaction lasts before [HavaCanEvent.ReactionFinished]; null for looping states. */
    fun reactionMillis(state: HavaCanState): Long? = when (state) {
        HavaCanState.Happy -> 700
        HavaCanState.WeatherChange -> 600
        HavaCanState.RunnerTransition -> 900
        HavaCanState.ChatOpening -> 700
        HavaCanState.Idle, HavaCanState.Running -> null
    }
}
