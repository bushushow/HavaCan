package com.kampplus.havacan.ui.home

import com.kampplus.havacan.domain.insight.WeatherInsight
import com.kampplus.havacan.domain.running.RunningAdvice
import com.kampplus.havacan.model.TemperatureUnit
import com.kampplus.havacan.model.deg
import kotlin.math.roundToInt

/**
 * What HavaCan says when tapped, one tip per tap, built only from the forecast on screen. The first
 * tip is the most useful one (rain beats everything), so it's also what HavaCan says on its own
 * when the screen opens with rain on the way.
 */
internal fun havaCanTips(state: HomeUiState, unit: TemperatureUnit): List<String> {
    val hour = state.selectedHour ?: return listOf("Hava bilgisi gelince sana ipuçları vereceğim!")
    val tips = mutableListOf<String>()

    if (state.mode == HomeMode.Runner) {
        when (val advice = state.runningAdvice) {
            is RunningAdvice.BestWindow -> tips += "Koşu için en iyi aralık ${advice.window.rangeLabel}. Hazır mısın?"
            is RunningAdvice.NoGoodTime -> tips += advice.mainBlocker?.let { "Bugün koşu zor: ${it.message.lowercase()}" }
                ?: "Bugün koşu için iyi bir saat görünmüyor."
            null -> Unit
        }
    }
    when (val insight = state.insight) {
        is WeatherInsight.RainNow -> tips += "Şu an yağmur var, şemsiyeni al! ☔"
        is WeatherInsight.RainComing ->
            tips += "Saat ${insight.hour.hourLabel} gibi yağmur bekleniyor" +
                (insight.hour.precipitationProbability?.let { " (%$it)." } ?: ".")
        is WeatherInsight.Dry ->
            tips +=
                "Yağmur yok! En serin saat ${insight.coolest.hourLabel}, ${insight.coolest.temperature.deg(unit)}°."
        null -> Unit
    }
    val feels = hour.apparentTemperature
    when {
        feels <= 5 -> tips += "Hissedilen ${feels.deg(unit)}°. Bere ve eldiven iyi olur 🧤"
        feels >= 30 -> tips += "Hissedilen ${feels.deg(unit)}°. Bol su iç, gölgede kal 💧"
        hour.temperature - feels >= 3 -> tips += "Rüzgar yüzünden ${feels.deg(unit)}° gibi hissettiriyor; üstüne bir şey al."
    }
    hour.uvIndex?.takeIf { it >= 6 }?.let { tips += "UV yüksek (${it.roundToInt()}). Güneş kremini unutma 😎" }
    hour.windSpeed?.takeIf { it >= 30 }?.let { tips += "Rüzgar sert: ${it.roundToInt()} km/sa. Şemsiye yerine mont!" }
    hour.humidity?.takeIf { it >= 85 && feels >= 24 }?.let { tips += "Nem %$it, hava bunaltıcı olabilir." }

    tips += "Beni sağa sola çevirebilirsin, telefonu sallarsan kombin seçerim!"
    return tips
}

/** Rain on the way is worth saying unprompted when the screen opens. */
internal fun HomeUiState.hasRainNews(): Boolean = insight is WeatherInsight.RainNow || insight is WeatherInsight.RainComing
